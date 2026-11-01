package com.eventapp.common.exception

import com.eventapp.common.response.ApiError
import com.eventapp.common.response.ErrorEnvelope
import com.eventapp.common.response.FieldDetail
import com.eventapp.common.web.CorrelationId
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Chuyển mọi exception thành [ErrorEnvelope] thống nhất (SRS 3.2, 3.4).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ErrorEnvelope> {
        if (ex.errorCode.status.is5xxServerError) {
            log.error("Business error {}: {}", ex.errorCode, ex.message, ex)
        } else {
            log.debug("Business error {}: {}", ex.errorCode, ex.message)
        }
        return build(ex.errorCode.status, ex.errorCode.name, ex.message, ex.fieldDetails, ex.extra)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorEnvelope> {
        val details = ex.bindingResult.allErrors.map { err ->
            FieldDetail(
                field = (err as? FieldError)?.field,
                message = err.defaultMessage ?: "Giá trị không hợp lệ"
            )
        }
        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR.name,
            ErrorCode.VALIDATION_ERROR.defaultMessage,
            details
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraint(ex: ConstraintViolationException): ResponseEntity<ErrorEnvelope> {
        val details = ex.constraintViolations.map { FieldDetail(it.propertyPath.toString(), it.message) }
        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR.name,
            ErrorCode.VALIDATION_ERROR.defaultMessage,
            details
        )
    }

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        IllegalArgumentException::class
    )
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorEnvelope> {
        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR.name,
            ex.message ?: ErrorCode.VALIDATION_ERROR.defaultMessage
        )
    }

    /** JSON hỏng hoặc thiếu trường bắt buộc — không lộ chi tiết kỹ thuật của Jackson ra client. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorEnvelope> {
        log.debug("Body không đọc được: {}", ex.message)
        val missingField = MISSING_PROPERTY.find(ex.message.orEmpty())?.groupValues?.getOrNull(1)
        return build(
            HttpStatus.BAD_REQUEST,
            ErrorCode.VALIDATION_ERROR.name,
            if (missingField != null) "Thiếu trường bắt buộc: $missingField"
            else "Dữ liệu gửi lên không đúng định dạng JSON",
            if (missingField != null) listOf(FieldDetail(missingField, "Trường này là bắt buộc")) else emptyList()
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorEnvelope> =
        build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.name, ErrorCode.FORBIDDEN.defaultMessage)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ResponseEntity<ErrorEnvelope> =
        build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED.name, ErrorCode.UNAUTHENTICATED.defaultMessage)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<ErrorEnvelope> =
        build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND.name, ErrorCode.NOT_FOUND.defaultMessage)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorEnvelope> {
        log.error("Unhandled exception", ex)
        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.INTERNAL_ERROR.name,
            ErrorCode.INTERNAL_ERROR.defaultMessage
        )
    }

    private fun build(
        status: HttpStatus,
        code: String,
        message: String,
        details: List<FieldDetail> = emptyList(),
        extra: Map<String, Any?>? = null
    ): ResponseEntity<ErrorEnvelope> =
        ResponseEntity.status(status).body(
            ErrorEnvelope(
                success = false,
                error = ApiError(code = code, message = message, details = details, extra = extra),
                correlationId = CorrelationId.current()
            )
        )

    companion object {
        private val MISSING_PROPERTY = Regex("""property "([A-Za-z0-9_]+)"""")
    }
}
