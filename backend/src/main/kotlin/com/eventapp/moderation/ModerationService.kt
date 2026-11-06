package com.eventapp.moderation

import com.eventapp.common.audit.AuditLogWriter
import com.eventapp.common.enums.ReviewStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.identity.repository.UserRepository
import com.eventapp.interaction.entity.Review
import com.eventapp.interaction.repository.ReviewRepository
import com.eventapp.review.ReviewAuditQuery
import com.eventapp.review.ReviewService
import com.eventapp.review.dto.AdminReviewDto
import com.eventapp.review.dto.ModerationActionRequest
import com.eventapp.review.dto.ModerationFlagDto
import com.eventapp.review.dto.RescanRequest
import com.eventapp.review.dto.RescanResultDto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * FR-A14 (quản lý đánh giá) và FR-A07 (cờ kiểm duyệt).
 *
 * Ràng buộc:
 *  - A14-R02/BR-020: không sửa nội dung, không xoá cứng — chỉ đổi trạng thái.
 *  - A14-R01/R04: luôn kèm mã lý do + ghi chú, ghi audit đầy đủ.
 *  - A07-R01/R03: cờ chỉ là gợi ý, mọi thay đổi do Admin bấm.
 *
 * Schema thật không có `moderation_flags` / `review_reports` nên cờ được suy ra:
 *  - HELD     ← reviews.status = PENDING (bộ lọc nội dung FR-U20 giữ lại)
 *  - REPORTED ← audit_logs action REVIEW_REPORTED
 * Mã cờ dùng dạng `review:{id}` để mở lại đúng đối tượng.
 */
@Service
class ModerationService(
    private val reviewRepository: ReviewRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val contentFilter: ContentFilterService,
    private val auditLogWriter: AuditLogWriter,
    private val reviewAuditQuery: ReviewAuditQuery
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ------------------------------------------------------------------
    // A14 — hàng chờ đánh giá
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun listReviews(
        status: String?,
        eventId: Long?,
        rating: Int?,
        page: Int,
        size: Int
    ): Pair<List<AdminReviewDto>, Long> {
        val statusFilter = status?.takeIf { it.isNotBlank() }?.let { parseStatus(it) }
        if (rating != null && rating !in 1..5) {
            throw validationError("rating", "Số sao phải trong khoảng 1..5")
        }
        val result = reviewRepository.searchForAdmin(
            statusFilter,
            eventId,
            rating?.toByte(),
            PageRequest.of(page, size)
        )
        val events = loadEvents(result.content.map { it.eventId })
        val reportCounts = reviewAuditQuery.reportCounts(result.content.mapNotNull { it.reviewId })
        val items = result.content.map { review ->
            toAdminDto(review, events[review.eventId], reportCounts[review.reviewId] ?: 0L)
        }
        return items to result.totalElements
    }

    @Transactional
    fun approve(reviewId: Long, actorUserId: Long, request: ModerationActionRequest): AdminReviewDto {
        val review = requireReview(reviewId)
        if (review.status == ReviewStatus.VISIBLE) {
            throw ConflictException(ErrorCode.CONFLICT, "Đánh giá đang hiển thị, không cần duyệt")
        }
        val old = review.status
        review.status = ReviewStatus.VISIBLE
        review.moderationReason = null
        review.moderatedByUserId = actorUserId
        review.moderatedAt = Instant.now()
        reviewRepository.save(review)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_REVIEW_APPROVED,
            entityType = ReviewService.ENTITY_TYPE,
            entityId = reviewId,
            reason = describe(request),
            oldValues = mapOf("status" to old.name),
            newValues = mapOf("status" to review.status.name),
            resultCode = "APPROVED"
        )
        return toAdminDto(review, eventRepository.findById(review.eventId).orElse(null), reviewAuditQuery.reporters(reviewId.toString()))
    }

    @Transactional
    fun hide(reviewId: Long, actorUserId: Long, request: ModerationActionRequest): AdminReviewDto {
        val review = requireReview(reviewId)
        val reasonCode = requireReasonCode(request.reasonCode)
        if (review.status == ReviewStatus.HIDDEN) {
            throw ConflictException(ErrorCode.CONFLICT, "Đánh giá đã bị ẩn")
        }
        val old = review.status
        review.status = ReviewStatus.HIDDEN
        review.moderationReason = reasonCode
        review.moderatedByUserId = actorUserId
        review.moderatedAt = Instant.now()
        reviewRepository.save(review)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_REVIEW_HIDDEN,
            entityType = ReviewService.ENTITY_TYPE,
            entityId = reviewId,
            reason = describe(request),
            oldValues = mapOf("status" to old.name),
            newValues = mapOf("status" to review.status.name, "reasonCode" to reasonCode),
            resultCode = "HIDDEN"
        )
        return toAdminDto(review, eventRepository.findById(review.eventId).orElse(null), reviewAuditQuery.reporters(reviewId.toString()))
    }

    @Transactional
    fun restore(reviewId: Long, actorUserId: Long, request: ModerationActionRequest): AdminReviewDto {
        val review = requireReview(reviewId)
        if (review.status != ReviewStatus.HIDDEN) {
            throw ConflictException(ErrorCode.CONFLICT, "Chỉ khôi phục được đánh giá đang bị ẩn")
        }
        review.status = ReviewStatus.VISIBLE
        review.moderationReason = null
        review.moderatedByUserId = actorUserId
        review.moderatedAt = Instant.now()
        reviewRepository.save(review)

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_REVIEW_RESTORED,
            entityType = ReviewService.ENTITY_TYPE,
            entityId = reviewId,
            reason = describe(request),
            oldValues = mapOf("status" to ReviewStatus.HIDDEN.name),
            newValues = mapOf("status" to review.status.name),
            resultCode = "RESTORED"
        )
        return toAdminDto(review, eventRepository.findById(review.eventId).orElse(null), reviewAuditQuery.reporters(reviewId.toString()))
    }

    // ------------------------------------------------------------------
    // A07 — hàng chờ cờ kiểm duyệt
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun listFlags(source: String?, page: Int, size: Int): Pair<List<ModerationFlagDto>, Long> {
        val held = reviewRepository.findByStatusOrderByCreatedAtAsc(ReviewStatus.PENDING, PageRequest.of(0, 200)).content
        val reported = reviewRepository.findByStatus(ReviewStatus.VISIBLE)
            .filter { reviewAuditQuery.reporters(it.reviewId!!.toString()) > 0 }

        val events = loadEvents((held + reported).map { it.eventId })
        val flags = mutableListOf<ModerationFlagDto>()

        held.forEach { review ->
            flags += ModerationFlagDto(
                id = flagId(review.reviewId!!),
                targetType = "REVIEW",
                targetId = review.reviewId!!,
                eventId = review.eventId,
                eventTitle = events[review.eventId]?.title,
                reasonCode = sourceCodeOf(review.moderationReason),
                reasonLabel = review.moderationReason ?: "Bộ lọc nội dung giữ lại",
                evidence = review.reviewText?.take(200),
                severity = severityOf(review.moderationReason, 0),
                status = "OPEN",
                createdAt = review.createdAt
            )
        }
        reported.forEach { review ->
            val reporters = reviewAuditQuery.reporters(review.reviewId!!.toString())
            flags += ModerationFlagDto(
                id = flagId(review.reviewId!!),
                targetType = "REVIEW",
                targetId = review.reviewId!!,
                eventId = review.eventId,
                eventTitle = events[review.eventId]?.title,
                reasonCode = "REPORTED",
                reasonLabel = "$reporters tài khoản đã báo cáo",
                evidence = review.reviewText?.take(200),
                severity = severityOf(null, reporters),
                status = "OPEN",
                createdAt = review.createdAt
            )
        }

        val filtered = when (source?.uppercase()) {
            null, "", "ALL" -> flags
            "HELD" -> flags.filter { it.reasonCode != "REPORTED" }
            "REPORTED" -> flags.filter { it.reasonCode == "REPORTED" }
            "FLAGGED" -> emptyList()
            else -> throw validationError("source", "source phải là ALL | HELD | FLAGGED | REPORTED")
        }.sortedByDescending { it.createdAt }

        val from = (page * size).coerceAtMost(filtered.size)
        val to = (from + size).coerceAtMost(filtered.size)
        return filtered.subList(from, to) to filtered.size.toLong()
    }

    /** Xác nhận vi phạm → ẩn đánh giá (A07-R01: Admin là người quyết định). */
    @Transactional
    fun confirmFlag(flagId: String, actorUserId: Long, request: ModerationActionRequest): AdminReviewDto {
        val reviewId = parseFlagId(flagId)
        val dto = hide(reviewId, actorUserId, request)
        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_FLAG_CONFIRMED,
            entityType = "moderation_flag",
            entityId = flagId,
            reason = describe(request),
            resultCode = "CONFIRMED"
        )
        return dto
    }

    /** Bỏ qua cờ → cho hiển thị lại (nếu đang PENDING) và ghi lý do. */
    @Transactional
    fun dismissFlag(flagId: String, actorUserId: Long, request: ModerationActionRequest): AdminReviewDto {
        val reviewId = parseFlagId(flagId)
        val review = requireReview(reviewId)
        if (review.status == ReviewStatus.PENDING) {
            review.status = ReviewStatus.VISIBLE
            review.moderationReason = null
            review.moderatedByUserId = actorUserId
            review.moderatedAt = Instant.now()
            reviewRepository.save(review)
        }
        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_FLAG_DISMISSED,
            entityType = "moderation_flag",
            entityId = flagId,
            reason = describe(request),
            resultCode = "DISMISSED"
        )
        return toAdminDto(
            review,
            eventRepository.findById(review.eventId).orElse(null),
            reviewAuditQuery.reporters(reviewId.toString())
        )
    }

    /**
     * A07-R07: quét lại idempotent theo (targetType, targetId).
     * Chỉ quét đánh giá đang VISIBLE có nội dung; đánh giá đã PENDING giữ nguyên.
     */
    @Transactional
    fun rescan(actorUserId: Long, request: RescanRequest): RescanResultDto {
        val targetType = request.targetType?.uppercase() ?: "REVIEW"
        if (targetType != "REVIEW") {
            throw validationError("targetType", "Hiện chỉ hỗ trợ targetType = REVIEW")
        }
        val since = request.sinceDays?.let { Instant.now().minus(Duration.ofDays(it.toLong())) }
        val candidates = reviewRepository.findByStatusAndReviewTextIsNotNull(ReviewStatus.VISIBLE)
            .filter { since == null || (it.createdAt != null && it.createdAt!!.isAfter(since)) }

        val flagged = mutableListOf<String>()
        candidates.forEach { review ->
            val result = contentFilter.check(review.reviewText)
            if (!result.passed) {
                review.status = ReviewStatus.PENDING
                review.moderationReason = result.reason
                reviewRepository.save(review)
                flagged += flagId(review.reviewId!!)
                auditLogWriter.write(
                    actorUserId = actorUserId,
                    actionCode = ACTION_RESCAN_FLAGGED,
                    entityType = ReviewService.ENTITY_TYPE,
                    entityId = review.reviewId,
                    reason = result.reason,
                    newValues = mapOf("violations" to result.violations.map { it.code }),
                    resultCode = "FLAGGED"
                )
            }
        }

        auditLogWriter.write(
            actorUserId = actorUserId,
            actionCode = ACTION_RESCAN,
            entityType = "moderation_flag",
            entityId = null,
            reason = "Quét lại nội dung đánh giá",
            newValues = mapOf("scanned" to candidates.size, "flagged" to flagged.size),
            resultCode = "OK"
        )
        log.info("Rescan đánh giá: quét {} mục, gắn cờ {}", candidates.size, flagged.size)
        return RescanResultDto(scanned = candidates.size, flagged = flagged.size, flagIds = flagged)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun requireReview(reviewId: Long): Review =
        reviewRepository.findById(reviewId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đánh giá")

    private fun parseStatus(raw: String): ReviewStatus = try {
        ReviewStatus.valueOf(raw.uppercase())
    } catch (_: IllegalArgumentException) {
        throw validationError("status", "status phải là PENDING | VISIBLE | HIDDEN | REMOVED")
    }

    private fun requireReasonCode(raw: String): String {
        val code = raw.trim().uppercase()
        if (code !in ReviewService.MODERATION_REASON_CODES) {
            throw validationError(
                "reasonCode",
                "Mã lý do phải là một trong: ${ReviewService.MODERATION_REASON_CODES.joinToString(", ")}"
            )
        }
        return code
    }

    private fun describe(request: ModerationActionRequest): String {
        val note = request.note?.trim()?.takeIf { it.isNotEmpty() }
        return if (note != null) "${request.reasonCode.trim().uppercase()}: $note" else request.reasonCode.trim().uppercase()
    }

    private fun toAdminDto(review: Review, event: Event?, reportCount: Long): AdminReviewDto {
        val user = userRepository.findById(review.userId).orElse(null)
        val codes = when {
            reportCount > 0 -> listOf("REPORTED")
            review.status == ReviewStatus.PENDING -> listOf(sourceCodeOf(review.moderationReason))
            else -> emptyList()
        }
        return AdminReviewDto(
            id = review.reviewId!!,
            eventId = review.eventId,
            eventTitle = event?.title,
            authorName = displayName(review.userId),
            authorEmail = user?.email,
            rating = review.rating.toInt(),
            text = review.reviewText,
            status = review.status.name,
            statusLabel = ReviewService.statusLabel(review.status),
            moderationReason = review.moderationReason,
            flaggedCodes = codes,
            reportCount = reportCount,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt,
            moderatedAt = review.moderatedAt
        )
    }

    private fun displayName(userId: Long): String {
        val user = userRepository.findById(userId).orElse(null) ?: return "Người dùng"
        val parts = user.fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "Người dùng"
            parts.size == 1 -> parts[0]
            else -> parts.first() + " " + parts.last()
        }
    }

    private fun loadEvents(eventIds: Collection<Long>): Map<Long, Event> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventRepository.findAllById(eventIds.toSet()).associateBy { it.eventId!! }
    }

    private fun flagId(reviewId: Long): String = "review:$reviewId"

    private fun parseFlagId(flagId: String): Long {
        val raw = flagId.substringAfterLast(':')
        return raw.toLongOrNull()
            ?: throw validationError("id", "Mã cờ phải có dạng review:{reviewId}")
    }

    private fun sourceCodeOf(reason: String?): String = when {
        reason == null -> "FILTER"
        reason.contains("điện thoại", ignoreCase = true) -> ContentFilterService.PHONE_NUMBER
        reason.contains("liên kết", ignoreCase = true) -> ContentFilterService.LINK
        reason.contains("từ khoá", ignoreCase = true) -> ContentFilterService.BLOCKED_KEYWORD
        reason.contains("báo cáo", ignoreCase = true) -> "REPORTED"
        else -> "FILTER"
    }

    private fun severityOf(reason: String?, reportCount: Long): String = when {
        reportCount >= ReviewService.REPORT_THRESHOLD -> "HIGH"
        reason?.contains("điện thoại", ignoreCase = true) == true -> "HIGH"
        else -> "MEDIUM"
    }

    companion object {
        const val ACTION_REVIEW_APPROVED = "REVIEW_APPROVED"
        const val ACTION_REVIEW_HIDDEN = "REVIEW_HIDDEN"
        const val ACTION_REVIEW_RESTORED = "REVIEW_RESTORED"
        const val ACTION_FLAG_CONFIRMED = "MODERATION_FLAG_CONFIRMED"
        const val ACTION_FLAG_DISMISSED = "MODERATION_FLAG_DISMISSED"
        const val ACTION_RESCAN = "MODERATION_RESCAN"
        const val ACTION_RESCAN_FLAGGED = "MODERATION_RESCAN_FLAGGED"
    }
}
