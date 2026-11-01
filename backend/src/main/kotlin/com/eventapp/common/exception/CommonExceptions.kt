package com.eventapp.common.exception

import com.eventapp.common.response.FieldDetail

class NotFoundException(
    errorCode: ErrorCode = ErrorCode.NOT_FOUND,
    message: String = errorCode.defaultMessage
) : BusinessException(errorCode, message)

class ForbiddenException(
    errorCode: ErrorCode = ErrorCode.FORBIDDEN,
    message: String = errorCode.defaultMessage
) : BusinessException(errorCode, message)

class ConflictException(
    errorCode: ErrorCode = ErrorCode.CONFLICT,
    message: String = errorCode.defaultMessage
) : BusinessException(errorCode, message)

class ValidationException(
    message: String = ErrorCode.VALIDATION_ERROR.defaultMessage,
    fieldDetails: List<FieldDetail> = emptyList()
) : BusinessException(ErrorCode.VALIDATION_ERROR, message, fieldDetails)

class UnauthenticatedException(
    errorCode: ErrorCode = ErrorCode.UNAUTHENTICATED,
    message: String = errorCode.defaultMessage
) : BusinessException(errorCode, message)

class IdempotencyConflictException : BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT)

/** Tiện ích tạo nhanh ValidationException cho một trường. */
fun validationError(field: String, message: String): ValidationException =
    ValidationException(message, listOf(FieldDetail(field = field, message = message)))
