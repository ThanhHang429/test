package com.eventapp.ticketing.payment

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.PaymentMethod
import com.eventapp.common.enums.PaymentStatus
import com.eventapp.common.enums.PaymentVerificationSource
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.TicketingCodes
import com.eventapp.ticketing.dto.CreatePaymentRequest
import com.eventapp.ticketing.dto.PaymentDto
import com.eventapp.ticketing.entity.Order
import com.eventapp.ticketing.entity.Payment
import com.eventapp.ticketing.order.OrderService
import com.eventapp.ticketing.repository.OrderItemRepository
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.PaymentRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * FR-U11 — thanh toán chuyển khoản bằng QR.
 *
 * Luồng hiện tại là chuyển khoản thủ công: backend sinh nội dung chuyển khoản (trùng mã đơn) và
 * chờ xác nhận. [confirm] mô phỏng callback của cổng thanh toán và chỉ bật khi
 * `app.payment.simulated-confirm-enabled=true` (mặc định chỉ bật ở profile dev) — production phải
 * nối webhook thật rồi xác thực chữ ký trước khi gọi cùng logic này.
 */
@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val orderService: OrderService,
    private val codes: TicketingCodes,
    @Value("\${app.payment.simulated-confirm-enabled:false}") private val simulatedConfirmEnabled: Boolean,
    @Value("\${app.payment.bank-code:}") private val bankCode: String,
    @Value("\${app.payment.bank-account:}") private val bankAccount: String,
    @Value("\${app.payment.account-name:}") private val accountName: String
) {

    @Transactional
    fun initiate(userId: Long, orderId: Long, request: CreatePaymentRequest): PaymentDto {
        val order = requireOwnedOrder(userId, orderId)
        orderService.expireIfDue(order)
        if (order.status == OrderStatus.EXPIRED) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn đã hết thời gian giữ chỗ")
        }
        if (order.status != OrderStatus.PENDING_PAYMENT) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn không ở trạng thái chờ thanh toán")
        }

        // Gọi lại nhiều lần không tạo thêm giao dịch.
        paymentRepository.findFirstByOrderIdAndStatusIn(
            orderId, listOf(PaymentStatus.INITIATED, PaymentStatus.PENDING)
        )?.let { return toDto(it) }

        val method = parseMethod(request.method)
        val now = Instant.now()
        val payment = paymentRepository.save(
            Payment(
                paymentReference = codes.newPaymentReference(now),
                orderId = orderId,
                providerCode = "MANUAL",
                paymentMethod = method,
                amount = order.totalAmount,
                currency = order.currency.trim(),
                status = PaymentStatus.PENDING,
                qrPayload = buildQrPayload(order, method)
            )
        )
        return toDto(payment)
    }

    @Transactional(readOnly = true)
    fun get(userId: Long, paymentId: Long): PaymentDto {
        val payment = paymentRepository.findById(paymentId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy giao dịch")
        val order = orderRepository.findById(payment.orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        if (order.userId != userId) throw ForbiddenException(ErrorCode.FORBIDDEN, "Giao dịch không thuộc về bạn")
        return toDto(payment)
    }

    /**
     * Xác nhận đã nhận tiền. Idempotent: gọi lại trên giao dịch SUCCESS trả nguyên trạng.
     */
    @Transactional
    fun confirm(userId: Long, paymentId: Long): PaymentDto {
        if (!simulatedConfirmEnabled) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Cổng thanh toán chưa được cấu hình để xác nhận tự động"
            )
        }
        val payment = paymentRepository.findById(paymentId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy giao dịch")
        val order = requireOwnedOrder(userId, payment.orderId)

        if (payment.status == PaymentStatus.SUCCESS) return toDto(payment)
        if (payment.status == PaymentStatus.FAILED || payment.status == PaymentStatus.REVERSED) {
            throw ConflictException(ErrorCode.CONFLICT, "Giao dịch đã kết thúc, không xác nhận được")
        }
        if (order.status == OrderStatus.EXPIRED) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn đã hết thời gian giữ chỗ")
        }
        if (order.totalAmount.compareTo(payment.amount) != 0) {
            throw ConflictException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
        }

        val now = Instant.now()
        payment.status = PaymentStatus.SUCCESS
        payment.verifiedAt = now
        payment.verificationSource = PaymentVerificationSource.PROVIDER_WEBHOOK
        payment.providerTransactionId = codes.newPaymentReference(now)

        val items = orderItemRepository.findByOrderId(order.orderId!!)
        val types = ticketTypeRepository.findByTicketTypeIdIn(items.map { it.ticketTypeId })
        orderService.markPaid(order, types, items, now)
        return toDto(payment)
    }

    private fun requireOwnedOrder(userId: Long, orderId: Long): Order {
        val order = orderRepository.findById(orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        if (order.userId != userId) throw ForbiddenException(ErrorCode.FORBIDDEN, "Đơn hàng không thuộc về bạn")
        return order
    }

    private fun parseMethod(raw: String?): PaymentMethod {
        val value = raw?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return PaymentMethod.BANK_TRANSFER_QR
        return runCatching { PaymentMethod.valueOf(value) }
            .getOrElse { throw validationError("method", "Phương thức thanh toán không hợp lệ") }
    }

    /** Nội dung chuyển khoản chính là mã đơn để đối soát tự động sau này. */
    private fun buildQrPayload(order: Order, method: PaymentMethod): String {
        val amount = order.totalAmount.stripTrailingZeros().toPlainString()
        return buildString {
            append("ETICKET|").append(method.name)
            if (bankCode.isNotBlank()) append("|BANK=").append(bankCode.trim())
            if (bankAccount.isNotBlank()) append("|ACC=").append(bankAccount.trim())
            if (accountName.isNotBlank()) append("|NAME=").append(accountName.trim())
            append("|AMOUNT=").append(amount)
            append("|CONTENT=").append(order.orderCode)
        }
    }

    private fun toDto(payment: Payment): PaymentDto {
        val order = orderRepository.findById(payment.orderId).orElse(null)
        return PaymentDto(
            paymentId = payment.paymentId!!,
            paymentReference = payment.paymentReference,
            orderId = payment.orderId,
            orderCode = order?.orderCode ?: "",
            providerCode = payment.providerCode,
            method = payment.paymentMethod.name,
            amount = payment.amount,
            currency = payment.currency.trim(),
            status = payment.status.name,
            qrPayload = payment.qrPayload,
            transferContent = order?.orderCode ?: payment.paymentReference,
            bankCode = bankCode.trim().ifBlank { null },
            bankAccount = bankAccount.trim().ifBlank { null },
            expiresAt = order?.paymentDueAt,
            initiatedAt = payment.initiatedAt,
            verifiedAt = payment.verifiedAt
        )
    }
}
