package com.eventapp.recommendation

import com.eventapp.recommendation.dto.RecommendationListDto
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache gợi ý theo User, TTL 15 phút (U07-R06).
 *
 * Tách riêng khỏi [RecommendationService] để module sở thích / yêu thích có thể vô hiệu
 * cache mà không tạo phụ thuộc vòng.
 *
 * Lưu ý: in-memory nên mất khi restart và không chia sẻ giữa nhiều instance. Với một instance
 * của đồ án là đủ; khi scale ngang thì thay bằng Redis.
 */
@Component
class RecommendationCache {

    data class Entry(val list: RecommendationListDto, val generatedAt: Instant)

    private val entries = ConcurrentHashMap<Long, Entry>()

    fun get(userId: Long): Entry? {
        val entry = entries[userId] ?: return null
        if (Duration.between(entry.generatedAt, Instant.now()) >= TTL) {
            entries.remove(userId)
            return null
        }
        return entry
    }

    fun put(userId: Long, list: RecommendationListDto, generatedAt: Instant = Instant.now()) {
        entries[userId] = Entry(list, generatedAt)
    }

    fun invalidate(userId: Long) {
        entries.remove(userId)
    }

    fun clear() = entries.clear()

    companion object {
        val TTL: Duration = Duration.ofMinutes(15)
    }
}
