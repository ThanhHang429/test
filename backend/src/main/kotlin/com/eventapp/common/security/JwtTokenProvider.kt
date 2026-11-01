package com.eventapp.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

/**
 * Cấp và xác minh JWT (HS256). Claim:
 *  - sub   : userId
 *  - roles : danh sách role code (USER / ORGANIZER / ADMIN)
 */
@Component
class JwtTokenProvider(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.ttl:PT24H}") private val ttl: Duration
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val key: SecretKey = run {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= 32) {
            "app.jwt.secret phải dài tối thiểu 32 byte cho HS256"
        }
        Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun issue(userId: Long, roles: Collection<String>): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles.toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttl)))
            .signWith(key)
            .compact()
    }

    fun ttlSeconds(): Long = ttl.seconds

    /** Trả null nếu token không hợp lệ/hết hạn — không ném lỗi ra ngoài filter. */
    fun parse(token: String): AuthUser? {
        return try {
            val claims: Claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
            val userId = claims.subject?.toLongOrNull() ?: return null
            @Suppress("UNCHECKED_CAST")
            val roles = (claims["roles"] as? Collection<*>)?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
            AuthUser(userId, roles)
        } catch (ex: JwtException) {
            log.debug("JWT không hợp lệ: {}", ex.message)
            null
        } catch (ex: IllegalArgumentException) {
            log.debug("JWT không hợp lệ: {}", ex.message)
            null
        }
    }
}
