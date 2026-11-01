package com.eventapp.ai.gateway

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Circuit breaker đơn giản cho lời gọi AI (A20: "circuit breaker đang mở" → DOWN).
 *
 * Mở sau [FAILURE_THRESHOLD] lỗi liên tiếp, giữ [OPEN_DURATION], sau đó cho thử lại
 * (half-open). Chỉ áp dụng cho lời gọi AI — không bao giờ bọc thao tác tài chính (A20-R03).
 */
@Component
class AiCircuitBreaker {

    private val log = LoggerFactory.getLogger(javaClass)

    private val failures = AtomicInteger(0)
    private val openedAt = AtomicReference<Instant?>(null)

    fun isOpen(): Boolean {
        val opened = openedAt.get() ?: return false
        if (Instant.now().isAfter(opened.plus(OPEN_DURATION))) {
            // Half-open: cho một lượt thử, nếu lỗi lại sẽ mở tiếp.
            openedAt.set(null)
            failures.set(FAILURE_THRESHOLD - 1)
            log.info("Circuit breaker AI chuyển sang half-open")
            return false
        }
        return true
    }

    fun recordSuccess() {
        if (failures.get() > 0) failures.set(0)
        if (openedAt.get() != null) {
            openedAt.set(null)
            log.info("Circuit breaker AI đóng lại")
        }
    }

    fun recordFailure() {
        val count = failures.incrementAndGet()
        if (count >= FAILURE_THRESHOLD && openedAt.get() == null) {
            openedAt.set(Instant.now())
            log.warn("Circuit breaker AI MỞ sau {} lỗi liên tiếp", count)
        }
    }

    fun state(): String = when {
        openedAt.get() != null -> "OPEN"
        failures.get() > 0 -> "HALF_OPEN"
        else -> "CLOSED"
    }

    fun consecutiveFailures(): Int = failures.get()

    companion object {
        private const val FAILURE_THRESHOLD = 5
        private val OPEN_DURATION: Duration = Duration.ofSeconds(60)
    }
}
