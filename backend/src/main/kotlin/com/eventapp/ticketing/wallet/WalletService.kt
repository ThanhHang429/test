package com.eventapp.ticketing.wallet

import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.dto.*
import com.eventapp.ticketing.entity.Wallet
import com.eventapp.ticketing.entity.WalletTopupIntent
import com.eventapp.ticketing.entity.WalletTransaction
import com.eventapp.ticketing.order.OrderService
import com.eventapp.ticketing.payment.PaymentSseHub
import com.eventapp.ticketing.repository.OrderItemRepository
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.WalletRepository
import com.eventapp.ticketing.repository.WalletTopupIntentRepository
import com.eventapp.ticketing.repository.WalletTransactionRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
open class WalletService(
    private val walletRepository: WalletRepository,
    private val transactionRepository: WalletTransactionRepository,
    private val topupIntentRepository: WalletTopupIntentRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val orderService: OrderService,
    private val paymentSseHub: PaymentSseHub,
    @Value("\${app.payment.bank-code:ACB}") private val bankCode: String,
    @Value("\${app.payment.bank-account:190368688888}") private val bankAccount: String,
    @Value("\${app.payment.account-name:CONG TY ETICKET}") private val accountName: String
) {
    private val pinEncoder = BCryptPasswordEncoder()

    @Transactional(readOnly = true)
    open fun getMyWallet(userId: Long): WalletDto? {
        val wallet = walletRepository.findByUserId(userId) ?: return null
        return toWalletDto(wallet)
    }

    @Transactional
    open fun createWallet(userId: Long): WalletDto {
        val existing = walletRepository.findByUserId(userId)
        if (existing != null) return toWalletDto(existing)

        val wallet = Wallet(
            userId = userId,
            currency = "VND",
            balance = BigDecimal.ZERO,
            status = "ACTIVE",
            pinHash = null
        )
        return toWalletDto(walletRepository.save(wallet))
    }

    /** FR-U13: Thiết lập mới hoặc đổi mã PIN 6 số lưu BCrypt trên Backend. */
    @Transactional
    open fun setPin(userId: Long, request: SetWalletPinRequest): WalletDto {
        val newPin = request.newPin.trim()
        if (!newPin.matches(Regex("^\\d{6}$"))) {
            throw validationError("newPin", "Mã PIN phải gồm đúng 6 chữ số")
        }

        val wallet = walletRepository.findByUserId(userId)
            ?: throw NotFoundException(message = "Ví ETicket chưa được kích hoạt")

        val currentHash = wallet.pinHash
        if (!currentHash.isNullOrBlank()) {
            val oldPin = request.oldPin?.trim().orEmpty()
            if (oldPin.isEmpty() || !pinEncoder.matches(oldPin, currentHash)) {
                throw ForbiddenException(ErrorCode.FORBIDDEN, "Mã PIN hiện tại không chính xác")
            }
        }

        wallet.pinHash = pinEncoder.encode(newPin)
        wallet.updatedAt = Instant.now()
        return toWalletDto(walletRepository.save(wallet))
    }

    @Transactional
    open fun topup(userId: Long, request: TopupWalletRequest): WalletDto {
        if (request.amount <= BigDecimal.ZERO) {
            throw ConflictException(ErrorCode.CONFLICT, "Số tiền nạp phải lớn hơn 0")
        }
        val wallet = walletRepository.findByUserId(userId)
            ?: walletRepository.save(
                Wallet(userId = userId, currency = "VND", balance = BigDecimal.ZERO, status = "ACTIVE")
            )
        wallet.balance = wallet.balance.add(request.amount)
        wallet.updatedAt = Instant.now()
        val saved = walletRepository.save(wallet)

        val ref = "TOPUP-" + UUID.randomUUID().toString().take(8).uppercase()
        transactionRepository.save(
            WalletTransaction(
                walletId = saved.walletId!!,
                createdByUserId = userId,
                transactionType = "TOPUP",
                direction = "IN",
                amount = request.amount,
                balanceAfter = saved.balance,
                referenceType = "TOPUP",
                referenceId = ref,
                idempotencyKey = ref,
                status = "SUCCESS",
                note = request.note ?: "Nạp tiền vào ví"
            )
        )
        return toWalletDto(saved)
    }

    /** Tạo yêu cầu nạp tiền ví VietQR (PENDING) kèm mã ETNAP... để đối soát Webhook + SSE. */
    @Transactional
    open fun createTopupIntent(userId: Long, request: TopupWalletRequest): TopupIntentResponse {
        if (request.amount <= BigDecimal.ZERO) {
            throw validationError("amount", "Số tiền nạp phải lớn hơn 0")
        }
        walletRepository.findByUserId(userId)
            ?: walletRepository.save(
                Wallet(userId = userId, currency = "VND", balance = BigDecimal.ZERO, status = "ACTIVE")
            )

        val now = Instant.now()
        val code = "ETNAP" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        val resolvedBank = bankCode.trim().ifBlank { "ACB" }
        val resolvedAccount = bankAccount.trim().ifBlank { "190368688888" }
        val resolvedName = accountName.trim().ifBlank { "CONG TY ETICKET" }
        val method = request.paymentMethod.trim().uppercase().ifBlank { "BANK_TRANSFER_QR" }
        val qrPayload = "ETICKET|TOPUP|BANK=$resolvedBank|ACC=$resolvedAccount|NAME=$resolvedName|AMOUNT=${request.amount.stripTrailingZeros().toPlainString()}|CONTENT=$code"

        val saved = topupIntentRepository.save(
            WalletTopupIntent(
                userId = userId,
                topupCode = code,
                amount = request.amount,
                currency = "VND",
                paymentMethod = method,
                status = "PENDING",
                bankCode = resolvedBank,
                bankAccount = resolvedAccount,
                accountName = resolvedName,
                transferContent = code,
                qrPayload = qrPayload,
                expiresAt = now.plus(15, ChronoUnit.MINUTES),
                createdAt = now
            )
        )
        return toTopupDto(saved)
    }

    /** Kiểm tra trạng thái yêu cầu nạp tiền vào ví (dùng cho Polling / SSE snapshot). */
    @Transactional
    open fun getTopupIntent(userId: Long, topupId: Long): TopupIntentResponse {
        val intent = topupIntentRepository.findById(topupId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy yêu cầu nạp tiền")
        if (intent.userId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Yêu cầu nạp tiền không thuộc về bạn")
        }
        val now = Instant.now()
        if (intent.status == "PENDING" && intent.expiresAt.isBefore(now)) {
            intent.status = "EXPIRED"
            topupIntentRepository.save(intent)
        }
        return toTopupDto(intent)
    }

    /** Cộng tiền vào ví khi nhận được Webhook chuyển khoản khớp mã ETNAP... */
    @Transactional
    open fun creditTopupFromWebhook(
        topupCode: String,
        amount: BigDecimal,
        providerTransactionId: String
    ): TopupIntentResponse {
        val intent = topupIntentRepository.findByTransferContentIgnoreCase(topupCode.trim())
            ?: throw NotFoundException(message = "Không tìm thấy yêu cầu nạp tiền $topupCode")

        if (intent.status == "SUCCESS" || intent.status == "FAILED" || intent.status == "EXPIRED") {
            return toTopupDto(intent)
        }

        val now = Instant.now()
        if (intent.expiresAt.isBefore(now)) {
            intent.status = "EXPIRED"
            intent.failureMessage = "Lệnh nạp tiền đã hết hạn"
            val expired = toTopupDto(topupIntentRepository.save(intent))
            paymentSseHub.publishTopup(intent.id, expired)
            return expired
        }

        if (amount.compareTo(intent.amount) != 0) {
            intent.status = "FAILED"
            intent.providerTransactionId = providerTransactionId
            intent.failureMessage = "Số tiền nhận (${amount.stripTrailingZeros().toPlainString()} đ) không khớp (${intent.amount.stripTrailingZeros().toPlainString()} đ)"
            val failed = toTopupDto(topupIntentRepository.save(intent))
            paymentSseHub.publishTopup(intent.id, failed)
            return failed
        }

        val wallet = walletRepository.findByUserId(intent.userId)
            ?: walletRepository.save(
                Wallet(userId = intent.userId, currency = "VND", balance = BigDecimal.ZERO, status = "ACTIVE")
            )

        wallet.balance = wallet.balance.add(amount)
        wallet.updatedAt = now
        val savedWallet = walletRepository.save(wallet)

        transactionRepository.save(
            WalletTransaction(
                walletId = savedWallet.walletId!!,
                createdByUserId = intent.userId,
                transactionType = "TOPUP",
                direction = "IN",
                amount = amount,
                balanceAfter = savedWallet.balance,
                referenceType = "WALLET_TOPUP",
                referenceId = intent.id.toString(),
                idempotencyKey = "TOPUP-$providerTransactionId",
                status = "SUCCESS",
                note = "Nạp tiền VietQR (${intent.transferContent})"
            )
        )

        intent.status = "SUCCESS"
        intent.completedAt = now
        intent.providerTransactionId = providerTransactionId
        val dto = toTopupDto(topupIntentRepository.save(intent))
        paymentSseHub.publishTopup(intent.id, dto)
        return dto
    }

    /** Thanh toán đơn hàng bằng số dư Ví ETicket kèm xác thực PIN trên Backend. */
    @Transactional
    open fun payOrder(userId: Long, request: PayWalletRequest): WalletTransactionDto {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw NotFoundException(message = "Bạn chưa kích hoạt Ví ETicket")

        val hash = wallet.pinHash
        if (hash.isNullOrBlank()) {
            throw ConflictException(ErrorCode.CONFLICT, "Ví chưa thiết lập mã PIN bảo mật. Vui lòng tạo PIN trước khi thanh toán.")
        }
        val rawPin = request.pin?.trim().orEmpty()
        if (rawPin.isEmpty() || !pinEncoder.matches(rawPin, hash)) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Mã PIN ví không đúng")
        }

        val order = orderRepository.findById(request.orderId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đơn hàng")
        if (order.userId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Đơn hàng không thuộc về bạn")
        }
        orderService.expireIfDue(order)
        if (order.status != OrderStatus.PENDING_PAYMENT) {
            throw ConflictException(ErrorCode.CONFLICT, "Đơn hàng không ở trạng thái chờ thanh toán")
        }

        val payable = order.totalAmount
        if (wallet.balance.compareTo(payable) < 0) {
            throw ConflictException(ErrorCode.CONFLICT, "Số dư ví không đủ để thanh toán đơn này")
        }

        wallet.balance = wallet.balance.subtract(payable)
        wallet.updatedAt = Instant.now()
        val savedWallet = walletRepository.save(wallet)

        val tx = transactionRepository.save(
            WalletTransaction(
                walletId = savedWallet.walletId!!,
                createdByUserId = userId,
                transactionType = "PAYMENT",
                direction = "OUT",
                amount = payable, // Luôn lưu số dương; Android dựa vào direction = "OUT" để hiển thị dấu '-'
                balanceAfter = savedWallet.balance,
                referenceType = "ORDER",
                referenceId = order.orderId.toString(),
                idempotencyKey = "PAY-ORDER-${order.orderId}-${UUID.randomUUID().toString().take(8)}",
                status = "SUCCESS",
                note = request.note ?: "Thanh toán đơn hàng ${order.orderCode}"
            )
        )

        val items = orderItemRepository.findByOrderId(order.orderId!!)
        val types = ticketTypeRepository.findByTicketTypeIdIn(items.map { it.ticketTypeId })
        orderService.markPaid(order, types, items, Instant.now())

        return toTxDto(tx)
    }

    /** Cộng tiền hoàn trả tự động về Ví ETicket khi Admin duyệt hoàn tiền (FR-A12). */
    @Transactional
    open fun creditRefund(
        buyerUserId: Long,
        adminUserId: Long,
        orderId: Long,
        orderCode: String,
        amount: BigDecimal,
        note: String?
    ): WalletTransaction {
        if (amount <= BigDecimal.ZERO) {
            throw ConflictException(ErrorCode.CONFLICT, "Số tiền hoàn phải lớn hơn 0")
        }
        val wallet = walletRepository.findByUserId(buyerUserId)
            ?: walletRepository.save(
                Wallet(userId = buyerUserId, currency = "VND", balance = BigDecimal.ZERO, status = "ACTIVE")
            )

        val idemKey = "REFUND-ORDER-$orderId"
        val existing = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.walletId!!)
            .firstOrNull { it.idempotencyKey == idemKey }
        if (existing != null) return existing

        wallet.balance = wallet.balance.add(amount)
        wallet.updatedAt = Instant.now()
        val savedWallet = walletRepository.save(wallet)

        return transactionRepository.save(
            WalletTransaction(
                walletId = savedWallet.walletId!!,
                createdByUserId = adminUserId,
                transactionType = "REFUND",
                direction = "IN",
                amount = amount,
                balanceAfter = savedWallet.balance,
                referenceType = "REFUND_ORDER",
                referenceId = orderId.toString(),
                idempotencyKey = idemKey,
                status = "SUCCESS",
                note = note ?: "Hoàn tiền đơn hàng $orderCode"
            )
        )
    }

    @Transactional(readOnly = true)
    open fun getTransactions(userId: Long): List<WalletTransactionDto> {
        val wallet = walletRepository.findByUserId(userId) ?: return emptyList()
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.walletId!!)
            .map { toTxDto(it) }
    }

    fun toWalletDto(w: Wallet): WalletDto = WalletDto(
        walletId = w.walletId!!,
        userId = w.userId,
        currency = w.currency,
        balance = w.balance,
        status = w.status,
        hasPin = !w.pinHash.isNullOrBlank(),
        createdAt = w.createdAt,
        updatedAt = w.updatedAt
    )

    private fun toTxDto(t: WalletTransaction): WalletTransactionDto = WalletTransactionDto(
        walletTransactionId = t.walletTransactionId!!,
        walletId = t.walletId,
        transactionType = t.transactionType,
        direction = t.direction,
        amount = t.amount,
        balanceAfter = t.balanceAfter,
        referenceType = t.referenceType,
        referenceId = t.referenceId,
        status = t.status,
        note = t.note,
        createdAt = t.createdAt.toString()
    )

    private fun toTopupDto(i: WalletTopupIntent): TopupIntentResponse = TopupIntentResponse(
        topupId = i.id,
        topupCode = i.topupCode,
        amount = i.amount,
        currency = i.currency,
        paymentMethod = i.paymentMethod,
        status = i.status,
        bankCode = i.bankCode,
        bankAccount = i.bankAccount,
        accountName = i.accountName,
        transferContent = i.transferContent,
        qrPayload = i.qrPayload,
        expiresAt = i.expiresAt,
        createdAt = i.createdAt,
        completedAt = i.completedAt,
        providerTransactionId = i.providerTransactionId,
        failureMessage = i.failureMessage
    )
}