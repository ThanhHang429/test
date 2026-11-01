package com.eventapp.ai.gateway

import com.eventapp.common.enums.AiFeatureCode
import com.fasterxml.jackson.databind.JsonNode

/** Tác vụ AI — khớp `ai_requests.feature_code` và công tắc khẩn FR-A20. */
enum class AiTask(val featureCode: AiFeatureCode, val code: String) {
    RECOMMEND(AiFeatureCode.EVENT_RECOMMENDATION, "RECOMMEND"),
    CHAT(AiFeatureCode.CHATBOT, "CHAT"),
    DESCRIPTION(AiFeatureCode.EVENT_DESCRIPTION, "DESCRIPTION"),
    ANALYTICS(AiFeatureCode.ORGANIZER_ANALYTICS, "ANALYTICS"),
    MODERATION(AiFeatureCode.OTHER, "MODERATION")
}

/** Prompt đã dựng sẵn — `user` luôn là JSON có nhãn nhiệm vụ để nhà cung cấp giả lập xử lý được. */
data class AiPrompt(
    val task: AiTask,
    val system: String,
    val user: String,
    val maxOutputTokens: Int = 900,
    val temperature: Double = 0.2
)

data class AiCompletion(
    val text: String,
    val inputTokens: Int?,
    val outputTokens: Int?,
    val latencyMs: Long
)

/** Kết quả sau khi qua đủ 6 bước của Gateway. */
data class AiGatewayResult(
    val aiRequestId: Long?,
    val task: AiTask,
    val data: JsonNode,
    val latencyMs: Long,
    val correlationId: String?,
    val modelCode: String
)

/** Ném khi công tắc khẩn của tác vụ đang tắt — nơi gọi phải dùng fallback. */
class AiDisabledException(message: String = "Tính năng AI đang tạm tắt") : RuntimeException(message)

/** Ném khi không gọi được nhà cung cấp (timeout, circuit breaker mở). */
class AiUnavailableException(message: String = "Dịch vụ AI tạm thời không khả dụng") : RuntimeException(message)

/** Ném khi đầu ra không đúng schema sau 1 lần thử lại. */
class AiInvalidOutputException(message: String = "AI trả kết quả không hợp lệ") : RuntimeException(message)

/** Ném khi vượt quota — nơi gọi quyết định fallback hay trả 429. */
class AiQuotaExceededException(val retryAfterSeconds: Long, message: String) : RuntimeException(message)
