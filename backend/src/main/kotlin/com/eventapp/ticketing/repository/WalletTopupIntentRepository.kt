package com.eventapp.ticketing.repository

import com.eventapp.ticketing.entity.WalletTopupIntent
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface WalletTopupIntentRepository : JpaRepository<WalletTopupIntent, Long> {
    fun findByTransferContentIgnoreCase(transferContent: String): WalletTopupIntent?
    fun findAllByStatusAndExpiresAtBefore(status: String, now: Instant): List<WalletTopupIntent>
}