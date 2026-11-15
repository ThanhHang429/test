package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

data class TopupIntentResponse(
    val topupId: Long,
    val topupCode: String,
    val amount: BigDecimal,
    val currency: String,
    val paymentMethod: String,
    val status: String,
    val bankCode: String,
    val bankAccount: String,
    val accountName: String,
    val transferContent: String,
    val qrPayload: String?,
    val expiresAt: Instant,
    val createdAt: Instant,
    val completedAt: Instant?,
    val providerTransactionId: String?,
    val failureMessage: String?
)

data class DevWebhookRequest(
    val transferContent: String,
    val amount: BigDecimal,
    val providerCode: String? = "SEPAY",
    val providerTransactionId: String? = null
)

/** Payload chuẩn của SePay Webhook (tương thích cả trường content/description) */
data class SePayWebhookPayload(
    val id: Long? = null,
    val gateway: String? = null,
    val transactionDate: String? = null,
    val accountNumber: String? = null,
    val code: String? = null,
    val content: String? = null,
    val description: String? = null,
    val transferType: String? = null, // "in" | "out"
    val transferAmount: BigDecimal? = null,
    val referenceCode: String? = null
)