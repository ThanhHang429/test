package com.eventapp.ticketing.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "wallet_topup_intents")
open class WalletTopupIntent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0,

    @Column(nullable = false)
    open var userId: Long = 0,

    @Column(nullable = false, unique = true, length = 40)
    open var topupCode: String = "",

    @Column(nullable = false, precision = 15, scale = 2)
    open var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 10)
    open var currency: String = "VND",

    @Column(nullable = false, length = 30)
    open var paymentMethod: String = "BANK_TRANSFER_QR",

    /** PENDING | SUCCESS | FAILED | EXPIRED */
    @Column(nullable = false, length = 20)
    open var status: String = "PENDING",

    @Column(nullable = false, length = 20)
    open var bankCode: String = "ACB",

    @Column(nullable = false, length = 50)
    open var bankAccount: String = "",

    @Column(nullable = false, length = 120)
    open var accountName: String = "",

    @Column(nullable = false, unique = true, length = 60)
    open var transferContent: String = "",

    @Column(length = 500)
    open var qrPayload: String? = null,

    @Column(nullable = false)
    open var expiresAt: Instant = Instant.now(),

    @Column(nullable = false)
    open var createdAt: Instant = Instant.now(),

    open var completedAt: Instant? = null,

    @Column(length = 100)
    open var providerTransactionId: String? = null,

    @Column(length = 255)
    open var failureMessage: String? = null
)