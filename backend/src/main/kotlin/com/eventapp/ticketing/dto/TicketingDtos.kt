package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

// ---------- Tạo đơn ----------

data class CreateOrderItemRequest(
    val ticketTypeId: Long,
    val quantity: Int
)

data class CreateOrderRequest(
    val eventId: Long,
    val items: List<CreateOrderItemRequest> = emptyList(),
    val customerNote: String? = null,
    /** Client gửi lại cùng khoá khi retry để không tạo đơn trùng. */
    val idempotencyKey: String? = null
)

data class OrderItemDto(
    val orderItemId: Long,
    val ticketTypeId: Long,
    val ticketName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal
)

data class OrderDto(
    val orderId: Long,
    val orderCode: String,
    /** PENDING_PAYMENT | PAID | EXPIRED | CANCELLED | REFUND_PENDING | PARTIALLY_REFUNDED | REFUNDED */
    val status: String,
    val eventId: Long,
    val eventTitle: String,
    val eventStartAt: Instant?,
    val eventEndAt: Instant?,
    val eventCoverUrl: String?,
    val venueName: String?,
    val city: String?,
    val currency: String,
    val subtotalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val platformFeeAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paymentDueAt: Instant?,
    val paidAt: Instant?,
    val createdAt: Instant?,
    val customerNote: String?,
    val items: List<OrderItemDto>,
    /** Số vé đã phát hành cho đơn này (đơn miễn phí phát hành ngay). */
    val ticketsIssued: Int
)

// ---------- Thanh toán ----------

data class CreatePaymentRequest(
    /** BANK_TRANSFER_QR | WALLET | CARD | OTHER */
    val method: String? = null
)

data class PaymentDto(
    val paymentId: Long,
    val paymentReference: String,
    val orderId: Long,
    val orderCode: String,
    val providerCode: String,
    val method: String,
    val amount: BigDecimal,
    val currency: String,
    /** INITIATED | PENDING | SUCCESS | FAILED | REVERSED */
    val status: String,
    val qrPayload: String?,
    /** Nội dung chuyển khoản ngân hàng (trùng với orderCode). */
    val transferContent: String,
    val bankCode: String?,
    val bankAccount: String?,
    val expiresAt: Instant?,
    val initiatedAt: Instant?,
    val verifiedAt: Instant?
)

// ---------- Vé ----------

data class TicketDto(
    val ticketId: Long,
    val ticketCode: String,
    /** VALID | CHECKED_IN | CANCELLED | REFUNDED | REVOKED */
    val status: String,
    val eventId: Long,
    val eventTitle: String,
    val eventStartAt: Instant?,
    val eventEndAt: Instant?,
    val venueName: String?,
    val city: String?,
    val coverUrl: String?,
    val ticketTypeName: String,
    val issuedAt: Instant?,
    val checkedInAt: Instant?,
    /** Mã QR để soát vé; sinh lại được từ mã vé nên không cần lưu bản rõ. */
    val qrToken: String
)

// ---------- Đăng ký vé miễn phí ----------

data class CreateRegistrationRequest(
    val ticketTypeId: Long,
    val quantity: Int = 1,
    val idempotencyKey: String? = null
)

data class RegistrationDto(
    val registrationId: Long,
    val registrationCode: String,
    val eventId: Long,
    val eventTitle: String,
    /** CONFIRMED | CANCELLED | WAITLISTED */
    val status: String,
    val quantity: Int,
    val registeredAt: Instant?,
    val tickets: List<TicketDto>
)
