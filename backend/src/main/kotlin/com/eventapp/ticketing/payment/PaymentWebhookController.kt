package com.eventapp.ticketing.payment

import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.response.ApiResponse
import com.eventapp.ticketing.dto.DevWebhookRequest
import com.eventapp.ticketing.dto.SePayWebhookPayload
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/webhooks/payments")
open class PaymentWebhookController(
    private val webhookService: PaymentWebhookService,
    @Value("\${app.webhook.sepay-secret:}") private val sepaySecret: String
) {

    @PostMapping("/sepay")
    open fun handleSePayWebhook(
        @RequestHeader(value = "Authorization", required = false) authHeader: String?,
        @RequestBody payload: SePayWebhookPayload
    ): ResponseEntity<Map<String, Any?>> {
        if (sepaySecret.isNotBlank()) {
            val expected = "Apikey $sepaySecret"
            if (authHeader == null || authHeader.trim() != expected) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("success" to false, "message" to "Invalid webhook secret"))
            }
        }

        if (!payload.transferType.isNullOrBlank() && !payload.transferType.equals("in", ignoreCase = true)) {
            return ResponseEntity.ok(mapOf("success" to true, "data" to null))
        }

        val content = (payload.code ?: payload.content ?: payload.description ?: "").trim()
        val amount = payload.transferAmount ?: BigDecimal.ZERO
        val txId = payload.referenceCode?.takeIf { it.isNotBlank() }
            ?: payload.id?.let { "SEPAY-$it" }
            ?: "SEPAY-${UUID.randomUUID()}"

        val result = webhookService.processIncomingTransfer(
            rawContent = content,
            amount = amount,
            providerCode = "SEPAY",
            providerTransactionId = txId
        )
        return ResponseEntity.ok(mapOf("success" to true, "data" to result))
    }

    @Profile("dev", "local")
    @PostMapping("/dev-simulate")
    open fun simulateDevWebhook(
        @RequestBody request: DevWebhookRequest
    ): ApiResponse<Any?> {
        val txId = request.providerTransactionId?.takeIf { it.isNotBlank() }
            ?: "DEV-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"

        val result = webhookService.processIncomingTransfer(
            rawContent = request.transferContent.trim(),
            amount = request.amount,
            providerCode = request.providerCode ?: "SEPAY",
            providerTransactionId = txId
        ) ?: throw NotFoundException(message = "Không tìm thấy đơn hàng hoặc lệnh nạp ví cho mã: ${request.transferContent}")

        return ApiResponse.ok(result)
    }
}