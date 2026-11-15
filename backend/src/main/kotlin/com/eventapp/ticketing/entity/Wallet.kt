package com.eventapp.ticketing.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "wallets")
class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    var walletId: Long? = null,

    @Column(name = "user_id", nullable = false, unique = true)
    var userId: Long = 0L,

    @Column(name = "currency", nullable = false, length = 3)
    var currency: String = "VND",

    @Column(name = "balance", nullable = false)
    var balance: BigDecimal = BigDecimal.ZERO,

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE",

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null,

    @Column(name = "pin_hash", length = 100)
    var pinHash: String? = null

)