package com.eventapp.ticketing.refund

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.PaymentStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.EventRepository
import com.eventapp.ticketing.EventBriefLoader
import com.eventapp.ticketing.TicketingIdentitySupport
import com.eventapp.ticketing.dto.*
import com.eventapp.ticketing.entity.Refund
import com.eventapp.ticketing.entity.RefundStatusHistory
import com.eventapp.ticketing.repository.*
import com.eventapp.ticketing.wallet.WalletService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class RefundService(
    private val refundRepository: RefundRepository,
    private val historyRepository: RefundStatusHistoryRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val paymentRepository: PaymentRepository,
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val walletService: WalletService,
    private val briefs: EventBriefLoader,
    private val identitySupport: TicketingIdentitySupport
) {
    @Transactional
    fun requestRefund(actorUserId: Long, request: CreateRefundRequest): RefundResponseDto {
        val reason = request.reason.trim()
        if (reason.isEmpty()) throw validationError("reason", "Vui lòng nhập lý do hoàn tiền")

        val order = orderRepository.findById(request.orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        val event = eventRepository.findById(order.eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")

        val isBuyer = order.userId == actorUserId
        val isOrganizerOwner = event.createdByUserId == actorUserId
        if (!isBuyer && !isOrganizerOwner && !identitySupport.isAdmin()) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không có quyền gửi yêu cầu hoàn tiền cho đơn này")
        }

        if (order.status != OrderStatus.PAID) {
            throw ConflictException(ErrorCode.CONFLICT, "Chỉ đơn hàng đã thanh toán mới được yêu cầu hoàn tiền")
        }
        if (order.totalAmount <= BigDecimal.ZERO) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn 0 đ không phát sinh hoàn tiền")
        }
        if (refundRepository.existsByOrderIdAndStatusIn(order.orderId!!, listOf("REQUESTED", "APPROVED", "PROCESSING", "SUCCEEDED"))) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn hàng này đã có yêu cầu hoàn tiền đang xử lý hoặc đã hoàn")
        }

        val successPaymentId = paymentRepository.findByOrderId(order.orderId!!)
            .lastOrNull { it.status == PaymentStatus.SUCCESS }
            ?.paymentId

        val now = Instant.now()
        val refundCode = "RFD-${UUID.randomUUID().toString().take(8).uppercase()}"

        val saved = refundRepository.save(
            Refund(
                refundCode = refundCode,
                orderId = order.orderId!!,
                paymentId = successPaymentId,
                requestedByUserId = order.userId,
                amount = order.totalAmount,
                currency = order.currency.trim(),
                reason = reason,
                status = "REQUESTED",
                requestedAt = now
            )
        )
        historyRepository.save(
            RefundStatusHistory(
                refundId = saved.refundId!!,
                oldStatus = null,
                newStatus = "REQUESTED",
                note = reason,
                actorUserId = actorUserId
            )
        )
        order.status = OrderStatus.REFUND_PENDING
        orderRepository.save(order)

        return toDtos(listOf(saved)).first()
    }

    @Transactional(readOnly = true)
    fun listForCaller(userId: Long): List<RefundResponseDto> {
        val list = if (identitySupport.isAdmin()) {
            refundRepository.findAllByOrderByRequestedAtDesc()
        } else {
            refundRepository.findByRequestedByUserIdOrderByRequestedAtDesc(userId)
        }
        return toDtos(list)
    }

    @Transactional(readOnly = true)
    fun listForOrganizer(organizerUserId: Long, eventId: Long?): List<RefundResponseDto> {
        val eventIds = if (eventId != null) {
            val event = eventRepository.findById(eventId).orElse(null)
                ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
            if (event.createdByUserId != organizerUserId && !identitySupport.isAdmin()) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không phải chủ sở hữu sự kiện này")
            }
            listOf(eventId)
        } else {
            eventRepository
                .findByCreatedByUserIdAndDeletedAtIsNullOrderByStartAtDesc(organizerUserId)
                .mapNotNull { it.eventId }
        }
        if (eventIds.isEmpty()) return emptyList()
        val orderIds = orderRepository.findByEventIdInOrderByCreatedAtDesc(eventIds).mapNotNull { it.orderId }
        if (orderIds.isEmpty()) return emptyList()
        return toDtos(refundRepository.findByOrderIdInOrderByRequestedAtDesc(orderIds))
    }

    /** FR-A12: Admin duyệt -> tự động cộng tiền về Ví ETicket người mua & thu hồi vé. */
    @Transactional
    fun process(adminUserId: Long, refundId: Long, request: ProcessRefundRequest): RefundResponseDto {
        identitySupport.requireAdmin()

        val refund = refundRepository.findById(refundId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy yêu cầu hoàn tiền")
        val previousStatus = refund.status
        if (previousStatus != "REQUESTED" && previousStatus != "PROCESSING") {
            throw ConflictException(ErrorCode.CONFLICT, "Yêu cầu hoàn tiền đã được xử lý ($previousStatus)")
        }
        val order = orderRepository.findById(refund.orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng liên quan")

        val now = Instant.now()
        if (request.approved) {
            val walletTx = walletService.creditRefund(
                buyerUserId = refund.requestedByUserId,
                adminUserId = adminUserId,
                orderId = order.orderId!!,
                orderCode = order.orderCode,
                amount = refund.amount,
                note = "Hoàn tiền đơn ${order.orderCode} về Ví ETicket"
            )
            refund.status = "SUCCEEDED"
            refund.approvedByUserId = adminUserId
            refund.processedAt = now
            refund.providerRefundId = refund.refundCode
            refund.providerRefundResult = "WALLET_CREDITED"
            refund.walletTransactionId = walletTx.walletTransactionId
            refund.walletBalanceAfter = walletTx.balanceAfter

            order.status = OrderStatus.REFUNDED
            orderRepository.save(order)

            val itemIds = orderItemRepository.findByOrderId(order.orderId!!).mapNotNull { it.orderItemId }
            if (itemIds.isNotEmpty()) {
                val tickets = ticketRepository.findByOrderItemIdIn(itemIds)
                tickets.forEach { ticket ->
                    if (ticket.status == TicketStatus.VALID) {
                        ticket.status = TicketStatus.REFUNDED
                        ticket.revokedAt = now
                    }
                }
                ticketRepository.saveAll(tickets)
            }

            historyRepository.save(
                RefundStatusHistory(
                    refundId = refund.refundId!!,
                    oldStatus = previousStatus,
                    newStatus = "SUCCEEDED",
                    note = "Đã hoàn ${refund.amount.stripTrailingZeros().toPlainString()} đ về Ví ETicket",
                    actorUserId = adminUserId
                )
            )
        } else {
            val rejectReason = request.rejectReason?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw validationError("rejectReason", "Vui lòng nhập lý do từ chối")
            refund.status = "REJECTED"
            refund.failureReason = rejectReason
            refund.approvedByUserId = adminUserId
            refund.processedAt = now

            if (order.status == OrderStatus.REFUND_PENDING) {
                order.status = OrderStatus.PAID
                orderRepository.save(order)
            }

            historyRepository.save(
                RefundStatusHistory(
                    refundId = refund.refundId!!,
                    oldStatus = previousStatus,
                    newStatus = "REJECTED",
                    note = rejectReason,
                    actorUserId = adminUserId
                )
            )
        }

        val saved = refundRepository.save(refund)
        return toDtos(listOf(saved)).first()
    }

    private fun toDtos(refunds: List<Refund>): List<RefundResponseDto> {
        if (refunds.isEmpty()) return emptyList()
        val orderMap = orderRepository.findAllById(refunds.map { it.orderId }.distinct())
            .associateBy { it.orderId!! }
        val eventIds = orderMap.values.map { it.eventId }.distinct()
        val briefMap = briefs.load(eventIds)
        val userMap = identitySupport.loadUsers(refunds.map { it.requestedByUserId }.distinct())

        return refunds.map { r ->
            val order = orderMap[r.orderId]
            val eventId = order?.eventId ?: 0L
            RefundResponseDto(
                refundId = r.refundId!!,
                refundCode = r.refundCode,
                orderId = r.orderId,
                orderCode = order?.orderCode ?: "#${r.orderId}",
                eventTitle = briefMap[eventId]?.title ?: "Sự kiện #$eventId",
                eventId = eventId,
                userId = r.requestedByUserId,
                buyerName = userMap[r.requestedByUserId]?.fullName,
                amount = r.amount,
                reason = r.reason,
                status = r.status,
                rejectReason = r.failureReason,
                processedBy = r.approvedByUserId,
                walletBalanceAfter = r.walletBalanceAfter,
                createdAt = r.requestedAt,
                processedAt = r.processedAt
            )
        }
    }
}