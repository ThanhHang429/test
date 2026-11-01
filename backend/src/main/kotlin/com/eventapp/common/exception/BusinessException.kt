package com.eventapp.common.exception

import com.eventapp.common.response.FieldDetail

/**
 * Exception nghiệp vụ. [details] cho phép trả lỗi theo từng trường (SRS 3.2).
 * [extra] cho metadata bổ sung (retryAfterSeconds, fallbackAvailable...).
 */
open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
    val fieldDetails: List<FieldDetail> = emptyList(),
    val extra: Map<String, Any?>? = null
) : RuntimeException(message)
