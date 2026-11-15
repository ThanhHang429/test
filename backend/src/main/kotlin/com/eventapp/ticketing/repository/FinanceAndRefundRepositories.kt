package com.eventapp.ticketing.repository

import com.eventapp.ticketing.entity.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository("ticketingPlatformSettingRepository")
interface PlatformSettingRepository : JpaRepository<PlatformSetting, Long> {
    fun findFirstBySettingKeyOrderByVersionNoDesc(settingKey: String): PlatformSetting?
    fun findFirstBySettingKeyAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionNoDesc(
        settingKey: String,
        effectiveFrom: Instant
    ): PlatformSetting?
}

@Repository
interface CouponRepository : JpaRepository<Coupon, Long> {
    fun findByCouponCodeIgnoreCase(couponCode: String): Coupon?
    fun findByEventIdOrderByCreatedAtDesc(eventId: Long): List<Coupon>

    @Modifying
    @Query(
        """
        update Coupon c
           set c.usedCount = c.usedCount + 1
         where c.couponId = :couponId
           and (c.maxTotalUses is null or c.usedCount < c.maxTotalUses)
        """
    )
    fun incrementUsageIfAvailable(couponId: Long): Int
}

@Repository
interface CouponRedemptionRepository : JpaRepository<CouponRedemption, Long> {
    fun existsByOrderId(orderId: Long): Boolean
    fun countByCouponIdAndUserId(couponId: Long, userId: Long): Long
}

@Repository
interface RefundRepository : JpaRepository<Refund, Long> {
    fun existsByOrderIdAndStatusIn(orderId: Long, statuses: Collection<String>): Boolean
    fun findAllByOrderByRequestedAtDesc(): List<Refund>
    fun findByRequestedByUserIdOrderByRequestedAtDesc(userId: Long): List<Refund>
    fun findByOrderIdInOrderByRequestedAtDesc(orderIds: Collection<Long>): List<Refund>

    @Query(
        """
        select coalesce(sum(r.amount), 0)
          from Refund r, Order o
         where r.orderId = o.orderId
           and o.eventId = :eventId
           and r.status in ('APPROVED', 'SUCCEEDED')
        """
    )
    fun sumApprovedAmountByEventId(eventId: Long): BigDecimal

    @Query(
        """
        select count(r)
          from Refund r, Order o
         where r.orderId = o.orderId
           and o.eventId = :eventId
           and r.status in ('REQUESTED', 'PROCESSING')
        """
    )
    fun countPendingByEventId(eventId: Long): Long
}

@Repository
interface RefundStatusHistoryRepository : JpaRepository<RefundStatusHistory, Long> {
    fun findByRefundIdOrderByCreatedAtAsc(refundId: Long): List<RefundStatusHistory>
}