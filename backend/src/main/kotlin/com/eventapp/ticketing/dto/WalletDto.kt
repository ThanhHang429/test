package com.eventapp.ticketing.dto

import java.math.BigDecimal
import java.time.Instant

data class WalletDto(
    val walletId: Long,
    val userId: Long,
    val currency: String,
    val balance: BigDecimal,
    val status: String,
    val hasPin: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class WalletTransactionDto(
    val walletTransactionId: Long,
    val walletId: Long,
    val transactionType: String,
    val direction: String,
    val amount: BigDecimal,
    val balanceAfter: BigDecimal,
    val referenceType: String?,
    val referenceId: String?,
    val status: String,
    val note: String?,
    val createdAt: String?
)

data class SetWalletPinRequest(
    val oldPin: String? = null,
    val newPin: String
)

data class TopupWalletRequest(
    val amount: BigDecimal = BigDecimal.ZERO,
    val paymentMethod: String = "VIETQR",
    val note: String? = null
)

data class PayWalletRequest(
    val orderId: Long,
    val amount: BigDecimal? = null,
    val note: String? = null,
    val pin: String? = null
)