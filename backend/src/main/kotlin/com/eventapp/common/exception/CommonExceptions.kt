package com.eventapp.common.exception

class NotFoundException(errorCode: ErrorCode = ErrorCode.NOT_FOUND, message: String = errorCode.defaultMessage) : BusinessException(errorCode, message)
class ForbiddenException(errorCode: ErrorCode = ErrorCode.FORBIDDEN, message: String = errorCode.defaultMessage) : BusinessException(errorCode, message)
class ConflictException(errorCode: ErrorCode = ErrorCode.CONFLICT, message: String = errorCode.defaultMessage) : BusinessException(errorCode, message)
class ValidationException(errorCode: ErrorCode = ErrorCode.VALIDATION_ERROR, message: String = errorCode.defaultMessage) : BusinessException(errorCode, message)
class IdempotencyConflictException : BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT)