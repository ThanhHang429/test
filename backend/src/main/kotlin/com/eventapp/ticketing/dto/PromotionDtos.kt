package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

data class ApplyCouponRequest(
    val code: String,
    val eventId: Long,
    val subtotalAmount: BigDecimal
)

data class CouponResponseDto(
    val couponId: Long,
    val code: String,
    val discountType: String,
    val discountValue: BigDecimal,
    val discountAmount: BigDecimal,
    val finalAmount: BigDecimal,
    val description: String?
)

data class OrganizerCouponDto(
    val couponId: Long,
    val eventId: Long,
    val code: String,
    val description: String?,
    val discountType: String,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal?,
    val maxDiscountAmount: BigDecimal?,
    val usageLimit: Int?,
    val usedCount: Int,
    val status: String,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val createdAt: Instant?
)

data class CreateCouponRequest(
    val code: String,
    val description: String? = null,
    val discountType: String = "PERCENT",
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal? = null,
    val maxDiscountAmount: BigDecimal? = null,
    val usageLimit: Int? = null,
    val maxUsesPerUser: Int? = null,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null
)

data class UpdateCouponStatusRequest(
    val status: String
)