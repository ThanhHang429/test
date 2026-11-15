package com.eventapp.ticketing.promotion

import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.ticketing.dto.*
import com.eventapp.ticketing.entity.Coupon
import com.eventapp.ticketing.repository.CouponRedemptionRepository
import com.eventapp.ticketing.repository.CouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Service
class PromotionService(
    private val couponRepository: CouponRepository,
    private val couponRedemptionRepository: CouponRedemptionRepository,
    private val eventRepository: EventRepository
) {
    @Transactional(readOnly = true)
    fun previewApply(userId: Long, request: ApplyCouponRequest): CouponResponseDto {
        val code = request.code.trim().uppercase()
        if (code.isEmpty()) throw validationError("code", "Vui lòng nhập mã giảm giá")
        val coupon = validateAndGetCoupon(userId, code, request.eventId, request.subtotalAmount, Instant.now())
        val discount = computeDiscount(coupon, request.subtotalAmount)
        val finalAmount = request.subtotalAmount.subtract(discount).max(BigDecimal.ZERO)
        return CouponResponseDto(
            couponId = coupon.couponId!!,
            code = coupon.couponCode,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            discountAmount = discount,
            finalAmount = finalAmount,
            description = coupon.description
        )
    }

    fun validateAndGetCoupon(
        userId: Long,
        rawCode: String,
        eventId: Long,
        subtotalAmount: BigDecimal,
        now: Instant
    ): Coupon {
        val coupon = couponRepository.findByCouponCodeIgnoreCase(rawCode.trim())
            ?: throw NotFoundException(message = "Mã giảm giá \"$rawCode\" không tồn tại")

        if (coupon.eventId != eventId) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá không áp dụng cho sự kiện này")
        }
        if (!coupon.status.equals("ACTIVE", ignoreCase = true)) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá hiện đang tạm dừng hoặc đã hết hiệu lực")
        }
        if (coupon.startsAt != null && now.isBefore(coupon.startsAt)) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá chưa đến thời gian áp dụng")
        }
        if (coupon.endsAt != null && now.isAfter(coupon.endsAt)) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá đã hết hạn sử dụng")
        }
        if (coupon.maxTotalUses != null && coupon.usedCount >= coupon.maxTotalUses!!) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá đã hết lượt sử dụng")
        }
        if (coupon.maxUsesPerUser != null) {
            val usedByUser = couponRedemptionRepository.countByCouponIdAndUserId(coupon.couponId!!, userId)
            if (usedByUser >= coupon.maxUsesPerUser!!) {
                throw ConflictException(ErrorCode.CONFLICT, "Bạn đã dùng hết số lượt cho phép của mã này")
            }
        }
        if (coupon.minimumOrderAmount != null && subtotalAmount < coupon.minimumOrderAmount!!) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Đơn tối thiểu ${coupon.minimumOrderAmount!!.stripTrailingZeros().toPlainString()} đ mới dùng được mã này"
            )
        }
        return coupon
    }

    fun computeDiscount(coupon: Coupon, subtotalAmount: BigDecimal): BigDecimal {
        if (subtotalAmount <= BigDecimal.ZERO) return BigDecimal.ZERO
        var discount = when (coupon.discountType.uppercase()) {
            "PERCENT" -> subtotalAmount
                .multiply(coupon.discountValue)
                .divide(BigDecimal("100"), 0, RoundingMode.HALF_UP)
            else -> coupon.discountValue
        }
        if (coupon.maxDiscountAmount != null && discount > coupon.maxDiscountAmount!!) {
            discount = coupon.maxDiscountAmount!!
        }
        return discount.min(subtotalAmount).max(BigDecimal.ZERO)
    }

    @Transactional(readOnly = true)
    fun listEventCoupons(userId: Long, eventId: Long): List<OrganizerCouponDto> {
        requireEventOwner(userId, eventId)
        return couponRepository.findByEventIdOrderByCreatedAtDesc(eventId).map { toOrganizerDto(it) }
    }

    @Transactional
    fun createCoupon(userId: Long, eventId: Long, request: CreateCouponRequest): OrganizerCouponDto {
        val event = requireEventOwner(userId, eventId)
        val code = request.code.trim().uppercase()
        if (code.length < 3) throw validationError("code", "Mã giảm giá phải có ít nhất 3 ký tự")
        if (couponRepository.findByCouponCodeIgnoreCase(code) != null) {
            throw ConflictException(ErrorCode.CONFLICT, "Mã giảm giá $code đã tồn tại")
        }
        val rawType = request.discountType.trim().uppercase()
        val type = if (rawType == "FIXED_AMOUNT") "FIXED" else rawType
        if (type != "PERCENT" && type != "FIXED") {
            throw validationError("discountType", "Loại giảm giá chỉ hỗ trợ PERCENT hoặc FIXED")
        }
        if (request.discountValue <= BigDecimal.ZERO) {
            throw validationError("discountValue", "Giá trị giảm phải lớn hơn 0")
        }
        if (type == "PERCENT" && request.discountValue > BigDecimal("100")) {
            throw validationError("discountValue", "Giảm theo phần trăm tối đa 100%")
        }

        val saved = couponRepository.save(
            Coupon(
                eventId = eventId,
                organizerProfileId = event.organizerProfileId,
                couponCode = code,
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                discountType = type,
                discountValue = request.discountValue,
                currency = "VND",
                minimumOrderAmount = request.minOrderAmount,
                maxDiscountAmount = request.maxDiscountAmount,
                maxTotalUses = request.usageLimit?.takeIf { it > 0 },
                maxUsesPerUser = request.maxUsesPerUser?.takeIf { it > 0 },
                usedCount = 0,
                status = "ACTIVE",
                startsAt = request.startsAt,
                endsAt = request.endsAt,
                createdByUserId = userId,
                updatedAt = Instant.now()
            )
        )
        return toOrganizerDto(saved)
    }

    @Transactional
    fun updateStatus(userId: Long, couponId: Long, request: UpdateCouponStatusRequest): OrganizerCouponDto {
        val coupon = couponRepository.findById(couponId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy mã giảm giá")
        requireEventOwner(userId, coupon.eventId)
        val next = request.status.trim().uppercase()
        if (next !in setOf("ACTIVE", "PAUSED", "EXPIRED", "ARCHIVED")) {
            throw validationError("status", "Trạng thái phải là ACTIVE, PAUSED, EXPIRED hoặc ARCHIVED")
        }
        coupon.status = next
        coupon.updatedAt = Instant.now()
        return toOrganizerDto(couponRepository.save(coupon))
    }

    private fun requireEventOwner(userId: Long, eventId: Long): Event {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.createdByUserId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không phải chủ sở hữu sự kiện này")
        }
        return event
    }

    private fun toOrganizerDto(c: Coupon): OrganizerCouponDto = OrganizerCouponDto(
        couponId = c.couponId!!,
        eventId = c.eventId,
        code = c.couponCode,
        description = c.description,
        discountType = c.discountType,
        discountValue = c.discountValue,
        minOrderAmount = c.minimumOrderAmount,
        maxDiscountAmount = c.maxDiscountAmount,
        usageLimit = c.maxTotalUses,
        usedCount = c.usedCount,
        status = c.status,
        startsAt = c.startsAt,
        endsAt = c.endsAt,
        createdAt = c.createdAt
    )
}