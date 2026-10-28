package com.eventapp.common.response

// TODO: ApiResponse - khớp contract Android { success, data, message }
data class ApiResponse<T>(
    val success: Boolean = true,
    val data: T? = null,
    val message: String? = null
)