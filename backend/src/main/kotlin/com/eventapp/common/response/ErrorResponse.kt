package com.eventapp.common.response

// TODO: ErrorResponse - { code, message, details[], requestId }
data class ErrorResponse(
    val code: String = "",
    val message: String = "",
    val details: List<String> = emptyList(),
    val requestId: String? = null
)