package com.eventapp.ticketing.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "wallet_transactions")
class WalletTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_transaction_id")
    var walletTransactionId: Long? = null,

    @Column(name = "wallet_id", nullable = false)
    var walletId: Long = 0L,

    @Column(name = "transaction_type", nullable = false, length = 30)
    var transactionType: String = "",

    @Column(name = "direction", nullable = false, length = 10)
    var direction: String = "IN",

    @Column(name = "amount", nullable = false)
    var amount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "balance_after", nullable = false)
    var balanceAfter: BigDecimal = BigDecimal.ZERO,

    @Column(name = "reference_type", length = 30)
    var referenceType: String? = null,

    @Column(name = "reference_id", length = 100)
    var referenceId: String? = null,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "SUCCESS",

    @Column(name = "note")
    var note: String? = null,

    @Column(name = "created_by_user_id")
    var createdByUserId: Long? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now(),

    @Column(name = "idempotency_key", length = 100)
    var idempotencyKey: String? = null
)