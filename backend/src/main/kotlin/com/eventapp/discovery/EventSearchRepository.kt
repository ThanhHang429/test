package com.eventapp.discovery

import com.eventapp.common.util.VietnameseTextNormalizer
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

/**
 * Truy vấn tìm kiếm sự kiện bằng SQL có tham số hoá (SRS U04-R07: không nối chuỗi người dùng).
 *
 * Điều kiện công khai được gắn CỨNG trong câu SQL (U04-R01):
 *   review_status = 'APPROVED' AND lifecycle_status IN ('PUBLISHED') — hoặc thêm 'COMPLETED'
 *   khi includePast=true.
 * nên không tham số nào có thể vượt qua. Sự kiện CANCELLED/SUSPENDED luôn bị loại (U04-R03).
 */
@Repository
class EventSearchRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    data class Criteria(
        val q: String? = null,
        val categoryIds: List<Long> = emptyList(),
        val dateFrom: Instant? = null,
        val dateTo: Instant? = null,
        val city: String? = null,
        val format: String? = null,
        val priceType: String? = null,
        val priceMin: Long? = null,
        val priceMax: Long? = null,
        val availableOnly: Boolean = false,
        val includePast: Boolean = false,
        val sort: String = "START_TIME_ASC",
        val page: Int = 0,
        val size: Int = 20
    )

    /** Một dòng kết quả đã gộp sẵn giá/còn vé (không gọi từng sự kiện — U04 luồng chính bước 4). */
    data class Row(
        val eventId: Long,
        val eventCode: String,
        val title: String,
        val slug: String,
        val shortDescription: String?,
        val eventFormat: String,
        val startAt: Instant,
        val endAt: Instant,
        val timezone: String,
        val city: String?,
        val venueName: String?,
        val capacityLimit: Int?,
        val coverUrl: String?,
        val minPrice: BigDecimal?,
        val maxPrice: BigDecimal?,
        val freeCount: Int,
        val availableQuantity: Long?,
        val onSaleCount: Int,
        val futureCount: Int,
        val activeCount: Int,
        val popularity: Long
    )

    data class Result(val rows: List<Row>, val totalItems: Long)

    fun search(criteria: Criteria): Result {
        val params = MapSqlParameterSource()
        val where = buildWhere(criteria, params)

        val total = jdbc.queryForObject(
            "SELECT COUNT(*) $FROM_AND_JOINS $where",
            params,
            Long::class.java
        ) ?: 0L

        if (total == 0L) return Result(emptyList(), 0L)

        params.addValue("limit", criteria.size)
        params.addValue("offset", criteria.page.toLong() * criteria.size)

        val sql = """
            SELECT e.event_id, e.event_code, e.title, e.slug, e.short_description,
                   e.event_format, e.start_at, e.end_at, e.timezone, e.city, e.venue_name,
                   e.capacity_limit,
                   cov.cover_url,
                   tt.min_price, tt.max_price, tt.free_cnt, tt.available_qty, tt.on_sale_cnt,
                   tt.future_cnt, tt.active_cnt,
                   COALESCE(pop.popularity, 0) AS popularity
            $FROM_AND_JOINS
            $where
            ORDER BY ${orderBy(criteria, params)}
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val rows = jdbc.query(sql, params) { rs, _ -> mapRow(rs) }
        return Result(rows, total)
    }

    /** Lấy thẻ sự kiện theo danh sách id, KHÔNG áp điều kiện công khai (dùng cho mục Yêu thích). */
    fun findRowsByIds(ids: Collection<Long>): List<Row> {
        if (ids.isEmpty()) return emptyList()
        val params = MapSqlParameterSource()
        params.addValue("ids", ids)
        val sql = """
            SELECT e.event_id, e.event_code, e.title, e.slug, e.short_description,
                   e.event_format, e.start_at, e.end_at, e.timezone, e.city, e.venue_name,
                   e.capacity_limit,
                   cov.cover_url,
                   tt.min_price, tt.max_price, tt.free_cnt, tt.available_qty, tt.on_sale_cnt,
                   tt.future_cnt, tt.active_cnt,
                   COALESCE(pop.popularity, 0) AS popularity
            $FROM_AND_JOINS
            WHERE e.event_id IN (:ids)
        """.trimIndent()
        return jdbc.query(sql, params) { rs, _ -> mapRow(rs) }
    }

    /**
     * Danh sách thành phố đang có sự kiện công khai sắp diễn ra — nguồn cho chip lọc địa điểm
     * ở màn Khám phá (FR-U04). Chỉ trả về giá trị thực có trong dữ liệu nên bộ lọc luôn khớp.
     */
    fun cities(): List<String> {
        val sql = """
            SELECT DISTINCT e.city
            FROM events e
            WHERE e.deleted_at IS NULL
              AND e.review_status = 'APPROVED'
              AND e.lifecycle_status = 'PUBLISHED'
              AND e.end_at >= UTC_TIMESTAMP(3)
              AND e.city IS NOT NULL
              AND TRIM(e.city) <> ''
            ORDER BY e.city ASC
            LIMIT 40
        """.trimIndent()
        return jdbc.queryForList(sql, MapSqlParameterSource(), String::class.java)
    }

    private fun mapRow(rs: java.sql.ResultSet): Row = Row(
        eventId = rs.getLong("event_id"),
        eventCode = rs.getString("event_code"),
        title = rs.getString("title"),
        slug = rs.getString("slug"),
        shortDescription = rs.getString("short_description"),
        eventFormat = rs.getString("event_format"),
        startAt = rs.getTimestamp("start_at").toInstant(),
        endAt = rs.getTimestamp("end_at").toInstant(),
        timezone = rs.getString("timezone"),
        city = rs.getString("city"),
        venueName = rs.getString("venue_name"),
        capacityLimit = rs.getObject("capacity_limit")?.let { (it as Number).toInt() },
        coverUrl = rs.getString("cover_url"),
        minPrice = rs.getBigDecimal("min_price"),
        maxPrice = rs.getBigDecimal("max_price"),
        freeCount = (rs.getObject("free_cnt") as? Number)?.toInt() ?: 0,
        availableQuantity = (rs.getObject("available_qty") as? Number)?.toLong(),
        onSaleCount = (rs.getObject("on_sale_cnt") as? Number)?.toInt() ?: 0,
        futureCount = (rs.getObject("future_cnt") as? Number)?.toInt() ?: 0,
        activeCount = (rs.getObject("active_cnt") as? Number)?.toInt() ?: 0,
        popularity = (rs.getObject("popularity") as? Number)?.toLong() ?: 0L
    )

    private fun buildWhere(c: Criteria, params: MapSqlParameterSource): String {
        val conditions = mutableListOf<String>()

        // Điều kiện công khai cố định — không có tham số nào bỏ qua được
        conditions += "e.deleted_at IS NULL"
        conditions += "e.review_status = 'APPROVED'"
        // includePast=true mở thêm sự kiện COMPLETED để màn «Đã kết thúc» có dữ liệu;
        // mặc định chỉ sự kiện đang PUBLISHED.
        conditions += if (c.includePast) {
            "e.lifecycle_status IN ('PUBLISHED', 'COMPLETED')"
        } else {
            "e.lifecycle_status = 'PUBLISHED'"
        }

        if (!c.includePast) {
            conditions += "e.end_at >= UTC_TIMESTAMP(3)"
        }
        c.dateFrom?.let {
            conditions += "e.start_at >= :dateFrom"
            params.addValue("dateFrom", Timestamp.from(it))
        }
        c.dateTo?.let {
            conditions += "e.start_at <= :dateTo"
            params.addValue("dateTo", Timestamp.from(it))
        }
        c.city?.takeIf { it.isNotBlank() }?.let {
            conditions += "e.city = :city"
            params.addValue("city", it.trim())
        }
        c.format?.takeIf { it.isNotBlank() && it.uppercase() != "ANY" }?.let {
            conditions += "e.event_format = :format"
            params.addValue("format", it.uppercase())
        }

        if (c.categoryIds.isNotEmpty()) {
            conditions += """
                EXISTS (SELECT 1 FROM event_category_map ecm
                        WHERE ecm.event_id = e.event_id AND ecm.category_id IN (:categoryIds))
            """.trimIndent()
            params.addValue("categoryIds", c.categoryIds)
        }

        // Lọc giá chỉ tính loại vé đang trong thời gian bán (U04-R06)
        when (c.priceType?.uppercase()) {
            "FREE" -> conditions += "tt.min_price = 0"
            "PAID" -> conditions += "tt.min_price > 0"
        }
        c.priceMin?.let {
            conditions += "tt.min_price >= :priceMin"
            params.addValue("priceMin", it)
        }
        c.priceMax?.let {
            conditions += "tt.min_price <= :priceMax"
            params.addValue("priceMax", it)
        }
        if (c.availableOnly) {
            conditions += "COALESCE(tt.available_qty, 0) > 0"
        }

        // Tìm không dấu: mỗi token là một nhóm điều kiện (AND giữa các token)
        VietnameseTextNormalizer.tokens(c.q).forEachIndexed { index, token ->
            val pattern = VietnameseTextNormalizer.regexPattern(token)
            val key = "q$index"
            params.addValue(key, pattern)
            conditions += """
                (REGEXP_LIKE(e.title, :$key, 'i')
                 OR REGEXP_LIKE(e.short_description, :$key, 'i')
                 OR REGEXP_LIKE(op.public_name, :$key, 'i')
                 OR EXISTS (SELECT 1 FROM event_tag_map etm
                            JOIN event_tags et ON et.tag_id = etm.tag_id
                            WHERE etm.event_id = e.event_id AND REGEXP_LIKE(et.tag_name, :$key, 'i')))
            """.trimIndent()
        }

        return "WHERE " + conditions.joinToString(" AND ")
    }

    private fun orderBy(c: Criteria, params: MapSqlParameterSource): String {
        val idTieBreak = "e.event_id ASC"
        return when (c.sort.uppercase()) {
            "NEWEST" -> "e.created_at DESC, e.event_id DESC"
            "PRICE_ASC" -> "tt.min_price IS NULL, tt.min_price ASC, $idTieBreak"
            "PRICE_DESC" -> "tt.min_price IS NULL, tt.min_price DESC, e.event_id DESC"
            "POPULAR" -> "popularity DESC, $idTieBreak"
            "RELEVANCE" -> {
                val firstToken = VietnameseTextNormalizer.tokens(c.q).firstOrNull()
                if (firstToken == null) {
                    "e.start_at ASC, $idTieBreak"
                } else {
                    val pattern = VietnameseTextNormalizer.regexPattern(firstToken)
                    params.addValue("relPattern", pattern)
                    """
                    (CASE WHEN REGEXP_LIKE(e.title, :relPattern, 'i') THEN 100 ELSE 0 END
                     + CASE WHEN REGEXP_LIKE(e.short_description, :relPattern, 'i') THEN 10 ELSE 0 END) DESC,
                    e.start_at ASC, $idTieBreak
                    """.trimIndent()
                }
            }
            // START_TIME_ASC (mặc định)
            else -> "e.start_at ASC, $idTieBreak"
        }
    }

    companion object {
        /**
         * `tt` = loại vé ACTIVE đang trong thời gian bán; `cov` = ảnh bìa; `pop` = số đăng ký.
         * Dùng derived table để gộp theo lô, tránh N+1.
         */
        private val FROM_AND_JOINS = """
            FROM events e
            LEFT JOIN (
                SELECT event_id, MIN(storage_url) AS cover_url
                FROM event_media
                WHERE is_cover = 1
                GROUP BY event_id
            ) cov ON cov.event_id = e.event_id
            LEFT JOIN (
                SELECT event_id,
                       MIN(CASE WHEN on_sale THEN price END) AS min_price,
                       MAX(CASE WHEN on_sale THEN price END) AS max_price,
                       SUM(CASE WHEN on_sale AND ticket_kind = 'FREE' THEN 1 ELSE 0 END) AS free_cnt,
                       SUM(CASE WHEN on_sale THEN remaining ELSE 0 END) AS available_qty,
                       SUM(CASE WHEN on_sale THEN 1 ELSE 0 END) AS on_sale_cnt,
                       SUM(CASE WHEN status = 'ACTIVE' AND sale_start_at IS NOT NULL
                                 AND sale_start_at > UTC_TIMESTAMP(3) THEN 1 ELSE 0 END) AS future_cnt,
                       SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_cnt
                FROM (
                    SELECT event_id, status, price, ticket_kind, sale_start_at,
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
                SELECT event_id, COUNT(*) AS popularity
                FROM registrations
                WHERE status = 'CONFIRMED'
                GROUP BY event_id
            ) pop ON pop.event_id = e.event_id
            LEFT JOIN organizer_profiles op ON op.organizer_profile_id = e.organizer_profile_id
        """.trimIndent()
    }
}
