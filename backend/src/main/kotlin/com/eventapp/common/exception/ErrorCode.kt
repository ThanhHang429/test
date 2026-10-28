package com.eventapp.common.exception

// TODO: ErrorCode - mã lỗi thống nhất
enum class ErrorCode(val defaultMessage: String) {
    BAD_REQUEST("Bad request"),
    UNAUTHORIZED("Unauthorized"),
    FORBIDDEN("Forbidden"),
    NOT_FOUND("Not found"),
    CONFLICT("Conflict"),
    VALIDATION_ERROR("Validation error"),
    TOKEN_EXPIRED("Token expired"),
    TICKET_SOLD_OUT("Ticket sold out"),
    EVENT_NOT_APPROVED("Event not approved"),
    PAYMENT_AMOUNT_MISMATCH("Payment amount mismatch"),
    ALREADY_CHECKED_IN("Already checked in"),
    IDEMPOTENCY_CONFLICT("Idempotency conflict")
}