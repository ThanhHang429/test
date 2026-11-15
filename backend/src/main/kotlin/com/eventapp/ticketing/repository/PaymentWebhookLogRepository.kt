package com.eventapp.ticketing.repository

import com.eventapp.ticketing.entity.PaymentWebhookLog
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentWebhookLogRepository : JpaRepository<PaymentWebhookLog, Long> {
    fun existsByProviderTransactionId(providerTransactionId: String): Boolean
}