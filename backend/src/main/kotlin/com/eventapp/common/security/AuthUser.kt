package com.eventapp.common.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Principal gắn trong SecurityContext. Chỉ chứa userId + role code lấy từ token
 * (BR-001: không bao giờ tin role do client gửi trong body).
 */
data class AuthUser(
    val userId: Long,
    val roles: Set<String>
) {
    fun authorities(): List<GrantedAuthority> =
        roles.map { SimpleGrantedAuthority("ROLE_$it") }
}

object CurrentUser {

    fun orNull(): AuthUser? =
        SecurityContextHolder.getContext().authentication?.principal as? AuthUser

    /** userId của người gọi; ném lỗi 401 nếu chưa đăng nhập. */
    fun requireUserId(): Long =
        orNull()?.userId ?: throw com.eventapp.common.exception.UnauthenticatedException()

    fun require(): AuthUser =
        orNull() ?: throw com.eventapp.common.exception.UnauthenticatedException()

    fun hasRole(role: String): Boolean = orNull()?.roles?.contains(role) == true
}
