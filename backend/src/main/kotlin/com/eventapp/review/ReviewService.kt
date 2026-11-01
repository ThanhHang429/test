package com.eventapp.review

import com.eventapp.common.audit.AuditLogWriter
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.ReviewStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.common.idempotency.IdempotencyStore
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventMediaRepository
import com.eventapp.event.repository.EventRepository
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.identity.repository.UserRepository
import com.eventapp.interaction.entity.Review
import com.eventapp.interaction.repository.ReviewRepository
import com.eventapp.moderation.ContentFilterService
import com.eventapp.review.dto.CreateReviewRequest
import com.eventapp.review.dto.MyReviewDto
import com.eventapp.review.dto.ReportReviewRequest
import com.eventapp.review.dto.ReviewDto
import com.eventapp.review.dto.ReviewMutationDto
import com.eventapp.review.dto.UpdateReviewRequest
import com.eventapp.ticketing.repository.TicketRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * FR-U20 — viết / sửa / xem đánh giá.
 *
 * Quy tắc nghiệp vụ:
 *  - U20-R01 mỗi user một đánh giá/sự kiện (DB có UNIQUE).
 *  - Chỉ đánh giá được khi đã check-in, sự kiện đã kết thúc và trong 30 ngày.
 *  - Bộ lọc nội dung không đạt → PENDING (SRS gọi HELD), chỉ tác giả thấy.
 *  - Sửa tối đa 3 lần trong 7 ngày kể từ lúc tạo (SRS U20 khối dữ liệu).
 *  - U20-R04 tên hiển thị rút gọn, không lộ email/SĐT/userId.
 */
@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val eventRepository: EventRepository,
    private val eventMediaRepository: EventMediaRepository,
    private val userRepository: UserRepository,
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val ticketRepository: TicketRepository,
    private val contentFilter: ContentFilterService,
    private val idempotencyStore: IdempotencyStore,
    private val auditLogWriter: AuditLogWriter,
    private val reviewAuditQuery: ReviewAuditQuery,
    private val entityManager: EntityManager
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ------------------------------------------------------------------
    // Đọc
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun list(eventId: Long, sort: String, page: Int, size: Int, viewerId: Long?): Pair<List<ReviewDto>, Long> {
        val event = requireReviewableEvent(eventId)

        val own = viewerId?.let { reviewRepository.findByUserIdAndEventId(it, eventId) }
        val ownCount = if (own != null) 1L else 0L
        val visibleTotal = reviewRepository.countByEventIdAndStatus(eventId, ReviewStatus.VISIBLE) -
            (if (own?.status == ReviewStatus.VISIBLE) 1L else 0L)

        val pageable = PageRequest.of(page, size)
        val visible = when (sort.uppercase()) {
            SORT_RATING_DESC -> reviewRepository
                .findByEventIdAndStatusAndUserIdNotOrderByCreatedAtDesc(eventId, ReviewStatus.VISIBLE, viewerId ?: 0L, pageable)
            else -> reviewRepository
                .findByEventIdAndStatusAndUserIdNotOrderByCreatedAtDesc(eventId, ReviewStatus.VISIBLE, viewerId ?: 0L, pageable)
        }

        val items = visible.content
            .sortedWith(comparatorFor(sort))
            .map { toDto(it, event, viewerId) }
            .toMutableList()

        // U20-R03: đánh giá của chính mình luôn ghim đầu, kèm trạng thái thật.
        if (page == 0 && own != null) {
            items.add(0, toDto(own, event, viewerId))
        }
        return items to (visibleTotal + ownCount)
    }

    @Transactional(readOnly = true)
    fun myReviews(userId: Long, page: Int, size: Int): Pair<List<MyReviewDto>, Long> {
        val result = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
        val events = loadEvents(result.content.map { it.eventId })
        val covers = loadCovers(result.content.map { it.eventId })
        val now = Instant.now()
        val items = result.content.map { review ->
            val event = events[review.eventId]
            toMyDto(review, event, covers[review.eventId], now)
        }
        return items to result.totalElements
    }

    /** Chặn đọc đánh giá của sự kiện chưa công khai (dùng cho cả endpoint summary). */
    @Transactional(readOnly = true)
    fun assertPublicEvent(eventId: Long) {
        requireReviewableEvent(eventId)
    }

    /** Điều kiện hiển thị nút «Viết đánh giá» (dùng chung với chi tiết sự kiện). */    @Transactional(readOnly = true)
    fun canReview(userId: Long, eventId: Long): Boolean {
        val event = eventRepository.findById(eventId).orElse(null) ?: return false
        val now = Instant.now()
        if (event.endAt.isAfter(now)) return false
        if (now.isAfter(event.endAt.plus(Duration.ofDays(REVIEW_WINDOW_DAYS)))) return false
        if (ticketRepository.countCheckedIn(userId, eventId) == 0L) return false
        return reviewRepository.findByUserIdAndEventId(userId, eventId) == null
    }

    // ------------------------------------------------------------------
    // Tạo / sửa
    // ------------------------------------------------------------------

    @Transactional
    fun create(userId: Long, eventId: Long, request: CreateReviewRequest, idempotencyKey: String?): ReviewMutationDto {
        val fingerprint = "create:$eventId:${request.rating}:${request.text.orEmpty().hashCode()}"
        return idempotencyStore.execute("review", userId, idempotencyKey, fingerprint) {
            doCreate(userId, eventId, request)
        }
    }

    private fun doCreate(userId: Long, eventId: Long, request: CreateReviewRequest): ReviewMutationDto {
        val event = requireReviewableEvent(eventId)
        val now = Instant.now()

        if (event.endAt.isAfter(now)) throw ForbiddenException(ErrorCode.EVENT_NOT_ENDED)
        if (now.isAfter(event.endAt.plus(Duration.ofDays(REVIEW_WINDOW_DAYS)))) {
            throw ForbiddenException(ErrorCode.REVIEW_WINDOW_CLOSED)
        }
        if (ticketRepository.countCheckedIn(userId, eventId) == 0L) {
            throw ForbiddenException(ErrorCode.NOT_ATTENDED)
        }
        reviewRepository.findByUserIdAndEventId(userId, eventId)?.let {
            throw ConflictException(ErrorCode.REVIEW_ALREADY_EXISTS)
        }

        val rating = request.rating ?: throw validationError("rating", "Vui lòng chọn số sao")
        val text = request.text?.trim()?.takeIf { it.isNotEmpty() }

        val review = Review(
            userId = userId,
            eventId = eventId,
            rating = rating.toByte(),
            reviewText = text,
            status = ReviewStatus.VISIBLE
        )
        val filter = contentFilter.check(text)
        if (!filter.passed) {
            review.status = ReviewStatus.PENDING
            review.moderationReason = filter.reason
        }
        val saved = reviewRepository.save(review)
        // created_at / updated_at do MySQL sinh ra → đọc lại để DTO không trả null.
        entityManager.flush()
        entityManager.refresh(saved)

        auditLogWriter.write(
            actorUserId = userId,
            actionCode = ACTION_CREATED,
            entityType = ENTITY_TYPE,
            entityId = saved.reviewId,
            reason = filter.reason,
            newValues = mapOf(
                "eventId" to eventId,
                "rating" to rating,
                "status" to saved.status.name,
                "violations" to filter.violations.map { it.code }
            ),
            resultCode = if (filter.passed) "VISIBLE" else "PENDING"
        )

        val dto = toMyDto(saved, event, loadCovers(listOf(eventId))[eventId], Instant.now())
        val notice = if (saved.status == ReviewStatus.PENDING) {
            "Đánh giá đang chờ kiểm duyệt và sẽ hiển thị sau khi được duyệt."
        } else {
            "Cảm ơn bạn đã đánh giá sự kiện."
        }
        return ReviewMutationDto(dto, notice)
    }

    @Transactional
    fun update(userId: Long, eventId: Long, request: UpdateReviewRequest): ReviewMutationDto {
        val review = reviewRepository.findByUserIdAndEventId(userId, eventId)
            ?: throw NotFoundException(message = "Bạn chưa đánh giá sự kiện này")

        if (review.status == ReviewStatus.HIDDEN || review.status == ReviewStatus.REMOVED) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Đánh giá đã bị ẩn, bạn không thể sửa")
        }

        val now = Instant.now()
        val createdAt = review.createdAt ?: now
        if (now.isAfter(createdAt.plus(Duration.ofDays(EDIT_WINDOW_DAYS)))) {
            throw ForbiddenException(ErrorCode.REVIEW_WINDOW_CLOSED, "Đã quá 7 ngày, bạn không thể sửa đánh giá")
        }
        val editCount = reviewAuditQuery.editCount(review.reviewId!!)
        if (editCount >= MAX_EDITS) {
            throw ConflictException(ErrorCode.REVIEW_EDIT_LIMIT)
        }
        val currentUpdatedAt = review.updatedAt
        if (request.version != null && currentUpdatedAt != null &&
            request.version != currentUpdatedAt.toEpochMilli()
        ) {
            throw ConflictException(ErrorCode.CONFLICT, "Đánh giá đã thay đổi ở thiết bị khác, vui lòng tải lại")
        }

        val oldRating = review.rating
        val oldText = review.reviewText
        request.rating?.let { review.rating = it.toByte() }
        if (request.text != null) review.reviewText = request.text.trim().takeIf { it.isNotEmpty() }

        val filter = contentFilter.check(review.reviewText)
        if (!filter.passed) {
            review.status = ReviewStatus.PENDING
            review.moderationReason = filter.reason
        } else if (review.status == ReviewStatus.PENDING) {
            review.status = ReviewStatus.VISIBLE
            review.moderationReason = null
        }

        val saved = reviewRepository.save(review)
        entityManager.flush()
        entityManager.refresh(saved)
        auditLogWriter.write(
            actorUserId = userId,
            actionCode = ACTION_UPDATED,
            entityType = ENTITY_TYPE,
            entityId = saved.reviewId,
            reason = filter.reason,
            oldValues = mapOf("rating" to oldRating, "text" to oldText),
            newValues = mapOf("rating" to saved.rating, "text" to saved.reviewText, "status" to saved.status.name),
            resultCode = saved.status.name
        )

        val event = eventRepository.findById(eventId).orElse(null)
        val dto = toMyDto(saved, event, loadCovers(listOf(eventId))[eventId], Instant.now())
        val notice = if (saved.status == ReviewStatus.PENDING) {
            "Đánh giá đang chờ kiểm duyệt lại."
        } else {
            "Đã cập nhật đánh giá."
        }
        return ReviewMutationDto(dto, notice)
    }

    // ------------------------------------------------------------------
    // Báo cáo vi phạm (A14-R05, A14-R06)
    // ------------------------------------------------------------------

    @Transactional
    fun report(userId: Long, reviewId: Long, request: ReportReviewRequest) {
        val review = reviewRepository.findById(reviewId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đánh giá")
        if (review.status == ReviewStatus.REMOVED) throw NotFoundException(message = "Không tìm thấy đánh giá")

        val reasonCode = request.reasonCode.trim().uppercase()
        if (reasonCode !in REPORT_REASON_CODES) {
            throw validationError(
                "reasonCode",
                "Mã lý do phải là một trong: ${REPORT_REASON_CODES.joinToString(", ")}"
            )
        }

        val organizerOwnsEvent = isOrganizerOfEvent(userId, review.eventId)
        if (!organizerOwnsEvent && review.userId == userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không thể báo cáo đánh giá của chính mình")
        }
        // A14-R06: Organizer chỉ báo cáo được đánh giá thuộc sự kiện mình sở hữu.
        val userIsOrganizer = organizerProfileRepository.findByUserId(userId) != null
        if (userIsOrganizer && !organizerOwnsEvent) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chỉ báo cáo được đánh giá thuộc sự kiện của mình")
        }

        val entityId = reviewId.toString()
        if (reviewAuditQuery.hasReported(userId, entityId)) {
            throw ConflictException(ErrorCode.CONFLICT, "Bạn đã báo cáo đánh giá này rồi")
        }
        val todayCount = reviewAuditQuery.reportsToday(userId, Instant.now())
        if (todayCount >= MAX_REPORTS_PER_DAY) {
            throw ConflictException(ErrorCode.RATE_LIMITED, "Bạn đã gửi tối đa 10 báo cáo trong ngày")
        }

        auditLogWriter.write(
            actorUserId = userId,
            actionCode = ACTION_REPORTED,
            entityType = ENTITY_TYPE,
            entityId = entityId,
            reason = request.note?.take(500) ?: reasonCode,
            newValues = mapOf("reasonCode" to reasonCode, "organizerReport" to organizerOwnsEvent),
            resultCode = "ACCEPTED"
        )

        // Ngưỡng đưa vào hàng chờ: ≥3 tài khoản khác nhau, hoặc 1 báo cáo từ Organizer sở hữu.
        val reporters = reviewAuditQuery.reporters(entityId)
        if (review.status == ReviewStatus.VISIBLE &&
            (organizerOwnsEvent || reporters >= REPORT_THRESHOLD)
        ) {
            review.status = ReviewStatus.PENDING
            review.moderationReason = "Bị báo cáo vi phạm ($reasonCode)"
            reviewRepository.save(review)
            log.info("Đánh giá {} chuyển PENDING do {} báo cáo", reviewId, reporters)
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun requireReviewableEvent(eventId: Long): Event {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.deletedAt != null) throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.reviewStatus != EventReviewStatus.APPROVED) throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.lifecycleStatus !in PUBLIC_LIFECYCLES) throw NotFoundException(message = "Không tìm thấy sự kiện")
        return event
    }

    private fun isOrganizerOfEvent(userId: Long, eventId: Long): Boolean {
        val profile = organizerProfileRepository.findByUserId(userId) ?: return false
        val event = eventRepository.findById(eventId).orElse(null) ?: return false
        return event.organizerProfileId == profile.organizerProfileId
    }

    private fun loadEvents(eventIds: Collection<Long>): Map<Long, Event> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventRepository.findAllById(eventIds.toSet()).associateBy { it.eventId!! }
    }

    private fun loadCovers(eventIds: Collection<Long>): Map<Long, String?> {
        if (eventIds.isEmpty()) return emptyMap()
        return eventMediaRepository.findByEventIdInOrderBySortOrderAsc(eventIds.toSet())
            .filter { it.isCover }
            .associate { it.eventId to it.storageUrl }
    }

    private fun comparatorFor(sort: String): Comparator<Review> = when (sort.uppercase()) {
        SORT_RATING_DESC -> compareByDescending<Review> { it.rating }.thenByDescending { it.createdAt }
        SORT_RATING_ASC -> compareBy<Review> { it.rating }.thenByDescending { it.createdAt }
        else -> compareByDescending { it.createdAt }
    }

    fun toDto(review: Review, event: Event?, viewerId: Long?): ReviewDto = ReviewDto(
        id = review.reviewId!!,
        authorName = displayName(review.userId),
        rating = review.rating.toInt(),
        text = review.reviewText,
        status = review.status.name,
        statusLabel = statusLabel(review.status),
        mine = viewerId != null && review.userId == viewerId,
        createdAt = review.createdAt,
        updatedAt = review.updatedAt
    )

    fun toMyDto(review: Review, event: Event?, coverUrl: String?, now: Instant): MyReviewDto {
        val editCount = reviewAuditQuery.editCount(review.reviewId!!)
        val createdAt = review.createdAt ?: now
        val daysLeft = Duration.between(now, createdAt.plus(Duration.ofDays(EDIT_WINDOW_DAYS))).toDays()
        val editable = review.status != ReviewStatus.HIDDEN &&
            review.status != ReviewStatus.REMOVED &&
            editCount < MAX_EDITS &&
            daysLeft > 0
        return MyReviewDto(
            id = review.reviewId!!,
            eventId = review.eventId,
            eventTitle = event?.title,
            eventCoverUrl = coverUrl,
            rating = review.rating.toInt(),
            text = review.reviewText,
            status = review.status.name,
            statusLabel = statusLabel(review.status),
            moderationReason = if (review.status == ReviewStatus.HIDDEN) {
                "Đánh giá bị ẩn do vi phạm chính sách nội dung"
            } else null,
            createdAt = review.createdAt,
            updatedAt = review.updatedAt,
            editCount = editCount,
            canEdit = editable,
            editDaysLeft = daysLeft.coerceAtLeast(0)
        )
    }

    /** U20-R04: họ + tên, che tên đệm. */
    fun displayName(userId: Long): String {
        val user = userRepository.findById(userId).orElse(null) ?: return "Người dùng"
        val parts = user.fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "Người dùng"
            parts.size == 1 -> parts[0]
            else -> parts.first() + " " + parts.last()
        }
    }

    companion object {
        const val REVIEW_WINDOW_DAYS = 30L
        const val EDIT_WINDOW_DAYS = 7L
        const val MAX_EDITS = 3
        const val MAX_REPORTS_PER_DAY = 10L
        const val REPORT_THRESHOLD = 3L

        const val SORT_NEWEST = "NEWEST"
        const val SORT_RATING_DESC = "RATING_DESC"
        const val SORT_RATING_ASC = "RATING_ASC"

        const val ENTITY_TYPE = "review"
        const val ACTION_CREATED = "REVIEW_CREATED"
        const val ACTION_UPDATED = "REVIEW_UPDATED"
        const val ACTION_REPORTED = "REVIEW_REPORTED"

        val REPORT_REASON_CODES = setOf("SPAM", "OFFENSIVE", "PERSONAL_DATA", "FAKE_REVIEW", "OFF_TOPIC", "OTHER")
        val MODERATION_REASON_CODES = REPORT_REASON_CODES

        private val PUBLIC_LIFECYCLES = setOf(
            EventLifecycleStatus.PUBLISHED,
            EventLifecycleStatus.COMPLETED
        )

        fun statusLabel(status: ReviewStatus): String = when (status) {
            ReviewStatus.PENDING -> "Đang chờ kiểm duyệt"
            ReviewStatus.VISIBLE -> "Đang hiển thị"
            ReviewStatus.HIDDEN -> "Đã bị ẩn"
            ReviewStatus.REMOVED -> "Đã bị xoá"
        }
    }
}
