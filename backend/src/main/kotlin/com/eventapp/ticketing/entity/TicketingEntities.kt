package com.eventapp.ticketing.entity

import com.eventapp.common.enums.CheckinMethod
import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.PaymentMethod
import com.eventapp.common.enums.PaymentStatus
import com.eventapp.common.enums.PaymentVerificationSource
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.TicketStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "registrations")
class Registration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    var registrationId: Long? = null,

    @Column(name = "registration_code", nullable = false, unique = true)
    var registrationCode: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "ticket_type_id", nullable = false)
    var ticketTypeId: Long = 0L,

    @Column(name = "quantity", nullable = false)
    var quantity: Short = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: RegistrationStatus = RegistrationStatus.CONFIRMED,

    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String = "",

    @Column(name = "registered_at", insertable = false, updatable = false)
    var registeredAt: Instant? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "cancellation_reason")
    var cancellationReason: String? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "tickets")
class Ticket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    var ticketId: Long? = null,

    @Column(name = "ticket_code", nullable = false, unique = true)
    var ticketCode: String = "",

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "qr_token_hash", nullable = false, unique = true)
    var qrTokenHash: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "ticket_type_id", nullable = false)
    var ticketTypeId: Long = 0L,

    @Column(name = "registration_id")
    var registrationId: Long? = null,

    @Column(name = "order_item_id")
    var orderItemId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TicketStatus = TicketStatus.VALID,

    @Column(name = "issued_at", insertable = false, updatable = false)
    var issuedAt: Instant? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "check_ins")
class CheckIn(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_in_id")
    var checkInId: Long? = null,

    @Column(name = "ticket_id", nullable = false, unique = true)
    var ticketId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: Long = 0L,

    @Column(name = "event_qr_token_id")
    var eventQrTokenId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_method", nullable = false)
    var checkInMethod: CheckinMethod = CheckinMethod.SELF_SCAN,

    @Column(name = "checked_in_at", insertable = false, updatable = false)
    var checkedInAt: Instant? = null,

    @Column(name = "note")
    var note: String? = null
)

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    var orderId: Long? = null,

    @Column(name = "order_code", nullable = false, unique = true)
    var orderCode: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "coupon_id")
    var couponId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.PENDING_PAYMENT,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false)
    var currency: String = "VND",

    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "platform_fee_amount", nullable = false)
    var platformFeeAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_amount", nullable = false)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String = "",

    @Column(name = "payment_due_at")
    var paymentDueAt: Instant? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Column(name = "expired_at")
    var expiredAt: Instant? = null,

    @Column(name = "customer_note")
    var customerNote: String? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    var orderItemId: Long? = null,

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0L,

    @Column(name = "ticket_type_id", nullable = false)
    var ticketTypeId: Long = 0L,

    @Column(name = "ticket_name_snapshot", nullable = false)
    var ticketNameSnapshot: String = "",

    @Column(name = "quantity", nullable = false)
    var quantity: Short = 1,

    @Column(name = "unit_price", nullable = false)
    var unitPrice: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "line_total", nullable = false)
    var lineTotal: BigDecimal = BigDecimal.ZERO,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false)
    var currency: String = "VND",

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "payments")
class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    var paymentId: Long? = null,

    @Column(name = "payment_reference", nullable = false, unique = true)
    var paymentReference: String = "",

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0L,

    @Column(name = "provider_code", nullable = false)
    var providerCode: String = "MANUAL",

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    var paymentMethod: PaymentMethod = PaymentMethod.BANK_TRANSFER_QR,

    @Column(name = "amount", nullable = false)
    var amount: BigDecimal = BigDecimal.ZERO,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false)
    var currency: String = "VND",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus = PaymentStatus.INITIATED,

    @Column(name = "provider_transaction_id")
    var providerTransactionId: String? = null,

    @Column(name = "provider_reference")
    var providerReference: String? = null,

    @Column(name = "idempotency_key")
    var idempotencyKey: String? = null,

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    var qrPayload: String? = null,

    @Column(name = "failure_code")
    var failureCode: String? = null,

    @Column(name = "failure_message")
    var failureMessage: String? = null,

    @Column(name = "initiated_at", insertable = false, updatable = false)
    var initiatedAt: Instant? = null,

    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_source")
    var verificationSource: PaymentVerificationSource? = null,

    @Column(name = "verified_by_user_id")
    var verifiedByUserId: Long? = null,

    @Column(name = "verification_note")
    var verificationNote: String? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null
)
