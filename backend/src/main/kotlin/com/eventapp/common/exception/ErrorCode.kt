package com.eventapp.common.exception

import org.springframework.http.HttpStatus

/**
 * Mã lỗi thống nhất (SRS mục 3.4). Mỗi mã gắn một HTTP status cố định.
 */
enum class ErrorCode(val status: HttpStatus, val defaultMessage: String) {
    // Lỗi chung
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đã hết hạn"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đã hết hạn"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu xung đột hoặc trạng thái không cho phép"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống gặp sự cố, vui lòng thử lại"),

    // Token
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"),

    // Sự kiện
    EVENT_NOT_APPROVED(HttpStatus.CONFLICT, "Sự kiện chưa được duyệt"),
    EVENT_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "Sự kiện không còn khả dụng"),
    EVENT_NOT_ENDED(HttpStatus.FORBIDDEN, "Sự kiện chưa kết thúc"),
    NOT_ATTENDED(HttpStatus.FORBIDDEN, "Bạn chưa tham dự sự kiện này"),

    // Yêu thích
    FAVORITE_LIMIT_REACHED(HttpStatus.CONFLICT, "Đã đạt giới hạn 500 sự kiện yêu thích"),

    // Đánh giá
    REVIEW_WINDOW_CLOSED(HttpStatus.FORBIDDEN, "Đã quá hạn đánh giá sự kiện này"),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bạn đã đánh giá sự kiện này"),
    REVIEW_EDIT_LIMIT(HttpStatus.CONFLICT, "Đã đạt giới hạn chỉnh sửa đánh giá"),

    // Danh mục
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"),
    CATEGORY_INACTIVE(HttpStatus.CONFLICT, "Danh mục đã bị ẩn"),
    CATEGORY_IN_USE(HttpStatus.CONFLICT, "Danh mục đang được sử dụng, không thể xoá"),
    CATEGORY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "Danh mục chỉ hỗ trợ tối đa 2 cấp"),

    // Bán vé / check-in (dùng khi tích hợp module khác)
    TICKET_SOLD_OUT(HttpStatus.CONFLICT, "Đã hết vé"),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "Vé đã được check-in"),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "Số tiền thanh toán không khớp"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Yêu cầu trùng lặp"),

    // AI
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ AI tạm thời không khả dụng"),
    AI_INVALID_OUTPUT(HttpStatus.BAD_GATEWAY, "AI trả kết quả không hợp lệ"),
    AI_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "Tính năng AI đang tạm tắt")
}
