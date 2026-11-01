package com.eventapp.common.security

/**
 * Cho phép lớp security lấy danh sách role của một user mà không phụ thuộc trực tiếp
 * vào module identity (tránh vòng phụ thuộc). Triển khai ở `identity`.
 */
interface UserRoleProvider {
    fun rolesOf(userId: Long): List<String>

    /** Dùng cho đường dev `X-User-Id`: chặn id không tồn tại thay vì để FK đổ vỡ ở tầng DB. */
    fun exists(userId: Long): Boolean
}
