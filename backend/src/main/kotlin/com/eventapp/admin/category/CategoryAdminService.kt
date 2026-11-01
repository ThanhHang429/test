package com.eventapp.admin.category

import com.eventapp.catalog.entity.EventCategory
import com.eventapp.catalog.repository.EventCategoryMapRepository
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.common.audit.AuditLogWriter
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.Locale

/**
 * FR-A09 — quản lý danh mục (chỉ Admin).
 *
 * Ràng buộc:
 *  - A09-R01/BR-020: KHÔNG xoá cứng; chỉ ẩn. Ẩn danh mục đang dùng vẫn giữ dữ liệu cũ.
 *  - A09-R03: mọi thay đổi ghi `audit_logs` (người thực hiện, thời gian, trước/sau, lý do).
 *  - A09-R07: tối đa 50 danh mục ACTIVE.
 *  - Chỉ hỗ trợ 2 cấp (SRS O09).
 */
@Service
class CategoryAdminService(
    private val categoryRepository: EventCategoryRepository,
    private val categoryMapRepository: EventCategoryMapRepository,
    private val auditLogWriter: AuditLogWriter
) {

    @Transactional(readOnly = true)
    fun tree(includeInactive: Boolean): List<AdminCategoryDto> {
        val all = categoryRepository.findAllByOrderBySortOrderAscCategoryIdAsc()
            .filter { includeInactive || it.isActive }
        val counts = eventCounts()
        val byParent = all.groupBy { it.parentCategoryId }
        return byParent[null].orEmpty().map { root ->
            toDto(root, byParent, counts)
        }
    }

    @Transactional
    fun create(actorUserId: Long, request: CategoryCreateRequest): AdminCategoryDto {
        val code = request.categoryCode.trim().uppercase(Locale.ROOT)
        categoryRepository.findByCategoryCode(code)?.let {
            throw ConflictException(ErrorCode.CONFLICT, "Mã danh mục đã tồn tại")
        }

        request.parentCategoryId?.let { parentId ->
            val parent = categoryRepository.findById(parentId).orElse(null)
                ?: throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
            if (parent.parentCategoryId != null) throw validationError(
                "parentCategoryId",
                "Danh mục chỉ hỗ trợ 2 cấp, không thể tạo cấp 3"
            )
        }

        if (activeCount() >= MAX_ACTIVE) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Đã đạt giới hạn $MAX_ACTIVE danh mục đang hoạt động, hãy ẩn bớt trước khi tạo mới"
            )
        }

        val name = request.categoryName.trim()
        val slug = request.slug?.trim()?.takeIf { it.isNotEmpty() } ?: slugify(name)
        categoryRepository.findBySlug(slug)?.let {
            throw ConflictException(ErrorCode.CONFLICT, "Slug danh mục đã tồn tại")
        }

        val saved = categoryRepository.save(
            EventCategory(
                parentCategoryId = request.parentCategoryId,
                categoryCode = code,
                categoryName = name,
                slug = slug,
                description = request.description?.trim(),
                isActive = true,
                sortOrder = request.sortOrder ?: nextSortOrder(request.parentCategoryId)
            )
        )

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = "CATEGORY_CREATED",
            entityType = "event_category",
            entityId = saved.categoryId,
            newValues = mapOf("code" to code, "name" to name, "parentId" to request.parentCategoryId),
            resultCode = "OK"
        )
        return toDto(saved, emptyMap(), eventCounts())
    }

    @Transactional
    fun update(actorUserId: Long, categoryId: Long, request: CategoryUpdateRequest): AdminCategoryDto {
        val category = categoryRepository.findById(categoryId).orElse(null)
            ?: throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)

        if (request.version != null && category.updatedAt != null &&
            request.version != category.updatedAt!!.toEpochMilli()
        ) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Danh mục đã được Admin khác cập nhật, vui lòng tải lại trước khi lưu"
            )
        }

        val before = mapOf(
            "name" to category.categoryName,
            "slug" to category.slug,
            "description" to category.description,
            "sortOrder" to category.sortOrder
        )

        request.categoryName?.trim()?.takeIf { it.isNotEmpty() }?.let { category.categoryName = it }
        request.slug?.trim()?.takeIf { it.isNotEmpty() }?.let { newSlug ->
            val owner = categoryRepository.findBySlug(newSlug)
            if (owner != null && owner.categoryId != categoryId) {
                throw ConflictException(ErrorCode.CONFLICT, "Slug danh mục đã tồn tại")
            }
            category.slug = newSlug
        }
        request.description?.let { category.description = it.trim() }
        request.sortOrder?.let { category.sortOrder = it }

        val saved = categoryRepository.save(category)
        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = "CATEGORY_UPDATED",
            entityType = "event_category",
            entityId = categoryId,
            oldValues = before,
            newValues = mapOf(
                "name" to saved.categoryName,
                "slug" to saved.slug,
                "description" to saved.description,
                "sortOrder" to saved.sortOrder
            ),
            resultCode = "OK"
        )
        return toDto(saved, emptyMap(), eventCounts())
    }

    @Transactional
    fun hide(actorUserId: Long, categoryId: Long, reason: String): AdminCategoryDto {
        val category = categoryRepository.findById(categoryId).orElse(null)
            ?: throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
        if (!category.isActive) {
            throw ConflictException(ErrorCode.CONFLICT, "Danh mục đã bị ẩn trước đó")
        }
        category.isActive = false
        val saved = categoryRepository.save(category)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = "CATEGORY_HIDDEN",
            entityType = "event_category",
            entityId = categoryId,
            reason = reason,
            oldValues = mapOf("isActive" to true),
            newValues = mapOf("isActive" to false),
            resultCode = "HIDDEN"
        )
        return toDto(saved, emptyMap(), eventCounts())
    }

    @Transactional
    fun unhide(actorUserId: Long, categoryId: Long): AdminCategoryDto {
        val category = categoryRepository.findById(categoryId).orElse(null)
            ?: throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
        if (category.isActive) {
            throw ConflictException(ErrorCode.CONFLICT, "Danh mục đang hoạt động")
        }
        if (activeCount() >= MAX_ACTIVE) {
            throw ConflictException(ErrorCode.CONFLICT, "Đã đạt giới hạn $MAX_ACTIVE danh mục đang hoạt động")
        }
        category.isActive = true
        val saved = categoryRepository.save(category)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = "CATEGORY_UNHIDDEN",
            entityType = "event_category",
            entityId = categoryId,
            oldValues = mapOf("isActive" to false),
            newValues = mapOf("isActive" to true),
            resultCode = "ACTIVE"
        )
        return toDto(saved, emptyMap(), eventCounts())
    }

    /** Kéo-thả sắp xếp trong màn Admin: cập nhật hàng loạt `sort_order`. */
    @Transactional
    fun reorder(actorUserId: Long, items: List<CategoryOrderItem>): List<AdminCategoryDto> {
        val ids = items.map { it.categoryId }
        if (ids.distinct().size != ids.size) throw validationError("items", "Danh sách có danh mục bị lặp")
        val categories = categoryRepository.findAllById(ids).associateBy { it.categoryId!! }
        ids.forEach { id ->
            if (!categories.containsKey(id)) throw NotFoundException(ErrorCode.CATEGORY_NOT_FOUND)
        }
        items.forEach { item -> categories[item.categoryId]!!.sortOrder = item.sortOrder }
        categoryRepository.saveAll(categories.values)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = "CATEGORY_REORDERED",
            entityType = "event_category",
            entityId = null,
            newValues = mapOf("items" to items.map { mapOf("id" to it.categoryId, "sortOrder" to it.sortOrder) }),
            resultCode = "OK"
        )
        return tree(includeInactive = true)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun activeCount(): Long = categoryRepository.findByIsActiveTrueOrderBySortOrderAscCategoryIdAsc().size.toLong()

    private fun nextSortOrder(parentId: Long?): Int =
        categoryRepository.findAllByOrderBySortOrderAscCategoryIdAsc()
            .filter { it.parentCategoryId == parentId }
            .maxOfOrNull { it.sortOrder }
            ?.plus(1)
            ?: 0

    private fun eventCounts(): Map<Long, Long> =
        categoryMapRepository.findAll()
            .groupingBy { it.id.categoryId }
            .eachCount()
            .mapValues { it.value.toLong() }

    private fun toDto(
        category: EventCategory,
        byParent: Map<Long?, List<EventCategory>>,
        counts: Map<Long, Long>
    ): AdminCategoryDto = AdminCategoryDto(
        id = category.categoryId!!,
        code = category.categoryCode,
        name = category.categoryName,
        slug = category.slug,
        description = category.description,
        parentCategoryId = category.parentCategoryId,
        active = category.isActive,
        sortOrder = category.sortOrder,
        eventCount = counts[category.categoryId] ?: 0L,
        children = byParent[category.categoryId].orEmpty().map { toDto(it, byParent, counts) },
        updatedAt = category.updatedAt ?: Instant.now()
    )

    private fun slugify(value: String): String = java.text.Normalizer
        .normalize(value.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('đ', 'd')
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(160)
        .ifEmpty { "danh-muc" }

    companion object {
        const val MAX_ACTIVE = 50L
    }
}
