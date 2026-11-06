package com.eventapp.common.idempotency

import com.eventapp.common.exception.IdempotencyConflictException
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

/**
 * Idempotency-Key cho thao tác tạo (SRS 3.5, CF-NFR-02).
 *
 * Schema Aiven không có bảng `idempotency_keys` nên lưu in-memory TTL 24h.
 * Hạn chế đã biết: mất khi restart và không chia sẻ giữa nhiều instance — đủ cho
 * phạm vi đồ án; khi lên production thay bằng bảng riêng có UNIQUE(key, user_id).
 *
 * Quy ước: key được namespace theo `scope` + `userId` để hai user khác nhau dùng
 * cùng một key không đụng nhau.
 */
@Component
class IdempotencyStore {

    private data class Entry(val fingerprint: String, val value: Any?, val storedAt: Instant)

    private val entries = ConcurrentHashMap<String, Entry>()
    private val ttl: Duration = Duration.ofHours(24)

    /**
     * Trả lại kết quả của lần gọi trước nếu cùng key + cùng nội dung.
     * Ném [IdempotencyConflictException] nếu cùng key nhưng nội dung khác.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> execute(scope: String, userId: Long, key: String?, fingerprint: String, action: Supplier<T>): T {
        purgeExpired()
        if (key.isNullOrBlank()) return action.get()

        val cacheKey = "$scope:$userId:$key"
        val existing = entries[cacheKey]
        if (existing != null) {
            if (existing.fingerprint != fingerprint) throw IdempotencyConflictException()
            return existing.value as T
        }
        val result = action.get()
        entries[cacheKey] = Entry(fingerprint, result, Instant.now())
        return result
    }

    private fun purgeExpired() {
        if (entries.size < 512) return
        val cutoff = Instant.now().minus(ttl)
        entries.entries.removeIf { it.value.storedAt.isBefore(cutoff) }
    }
}
