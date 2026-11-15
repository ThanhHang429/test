package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

/** DTO gửi lên khi Organizer tạo hoặc sửa loại vé (FR-O10, FR-O11, FR-O12) */
data class CreateTicketTypeRequest(
    val ticketTypeName: String,
    val description: String? = null,
    val price: BigDecimal = BigDecimal.ZERO,
    val currency: String = "VND",
    val quantityTotal: Int? = null,
    val minPerOrder: Short = 1,
    val maxPerUser: Short = 10,
    val saleStartAt: Instant? = null,
    val saleEndAt: Instant? = null
)

/** DTO trả về cho Client hiển thị */
data class TicketTypeDto(
    val ticketTypeId: Long,
    val eventId: Long,
    val ticketTypeName: String,
    val description: String?,
    val ticketKind: String,
    val price: BigDecimal,
    val currency: String,
    val quantityTotal: Int?,
    val quantityReserved: Int,
    val quantitySold: Int,
    val remainingQuantity: Int?,
    val minPerOrder: Short,
    val maxPerUser: Short,
    val saleStartAt: Instant?,
    val saleEndAt: Instant?,
    val status: String
)