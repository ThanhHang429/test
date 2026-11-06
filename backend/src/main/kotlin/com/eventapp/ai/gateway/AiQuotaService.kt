package com.eventapp.ai.gateway

import com.eventapp.ai.repository.AiRequestRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Quota gọi AI thật (U07-R11, O06, O27).
 *
 * Đếm trực tiếp trên `ai_requests` nên không cần bảng đếm riêng và vẫn đúng sau khi restart.
 * Chỉ đếm yêu cầu đã thực sự gọi nhà cung cấp (mọi dòng đều là lời gọi thật).
 */
@Service
class AiQuotaService(
    private val aiRequestRepository: AiRequestRepository
) {

    data class Limit(val max: Long, val window: Duration)

    fun limitOf(task: AiTask): Limit = when (task) {
        AiTask.RECOMMEND -> Limit(20, Duration.ofHours(1))
        AiTask.DESCRIPTION -> Limit(10, Duration.ofHours(1))
        // O06 dùng chung quota với DESCRIPTION (cùng feature_code EVENT_DESCRIPTION).
        AiTask.ANNOUNCEMENT -> Limit(10, Duration.ofHours(1))
        AiTask.CHAT -> Limit(30, Duration.ofDays(1))
        AiTask.ANALYTICS -> Limit(5, Duration.ofDays(1))
        AiTask.MODERATION -> Limit(100, Duration.ofHours(1))
    }

    /**
     * Ném [AiQuotaExceededException] kèm số giây cần chờ.
     *
     * ANALYTICS đếm theo **sự kiện** (SRS 5.1: 5 lượt/ngày/sự kiện); các tác vụ còn lại
     * đếm theo người dùng.
     */
    fun ensureWithinQuota(task: AiTask, userId: Long?, eventId: Long? = null) {
        if (task == AiTask.ANALYTICS && eventId != null) {
            ensureWithinEventQuota(task, eventId)
            return
        }
        if (userId == null) return
        val limit = limitOf(task)
        val since = Instant.now().minus(limit.window)
        val used = aiRequestRepository.countByUserIdAndFeatureCodeAndCreatedAtAfter(
            userId,
            task.featureCode,
            since
        )
        if (used < limit.max) return

        throw quotaExceeded(limit, "Bạn đã dùng hết ${limit.max} lượt AI")
    }

    private fun ensureWithinEventQuota(task: AiTask, eventId: Long) {
        val limit = limitOf(task)
        val used = aiRequestRepository.countByEventIdAndFeatureCodeAndCreatedAtAfter(
            eventId,
            task.featureCode,
            Instant.now().minus(limit.window)
        )
        if (used < limit.max) return

        throw quotaExceeded(limit, "Sự kiện này đã dùng hết ${limit.max} lượt AI")
    }

    private fun quotaExceeded(limit: Limit, prefix: String): AiQuotaExceededException {
        val windowSeconds = limit.window.seconds
        val retryAfter = if (windowSeconds <= 3600) windowSeconds else 3600
        return AiQuotaExceededException(
            retryAfterSeconds = retryAfter,
            message = "$prefix trong ${humanWindow(limit.window)}. Vui lòng thử lại sau."
        )
    }

    fun used(task: AiTask, userId: Long): Long =
        aiRequestRepository.countByUserIdAndFeatureCodeAndCreatedAtAfter(
            userId,
            task.featureCode,
            Instant.now().minus(limitOf(task).window)
        )

    private fun humanWindow(window: Duration): String =
        if (window.toHours() >= 24) "${window.toDays()} ngày" else "${window.toHours()} giờ"
}
