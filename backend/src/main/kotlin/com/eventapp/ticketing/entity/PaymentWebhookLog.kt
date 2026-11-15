package com.eventapp.ticketing.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payment_webhook_logs")
open class PaymentWebhookLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long = 0,

    @Column(nullable = false, unique = true, length = 120)
    open var providerTransactionId: String = "",

    @Column(nullable = false, length = 30)
    open var providerCode: String = "SEPAY",

    @Column(length = 80)
    open var transferContent: String? = null,

    @Column(precision = 15, scale = 2)
    open var amount: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 30)
    open var targetType: String = "UNKNOWN", // ORDER_PAYMENT | WALLET_TOPUP | UNMATCHED

    open var targetId: Long? = null,

    @Column(nullable = false)
    open var processedAt: Instant = Instant.now()
)