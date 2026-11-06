package com.eventapp.recommendation

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.Instant

/**
 * Truy vấn ứng viên cho FR-U07 (bước 1: tối đa 100 sự kiện).
 *
 * Chỉ một truy vấn cho toàn bộ ứng viên — không N+1 (RE-R04, NFR-04). Số vé còn lấy theo lô
 * bằng derived table giống module khám phá, không gọi từng sự kiện.
 */
@Repository
class RecommendationRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    data class CandidateRow(
        val eventId: Long,
        val organizerProfileId: Long,
        val title: String,
        val slug: String,
        val shortDescription: String?,
        val eventFormat: String,
        val startAt: Instant,
        val endAt: Instant,
        val city: String?,
        val venueName: String?,
        val registrationStartAt: Instant?,
        val minPrice: BigDecimal?,
        val freeCount: Long,
        val onSaleCount: Long,
        val availableQuantity: Long?,
        val popularity7d: Long
    )

    fun candidates(userId: Long?, limit: Int): List<CandidateRow> {
        val params = MapSqlParameterSource()
        params.addValue("userId", userId ?: -1L)
        params.addValue("limit", limit)

        val sql = """
            SELECT e.event_id, e.organizer_profile_id, e.title, e.slug, e.short_description,
                   e.event_format, e.start_at, e.end_at, e.city, e.venue_name, e.registration_start_at,
                   tt.min_price, tt.free_cnt, tt.on_sale_cnt, tt.available_qty,
                   COALESCE(pop7.cnt, 0) AS popularity_7d
            $FROM_AND_JOINS
            WHERE e.deleted_at IS NULL
              AND e.review_status = 'APPROVED'
              AND e.lifecycle_status = 'PUBLISHED'
              AND e.visibility = 'PUBLIC'
              AND e.end_at >= UTC_TIMESTAMP(3)
              AND e.start_at <= UTC_TIMESTAMP(3) + INTERVAL 90 DAY
              AND NOT EXISTS (
                    SELECT 1 FROM registrations r
                    WHERE r.event_id = e.event_id AND r.user_id = :userId AND r.status = 'CONFIRMED'
              )
              AND NOT EXISTS (
                    SELECT 1 FROM tickets tk
                    WHERE tk.event_id = e.event_id AND tk.user_id = :userId
                      AND tk.status IN ('VALID', 'CHECKED_IN')
              )
            ORDER BY COALESCE(pop7.cnt, 0) DESC, e.start_at ASC, e.event_id ASC
            LIMIT :limit
        """.trimIndent()

        return jdbc.query(sql, params) { rs, _ -> map(rs) }
    }

    private fun map(rs: ResultSet) = CandidateRow(
        eventId = rs.getLong("event_id"),
        organizerProfileId = rs.getLong("organizer_profile_id"),
        title = rs.getString("title"),
        slug = rs.getString("slug"),
        shortDescription = rs.getString("short_description"),
        eventFormat = rs.getString("event_format"),
        startAt = rs.getTimestamp("start_at").toInstant(),
        endAt = rs.getTimestamp("end_at").toInstant(),
        city = rs.getString("city"),
        venueName = rs.getString("venue_name"),
        registrationStartAt = rs.getTimestamp("registration_start_at")?.toInstant(),
        minPrice = rs.getBigDecimal("min_price"),
        freeCount = rs.getLong("free_cnt"),
        onSaleCount = rs.getLong("on_sale_cnt"),
        availableQuantity = rs.getObject("available_qty")?.let { (it as Number).toLong() },
        popularity7d = rs.getLong("popularity_7d")
    )

    companion object {
        private val FROM_AND_JOINS = """
            FROM events e
            LEFT JOIN (
                SELECT event_id,
                       MIN(CASE WHEN on_sale THEN price END) AS min_price,
                       SUM(CASE WHEN on_sale AND ticket_kind = 'FREE' THEN 1 ELSE 0 END) AS free_cnt,
                       SUM(CASE WHEN on_sale THEN 1 ELSE 0 END) AS on_sale_cnt,
                       SUM(CASE WHEN on_sale THEN remaining ELSE 0 END) AS available_qty
                FROM (
                    SELECT event_id, status, price, ticket_kind,
                           (status = 'ACTIVE'
                            AND (sale_start_at IS NULL OR sale_start_at <= UTC_TIMESTAMP(3))
                            AND (sale_end_at IS NULL OR sale_end_at > UTC_TIMESTAMP(3))) AS on_sale,
                           CASE WHEN quantity_total IS NULL THEN 1000000
                                ELSE GREATEST(CAST(quantity_total AS SIGNED)
                                              - CAST(quantity_sold AS SIGNED)
                                              - CAST(quantity_reserved AS SIGNED), 0) END AS remaining
                    FROM ticket_types
                ) x
                GROUP BY event_id
            ) tt ON tt.event_id = e.event_id
            LEFT JOIN (
                SELECT event_id, COUNT(*) AS cnt
                FROM registrations
                WHERE status = 'CONFIRMED'
                  AND registered_at >= UTC_TIMESTAMP(3) - INTERVAL 7 DAY
                GROUP BY event_id
            ) pop7 ON pop7.event_id = e.event_id
        """.trimIndent()
    }
}
