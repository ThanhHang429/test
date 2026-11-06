package com.eventapp.organizer.classification

import com.eventapp.catalog.entity.EventCategory
import com.eventapp.catalog.entity.EventCategoryMap
import com.eventapp.catalog.entity.EventCategoryMapId
import com.eventapp.catalog.entity.EventTag
import com.eventapp.catalog.entity.EventTagMap
import com.eventapp.catalog.entity.EventTagMapId
import com.eventapp.catalog.repository.EventCategoryMapRepository
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.catalog.repository.EventTagMapRepository
import com.eventapp.catalog.repository.EventTagRepository
import com.eventapp.common.audit.AuditLogWriter
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.moderation.ContentFilterService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Locale

/**
 * FR-O09 — gắn danh mục và nhãn phụ cho sự kiện.
 *
 * Ràng buộc:
 *  - O09-R01: chỉ chọn danh mục ACTIVE; Organizer không tạo danh mục toàn nền tảng.
 *  - Danh mục chính đúng 1, danh mục phụ tối đa 2.
 *  - Nhãn phụ tối đa 5, mỗi nhãn 2–30 ký tự, chuẩn hoá và lọc nội dung.
 *  - O09-R03: đổi danh mục chính của sự kiện đã duyệt → trả cờ `requiresReReview`
 *    (backend của Cảnh không tự đổi trạng thái duyệt của sự kiện).
 */
@Service
class ClassificationService(
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val eventRepository: EventRepository,
    private val categoryRepository: EventCategoryRepository,
    private val categoryMapRepository: EventCategoryMapRepository,
    private val tagRepository: EventTagRepository,
    private val tagMapRepository: EventTagMapRepository,
    private val contentFilter: ContentFilterService,
    private val auditLogWriter: AuditLogWriter
) {

    @Transactional
    fun update(userId: Long, eventId: Long, request: ClassificationRequest): ClassificationDto {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.organizerProfileId != profile.organizerProfileId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Sự kiện không thuộc quyền quản lý của bạn")
        }
        if (event.lifecycleStatus in LOCKED_LIFECYCLES) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Sự kiện ở trạng thái ${event.lifecycleStatus.name}, không thể đổi phân loại"
            )
        }

        val primary = requireActiveCategory(request.primaryCategoryId!!)
        val secondary = request.secondaryCategoryIds
            .filter { it != primary.categoryId }
            .distinct()
            .map { requireActiveCategory(it) }

        val tags = normalizeTags(request.tags)
        rejectBlockedTags(tags)

        val oldPrimary = categoryMapRepository.findByIdEventId(eventId)
            .map { it.id.categoryId }
            .firstOrNull()

        // Ghi đè toàn bộ phân loại trong cùng một transaction.
        categoryMapRepository.deleteByIdEventId(eventId)
        val assigned = (listOf(primary) + secondary)
        categoryMapRepository.saveAll(assigned.map { EventCategoryMap(EventCategoryMapId(eventId, it.categoryId!!)) })

        tagMapRepository.deleteByIdEventId(eventId)
        tagMapRepository.saveAll(tags.map { tag ->
            EventTagMap(EventTagMapId(eventId, resolveTag(tag, userId).tagId!!), assignedByUserId = userId)
        })

        val requiresReReview = oldPrimary != null &&
            oldPrimary != primary.categoryId &&
            event.reviewStatus == EventReviewStatus.APPROVED

        auditLogWriter.write(
            actorUserId = userId,
            actionCode = "EVENT_CLASSIFICATION_UPDATED",
            entityType = "event",
            entityId = eventId,
            oldValues = mapOf("primaryCategoryId" to oldPrimary),
            newValues = mapOf(
                "primaryCategoryId" to primary.categoryId,
                "secondaryCategoryIds" to secondary.map { it.categoryId },
                "tags" to tags,
                "requiresReReview" to requiresReReview
            ),
            resultCode = "OK"
        )

        return toDto(eventId, requiresReReview)
    }

    @Transactional(readOnly = true)
    fun get(userId: Long, eventId: Long): ClassificationDto {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.organizerProfileId != profile.organizerProfileId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Sự kiện không thuộc quyền quản lý của bạn")
        }
        return toDto(eventId, requiresReReview = false)
    }

    @Transactional(readOnly = true)
    fun suggestTags(query: String?, limit: Int): List<TagSuggestionDto> {
        val keyword = query?.trim().orEmpty()
        val tags = if (keyword.isEmpty()) {
            tagRepository.findByIsActiveTrueOrderByTagNameAsc()
        } else {
            tagRepository.findByIsActiveTrueAndTagNameContainingIgnoreCaseOrderByTagNameAsc(keyword)
        }
        if (tags.isEmpty()) return emptyList()
        val usage = tags.map { it.tagId!! to tagMapRepository.countByTagId(it.tagId!!) }.toMap()
        return tags
            .sortedWith(compareByDescending<EventTag> { usage[it.tagId] ?: 0L }.thenBy { it.tagName })
            .take(limit.coerceIn(1, 20))
            .map {
                TagSuggestionDto(
                    id = it.tagId!!,
                    name = it.tagName,
                    slug = it.slug,
                    usageCount = usage[it.tagId] ?: 0L
                )
            }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun requireActiveCategory(categoryId: Long): EventCategory {
        val category = categoryRepository.findById(categoryId).orElse(null)
            ?: throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
        if (!category.isActive) throw ConflictException(ErrorCode.CATEGORY_INACTIVE)
        return category
    }

    /** Chuẩn hoá theo SRS: chữ thường, bỏ khoảng trắng thừa, không trùng, 2–30 ký tự. */
    private fun normalizeTags(raw: List<String>): List<String> {
        val normalized = raw.map { it.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }
        normalized.forEach { tag ->
            if (tag.length < 2 || tag.length > 30) {
                throw validationError("tags", "Nhãn \"$tag\" phải từ 2 đến 30 ký tự")
            }
        }
        val distinct = normalized.distinct()
        if (distinct.size != normalized.size) {
            throw validationError("tags", "Danh sách nhãn bị trùng sau khi chuẩn hoá")
        }
        return distinct
    }

    private fun rejectBlockedTags(tags: List<String>) {
        tags.forEach { tag ->
            val result = contentFilter.check(tag)
            if (!result.passed) {
                throw validationError("tags", "Nhãn \"$tag\" bị từ chối: ${result.reason}")
            }
        }
    }

    private fun resolveTag(name: String, userId: Long): EventTag {
        val slug = slugify(name)
        tagRepository.findBySlug(slug)?.let { return it }
        return tagRepository.save(
            EventTag(
                tagCode = slug.uppercase(Locale.ROOT).replace('-', '_').take(60),
                tagName = name,
                slug = slug,
                isActive = true,
                createdByUserId = userId
            )
        )
    }

    private fun toDto(eventId: Long, requiresReReview: Boolean): ClassificationDto {
        val categoryIds = categoryMapRepository.findByIdEventId(eventId).map { it.id.categoryId }
        val categories = categoryRepository.findAllById(categoryIds).associateBy { it.categoryId!! }
        val ordered = categoryIds.mapNotNull { categories[it] }
        val tags = tagMapRepository.findByIdEventId(eventId)
            .mapNotNull { tagRepository.findById(it.id.tagId).orElse(null)?.tagName }
            .sorted()

        return ClassificationDto(
            eventId = eventId,
            primaryCategory = ordered.firstOrNull()?.let { toBrief(it) },
            secondaryCategories = ordered.drop(1).map { toBrief(it) },
            tags = tags,
            requiresReReview = requiresReReview,
            updatedAt = Instant.now()
        )
    }

    private fun toBrief(category: EventCategory) = CategoryBriefDto(
        id = category.categoryId!!,
        code = category.categoryCode,
        name = category.categoryName,
        slug = category.slug,
        active = category.isActive
    )

    private fun slugify(value: String): String = java.text.Normalizer
        .normalize(value.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(160)
        .ifEmpty { "tag" }

    companion object {
        private val LOCKED_LIFECYCLES = setOf(
            EventLifecycleStatus.SUSPENDED,
            EventLifecycleStatus.CANCELLED,
            EventLifecycleStatus.COMPLETED
        )
    }
}
