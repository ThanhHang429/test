package com.eventapp.common.exception

// TODO: BusinessException base
open class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
    val details: List<String> = emptyList()
) : RuntimeException(message)