package com.eventapp.ticketing.repository

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.ticketing.entity.CheckIn
import com.eventapp.ticketing.entity.Order
import com.eventapp.ticketing.entity.OrderItem
import com.eventapp.ticketing.entity.Payment
import com.eventapp.ticketing.entity.Registration
import com.eventapp.ticketing.entity.Ticket
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
}

@Repository
interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByUserIdAndEventIdIn(userId: Long, eventIds: Collection<Long>): List<Ticket>
    fun findByEventIdAndStatus(eventId: Long, status: TicketStatus): List<Ticket>
    fun countByEventIdAndStatus(eventId: Long, status: TicketStatus): Long

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
}

@Repository
interface OrderRepository : JpaRepository<Order, Long> {
    fun findByEventIdAndStatus(eventId: Long, status: OrderStatus): List<Order>
    fun findByEventId(eventId: Long): List<Order>

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.eventId = :eventId and o.status = :status")
    fun sumTotalByEventAndStatus(eventId: Long, status: OrderStatus): java.math.BigDecimal

    @Query("select count(o) from Order o where o.eventId = :eventId and o.status = :status")
    fun countByEventAndStatus(eventId: Long, status: OrderStatus): Long
}

@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {
    fun findByOrderId(orderId: Long): List<OrderItem>
}

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findByOrderId(orderId: Long): List<Payment>

    @Query(
        """
        select coalesce(sum(p.amount), 0) from Payment p, Order o
        where p.orderId = o.orderId and o.eventId = :eventId and p.status = 'SUCCESS'
        """
    )
    fun sumVerifiedAmountByEvent(eventId: Long): java.math.BigDecimal
}
