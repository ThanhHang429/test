package com.eventapp.ticketing.order

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.TicketType
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.EventBriefLoader
import com.eventapp.ticketing.TicketIssuer
import com.eventapp.ticketing.TicketingCodes
import com.eventapp.ticketing.TicketingRules
import com.eventapp.ticketing.dto.CreateOrderRequest
import com.eventapp.ticketing.dto.OrderDto
import com.eventapp.ticketing.dto.OrderItemDto
import com.eventapp.ticketing.entity.Order
import com.eventapp.ticketing.entity.OrderItem
import com.eventapp.ticketing.repository.OrderItemRepository
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * FR-U10 — tạo và quản lý đơn vé.
 *
 * Nguyên tắc:
 *  - Giữ chỗ bằng `ticket_types.quantity_reserved` trong transaction có khoá ghi, nên hai người
 *    mua cùng lúc không thể bán vượt tồn kho.
 *  - Đơn chỉ chuyển PAID khi thanh toán được xác nhận; vé chỉ phát hành sau đó (BR-017).
 *  - `idempotencyKey` do client gửi lại khi retry để không tạo đơn trùng.
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val eventRepository: EventRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val ticketRepository: TicketRepository,
    private val codes: TicketingCodes,
    private val issuer: TicketIssuer,
    private val briefs: EventBriefLoader,
    @Value("\${app.ticketing.payment-window-minutes:15}") private val paymentWindowMinutes: Long
) {

    @Transactional
    fun create(userId: Long, request: CreateOrderRequest): OrderDto {
        if (request.items.isEmpty()) throw validationError("items", "Đơn phải có ít nhất một loại vé")
        if (request.eventId <= 0) throw validationError("eventId", "Thiếu sự kiện")

        val key = request.idempotencyKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            orderRepository.findFirstByUserIdAndIdempotencyKey(userId, key)?.let { return toDto(it) }
        }

        val now = Instant.now()
        val event = TicketingRules.requireOrderable(
            eventRepository.findById(request.eventId).orElse(null), now
        )

        // Gộp các dòng trùng loại vé để kiểm tra số lượng một lần.
        val requested = LinkedHashMap<Long, Int>()
        for (item in request.items) {
            if (item.quantity <= 0) throw validationError("items", "Số lượng vé phải lớn hơn 0")
            requested.merge(item.ticketTypeId, item.quantity, Int::plus)
        }

        val types = ticketTypeRepository.findByTicketTypeIdIn(requested.keys)
        if (types.size != requested.size) {
            throw validationError("items", "Có loại vé không tồn tại")
        }
        for (type in types) {
            val quantity = requested[type.ticketTypeId]!!
            if (type.eventId != event.eventId) {
                throw validationError("items", "Loại vé \"${type.ticketTypeName}\" không thuộc sự kiện này")
            }
            if (quantity < type.minPerOrder) {
                throw validationError("items", "Loại vé \"${type.ticketTypeName}\" cần ít nhất ${type.minPerOrder} vé")
            }
            if (quantity > type.maxPerUser) {
                throw validationError("items", "Loại vé \"${type.ticketTypeName}\" tối đa ${type.maxPerUser} vé mỗi người")
            }
            TicketingRules.requireOnSale(type, now)
            val remaining = type.remaining()
            if (remaining != null && remaining < quantity) {
                throw ConflictException(
                    ErrorCode.TICKET_SOLD_OUT,
                    "Loại vé \"${type.ticketTypeName}\" chỉ còn ${remaining.coerceAtLeast(0)} vé"
                )
            }
            val owned = ticketRepository.countByUserIdAndTicketTypeIdAndStatusNot(
                userId, type.ticketTypeId!!, TicketStatus.CANCELLED
            )
            if (owned + quantity > type.maxPerUser) {
                throw ConflictException(
                    ErrorCode.CONFLICT,
                    "Bạn đã có $owned vé \"${type.ticketTypeName}\", vượt giới hạn ${type.maxPerUser} vé mỗi người"
                )
            }
        }

        val ordered = types.sortedBy { it.price }
        val order = orderRepository.save(
            Order(
                orderCode = codes.newOrderCode(now),
                userId = userId,
                eventId = event.eventId!!,
                status = OrderStatus.PENDING_PAYMENT,
                currency = ordered.first().currency.trim(),
                subtotalAmount = ordered.sumOf { it.price * BigDecimal(requested[it.ticketTypeId]!!) },
                discountAmount = BigDecimal.ZERO,
                platformFeeAmount = BigDecimal.ZERO,
                totalAmount = ordered.sumOf { it.price * BigDecimal(requested[it.ticketTypeId]!!) },
                idempotencyKey = key ?: codes.newOrderCode(now),
                paymentDueAt = now.plus(paymentWindowMinutes, ChronoUnit.MINUTES),
                customerNote = request.customerNote?.trim()?.takeIf { it.isNotEmpty() }
            )
        )

        val items = ordered.map { type ->
            val quantity = requested[type.ticketTypeId]!!
            type.quantityReserved += quantity
            orderItemRepository.save(
                OrderItem(
                    orderId = order.orderId!!,
                    ticketTypeId = type.ticketTypeId!!,
                    ticketNameSnapshot = type.ticketTypeName,
                    quantity = quantity.toShort(),
                    unitPrice = type.price,
                    discountAmount = BigDecimal.ZERO,
                    lineTotal = type.price * BigDecimal(quantity),
                    currency = type.currency.trim()
                )
            )
        }

        // Vé miễn phí: không có bước thanh toán nên phát hành ngay trong cùng transaction.
        if (order.totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            markPaid(order, ordered, items, now)
        }
        return toDto(order)
    }

    /** Chuyển đơn sang PAID, chốt tồn kho và phát hành vé. Dùng chung cho đơn 0đ và thanh toán. */
    @Transactional
    fun markPaid(order: Order, types: List<TicketType>, items: List<OrderItem>, now: Instant) {
        if (order.status == OrderStatus.PAID) return
        val byTypeId = types.associateBy { it.ticketTypeId }
        for (item in items) {
            val type = byTypeId[item.ticketTypeId] ?: ticketTypeRepository.findById(item.ticketTypeId).orElse(null)
                ?: continue
            type.quantityReserved = (type.quantityReserved - item.quantity).coerceAtLeast(0)
            type.quantitySold += item.quantity
            issuer.issue(
                userId = order.userId,
                eventId = order.eventId,
                ticketTypeId = item.ticketTypeId,
                quantity = item.quantity.toInt(),
                orderItemId = item.orderItemId
            )
        }
        order.status = OrderStatus.PAID
        order.paidAt = now
    }

    @Transactional
    fun get(userId: Long, orderId: Long): OrderDto {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        if (order.userId != userId) throw ForbiddenException(ErrorCode.FORBIDDEN, "Đơn hàng không thuộc về bạn")
        expireIfDue(order)
        return toDto(order)
    }

    @Transactional
    fun cancel(userId: Long, orderId: Long): OrderDto {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        if (order.userId != userId) throw ForbiddenException(ErrorCode.FORBIDDEN, "Đơn hàng không thuộc về bạn")
        when (order.status) {
            OrderStatus.CANCELLED -> return toDto(order)
            OrderStatus.PENDING_PAYMENT -> Unit
            OrderStatus.EXPIRED -> return toDto(order)
            else -> throw ConflictException(
                ErrorCode.CONFLICT, "Đơn đã thanh toán, không thể huỷ. Vui lòng liên hệ ban tổ chức."
            )
        }
        releaseReservation(order)
        order.status = OrderStatus.CANCELLED
        order.cancelledAt = Instant.now()
        return toDto(order)
    }

    @Transactional
    fun list(userId: Long, status: String?, page: Int, size: Int): Pair<List<OrderDto>, Long> {
        val pageable = PageRequest.of(page, size)
        val normalized = status?.trim()?.uppercase()?.takeIf { it.isNotEmpty() && it != "ALL" }
        val result = if (normalized == null) {
            orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        } else {
            val parsed = runCatching { OrderStatus.valueOf(normalized) }.getOrNull()
                ?: throw validationError("status", "Trạng thái đơn không hợp lệ")
            orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, parsed, pageable)
        }
        val orders = result.content
        orders.forEach { expireIfDue(it) }
        return toDtos(orders) to result.totalElements
    }

    /** Đơn quá hạn giữ chỗ: trả tồn kho và đánh dấu EXPIRED ngay khi có người đọc. */
    @Transactional
    fun expireIfDue(order: Order, now: Instant = Instant.now()) {
        if (order.status != OrderStatus.PENDING_PAYMENT) return
        val due = order.paymentDueAt ?: return
        if (due.isAfter(now)) return
        releaseReservation(order)
        order.status = OrderStatus.EXPIRED
        order.expiredAt = now
    }

    private fun releaseReservation(order: Order) {
        for (item in orderItemRepository.findByOrderId(order.orderId!!)) {
            val type = ticketTypeRepository.findById(item.ticketTypeId).orElse(null) ?: continue
            type.quantityReserved = (type.quantityReserved - item.quantity).coerceAtLeast(0)
        }
    }

    fun toDto(order: Order): OrderDto = toDtos(listOf(order)).first()

    fun toDtos(orders: List<Order>): List<OrderDto> {
        if (orders.isEmpty()) return emptyList()
        val ids = orders.mapNotNull { it.orderId }
        val items = orderItemRepository.findByOrderIdIn(ids).groupBy { it.orderId }
        val itemIds = items.values.flatten().mapNotNull { it.orderItemId }
        val issued = if (itemIds.isEmpty()) emptyMap()
        else ticketRepository.findByOrderItemIdIn(itemIds)
            .groupBy { it.orderItemId }
            .mapValues { it.value.size }
        val briefsById = briefs.load(orders.map { it.eventId }.distinct())

        return orders.map { order ->
            val brief = briefsById[order.eventId]
            val orderItems = items[order.orderId] ?: emptyList()
            OrderDto(
                orderId = order.orderId!!,
                orderCode = order.orderCode,
                status = order.status.name,
                eventId = order.eventId,
                eventTitle = brief?.title ?: "",
                eventStartAt = brief?.startAt,
                eventEndAt = brief?.endAt,
                eventCoverUrl = brief?.coverUrl,
                venueName = brief?.venueName,
                city = brief?.city,
                currency = order.currency.trim(),
                subtotalAmount = order.subtotalAmount,
                discountAmount = order.discountAmount,
                platformFeeAmount = order.platformFeeAmount,
                totalAmount = order.totalAmount,
                paymentDueAt = order.paymentDueAt,
                paidAt = order.paidAt,
                createdAt = order.createdAt,
                customerNote = order.customerNote,
                items = orderItems.map { item ->
                    OrderItemDto(
                        orderItemId = item.orderItemId!!,
                        ticketTypeId = item.ticketTypeId,
                        ticketName = item.ticketNameSnapshot,
                        quantity = item.quantity.toInt(),
                        unitPrice = item.unitPrice,
                        lineTotal = item.lineTotal
                    )
                },
                ticketsIssued = orderItems.sumOf { issued[it.orderItemId] ?: 0 }
            )
        }
    }
}
