package com.eventapp.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Phản hồi lỗi thống nhất (SRS mục 3.2):
 * { success: false, error: { code, message, details[] }, correlationId }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorEnvelope(
    val success: Boolean = false,
    val error: ApiError,
    val correlationId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val details: List<FieldDetail> = emptyList(),
    /** Với RATE_LIMITED: số giây phải chờ. Với AI_*: có fallback hay không. */
    val extra: Map<String, Any?>? = null
)

/** Lỗi theo từng trường (SRS 3.2: details chứa lỗi theo từng trường). */
data class FieldDetail(
    val field: String? = null,
    val message: String
)
