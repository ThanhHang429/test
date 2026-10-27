package com.eventapp.common.enums

// TODO: Enum per prompt section 5. FR-xxx / BR-xxx

enum class OrderStatus {
    PENDING_PAYMENT,
    PAID,
    EXPIRED,
    CANCELLED,
    REFUND_PENDING,
    PARTIALLY_REFUNDED,
    REFUNDED
}