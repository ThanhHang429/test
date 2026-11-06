package com.eventapp.organizer.dashboard

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

/**
 * Tổng hợp số liệu cho FR-O03 bằng truy vấn theo lô trên tập sự kiện của Organizer.
 *
 * Mọi truy vấn đều nhận `eventIds` đã lọc theo quyền sở hữu ở tầng service (O03-R01) —
 * không bao giờ nhận organizerId từ tham số request.
 */
@Repository
class OrganizerDashboardRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    data class RegistrationStats(
        val confirmed: Long,
        val cancelled: Long,
        val waitlisted: Long,
        val byEvent: Map<Long, Long>
    )

    data class FinanceStats(
        val verifiedAmount: BigDecimal,
        val pendingAmount: BigDecimal,
        val refundedAmount: BigDecimal,
        val feeAmount: BigDecimal,
        val byEventVerified: Map<Long, BigDecimal>
    )

    data class TicketStats(
        val issued: Long,
        val checkedIn: Long,
        val cancelled: Long
    )

    fun registrations(eventIds: Collection<Long>, from: Instant, to: Instant): RegistrationStats {
        if (eventIds.isEmpty()) return RegistrationStats(0, 0, 0, emptyMap())
        val params = MapSqlParameterSource()
            .addValue("ids", eventIds)
            .addValue("from", java.sql.Timestamp.from(from))
            .addValue("to", java.sql.Timestamp.from(to))

        var confirmed = 0L
        var cancelled = 0L
        var waitlisted = 0L
        val byEvent = mutableMapOf<Long, Long>()

        jdbc.query(
            """
            SELECT event_id, status, COUNT(*) AS cnt
            FROM registrations
            WHERE event_id IN (:ids) AND registered_at BETWEEN :from AND :to
            GROUP BY event_id, status
            """.trimIndent(),
            params
        ) { rs ->
            val eventId = rs.getLong("event_id")
            val count = rs.getLong("cnt")
            when (rs.getString("status")) {
                "CONFIRMED" -> {
                    confirmed += count
                    byEvent[eventId] = (byEvent[eventId] ?: 0L) + count
                }
                "CANCELLED" -> cancelled += count
                "WAITLISTED" -> waitlisted += count
            }
        }
        return RegistrationStats(confirmed, cancelled, waitlisted, byEvent)
    }

    fun finance(eventIds: Collection<Long>, from: Instant, to: Instant): FinanceStats {
        if (eventIds.isEmpty()) return FinanceStats(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, emptyMap())
        val params = MapSqlParameterSource()
            .addValue("ids", eventIds)
            .addValue("from", java.sql.Timestamp.from(from))
            .addValue("to", java.sql.Timestamp.from(to))

        var verified = BigDecimal.ZERO
        var pending = BigDecimal.ZERO
        var refunded = BigDecimal.ZERO
        var fee = BigDecimal.ZERO
        val byEvent = mutableMapOf<Long, BigDecimal>()

        jdbc.query(
            """
            SELECT event_id, status, SUM(total_amount) AS amount, SUM(platform_fee_amount) AS fee
            FROM orders
            WHERE event_id IN (:ids) AND created_at BETWEEN :from AND :to
            GROUP BY event_id, status
            """.trimIndent(),
            params
        ) { rs ->
            val eventId = rs.getLong("event_id")
            val amount = rs.getBigDecimal("amount") ?: BigDecimal.ZERO
            val feeAmount = rs.getBigDecimal("fee") ?: BigDecimal.ZERO
            when (rs.getString("status")) {
                "PAID" -> {
                    verified += amount
                    fee += feeAmount
                    byEvent[eventId] = (byEvent[eventId] ?: BigDecimal.ZERO) + amount
                }
                "PENDING_PAYMENT" -> pending += amount
                "REFUNDED", "PARTIALLY_REFUNDED" -> refunded += amount
            }
        }
        return FinanceStats(verified, pending, refunded, fee, byEvent)
    }

    fun tickets(eventIds: Collection<Long>): TicketStats {
        if (eventIds.isEmpty()) return TicketStats(0, 0, 0)
        val params = MapSqlParameterSource().addValue("ids", eventIds)
        var issued = 0L
        var checkedIn = 0L
        var cancelled = 0L
        jdbc.query(
            """
            SELECT status, COUNT(*) AS cnt
            FROM tickets
            WHERE event_id IN (:ids)
            GROUP BY status
            """.trimIndent(),
            params
        ) { rs ->
            val count = rs.getLong("cnt")
            when (rs.getString("status")) {
                "VALID", "CHECKED_IN" -> issued += count
                "CANCELLED", "REFUNDED", "REVOKED" -> cancelled += count
                else -> Unit
            }
            if (rs.getString("status") == "CHECKED_IN") checkedIn = count
        }
        return TicketStats(issued, checkedIn, cancelled)
    }

    fun checkInCount(eventIds: Collection<Long>): Long {
        if (eventIds.isEmpty()) return 0L
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM check_ins WHERE event_id IN (:ids)",
            MapSqlParameterSource("ids", eventIds),
            Long::class.java
        ) ?: 0L
    }

    fun countVisibleReviews(eventIds: Collection<Long>): Long {
        if (eventIds.isEmpty()) return 0L
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM reviews WHERE event_id IN (:ids) AND status = 'VISIBLE'",
            MapSqlParameterSource("ids", eventIds),
            Long::class.java
        ) ?: 0L
    }

    fun reviews(eventIds: Collection<Long>, from: Instant, to: Instant): Pair<Double, Long> {
        if (eventIds.isEmpty()) return 0.0 to 0L
        val params = MapSqlParameterSource()
            .addValue("ids", eventIds)
            .addValue("from", java.sql.Timestamp.from(from))
            .addValue("to", java.sql.Timestamp.from(to))
        val avg = jdbc.queryForObject(
            """
            SELECT AVG(rating) FROM reviews
            WHERE event_id IN (:ids) AND status = 'VISIBLE'
            """.trimIndent(),
            params,
            Double::class.java
        ) ?: 0.0
        val newCount = jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM reviews
            WHERE event_id IN (:ids) AND status = 'VISIBLE' AND created_at BETWEEN :from AND :to
            """.trimIndent(),
            params,
            Long::class.java
        ) ?: 0L
        return avg to newCount
    }
}
