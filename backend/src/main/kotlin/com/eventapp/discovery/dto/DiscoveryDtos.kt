package com.eventapp.discovery.dto

import java.math.BigDecimal
import java.time.Instant

data class CategoryNodeDto(
    val id: Long,
    val code: String,
    val name: String,
    val slug: String,
    val description: String?,
    val parentId: Long?,
    val sortOrder: Int,
    val children: List<CategoryNodeDto> = emptyList()
)

data class TicketTypeDto(
    val id: Long,
    val name: String,
    val description: String?,
    val kind: String,
    val price: BigDecimal,
    val currency: String,
    /** NOT_STARTED | ON_SALE | ENDED | SOLD_OUT */
    val saleStatus: String,
    val saleStartAt: Instant?,
    val saleEndAt: Instant?,
    val remaining: Int?,
    val remainingLabel: String?,
    val minPerOrder: Int,
    val maxPerUser: Int
)

data class OrganizerBriefDto(
    val id: Long,
    val publicName: String,
    val description: String?,
    val logoUrl: String?,
    val organizedEventCount: Long
)

data class ReviewBriefDto(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val text: String?,
    val createdAt: Instant?
)

data class ReviewSummaryDto(
    val averageRating: Double,
    val totalReviews: Long,
    val distribution: Map<String, Long>,
    val highlights: List<ReviewBriefDto>
)

/** Thẻ sự kiện dùng cho danh sách/tìm kiếm (FR-U04) và gợi ý (FR-U07). */
data class EventCardDto(
    val id: Long,
    val code: String,
    val title: String,
    val slug: String,
    val shortDescription: String?,
    val coverUrl: String?,
    val startAt: Instant,
    val endAt: Instant,
    val timezone: String,
    val city: String?,
    val venueName: String?,
    /** OFFLINE | ONLINE | HYBRID */
    val eventFormat: String,
    val categoryIds: List<Long>,
    val categoryNames: List<String>,
    val tags: List<String>,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val isFree: Boolean,
    val available: Boolean?,
    val availabilityUnknown: Boolean,
    /** NOT_STARTED | ON_SALE | ENDED | SOLD_OUT */
    val saleStatus: String,
    /** Nhãn hiển thị do server tính: Sắp diễn ra / Đang mở bán / Hết vé / Đã huỷ / Đã kết thúc */
    val statusLabel: String,
    val isFavorite: Boolean? = null,
    /** Chỉ có ở API gợi ý (FR-U07) */
    val reason: String? = null,
    val reasonSource: String? = null
)

data class EventDetailDto(
    val id: Long,
    val code: String,
    val title: String,
    val slug: String,
    val shortDescription: String?,
    val description: String?,
    val eventFormat: String,
    val lifecycleStatus: String,
    val startAt: Instant,
    val endAt: Instant,
    val timezone: String,
    val registrationStartAt: Instant?,
    val registrationEndAt: Instant?,
    val venueName: String?,
    val addressLine: String?,
    val city: String?,
    val province: String?,
    val onlineUrl: String?,
    val capacityLimit: Int?,
    val participationConditions: String?,
    val cancellationPolicy: String?,
    val coverUrl: String?,
    val mediaUrls: List<String>,
    val categoryIds: List<Long>,
    val categoryNames: List<String>,
    val tags: List<String>,
    val organizer: OrganizerBriefDto?,
    val ticketTypes: List<TicketTypeDto>,
    val minPrice: BigDecimal?,
    val isFree: Boolean,
    val reviewSummary: ReviewSummaryDto,
    val isFavorite: Boolean,
    val registrationStatus: String,
    val canReview: Boolean,
    val allowedActions: List<String>,
    val disabledReason: String?,
    val serverTime: Instant,
    val fetchedAt: Instant
)

/** Hành động hợp lệ do server tính (U05-R02) — app không tự suy luận. */
enum class EventAction { REGISTER_FREE, BUY_TICKET, FAVORITE, UNFAVORITE, VIEW_MY_TICKET, WRITE_REVIEW }

enum class DisabledReason {
    SOLD_OUT, SALES_NOT_STARTED, SALES_ENDED, EVENT_CANCELLED, EVENT_COMPLETED, LOGIN_REQUIRED, ALREADY_REGISTERED
}
