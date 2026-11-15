package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

data class CreateRefundRequest(
    val orderId: Long,
    val reason: String
)

data class ProcessRefundRequest(
    val approved: Boolean,
    val rejectReason: String? = null
)

data class RefundResponseDto(
    val refundId: Long,
    val refundCode: String,
    val orderId: Long,
    val orderCode: String,
    val eventTitle: String,
    val eventId: Long,
    val userId: Long,
    val buyerName: String?,
    val amount: BigDecimal,
    val reason: String,
    val status: String,
    val rejectReason: String?,
    val processedBy: Long?,
    val walletBalanceAfter: BigDecimal?,
    val createdAt: Instant?,
    val processedAt: Instant?
)