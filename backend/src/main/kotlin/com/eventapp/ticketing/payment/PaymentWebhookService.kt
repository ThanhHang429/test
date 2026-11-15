package com.eventapp.ticketing.payment

import com.eventapp.ticketing.entity.PaymentWebhookLog
import com.eventapp.ticketing.repository.PaymentWebhookLogRepository
import com.eventapp.ticketing.repository.WalletTopupIntentRepository
import com.eventapp.ticketing.wallet.WalletService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
open class PaymentWebhookService(
    private val webhookLogRepository: PaymentWebhookLogRepository,
    private val topupIntentRepository: WalletTopupIntentRepository,
    private val walletService: WalletService,
    private val paymentService: PaymentService
) {

    @Transactional
    open fun processIncomingTransfer(
        rawContent: String,
        amount: BigDecimal,
        providerCode: String,
        providerTransactionId: String
    ): Any? {
        // 1. Idempotency: nếu mã giao dịch ngân hàng đã xử lý thì bỏ qua
        if (webhookLogRepository.existsByProviderTransactionId(providerTransactionId)) {
            return null
        }

        val normalized = extractToken(rawContent)

        // 2. Đối soát lệnh Nạp Ví (ETNAP...)
        val topup = topupIntentRepository.findByTransferContentIgnoreCase(normalized)
        if (topup != null) {
            val dto = walletService.creditTopupFromWebhook(
                topupCode = topup.topupCode,
                amount = amount,
                providerTransactionId = providerTransactionId
            )
            webhookLogRepository.save(
                PaymentWebhookLog(
                    providerTransactionId = providerTransactionId,
                    providerCode = providerCode,
                    transferContent = normalized,
                    amount = amount,
                    targetType = "WALLET_TOPUP",
                    targetId = topup.id
                )
            )
            return dto
        }

        // 3. Đối soát Thanh toán Đơn Vé (ETK...)
        val paymentDto = paymentService.handleWebhookPayment(
            transferContent = normalized,
            receivedAmount = amount,
            providerCode = providerCode,
            providerTransactionId = providerTransactionId
        )

        webhookLogRepository.save(
            PaymentWebhookLog(
                providerTransactionId = providerTransactionId,
                providerCode = providerCode,
                transferContent = normalized,
                amount = amount,
                targetType = if (paymentDto != null) "ORDER_PAYMENT" else "UNMATCHED",
                targetId = paymentDto?.paymentId
            )
        )
        return paymentDto
    }

    /** Trích mã ETNAP... hoặc ETK... trong chuỗi nội dung chuyển khoản ngân hàng trả về */
    private fun extractToken(rawContent: String): String {
        val regex = Regex("(ETNAP[A-Z0-9]+|ETK[A-Z0-9-]+)", RegexOption.IGNORE_CASE)
        return regex.find(rawContent)?.value?.uppercase() ?: rawContent.trim().uppercase()
    }
}