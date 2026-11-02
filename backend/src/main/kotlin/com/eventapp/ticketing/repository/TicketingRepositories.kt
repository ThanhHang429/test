package com.eventapp.ticketing.repository

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.PaymentStatus
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.ticketing.entity.CheckIn
import com.eventapp.ticketing.entity.Order
import com.eventapp.ticketing.entity.OrderItem
import com.eventapp.ticketing.entity.Payment
import com.eventapp.ticketing.entity.Registration
import com.eventapp.ticketing.entity.Ticket
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RegistrationRepository : JpaRepository<Registration, Long> {
    fun findByUserIdAndEventId(userId: Long, eventId: Long): Registration?
    fun findByUserIdAndEventIdIn(userId: Long, eventIds: Collection<Long>): List<Registration>
    fun findByUserIdOrderByRegisteredAtDesc(userId: Long): List<Registration>
    fun countByEventIdAndStatus(eventId: Long, status: RegistrationStatus): Long
    fun findByEventIdOrderByRegisteredAtDesc(eventId: Long): List<Registration>

    /** Chống tạo trùng khi client retry cùng một yêu cầu đăng ký. */
    fun findFirstByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): Registration?
}

@Repository
interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByUserIdAndEventIdIn(userId: Long, eventIds: Collection<Long>): List<Ticket>
    fun findByEventIdAndStatus(eventId: Long, status: TicketStatus): List<Ticket>
    fun countByEventIdAndStatus(eventId: Long, status: TicketStatus): Long
    fun findByOrderItemIdIn(orderItemIds: Collection<Long>): List<Ticket>
    fun findByRegistrationIdIn(registrationIds: Collection<Long>): List<Ticket>
    fun countByOrderItemIdIn(orderItemIds: Collection<Long>): Long
    fun countByUserIdAndTicketTypeIdAndStatusNot(
        userId: Long,
        ticketTypeId: Long,
        status: TicketStatus
    ): Long
    fun findByUserIdOrderByIssuedAtDesc(userId: Long, pageable: Pageable): Page<Ticket>
    fun findByUserIdAndStatusOrderByIssuedAtDesc(
        userId: Long,
        status: TicketStatus,
        pageable: Pageable
    ): Page<Ticket>

    @Query(
        """
        select count(t) from Ticket t
        where t.userId = :userId and t.eventId = :eventId and t.status = 'CHECKED_IN'
        """
    )
    fun countCheckedIn(userId: Long, eventId: Long): Long
}

@Repository
interface CheckInRepository : JpaRepository<CheckIn, Long> {
    fun countByEventId(eventId: Long): Long
    fun findByEventIdOrderByCheckedInAtDesc(eventId: Long): List<CheckIn>
    fun findByTicketIdIn(ticketIds: Collection<Long>): List<CheckIn>
}

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByEventIdAndStatus(eventId: Long, status: OrderStatus): List<Order>
    fun findByEventId(eventId: Long): List<Order>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Order>
    fun findByUserIdAndStatusOrderByCreatedAtDesc(
        userId: Long,
        status: OrderStatus,
        pageable: Pageable
    ): Page<Order>

    /** Chống tạo trùng khi client retry cùng một yêu cầu đặt vé. */
    fun findFirstByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): Order?

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.eventId = :eventId and o.status = :status")
    fun sumTotalByEventAndStatus(eventId: Long, status: OrderStatus): java.math.BigDecimal

    @Query("select count(o) from Order o where o.eventId = :eventId and o.status = :status")
    fun countByEventAndStatus(eventId: Long, status: OrderStatus): Long
}

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findByOrderId(orderId: Long): List<OrderItem>
    fun findByOrderIdIn(orderIds: Collection<Long>): List<OrderItem>
}

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): List<Payment>
    fun findFirstByOrderIdAndStatusIn(orderId: Long, statuses: Collection<PaymentStatus>): Payment?

    @Query(
        """
        select coalesce(sum(p.amount), 0) from Payment p, Order o
        where p.orderId = o.orderId and o.eventId = :eventId and p.status = 'SUCCESS'
        """
    )
    fun sumVerifiedAmountByEvent(eventId: Long): java.math.BigDecimal
}
