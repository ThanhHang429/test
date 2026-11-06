package com.eventapp.organizer.event

import java.math.BigDecimal
import java.time.Instant

/** Loại vé kèm tồn kho — organizer cần thấy cả đã bán và đang giữ chỗ. */
data class OrganizerTicketTypeDto(
    val id: Long,
    val name: String,
    /** FREE | PAID */
    val kind: String,
    val price: BigDecimal,
    val currency: String,
    val quantityTotal: Int?,
    val quantitySold: Int,
    val quantityReserved: Int,
    val remaining: Int?,
    /** ACTIVE | PAUSED | ENDED | ARCHIVED */
    val status: String,
    val saleStartAt: Instant?,
    val saleEndAt: Instant?
)

data class OrganizerEventDto(
    val id: Long,
    val code: String,
    val title: String,
    val slug: String,
    val coverUrl: String?,
    /** DRAFT | PENDING_REVIEW | REVISION_REQUIRED | REJECTED | APPROVED */
    val reviewStatus: String,
    /** UNPUBLISHED | PUBLISHED | SUSPENDED | CANCELLED | COMPLETED */
    val lifecycleStatus: String,
    val startAt: Instant,
    val endAt: Instant,
    val timezone: String,
    val city: String?,
    val venueName: String?,
    val capacityLimit: Int?,
    val ticketTypes: List<OrganizerTicketTypeDto>,
    /** Tổng vé đã bán (PAID), không gồm vé đang giữ chỗ. */
    val ticketsSold: Int,
    /** Doanh thu đã thanh toán; đơn PENDING_PAYMENT không bao giờ được cộng (BR-022). */
    val revenue: BigDecimal,
    val currency: String
)
