package com.eventapp.admin.integration

import com.eventapp.ai.entity.AiRequest
import com.eventapp.ai.gateway.AiCircuitBreaker
import com.eventapp.ai.gateway.AiFeatureFlags
import com.eventapp.ai.gateway.AiTask
import com.eventapp.ai.repository.AiRequestRepository
import com.eventapp.ai.repository.IntegrationHealthLogRepository
import com.eventapp.common.enums.AiRequestStatus
import com.eventapp.common.enums.IntegrationResultStatus
import com.eventapp.common.enums.IntegrationServiceCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * FR-A20 — giám sát tích hợp AI / thanh toán / FCM.
 *
 * Nguyên tắc:
 *  - Chỉ đọc số liệu; riêng bật/tắt tính năng AI có ghi audit (A20-R04).
 *  - Không bao giờ trả API key, chỉ trả trạng thái "đã cấu hình: có/không" (A20-R01).
 *  - Mọi lời gọi AI đều có `correlation_id` để tra cứu (A20-R02).
 */
@Service
class IntegrationMonitorService(
    private val aiRequestRepository: AiRequestRepository,
    private val integrationHealthLogRepository: IntegrationHealthLogRepository,
    private val featureFlags: AiFeatureFlags,
    private val circuitBreaker: AiCircuitBreaker,
    @Value("\${app.ai.api-key:}") private val aiApiKey: String,
    @Value("\${app.ai.provider:mock}") private val aiProvider: String,
    @Value("\${app.ai.daily-budget-usd:2.0}") private val dailyBudgetUsd: Double,
    @Value("\${app.ai.cost-per-1k-tokens:0.0005}") private val costPer1kTokens: Double,
    @Value("\${app.fcm.credentials-path:}") private val fcmCredentialsPath: String,
    @Value("\${app.payment.webhook-secret:}") private val paymentWebhookSecret: String
) {

    // ------------------------------------------------------------------
    // Sức khoẻ 3 dịch vụ
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun health(): List<IntegrationHealthDto> {
        val since = Instant.now().minus(Duration.ofHours(24))
        return listOf(aiHealth(since), paymentHealth(since), fcmHealth(since))
    }

    private fun aiHealth(since: Instant): IntegrationHealthDto {
        val requests = aiRequestRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
            since,
            org.springframework.data.domain.PageRequest.of(0, 2000)
        ).content
        val total = requests.size.toLong()
        val failures = requests.count { it.status == AiRequestStatus.FAILED }.toLong()
        val lastSuccess = requests.firstOrNull { it.status == AiRequestStatus.SUCCEEDED }?.createdAt
        val lastError = requests.firstOrNull { it.status == AiRequestStatus.FAILED }
        val latencies = requests.mapNotNull { it.latencyMs }

        val status = when {
            total == 0L -> "UNKNOWN"
            circuitBreaker.isOpen() -> "DOWN"
            failures.toDouble() / total >= 0.5 -> "DOWN"
            failures.toDouble() / total >= 0.2 -> "DEGRADED"
            p95(latencies) > 10_000 -> "DEGRADED"
            else -> "HEALTHY"
        }
        val notes = buildString {
            append("provider=").append(aiProvider)
            append(", apiKeyConfigured=").append(if (aiApiKey.isNotBlank()) "có" else "không")
            append(", circuit=").append(circuitBreaker.state())
        }
        return IntegrationHealthDto(
            name = "AI",
            status = status,
            errorRate = if (total > 0) round4(failures.toDouble() / total) else 0.0,
            requestCount = total,
            lastSuccessAt = lastSuccess,
            lastErrorAt = lastError?.createdAt,
            lastErrorCode = lastError?.errorCode,
            p95LatencyMs = p95(latencies).takeIf { latencies.isNotEmpty() },
            notes = notes
        )
    }

    private fun paymentHealth(since: Instant): IntegrationHealthDto = serviceHealth(
        service = IntegrationServiceCode.PAYMENT,
        name = "PAYMENT",
        since = since,
        configured = paymentWebhookSecret.isNotBlank(),
        notes = "webhookSecretConfigured=" + if (paymentWebhookSecret.isNotBlank()) "có" else "không"
    )

    private fun fcmHealth(since: Instant): IntegrationHealthDto = serviceHealth(
        service = IntegrationServiceCode.FCM,
        name = "FCM",
        since = since,
        configured = fcmCredentialsPath.isNotBlank(),
        notes = "credentialsConfigured=" + if (fcmCredentialsPath.isNotBlank()) "có" else "không"
    )

    private fun serviceHealth(
        service: IntegrationServiceCode,
        name: String,
        since: Instant,
        configured: Boolean,
        notes: String
    ): IntegrationHealthDto {
        val logs = integrationHealthLogRepository.findTop200ByOrderByCreatedAtDesc()
            .filter { it.serviceCode == service && (it.createdAt == null || it.createdAt!!.isAfter(since)) }
        val total = logs.size.toLong()
        val errors = logs.count { it.resultStatus == IntegrationResultStatus.ERROR }.toLong()
        val status = when {
            !configured && total == 0L -> "UNKNOWN"
            total == 0L -> "UNKNOWN"
            errors.toDouble() / total >= 0.5 -> "DOWN"
            errors.toDouble() / total >= 0.2 -> "DEGRADED"
            else -> "HEALTHY"
        }
        val latencies = logs.mapNotNull { it.latencyMs }
        return IntegrationHealthDto(
            name = name,
            status = status,
            errorRate = if (total > 0) round4(errors.toDouble() / total) else 0.0,
            requestCount = total,
            lastSuccessAt = logs.firstOrNull { it.resultStatus == IntegrationResultStatus.SUCCESS }?.createdAt,
            lastErrorAt = logs.firstOrNull { it.resultStatus == IntegrationResultStatus.ERROR }?.createdAt,
            lastErrorCode = logs.firstOrNull { it.resultStatus == IntegrationResultStatus.ERROR }?.errorCode,
            p95LatencyMs = p95(latencies).takeIf { latencies.isNotEmpty() },
            notes = notes
        )
    }

    // ------------------------------------------------------------------
    // Số liệu AI
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun aiUsage(range: String): AiUsageSummaryDto {
        val window = parseRange(range)
        val from = Instant.now().minus(window)
        val requests = aiRequestRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
            from,
            org.springframework.data.domain.PageRequest.of(0, 5000)
        ).content

        val total = requests.size.toLong()
        val success = requests.count { it.status == AiRequestStatus.SUCCEEDED }.toLong()
        val failure = requests.count { it.status == AiRequestStatus.FAILED }.toLong()
        val latencies = requests.mapNotNull { it.latencyMs }
        val inputTokens = requests.sumOf { (it.inputTokens ?: 0).toLong() }
        val outputTokens = requests.sumOf { (it.outputTokens ?: 0).toLong() }
        val cost = (inputTokens + outputTokens) / 1000.0 * costPer1kTokens

        return AiUsageSummaryDto(
            range = range.lowercase(),
            from = from,
            to = Instant.now(),
            totalRequests = total,
            successCount = success,
            failureCount = failure,
            successRate = if (total > 0) round4(success.toDouble() / total) else 0.0,
            errorBreakdown = requests.filter { it.errorCode != null }
                .groupingBy { it.errorCode!! }.eachCount().mapValues { it.value.toLong() },
            byTaskType = requests.groupingBy { it.featureCode.name }
                .eachCount().mapValues { it.value.toLong() },
            avgLatencyMs = if (latencies.isEmpty()) 0 else latencies.average().toLong(),
            p95LatencyMs = p95(latencies),
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            estimatedCostUsd = BigDecimal(cost).setScale(4, RoundingMode.HALF_UP).toDouble(),
            dailyBudgetUsd = dailyBudgetUsd,
            budgetUsedRatio = if (dailyBudgetUsd > 0) round4(cost / dailyBudgetUsd) else 0.0,
            features = featureFlags.flags(),
            circuitState = circuitBreaker.state()
        )
    }

    @Transactional(readOnly = true)
    fun recentErrors(limit: Int): List<AiErrorItemDto> =
        aiRequestRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
            Instant.now().minus(Duration.ofDays(7)),
            org.springframework.data.domain.PageRequest.of(0, 200)
        ).content
            .filter { it.status == AiRequestStatus.FAILED }
            .take(limit.coerceIn(1, 100))
            .map {
                AiErrorItemDto(
                    at = it.createdAt,
                    taskType = it.featureCode.name,
                    errorCode = it.errorCode,
                    correlationId = it.correlationId,
                    latencyMs = it.latencyMs,
                    userRef = maskUser(it.userId)
                )
            }

    @Transactional(readOnly = true)
    fun requestsByCorrelation(correlationId: String): List<AiRequestDetailDto> {
        val rows = aiRequestRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId)
        if (rows.isEmpty()) throw NotFoundException(message = "Không tìm thấy yêu cầu AI với correlationId này")
        return rows.map { it.toDetail() }
    }

    // ------------------------------------------------------------------
    // Công tắc khẩn (API-22)
    // ------------------------------------------------------------------

    @Transactional
    fun toggleFeature(taskType: String, enabled: Boolean, reason: String, actorUserId: Long): AiFeatureToggleResponse {
        val task = AiTask.entries.firstOrNull { it.code.equals(taskType, ignoreCase = true) }
            ?: throw validationError(
                "taskType",
                "taskType phải là một trong: ${AiTask.entries.joinToString(", ") { it.code }}"
            )
        val flags = featureFlags.setEnabled(task, enabled, actorUserId, reason)
        return AiFeatureToggleResponse(
            taskType = task.code,
            enabled = enabled,
            features = flags,
            effectiveImmediately = true
        )
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun AiRequest.toDetail() = AiRequestDetailDto(
        aiRequestId = aiRequestId,
        taskType = featureCode.name,
        status = status.name,
        providerCode = providerCode,
        modelCode = modelCode,
        inputSummary = inputSummary,
        outputSummary = outputSummary,
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        latencyMs = latencyMs,
        errorCode = errorCode,
        correlationId = correlationId,
        createdAt = createdAt,
        completedAt = completedAt,
        userRef = maskUser(userId)
    )

    /** A20-R01: không trả userId thô trong màn hình giám sát. */
    private fun maskUser(userId: Long?): String? {
        if (userId == null) return null
        val raw = userId.toString()
        return if (raw.length <= 2) "*" else raw.take(1) + "*".repeat(raw.length - 2) + raw.takeLast(1)
    }

    private fun p95(latencies: List<Int>): Long {
        if (latencies.isEmpty()) return 0
        val sorted = latencies.sorted()
        val index = ((sorted.size - 1) * 0.95).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index].toLong()
    }

    private fun round4(value: Double): Double =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toDouble()

    private fun parseRange(range: String): Duration = when (range.lowercase()) {
        "1h", "1", "hour" -> Duration.ofHours(1)
        "24h", "1d", "day" -> Duration.ofHours(24)
        "7d", "week" -> Duration.ofDays(7)
        else -> throw validationError("range", "range phải là 1h | 24h | 7d")
    }
}
