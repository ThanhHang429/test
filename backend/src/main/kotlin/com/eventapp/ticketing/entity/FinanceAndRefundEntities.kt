package com.eventapp.ticketing.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

@Entity(name = "TicketingPlatformSetting")
@Table(name = "platform_settings")
class PlatformSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_setting_id")
    var platformSettingId: Long? = null,

    @Column(name = "setting_key", nullable = false, length = 100)
    var settingKey: String = "",

    @Column(name = "version_no", nullable = false)
    var versionNo: Int = 1,

    @Column(name = "setting_value", nullable = false, columnDefinition = "json")
    var settingValue: String = "\"5.00\"",

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: Instant = Instant.now(),

    @Column(name = "changed_by_user_id")
    var changedByUserId: Long? = null,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)

@Entity
@Table(name = "coupons")
class Coupon(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    var couponId: Long? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: Long = 0L,

    @Column(name = "organizer_profile_id", nullable = false)
    var organizerProfileId: Long = 0L,

    @Column(name = "coupon_code", nullable = false, unique = true, length = 50)
    var couponCode: String = "",

    @Column(name = "description", length = 500)
    var description: String? = null,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "discount_type", nullable = false, length = 20)
    var discountType: String = "PERCENT",

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    var discountValue: BigDecimal = BigDecimal.ZERO,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "VND",

    @Column(name = "minimum_order_amount", precision = 14, scale = 2)
    var minimumOrderAmount: BigDecimal? = null,

    @Column(name = "max_discount_amount", precision = 14, scale = 2)
    var maxDiscountAmount: BigDecimal? = null,

    @Column(name = "max_total_uses")
    var maxTotalUses: Int? = null,

    @Column(name = "max_uses_per_user")
    var maxUsesPerUser: Int? = null,

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0,

    @Column(name = "starts_at")
    var startsAt: Instant? = null,

    @Column(name = "ends_at")
    var endsAt: Instant? = null,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE",

    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: Long = 0L,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
)

@Entity
@Table(name = "coupon_redemptions")
class CouponRedemption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_redemption_id")
    var couponRedemptionId: Long? = null,

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = 0L,

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0L,

    @Column(name = "order_id", nullable = false, unique = true)
    var orderId: Long = 0L,

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "redeemed_at", insertable = false, updatable = false)
    var redeemedAt: Instant? = null
)

@Entity
@Table(name = "refunds")
class Refund(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    var refundId: Long? = null,

    @Column(name = "refund_code", nullable = false, unique = true, length = 40)
    var refundCode: String = "",

    @Column(name = "order_id", nullable = false)
    var orderId: Long = 0L,

    @Column(name = "payment_id")
    var paymentId: Long? = null,

    @Column(name = "requested_by_user_id", nullable = false)
    var requestedByUserId: Long = 0L,

    @Column(name = "approved_by_user_id")
    var approvedByUserId: Long? = null,

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "VND",

    @Column(name = "reason", nullable = false, length = 500)
    var reason: String = "",

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", nullable = false, length = 30)
    var status: String = "REQUESTED",

    @Column(name = "provider_refund_id", length = 100)
    var providerRefundId: String? = null,

    @Column(name = "provider_refund_result", length = 255)
    var providerRefundResult: String? = null,

    @Column(name = "wallet_transaction_id")
    var walletTransactionId: Long? = null,

    @Column(name = "wallet_balance_after", precision = 14, scale = 2)
    var walletBalanceAfter: BigDecimal? = null,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = Instant.now(),

    @Column(name = "processed_at")
    var processedAt: Instant? = null,

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null
)

@Entity
@Table(name = "refund_status_history")
class RefundStatusHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_status_history_id")
    var refundStatusHistoryId: Long? = null,

    @Column(name = "refund_id", nullable = false)
    var refundId: Long = 0L,

    @Column(name = "old_status", length = 30)
    var oldStatus: String? = null,

    @Column(name = "new_status", nullable = false, length = 30)
    var newStatus: String = "",

    @Column(name = "note", length = 500)
    var note: String? = null,

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: Long = 0L,

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null
)