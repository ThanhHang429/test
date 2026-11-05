package com.eventapp.common.enums


enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    EXPIRED,
    CANCELLED,
    REFUND_PENDING,
    PARTIALLY_REFUNDED,
    REFUNDED
}