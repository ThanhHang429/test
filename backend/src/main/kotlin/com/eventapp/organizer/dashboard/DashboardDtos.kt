package com.eventapp.organizer.dashboard

import java.math.BigDecimal
import java.time.Instant

data class BlockMetaDto(
    /** OK | ERROR | STALE */
    val status: String,
    val asOf: Instant,
    val note: String? = null
)

data class EventStatusBlockDto(
    /** DRAFT, PENDING_REVIEW, REVISION_REQUIRED, REJECTED, APPROVED_UNPUBLISHED, ON_SALE, UPCOMING, COMPLETED, SUSPENDED, CANCELLED */
    val counts: Map<String, Long>,
    val total: Long,
    val meta: BlockMetaDto
)

data class RegistrationBlockDto(
    val confirmed: Long,
    val cancelled: Long,
    val waitlisted: Long,
    val ticketsIssued: Long,
    val ticketsCancelled: Long,
    val meta: BlockMetaDto
)

data class FinanceBlockDto(
    /** Đã xác minh — chỉ đơn PAID (O03-R02, BR-022). */
    val verifiedAmount: BigDecimal,
    /** Đơn chờ thanh toán — KHÔNG cộng vào doanh thu. */
    val pendingAmount: BigDecimal,
    val refundedAmount: BigDecimal,
    val feeAmount: BigDecimal,
    val netAmount: BigDecimal,
    val currency: String,
    val meta: BlockMetaDto
)

data class CheckinBlockDto(
    val checkedIn: Long,
    val ticketsIssued: Long,
    val checkinRate: Double,
    val meta: BlockMetaDto
)

data class ReviewBlockDto(
    val averageRating: Double,
    val totalVisible: Long,
    val newInRange: Long,
    val meta: BlockMetaDto
)

data class DashboardTaskDto(
    /** EVENT_UPCOMING_48H | EVENT_REVISION_REQUIRED | EVENT_REJECTED | TICKET_TYPE_ENDING_SOON */
    val type: String,
    val eventId: Long,
    val title: String,
    val detail: String,
    val dueAt: Instant?,
    /** Số nhỏ = khẩn hơn (O03-R07). */
    val priority: Int
)

data class TopEventDto(
    val eventId: Long,
    val title: String,
    val verifiedAmount: BigDecimal,
    val registrations: Long
)

data class OrganizerDashboardDto(
    val range: String,
    val from: Instant,
    val to: Instant,
    val events: EventStatusBlockDto,
    val registrations: RegistrationBlockDto,
    val finance: FinanceBlockDto,
    val checkin: CheckinBlockDto,
    val reviews: ReviewBlockDto,
    val tasks: List<DashboardTaskDto>,
    val topEvents: List<TopEventDto>,
    val asOf: Instant
)
