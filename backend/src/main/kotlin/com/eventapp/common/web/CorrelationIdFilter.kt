package com.eventapp.common.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * Đọc/ sinh correlation ID và trả lại trong response header (SRS 3.6).
 * Chạy sớm nhất để mọi filter/log phía sau đều có ID.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val incoming = request.getHeader(CorrelationId.HEADER)?.trim()?.takeIf { it.isNotEmpty() }
        val id = incoming ?: UUID.randomUUID().toString()
        CorrelationId.set(id)
        response.setHeader(CorrelationId.HEADER, id)
        try {
            filterChain.doFilter(request, response)
        } finally {
            CorrelationId.clear()
        }
    }
}
