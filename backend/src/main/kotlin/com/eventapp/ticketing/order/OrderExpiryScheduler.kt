package com.eventapp.ticketing.order

import com.eventapp.common.enums.OrderStatus
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.WalletTopupIntentRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OrderExpiryScheduler(
    private val orderRepository: OrderRepository,
    private val orderService: OrderService,
    private val topupIntentRepository: WalletTopupIntentRepository
) {

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    fun expireOverdueOrdersAndTopups() {
        val now = Instant.now()

        // 1. Nhả vé các đơn quá hạn paymentDueAt
        val overdueOrders = orderRepository.findByStatusAndPaymentDueAtBefore(OrderStatus.PENDING_PAYMENT, now)
        for (order in overdueOrders) {
            orderService.expireIfDue(order, now)
        }

        // 2. [SỬA] Đổi thành findAllByStatusAndExpiresAtBefore khớp với WalletTopupIntentRepository
        val overdueTopups = topupIntentRepository.findAllByStatusAndExpiresAtBefore("PENDING", now)
        for (topup in overdueTopups) {
            topup.status = "EXPIRED"
        }
    }
}