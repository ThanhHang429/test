package com.eventapp.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Xác thực request theo 2 đường:
 *  1. `Authorization: Bearer <jwt>` — đường chính thức.
 *  2. `X-User-Id: <id>` — CHỈ bật khi profile dev, để test nhanh không cần đăng nhập.
 *     Role vẫn đọc từ database, không lấy từ header.
 */
@Component
class JwtAuthFilter(
    private val tokenProvider: JwtTokenProvider,
    private val userRoleProvider: UserRoleProvider,
    @Value("\${app.security.dev-header-enabled:false}") private val devHeaderEnabled: Boolean
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            val user = resolveFromBearer(request) ?: resolveFromDevHeader(request)
            if (user != null) {
                val auth = UsernamePasswordAuthenticationToken(user, null, user.authorities())
                auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveFromBearer(request: HttpServletRequest): AuthUser? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith("Bearer ", ignoreCase = true)) return null
        val token = header.substring(7).trim()
        if (token.isEmpty()) return null
        return tokenProvider.parse(token)
    }

    private fun resolveFromDevHeader(request: HttpServletRequest): AuthUser? {
        if (!devHeaderEnabled) return null
        val raw = request.getHeader("X-User-Id")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val userId = raw.toLongOrNull() ?: run {
            log.debug("X-User-Id không phải số: {}", raw)
            return null
        }
        if (!userRoleProvider.exists(userId)) {
            log.debug("X-User-Id trỏ tới user không tồn tại: {}", userId)
            return null
        }
        return AuthUser(userId, userRoleProvider.rolesOf(userId).toSet())
    }
}
