package com.eventapp.admin.integration

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class IntegrationHealthDto(
    /** AI | PAYMENT | FCM */
    val name: String,
    /** HEALTHY | DEGRADED | DOWN | UNKNOWN */
    val status: String,
    val errorRate: Double,
    val requestCount: Long,
    val lastSuccessAt: Instant?,
    val lastErrorAt: Instant?,
    val lastErrorCode: String?,
    val p95LatencyMs: Long?,
    /** Chỉ mô tả trạng thái cấu hình, KHÔNG bao giờ chứa khoá bí mật (A20-R01). */
    val notes: String?
)

data class AiUsageSummaryDto(
    val range: String,
    val from: Instant,
    val to: Instant,
    val totalRequests: Long,
    val successCount: Long,
    val failureCount: Long,
    val successRate: Double,
    val errorBreakdown: Map<String, Long>,
    val byTaskType: Map<String, Long>,
    val avgLatencyMs: Long,
    val p95LatencyMs: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val estimatedCostUsd: Double,
    val dailyBudgetUsd: Double,
    val budgetUsedRatio: Double,
    val features: Map<String, Boolean>,
    val circuitState: String
)

data class AiErrorItemDto(
    val at: Instant?,
    val taskType: String,
    val errorCode: String?,
    val correlationId: String?,
    val latencyMs: Int?,
    /** Mã người dùng đã che — không trả userId thô. */
    val userRef: String?
)

data class AiRequestDetailDto(
    val aiRequestId: Long?,
    val taskType: String,
    val status: String,
    val providerCode: String?,
    val modelCode: String?,
    val inputSummary: String?,
    val outputSummary: String?,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val latencyMs: Int?,
    val errorCode: String?,
    val correlationId: String?,
    val createdAt: Instant?,
    val completedAt: Instant?,
    val userRef: String?
)

data class AiFeatureToggleRequest(
    @field:NotNull(message = "Thiếu trạng thái bật/tắt")
    val enabled: Boolean?,

    @field:NotBlank(message = "Bắt buộc ghi lý do (A20-R04)")
    @field:Size(max = 500, message = "Lý do tối đa 500 ký tự")
    val reason: String? = null
)

data class AiFeatureToggleResponse(
    val taskType: String,
    val enabled: Boolean,
    val features: Map<String, Boolean>,
    val effectiveImmediately: Boolean
)
