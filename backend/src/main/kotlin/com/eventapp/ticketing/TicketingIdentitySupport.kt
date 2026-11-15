package com.eventapp.ticketing

import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

data class UserBrief(
    val userId: Long,
    val fullName: String,
    val email: String?
)

@Component
class TicketingIdentitySupport(
    private val jdbcTemplate: JdbcTemplate
) {
    fun isAdmin(): Boolean {
        val auth = SecurityContextHolder.getContext().authentication ?: return false
        return auth.authorities.any {
            it.authority.equals("ROLE_ADMIN", ignoreCase = true) ||
                    it.authority.equals("ADMIN", ignoreCase = true)
        }
    }

    fun requireAdmin() {
        if (!isAdmin()) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Chức năng chỉ dành cho Quản trị viên")
        }
    }

    fun loadUsers(userIds: Collection<Long>): Map<Long, UserBrief> {
        val ids = userIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        return try {
            jdbcTemplate.query(
                "SELECT user_id, full_name, email FROM users WHERE user_id IN ($placeholders)",
                { rs, _ ->
                    val id = rs.getLong("user_id")
                    val name = rs.getString("full_name")?.takeIf { it.isNotBlank() }
                        ?: rs.getString("email")
                        ?: "User #$id"
                    UserBrief(id, name, rs.getString("email"))
                },
                *ids.toTypedArray()
            ).associateBy { it.userId }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun loadOrganizerNames(organizerUserIds: Collection<Long>): Map<Long, String> {
        val ids = organizerUserIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val placeholders = ids.joinToString(",") { "?" }
        val result = mutableMapOf<Long, String>()
        try {
            jdbcTemplate.query(
                "SELECT user_id, organization_name FROM organizer_profiles WHERE user_id IN ($placeholders)",
                { rs ->
                    val uid = rs.getLong("user_id")
                    val orgName = rs.getString("organization_name")
                    if (!orgName.isNullOrBlank()) result[uid] = orgName
                },
                *ids.toTypedArray()
            )
        } catch (_: Exception) {
            // Fallback sang tên tài khoản user nếu tên cột organizer_profiles khác
        }
        val missing = ids.filter { !result.containsKey(it) }
        if (missing.isNotEmpty()) {
            loadUsers(missing).forEach { (id, brief) -> result[id] = brief.fullName }
        }
        return result
    }
}