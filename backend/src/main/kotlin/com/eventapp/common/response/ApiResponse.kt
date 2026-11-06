package com.eventapp.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Phong bì phản hồi thống nhất cho toàn bộ API (SRS mục 3.2).
 *
 * Thành công: { success: true, data: {...}, meta: {...}, correlationId }
 * Lỗi:       xem [ErrorEnvelope]
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val meta: Any? = null,
    val correlationId: String? = null
) {
    companion object {
        fun <T> ok(data: T, meta: Any? = null): ApiResponse<T> =
            ApiResponse(success = true, data = data, meta = meta)

        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun empty(): ApiResponse<Unit> = ApiResponse(success = true, data = null)
    }
}
