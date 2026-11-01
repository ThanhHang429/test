package com.eventapp.common.seed

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Bộ dữ liệu demo quy mô lớn, **bổ sung** vào dữ liệu của [DevDataSeeder] mà không sửa
 * hay xoá bất cứ bản ghi nào đang có (id của dữ liệu cũ giữ nguyên, nên smoke test và
 * Postman collection vẫn chạy đúng).
 *
 * Đặc điểm:
 *  - Chạy **sau** [DevDataSeeder] (`@Order`).
 *  - **Idempotent**: nếu đã có sự kiện mã `EVT-DEMO-%` thì bỏ qua toàn bộ.
 *  - Toàn bộ nằm trong **một transaction**: lỗi giữa đường sẽ rollback sạch, lần khởi
 *    động sau chạy lại từ đầu thay vì để dữ liệu nửa vời.
 *  - Dùng JDBC batch thay vì JPA vì cần **mốc thời gian phân tán** (30–60 ngày) để
 *    dashboard/lọc theo khoảng ngày có số liệu khác nhau thật; các cột `created_at`
 *    trong entity là `insertable = false` nên JPA luôn ghi `now`.
 *  - Sinh ngẫu nhiên có seed cố định → mỗi máy ra cùng một bộ dữ liệu.
 */
@Component
@Profile("dev")
@Order(2)
@ConditionalOnProperty(name = ["app.seed.demo-enabled"], havingValue = "true", matchIfMissing = true)
class DemoDataSeeder(
    private val jdbc: NamedParameterJdbcTemplate,
    private val txManager: PlatformTransactionManager,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val rnd = Random(SEED)
    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    private val zone = ZoneOffset.UTC

    private val counts = linkedMapOf<String, Int>()

    override fun run(vararg args: String) {
        val existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM events WHERE event_code LIKE 'EVT-DEMO-%'",
            MapSqlParameterSource(),
            Long::class.java
        ) ?: 0L
        if (existing > 0) {
            log.info("[seed-demo] Đã có {} sự kiện demo — bỏ qua. Xoá các bản ghi EVT-DEMO-% nếu muốn tạo lại.", existing)
            return
        }

        log.info("[seed-demo] Bắt đầu tạo dữ liệu demo...")
        val startedAt = System.currentTimeMillis()
        TransactionTemplate(txManager).execute { seed() }
        log.info(
            "[seed-demo] Xong sau {} ms — {}",
            System.currentTimeMillis() - startedAt,
            counts.entries.joinToString(", ") { "${it.key}=${it.value}" }
        )
    }

    // =====================================================================
    // Luồng chính
    // =====================================================================

    private fun seed() {
        val roleIds = mapOf(
            "USER" to requireId("SELECT role_id FROM roles WHERE role_code = :code", "USER"),
            "ORGANIZER" to requireId("SELECT role_id FROM roles WHERE role_code = :code", "ORGANIZER")
        )
        val adminId = requireId("SELECT user_id FROM users WHERE email = :code", "admin@eticket.local")

        val categoryIds = loadCategories(adminId)
        val tagIds = loadTags(adminId)

        val userIds = createUsers(roleIds)
        val profiles = createOrganizers(roleIds)
        val organizers = loadApprovedProfiles()

        val events = createEvents(organizers, categoryIds, tagIds)
        createAttendanceAndReviews(events)
        createOrders(events, adminId)
        createFavorites(events)
        createPreferences(userIds, categoryIds)
        createAiTelemetry(events)
        createReviewAudit(adminId)

        counts["users"] = userIds.size
        counts["organizers"] = profiles
    }

    // =====================================================================
    // Danh mục & nhãn
    // =====================================================================

    private data class CatSeed(val code: String, val name: String, val slug: String, val parentCode: String, val order: Int)
    private data class TagSeed(val code: String, val name: String, val slug: String)

    private fun loadCategories(adminId: Long): MutableMap<String, Long> {
        val map = jdbc.query("SELECT category_code, category_id FROM event_categories") { rs, _ ->
            rs.getString(1) to rs.getLong(2)
        }.toMap().toMutableMap()

        val newCats = listOf(
            CatSeed("CLASSICAL", "Nhạc cổ điển", "nhac-co-dien", "MUSIC", 2),
            CatSeed("RUNNING", "Chạy bộ", "chay-bo", "SPORTS", 1),
            CatSeed("FOOTBALL", "Bóng đá", "bong-da", "SPORTS", 2),
            CatSeed("COOKING", "Nấu ăn", "nau-an", "WORKSHOP", 1),
            CatSeed("PHOTO", "Nhiếp ảnh", "nhiep-anh", "WORKSHOP", 2),
            CatSeed("DIGITAL_ART", "Nghệ thuật số", "nghe-thuat-so", "EXHIBITION", 1),
            CatSeed("PAINTING", "Hội hoạ", "hoi-hoa", "EXHIBITION", 2)
        )
        newCats.forEach { c ->
            if (map.containsKey(c.code)) return@forEach
            val id = insert(
                """
                INSERT INTO event_categories
                    (parent_category_id, category_code, category_name, slug, description, is_active, sort_order, created_at, updated_at)
                VALUES (:parent, :code, :name, :slug, :desc, 1, :ord, :ts, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("parent", map[c.parentCode])
                    .addValue("code", c.code)
                    .addValue("name", c.name)
                    .addValue("slug", c.slug)
                    .addValue("desc", "Danh mục demo: ${c.name}")
                    .addValue("ord", c.order)
                    .addValue("ts", ts(now)),
                "category_id"
            )
            map[c.code] = id
            bump("categories")
        }
        return map
    }

    private fun loadTags(adminId: Long): MutableMap<String, Long> {
        val map = jdbc.query("SELECT tag_code, tag_id FROM event_tags") { rs, _ ->
            rs.getString(1) to rs.getLong(2)
        }.toMap().toMutableMap()

        val newTags = listOf(
            TagSeed("JAZZ", "Jazz", "jazz"),
            TagSeed("CLASSICAL", "Cổ điển", "co-dien"),
            TagSeed("ROCK", "Rock", "rock"),
            TagSeed("HIPHOP", "Hiphop", "hiphop"),
            TagSeed("KYNANG", "Kỹ năng", "ky-nang"),
            TagSeed("SINHVIEN", "Sinh viên", "sinh-vien"),
            TagSeed("GIADINH", "Gia đình", "gia-dinh"),
            TagSeed("NGOAITROI", "Ngoài trời", "ngoai-troi"),
            TagSeed("SUCKHOE", "Sức khoẻ", "suc-khoe"),
            TagSeed("THIENNGUYEN", "Thiện nguyện", "thien-nguyen"),
            TagSeed("AMTHUC", "Ẩm thực", "am-thuc"),
            TagSeed("NHIEPANH", "Nhiếp ảnh", "nhiep-anh"),
            TagSeed("KHOINGHIEP", "Khởi nghiệp", "khoi-nghiep"),
            TagSeed("DIGITALART", "Nghệ thuật số", "nghe-thuat-so-tag"),
            TagSeed("NGHEHTHUAT", "Nghệ thuật", "nghe-thuat"),
            TagSeed("GIOTRE", "Giới trẻ", "gioi-tre")
        )
        newTags.forEach { t ->
            if (map.containsKey(t.code)) return@forEach
            val id = insert(
                """
                INSERT INTO event_tags (tag_code, tag_name, slug, is_active, created_by_user_id, created_at, updated_at)
                VALUES (:code, :name, :slug, 1, :by, :ts, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("code", t.code)
                    .addValue("name", t.name)
                    .addValue("slug", t.slug)
                    .addValue("by", adminId)
                    .addValue("ts", ts(now)),
                "tag_id"
            )
            map[t.code] = id
            bump("tags")
        }
        return map
    }

    // =====================================================================
    // Người dùng & organizer
    // =====================================================================

    private fun createUsers(roleIds: Map<String, Long>): List<Long> {
        val passwordHash = passwordEncoder.encode(DevDataSeeder.DEFAULT_PASSWORD)
        val ids = mutableListOf<Long>()
        USER_NAMES.forEachIndexed { idx, name ->
            val email = "user${idx + 4}@eticket.local"
            val userId = insert(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified_at,
                                   locale, timezone, last_login_at, created_at, updated_at)
                VALUES (:email, :pwd, :name, 'ACTIVE', :verified, 'vi-VN', 'Asia/Ho_Chi_Minh', :login, :created, :created)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("email", email)
                    .addValue("pwd", passwordHash)
                    .addValue("name", name)
                    .addValue("verified", ts(now.minus(60, ChronoUnit.DAYS)))
                    .addValue("login", ts(now.minus(rnd.nextLong(1, 20), ChronoUnit.DAYS)))
                    .addValue("created", ts(now.minus(rnd.nextLong(30, 120), ChronoUnit.DAYS))),
                "user_id"
            )
            exec(
                "INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (:u, :r, :ts)",
                MapSqlParameterSource().addValue("u", userId).addValue("r", roleIds["USER"]).addValue("ts", ts(now))
            )
            ids += userId
            bump("users")
        }
        return ids
    }

    private data class OrgSeed(val email: String, val name: String, val org: String, val status: String, val desc: String)

    private fun createOrganizers(roleIds: Map<String, Long>): Int {
        val passwordHash = passwordEncoder.encode(DevDataSeeder.DEFAULT_PASSWORD)
        val seeds = listOf(
            OrgSeed("organizer3@eticket.local", "Sài Gòn Show", "Sài Gòn Show", "APPROVED",
                "Đơn vị tổ chức liveshow và sự kiện âm nhạc tại TP.HCM."),
            OrgSeed("organizer4@eticket.local", "Đà Nẵng Events", "Đà Nẵng Events", "APPROVED",
                "Chuyên hội thảo, workshop và giải chạy khu vực miền Trung."),
            OrgSeed("organizer5@eticket.local", "Hà Nội Arts", "Hà Nội Arts", "SUSPENDED",
                "Tổ chức triển lãm nghệ thuật (đang bị đình chỉ để test quyền)."),
            OrgSeed("organizer6@eticket.local", "Cần Thơ Media", "Cần Thơ Media", "MORE_INFO",
                "Truyền thông và sự kiện cộng đồng (đang chờ bổ sung hồ sơ).")
        )
        seeds.forEach { s ->
            val userId = insert(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified_at,
                                   locale, timezone, created_at, updated_at)
                VALUES (:email, :pwd, :name, 'ACTIVE', :verified, 'vi-VN', 'Asia/Ho_Chi_Minh', :ts, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("email", s.email)
                    .addValue("pwd", passwordHash)
                    .addValue("name", s.name)
                    .addValue("verified", ts(now.minus(90, ChronoUnit.DAYS)))
                    .addValue("ts", ts(now.minus(90, ChronoUnit.DAYS))),
                "user_id"
            )
            listOfNotNull(roleIds["ORGANIZER"], roleIds["USER"]).forEach { roleId ->
                exec(
                    "INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (:u, :r, :ts)",
                    MapSqlParameterSource().addValue("u", userId).addValue("r", roleId).addValue("ts", ts(now))
                )
            }
            exec(
                """
                INSERT INTO organizer_profiles
                    (user_id, organization_name, public_name, description, contact_email, website_url,
                     status, submitted_at, created_at, updated_at)
                VALUES (:u, :org, :org, :desc, :email, :web, :status, :submitted, :ts, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("u", userId)
                    .addValue("org", s.org)
                    .addValue("desc", s.desc)
                    .addValue("email", s.email)
                    .addValue("web", "https://example.com/${s.org.lowercase().replace(' ', '-')}")
                    .addValue("status", s.status)
                    .addValue("submitted", ts(now.minus(60, ChronoUnit.DAYS)))
                    .addValue("ts", ts(now.minus(60, ChronoUnit.DAYS)))
            )
            bump("organizers")
        }
        return seeds.size
    }

    private data class Organizer(val profileId: Long, val userId: Long)

    private fun loadApprovedProfiles(): List<Organizer> =
        jdbc.query(
            """
            SELECT organizer_profile_id, user_id FROM organizer_profiles
            WHERE status = 'APPROVED' ORDER BY organizer_profile_id
            """.trimIndent()
        ) { rs, _ -> Organizer(rs.getLong(1), rs.getLong(2)) }

    // =====================================================================
    // Sự kiện
    // =====================================================================

    private data class EventRef(
        val id: Long,
        val code: String,
        val title: String,
        val lifecycle: String,
        val startAt: Instant,
        val endAt: Instant,
        val tier: Int,
        val ticketTypes: List<TicketRef>
    )

    private data class TicketRef(val id: Long, val name: String, val price: Long)

    private fun createEvents(
        organizers: List<Organizer>,
        categoryIds: Map<String, Long>,
        tagIds: Map<String, Long>
    ): List<EventRef> {
        val plans = buildPlans()
        val result = mutableListOf<EventRef>()

        plans.forEach { p ->
            val organizer = organizers[p.n % organizers.size]
            val slug = "demo-%03d-%s".format(p.n, slugify(p.template))
            val code = "EVT-DEMO-%03d".format(p.n)
            val startAt = now.plus(p.startOffsetDays, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.HOURS)
                .plus(p.startHour, ChronoUnit.HOURS)
            val endAt = startAt.plus(p.durationHours, ChronoUnit.HOURS)
            val published = p.lifecycle != "UNPUBLISHED"

            val eventId = insert(
                """
                INSERT INTO events
                    (event_code, organizer_profile_id, created_by_user_id, title, slug, short_description,
                     description, event_format, review_status, lifecycle_status, visibility,
                     start_at, end_at, timezone, registration_start_at, registration_end_at,
                     venue_name, address_line, city, province, country_code, online_url, capacity_limit,
                     participation_conditions, cancellation_policy, published_at, cancelled_at, created_at, updated_at)
                VALUES
                    (:code, :orgProfile, :owner, :title, :slug, :short,
                     :desc, :format, :reviewStatus, :lifecycle, 'PUBLIC',
                     :startAt, :endAt, 'Asia/Ho_Chi_Minh', :regStart, :regEnd,
                     :venue, :address, :city, :city, 'VN', :onlineUrl, :capacity,
                     :conditions, :policy, :publishedAt, :cancelledAt, :createdAt, :createdAt)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("code", code)
                    .addValue("orgProfile", organizer.profileId)
                    .addValue("owner", organizer.userId)
                    .addValue("title", p.title)
                    .addValue("slug", slug)
                    .addValue("short", p.shortDescription)
                    .addValue("desc", p.description)
                    .addValue("format", p.format)
                    .addValue("reviewStatus", p.reviewStatus)
                    .addValue("lifecycle", p.lifecycle)
                    .addValue("startAt", ts(startAt))
                    .addValue("endAt", ts(endAt))
                    .addValue("regStart", ts(startAt.minus(30, ChronoUnit.DAYS)))
                    .addValue("regEnd", ts(startAt.minus(2, ChronoUnit.HOURS)))
                    .addValue("venue", p.venue)
                    .addValue("address", p.venue?.let { "Số ${rnd.nextInt(1, 300)}, $it" })
                    .addValue("city", p.city)
                    .addValue("onlineUrl", if (p.format == "ONLINE") "https://meet.example.com/demo-${p.n}" else null)
                    .addValue("capacity", p.capacity)
                    .addValue("conditions", "Mang theo vé điện tử (QR) khi tham dự.")
                    .addValue("policy", "Huỷ trước 48 giờ được hoàn 100%.")
                    .addValue("publishedAt", if (published) ts(now.minus(rnd.nextLong(10, 40), ChronoUnit.DAYS)) else null)
                    .addValue("cancelledAt", if (p.lifecycle == "CANCELLED") ts(now.minus(3, ChronoUnit.DAYS)) else null)
                    .addValue("createdAt", ts(now.minus(rnd.nextLong(40, 80), ChronoUnit.DAYS))),
                "event_id"
            )
            bump("events")

            exec(
                "INSERT INTO event_category_map (event_id, category_id, assigned_at) VALUES (:e, :c, :ts)",
                MapSqlParameterSource()
                    .addValue("e", eventId)
                    .addValue("c", categoryIds[p.categoryCode] ?: categoryIds.getValue("COMMUNITY"))
                    .addValue("ts", ts(now))
            )

            p.tagCodes.forEach { tagCode ->
                val tagId = tagIds[tagCode] ?: return@forEach
                exec(
                    """
                    INSERT IGNORE INTO event_tag_map (event_id, tag_id, assigned_by_user_id, assigned_at)
                    VALUES (:e, :t, :by, :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("e", eventId)
                        .addValue("t", tagId)
                        .addValue("by", organizer.userId)
                        .addValue("ts", ts(now))
                )
            }

            exec(
                """
                INSERT INTO event_media
                    (event_id, media_type, storage_url, original_file_name, mime_type, alt_text,
                     sort_order, is_cover, uploaded_by_user_id, created_at)
                VALUES (:e, 'IMAGE', :url, 'cover.jpg', 'image/jpeg', :alt, 0, 1, :by, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("e", eventId)
                    .addValue("url", "https://picsum.photos/seed/$slug/1200/675")
                    .addValue("alt", p.title)
                    .addValue("by", organizer.userId)
                    .addValue("ts", ts(now))
            )

            val ticketTypes = createTicketTypes(eventId, p)
            result += EventRef(eventId, code, p.title, p.lifecycle, startAt, endAt, p.tier, ticketTypes)
        }
        return result
    }

    private data class EventPlan(
        val n: Int,
        val title: String,
        val shortDescription: String,
        val description: String,
        val template: String,
        val categoryCode: String,
        val tagCodes: List<String>,
        val format: String,
        val city: String?,
        val venue: String?,
        val startOffsetDays: Long,
        val startHour: Long,
        val durationHours: Long,
        val reviewStatus: String,
        val lifecycle: String,
        val tier: Int,
        val capacity: Int
    )

    private fun buildPlans(): List<EventPlan> {
        val plans = mutableListOf<EventPlan>()
        var n = 0

        // 1) 28 sự kiện sắp/đang diễn ra, đã duyệt và công khai
        repeat(28) { i ->
            n += 1
            val tpl = TEMPLATES[i % TEMPLATES.size]
            val format = when {
                i % 9 == 0 -> "ONLINE"
                i % 13 == 0 -> "HYBRID"
                else -> "OFFLINE"
            }
            val city = if (format == "ONLINE") null else CITIES[i % CITIES.size]
            val tier = i % 5
            plans += EventPlan(
                n = n,
                title = "${tpl.title} #$n${city?.let { " — $it" } ?: " — Trực tuyến"}",
                shortDescription = tpl.short,
                description = tpl.description,
                template = tpl.title,
                categoryCode = tpl.categoryCode,
                tagCodes = TAGS_BY_CATEGORY[tpl.categoryCode] ?: emptyList(),
                format = format,
                city = city,
                venue = if (format == "ONLINE") null else "${city} ${VENUES[i % VENUES.size]}",
                startOffsetDays = 2L + (i * 4L) % 116,
                startHour = 9L + (i % 10),
                durationHours = 2L + (i % 6),
                reviewStatus = "APPROVED",
                lifecycle = "PUBLISHED",
                tier = tier,
                capacity = CAPACITIES[i % CAPACITIES.size]
            )
        }

        // 2) 10 sự kiện đã kết thúc — nguồn dữ liệu cho đánh giá, dashboard, báo cáo
        repeat(10) { i ->
            n += 1
            val tpl = TEMPLATES[(i + 3) % TEMPLATES.size]
            val format = if (i % 4 == 0) "ONLINE" else "OFFLINE"
            val city = if (format == "ONLINE") null else CITIES[(i + 2) % CITIES.size]
            plans += EventPlan(
                n = n,
                title = "${tpl.title} #$n${city?.let { " — $it" } ?: " — Trực tuyến"}",
                shortDescription = tpl.short,
                description = tpl.description,
                template = tpl.title,
                categoryCode = tpl.categoryCode,
                tagCodes = TAGS_BY_CATEGORY[tpl.categoryCode] ?: emptyList(),
                format = format,
                city = city,
                venue = if (format == "ONLINE") null else "${city} ${VENUES[(i + 1) % VENUES.size]}",
                startOffsetDays = -(5L + i * 5L),
                startHour = 18L,
                durationHours = 3L,
                reviewStatus = "APPROVED",
                lifecycle = "COMPLETED",
                tier = 2,
                capacity = CAPACITIES[(i + 2) % CAPACITIES.size]
            )
        }

        // 3) 7 sự kiện ở các trạng thái đặc biệt — kiểm thử phân quyền / luồng duyệt
        val specials = listOf(
            Triple("DRAFT", "UNPUBLISHED", 20L),
            Triple("PENDING_REVIEW", "UNPUBLISHED", 24L),
            Triple("REVISION_REQUIRED", "UNPUBLISHED", 28L),
            Triple("REJECTED", "UNPUBLISHED", 32L),
            Triple("APPROVED", "SUSPENDED", 10L),
            Triple("APPROVED", "CANCELLED", 14L),
            Triple("APPROVED", "UNPUBLISHED", 18L)
        )
        specials.forEachIndexed { i, (reviewStatus, lifecycle, offset) ->
            n += 1
            val tpl = TEMPLATES[(i + 5) % TEMPLATES.size]
            plans += EventPlan(
                n = n,
                title = "${tpl.title} #$n — ${CITIES[i % CITIES.size]}",
                shortDescription = "Sự kiện ở trạng thái $reviewStatus/$lifecycle (dữ liệu demo).",
                description = "Dùng để kiểm thử luồng duyệt và phân quyền: $reviewStatus / $lifecycle.",
                template = tpl.title,
                categoryCode = tpl.categoryCode,
                tagCodes = emptyList(),
                format = "OFFLINE",
                city = CITIES[i % CITIES.size],
                venue = "${CITIES[i % CITIES.size]} ${VENUES[i % VENUES.size]}",
                startOffsetDays = offset,
                startHour = 19L,
                durationHours = 2L,
                reviewStatus = reviewStatus,
                lifecycle = lifecycle,
                tier = 1,
                capacity = 300
            )
        }

        return plans
    }

    private fun createTicketTypes(eventId: Long, p: EventPlan): List<TicketRef> {
        val specs: List<Triple<String, Long, Int>> = when (p.tier) {
            0 -> listOf(Triple("Vé miễn phí", 0L, 300))
            1 -> listOf(Triple("Vé Standard", 120_000L, 200), Triple("Vé Sinh viên", 60_000L, 120))
            2 -> listOf(Triple("Vé Standard", 300_000L, 250), Triple("Vé VIP", 750_000L, 60))
            3 -> listOf(
                Triple("Vé Standard", 1_200_000L, 150),
                Triple("Vé VIP", 2_500_000L, 40),
                Triple("Vé Premium", 4_000_000L, 20)
            )
            else -> listOf(Triple("Vé vào cửa", 0L, 150), Triple("Vé Standard", 250_000L, 180))
        }

        val completed = p.lifecycle == "COMPLETED"
        val salePattern = p.n % 6

        return specs.mapIndexed { idx, (name, price, total) ->
            val kind = if (price == 0L) "FREE" else "PAID"
            var status = "ACTIVE"
            var sold = 0
            val saleStart: Instant
            val saleEnd: Instant

            when {
                completed -> {
                    saleStart = p.startOffsetDays.let { now.plus(it, ChronoUnit.DAYS) }.minus(30, ChronoUnit.DAYS)
                    saleEnd = p.startOffsetDays.let { now.plus(it, ChronoUnit.DAYS) }.minus(1, ChronoUnit.HOURS)
                    status = "ENDED"
                    sold = (total * (55 + rnd.nextInt(0, 40)) / 100).coerceAtMost(total)
                }
                salePattern == 0 -> {
                    saleStart = now.plus(10, ChronoUnit.DAYS)
                    saleEnd = now.plus(45, ChronoUnit.DAYS)
                }
                salePattern == 1 -> {
                    saleStart = now.minus(40, ChronoUnit.DAYS)
                    saleEnd = now.minus(2, ChronoUnit.DAYS)
                    status = "ENDED"
                    sold = total / 2
                }
                salePattern == 2 -> {
                    saleStart = now.minus(20, ChronoUnit.DAYS)
                    saleEnd = now.plus(30, ChronoUnit.DAYS)
                    sold = total
                }
                else -> {
                    saleStart = now.minus(20, ChronoUnit.DAYS)
                    saleEnd = now.plus(30, ChronoUnit.DAYS)
                    sold = total * (10 + idx * 15) / 100
                }
            }

            val id = insert(
                """
                INSERT INTO ticket_types
                    (event_id, ticket_type_name, description, ticket_kind, price, currency,
                     quantity_total, quantity_sold, quantity_reserved, min_per_order, max_per_user,
                     sale_start_at, sale_end_at, status, created_at, updated_at)
                VALUES
                    (:event, :name, :desc, :kind, :price, 'VND',
                     :total, :sold, 0, 1, :maxPerUser,
                     :saleStart, :saleEnd, :status, :ts, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("event", eventId)
                    .addValue("name", name)
                    .addValue("desc", "Hạng vé demo cho sự kiện #${p.n}")
                    .addValue("kind", kind)
                    .addValue("price", price)
                    .addValue("total", total)
                    .addValue("sold", sold.coerceAtMost(total))
                    .addValue("maxPerUser", if (kind == "FREE") 2 else 4)
                    .addValue("saleStart", ts(saleStart))
                    .addValue("saleEnd", ts(saleEnd))
                    .addValue("status", status)
                    .addValue("ts", ts(now)),
                "ticket_type_id"
            )
            bump("ticket_types")
            TicketRef(id, name, price)
        }
    }

    // =====================================================================
    // Đăng ký, vé, check-in, đánh giá
    // =====================================================================

    private data class Attendance(val eventId: Long, val userId: Long, val reviewId: Long?)

    private fun createAttendanceAndReviews(events: List<EventRef>): List<Attendance> {
        val allUserIds = jdbc.query("SELECT user_id FROM users ORDER BY user_id") { rs, _ -> rs.getLong(1) }
        val result = mutableListOf<Attendance>()

        events.forEach { event ->
            if (event.ticketTypes.isEmpty()) return@forEach
            val ticket = event.ticketTypes.first()
            val isCompleted = event.lifecycle == "COMPLETED"
            val isPublished = event.lifecycle == "PUBLISHED"
            if (!isCompleted && !isPublished) return@forEach

            val attendeeCount = if (isCompleted) rnd.nextInt(8, 17) else rnd.nextInt(0, 6)
            val attendees = allUserIds.shuffled(rnd).take(attendeeCount)

            attendees.forEach { userId ->
                val regCode = "DEMO-REG-${event.id}-$userId"
                val regId = insert(
                    """
                    INSERT INTO registrations
                        (registration_code, user_id, event_id, ticket_type_id, quantity, status,
                         idempotency_key, registered_at, created_at, updated_at)
                    VALUES (:code, :u, :e, :tt, 1, 'CONFIRMED', :idem, :ts, :ts, :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("code", regCode)
                        .addValue("u", userId)
                        .addValue("e", event.id)
                        .addValue("tt", ticket.id)
                        .addValue("idem", "demo-reg-$regCode")
                        .addValue("ts", ts(event.startAt.minus(rnd.nextLong(3, 25), ChronoUnit.DAYS))),
                    "registration_id"
                )

                val ticketId = insert(
                    """
                    INSERT INTO tickets
                        (ticket_code, qr_token_hash, user_id, event_id, ticket_type_id, registration_id,
                         status, issued_at, created_at, updated_at)
                    VALUES (:code, :hash, :u, :e, :tt, :reg, :status, :ts, :ts, :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("code", "DEMO-TKT-${event.id}-$userId")
                        .addValue("hash", qrHash("demo-${event.id}-$userId"))
                        .addValue("u", userId)
                        .addValue("e", event.id)
                        .addValue("tt", ticket.id)
                        .addValue("reg", regId)
                        .addValue("status", if (isCompleted) "CHECKED_IN" else "VALID")
                        .addValue("ts", ts(event.startAt.minus(rnd.nextLong(3, 25), ChronoUnit.DAYS))),
                    "ticket_id"
                )
                bump("registrations")

                if (isCompleted) {
                    exec(
                        """
                        INSERT INTO check_ins (ticket_id, event_id, actor_user_id, check_in_method, checked_in_at, note)
                        VALUES (:t, :e, :u, 'SELF_SCAN', :ts, 'Dữ liệu demo')
                        """.trimIndent(),
                        MapSqlParameterSource()
                            .addValue("t", ticketId)
                            .addValue("e", event.id)
                            .addValue("u", userId)
                            .addValue("ts", ts(event.endAt.minus(rnd.nextLong(10, 120), ChronoUnit.MINUTES)))
                    )
                    bump("check_ins")
                }

                // ~75% người tham dự viết đánh giá
                var reviewId: Long? = null
                if (isCompleted && rnd.nextInt(100) < 75) {
                    reviewId = createReview(event, userId)
                }
                result += Attendance(event.id, userId, reviewId)
            }
        }
        return result
    }

    private fun createReview(event: EventRef, userId: Long): Long {
        val rating = weightedRating()
        // Một lần rút duy nhất — nếu rút lại ở từng nhánh thì xác suất bị lệch.
        val status = when (rnd.nextInt(100)) {
            in 0..85 -> "VISIBLE"
            in 86..92 -> "PENDING"
            in 93..97 -> "HIDDEN"
            else -> "REMOVED"
        }
        val text = if (status == "PENDING") PENDING_TEXTS[rnd.nextInt(PENDING_TEXTS.size)] else reviewText(rating)
        val createdAt = minOf(
            event.endAt.plus(rnd.nextLong(1, 21), ChronoUnit.DAYS),
            now.minus(rnd.nextLong(1, 48), ChronoUnit.HOURS)
        )
        val id = insert(
            """
            INSERT INTO reviews (user_id, event_id, rating, review_text, status, created_at, updated_at)
            VALUES (:u, :e, :rating, :text, :status, :ts, :ts)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("u", userId)
                .addValue("e", event.id)
                .addValue("rating", rating)
                .addValue("text", text)
                .addValue("status", status)
                .addValue("ts", ts(createdAt)),
            "review_id"
        )
        bump("reviews")
        return id
    }

    private fun weightedRating(): Int {
        val r = rnd.nextInt(100)
        return when {
            r < 42 -> 5
            r < 72 -> 4
            r < 87 -> 3
            r < 95 -> 2
            else -> 1
        }
    }

    private fun reviewText(rating: Int): String = when (rating) {
        5 -> REVIEW_TEXTS_5[rnd.nextInt(REVIEW_TEXTS_5.size)]
        4 -> REVIEW_TEXTS_4[rnd.nextInt(REVIEW_TEXTS_4.size)]
        3 -> REVIEW_TEXTS_3[rnd.nextInt(REVIEW_TEXTS_3.size)]
        2 -> REVIEW_TEXTS_2[rnd.nextInt(REVIEW_TEXTS_2.size)]
        else -> REVIEW_TEXTS_1[rnd.nextInt(REVIEW_TEXTS_1.size)]
    }

    // =====================================================================
    // Đơn hàng & thanh toán
    // =====================================================================

    private fun createOrders(events: List<EventRef>, adminId: Long) {
        val buyers = jdbc.query("SELECT user_id FROM users ORDER BY user_id") { rs, _ -> rs.getLong(1) }
        events.filter { it.lifecycle == "PUBLISHED" }.forEach { event ->
            val paidTicket = event.ticketTypes.firstOrNull { it.price > 0 } ?: return@forEach
            val orderCount = rnd.nextInt(2, 7)
            repeat(orderCount) { seq ->
                val buyer = buyers[rnd.nextInt(buyers.size)]
                val quantity = rnd.nextInt(1, 4)
                val unitPrice = paidTicket.price
                val subtotal = unitPrice * quantity
                val status = when (rnd.nextInt(100)) {
                    in 0..54 -> "PAID"
                    in 55..74 -> "PENDING_PAYMENT"
                    in 75..84 -> "EXPIRED"
                    in 85..91 -> "REFUNDED"
                    else -> "PARTIALLY_REFUNDED"
                }
                val createdAt = now.minus(rnd.nextLong(1, 56), ChronoUnit.DAYS)
                val orderId = insert(
                    """
                    INSERT INTO orders
                        (order_code, user_id, event_id, status, currency, subtotal_amount, discount_amount,
                         platform_fee_amount, total_amount, idempotency_key, payment_due_at, paid_at,
                         cancelled_at, expired_at, created_at, updated_at)
                    VALUES
                        (:code, :u, :e, :status, 'VND', :subtotal, 0,
                         0, :total, :idem, :due, :paidAt,
                         :cancelledAt, :expiredAt, :createdAt, :createdAt)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("code", "DEMO-ORD-${event.id}-$seq")
                        .addValue("u", buyer)
                        .addValue("e", event.id)
                        .addValue("status", status)
                        .addValue("subtotal", subtotal)
                        .addValue("total", subtotal)
                        .addValue("idem", "demo-order-${event.id}-$seq")
                        .addValue("due", ts(createdAt.plus(1, ChronoUnit.DAYS)))
                        .addValue("paidAt", if (status == "PAID" || status == "REFUNDED" || status == "PARTIALLY_REFUNDED")
                            ts(createdAt.plus(2, ChronoUnit.HOURS)) else null)
                        .addValue("cancelledAt", if (status == "REFUNDED") ts(createdAt.plus(5, ChronoUnit.DAYS)) else null)
                        .addValue("expiredAt", if (status == "EXPIRED") ts(createdAt.plus(1, ChronoUnit.DAYS)) else null)
                        .addValue("createdAt", ts(createdAt)),
                    "order_id"
                )
                bump("orders")

                insert(
                    """
                    INSERT INTO order_items
                        (order_id, ticket_type_id, ticket_name_snapshot, quantity, unit_price,
                         discount_amount, line_total, currency, created_at)
                    VALUES (:order, :tt, :name, :qty, :price, 0, :total, 'VND', :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("order", orderId)
                        .addValue("tt", paidTicket.id)
                        .addValue("name", paidTicket.name)
                        .addValue("qty", quantity)
                        .addValue("price", unitPrice)
                        .addValue("total", subtotal)
                        .addValue("ts", ts(createdAt)),
                    "order_item_id"
                )
                bump("order_items")

                val paymentStatus = when (status) {
                    "PAID", "REFUNDED", "PARTIALLY_REFUNDED" -> "SUCCESS"
                    "PENDING_PAYMENT" -> "PENDING"
                    else -> null
                } ?: return@repeat

                exec(
                    """
                    INSERT INTO payments
                        (payment_reference, order_id, provider_code, payment_method, amount, currency, status,
                         provider_transaction_id, provider_reference, idempotency_key, initiated_at,
                         verified_at, verification_source, verified_by_user_id, created_at, updated_at)
                    VALUES
                        (:ref, :order, :provider, :method, :amount, 'VND', :status,
                         :txn, :txn, :idem, :initiated,
                         :verifiedAt, :source, :admin, :ts, :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("ref", "DEMO-PAY-${event.id}-$seq")
                        .addValue("order", orderId)
                        .addValue("provider", if (seq % 3 == 0) "VNPAY" else "MOCK_BANK")
                        .addValue("method", if (seq % 2 == 0) "BANK_TRANSFER_QR" else "WALLET")
                        .addValue("amount", subtotal)
                        .addValue("status", paymentStatus)
                        .addValue("txn", "DEMO-TXN-${event.id}-$seq")
                        .addValue("idem", "demo-pay-${event.id}-$seq")
                        .addValue("initiated", ts(createdAt))
                        .addValue("verifiedAt", if (paymentStatus == "SUCCESS") ts(createdAt.plus(2, ChronoUnit.HOURS)) else null)
                        .addValue("source", if (paymentStatus == "SUCCESS") "PROVIDER_WEBHOOK" else null)
                        .addValue("admin", adminId)
                        .addValue("ts", ts(createdAt))
                )
                bump("payments")
            }
        }
    }

    // =====================================================================
    // Yêu thích, sở thích, telemetry, audit
    // =====================================================================

    private fun createFavorites(events: List<EventRef>) {
        val candidates = events.filter { it.lifecycle == "PUBLISHED" }
        if (candidates.isEmpty()) return
        val users = jdbc.query("SELECT user_id FROM users ORDER BY user_id") { rs, _ -> rs.getLong(1) }
        val seen = mutableSetOf<Pair<Long, Long>>()

        repeat(180) {
            val user = users[rnd.nextInt(users.size)]
            val event = candidates[rnd.nextInt(candidates.size)]
            if (!seen.add(user to event.id)) return@repeat
            exec(
                """
                INSERT IGNORE INTO favorites (user_id, event_id, created_at)
                VALUES (:u, :e, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("u", user)
                    .addValue("e", event.id)
                    .addValue("ts", ts(now.minus(rnd.nextLong(1, 40), ChronoUnit.DAYS)))
            )
            bump("favorites")
        }
    }

    private fun createPreferences(userIds: List<Long>, categoryIds: Map<String, Long>) {
        val allCategories = categoryIds.values.toList()
        val cities = listOf("Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Huế", "Hải Phòng")
        val priceModes = listOf("ANY", "FREE_ONLY", "PAID_ONLY")
        val formats = listOf("ANY", "ONSITE", "ONLINE")

        userIds.forEach { userId ->
            val picked = allCategories.shuffled(rnd).take(rnd.nextInt(1, 4))
            val budget = listOf(0L, 200_000L, 500_000L, 1_000_000L, 2_000_000L)[rnd.nextInt(5)]
            val settings = """
                {"priceMode":"${priceModes[rnd.nextInt(priceModes.size)]}",
                 "maxBudgetVnd":$budget,
                 "preferredDays":["WEEKDAY","WEEKEND"],
                 "onboardingCompleted":true,
                 "version":1}
            """.trimIndent().replace("\n", "").replace(" ", "")

            exec(
                """
                INSERT IGNORE INTO user_preferences
                    (user_id, preferred_categories_json, preferred_city, preferred_event_formats_json,
                     personalization_enabled, profile_visibility, allow_product_emails, settings_json, updated_at)
                VALUES (:u, :cats, :city, :formats, 1, 'PRIVATE', 0, :settings, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("u", userId)
                    .addValue("cats", picked.joinToString(",", "[", "]"))
                    .addValue("city", cities[rnd.nextInt(cities.size)])
                    .addValue("formats", "[\"${formats[rnd.nextInt(formats.size)]}\"]")
                    .addValue("settings", settings)
                    .addValue("ts", ts(now.minus(rnd.nextLong(1, 30), ChronoUnit.DAYS)))
            )
            bump("preferences")
        }
    }

    private fun createAiTelemetry(events: List<EventRef>) {
        val features = listOf("EVENT_RECOMMENDATION", "EVENT_DESCRIPTION", "CHATBOT", "ORGANIZER_ANALYTICS")
        val users = jdbc.query("SELECT user_id FROM users ORDER BY user_id") { rs, _ -> rs.getLong(1) }

        repeat(40) { i ->
            val feature = features[i % features.size]
            val failed = i % 11 == 0
            val createdAt = now.minus(rnd.nextLong(1, 7 * 24 * 60).toLong(), ChronoUnit.MINUTES)
            val latency = if (failed) 5000 else rnd.nextInt(180, 1600)
            val eventId = events[rnd.nextInt(events.size)].id
            exec(
                """
                INSERT INTO ai_requests
                    (user_id, event_id, feature_code, provider_code, model_code, status,
                     input_summary, output_summary, input_tokens, output_tokens, latency_ms,
                     error_code, correlation_id, created_at, completed_at)
                VALUES
                    (:u, :e, :feature, 'mock', 'mock-rules-v1', :status,
                     :in, :out, :inTok, :outTok, :latency,
                     :error, :corr, :ts, :done)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("u", users[rnd.nextInt(users.size)])
                    .addValue("e", eventId)
                    .addValue("feature", feature)
                    .addValue("status", if (failed) "FAILED" else "SUCCEEDED")
                    .addValue("in", "feature=$feature, eventId=$eventId (dữ liệu demo)")
                    .addValue("out", if (failed) null else "Sinh xong kết quả demo cho $feature")
                    .addValue("inTok", rnd.nextInt(80, 400))
                    .addValue("outTok", if (failed) 0 else rnd.nextInt(40, 300))
                    .addValue("latency", latency)
                    .addValue("error", if (failed) "AI_TIMEOUT" else null)
                    .addValue("corr", "demo-ai-%04d".format(i))
                    .addValue("ts", ts(createdAt))
                    .addValue("done", ts(createdAt.plus(latency.toLong(), ChronoUnit.MILLIS)))
            )
            bump("ai_requests")
        }

        val services = listOf("PAYMENT", "FCM", "AI", "EMAIL")
        val operations = mapOf(
            "PAYMENT" to "webhook.verify",
            "FCM" to "push.send",
            "AI" to "provider.complete",
            "EMAIL" to "mail.send"
        )
        repeat(60) { i ->
            val service = services[i % services.size]
            val failed = i % 7 == 0 && service != "AI"
            val createdAt = now.minus(rnd.nextLong(1, 5 * 24 * 60).toLong(), ChronoUnit.MINUTES)
            exec(
                """
                INSERT INTO integration_health_logs
                    (service_code, operation_code, result_status, error_code, message, latency_ms,
                     correlation_id, metadata_redacted, actor_user_id, created_at)
                VALUES (:service, :op, :status, :error, :msg, :latency, :corr, :meta, :actor, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("service", service)
                    .addValue("op", operations[service])
                    .addValue("status", if (failed) "ERROR" else "SUCCESS")
                    .addValue("error", if (failed) "${service}_TIMEOUT" else null)
                    .addValue("msg", if (failed) "Gọi $service quá thời gian chờ (dữ liệu demo)" else "Gọi $service thành công (dữ liệu demo)")
                    .addValue("latency", if (failed) rnd.nextInt(5000, 12000) else rnd.nextInt(40, 900))
                    .addValue("corr", "demo-int-%04d".format(i))
                    .addValue("meta", """{"demo":true,"service":"$service"}""")
                    .addValue("actor", users[rnd.nextInt(users.size)])
                    .addValue("ts", ts(createdAt))
            )
            bump("integration_health_logs")
        }
    }

    /** Sinh audit_logs để màn kiểm duyệt có dữ liệu: lịch sử tạo/sửa và các báo cáo. */
    private fun createReviewAudit(adminId: Long) {
        val reviewRows = jdbc.query(
            """
            SELECT review_id, user_id, event_id, status, created_at
            FROM reviews WHERE event_id IN (SELECT event_id FROM events WHERE event_code LIKE 'EVT-DEMO-%')
            """.trimIndent()
        ) { rs, _ ->
            ReviewRow(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getTimestamp(5).toInstant())
        }

        reviewRows.forEach { r ->
            exec(
                """
                INSERT INTO audit_logs (actor_user_id, action_code, entity_type, entity_id, new_values_json,
                                        result_code, correlation_id, created_at)
                VALUES (:u, 'REVIEW_CREATED', 'review', :id, :new, 'OK', :corr, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("u", r.userId)
                    .addValue("id", r.id.toString())
                    .addValue("new", """{"status":"${r.status}"}""")
                    .addValue("corr", "demo-review-${r.id}")
                    .addValue("ts", ts(r.createdAt))
            )
            bump("audit_logs")
        }

        // Một số đánh giá bị nhiều người báo cáo → xuất hiện trong hàng chờ cờ REPORTED.
        val reporters = jdbc.query("SELECT user_id FROM users ORDER BY user_id") { rs, _ -> rs.getLong(1) }
        reviewRows.filter { it.status == "VISIBLE" }.shuffled(rnd).take(6).forEach { r ->
            val reporterCount = rnd.nextInt(1, 4)
            reporters.shuffled(rnd).filter { it != r.userId }.take(reporterCount).forEach { reporter ->
                exec(
                    """
                    INSERT INTO audit_logs (actor_user_id, action_code, entity_type, entity_id, reason,
                                            new_values_json, result_code, correlation_id, created_at)
                    VALUES (:u, 'REVIEW_REPORTED', 'review', :id, :reason, :new, 'OK', :corr, :ts)
                    """.trimIndent(),
                    MapSqlParameterSource()
                        .addValue("u", reporter)
                        .addValue("id", r.id.toString())
                        .addValue("reason", REPORT_REASONS[rnd.nextInt(REPORT_REASONS.size)])
                        .addValue("new", """{"reasonCode":"${REPORT_REASONS[rnd.nextInt(REPORT_REASONS.size)]}"}""")
                        .addValue("corr", "demo-report-${r.id}-$reporter")
                        .addValue("ts", ts(now.minus(rnd.nextLong(1, 20), ChronoUnit.DAYS)))
                )
                bump("audit_logs")
            }
        }

        // Vài lần admin ẩn nội dung — lịch sử kiểm duyệt có thật.
        reviewRows.filter { it.status == "HIDDEN" }.take(5).forEach { r ->
            exec(
                """
                INSERT INTO audit_logs (actor_user_id, action_code, entity_type, entity_id, reason,
                                        old_values_json, new_values_json, result_code, correlation_id, created_at)
                VALUES (:admin, 'REVIEW_HIDDEN', 'review', :id, 'Nội dung không phù hợp (dữ liệu demo)',
                        :old, :new, 'HIDDEN', :corr, :ts)
                """.trimIndent(),
                MapSqlParameterSource()
                    .addValue("admin", adminId)
                    .addValue("id", r.id.toString())
                    .addValue("old", """{"status":"VISIBLE"}""")
                    .addValue("new", """{"status":"HIDDEN"}""")
                    .addValue("corr", "demo-hide-${r.id}")
                    .addValue("ts", ts(now.minus(rnd.nextLong(1, 10), ChronoUnit.DAYS)))
            )
            bump("audit_logs")
        }
    }

    private data class ReviewRow(val id: Long, val userId: Long, val eventId: Long, val status: String, val createdAt: Instant)

    // =====================================================================
    // Tiện ích
    // =====================================================================

    private fun ts(instant: Instant): Timestamp = Timestamp.from(instant)

    private fun bump(key: String, amount: Int = 1) {
        counts[key] = (counts[key] ?: 0) + amount
    }

    private fun insert(sql: String, params: MapSqlParameterSource, idColumn: String): Long {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update(sql, params, keyHolder, arrayOf(idColumn))
        return keyHolder.key?.toLong() ?: error("Không lấy được $idColumn sau khi insert")
    }

    private fun exec(sql: String, params: MapSqlParameterSource): Int = jdbc.update(sql, params)

    private fun requireId(sql: String, value: String): Long =
        jdbc.queryForObject(sql, MapSqlParameterSource("code", value), Long::class.java)
            ?: error("Không tìm thấy bản ghi cho '$value'")

    /** Băm 64 ký tự hex tất định — dùng cho `tickets.qr_token_hash` (CHAR(64) UNIQUE). */
    private fun qrHash(seed: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun slugify(text: String): String =
        text.lowercase()
            .replace(Regex("[àáạảãâầấậẩẫăằắặẳẵ]"), "a")
            .replace(Regex("[èéẹẻẽêềếệểễ]"), "e")
            .replace(Regex("[ìíịỉĩ]"), "i")
            .replace(Regex("[òóọỏõôồốộổỗơờớợởỡ]"), "o")
            .replace(Regex("[ùúụủũưừứựửữ]"), "u")
            .replace(Regex("[ỳýỵỷỹ]"), "y")
            .replace(Regex("đ"), "d")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(60)

    private data class Template(
        val title: String,
        val short: String,
        val description: String,
        val categoryCode: String
    )

    private companion object {
        const val SEED = 20261101L

        val CITIES = listOf("Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Huế", "Hải Phòng")

        val VENUES = listOf(
            "Nhà hát Thành phố", "Trung tâm Hội nghị", "Cung Văn hoá", "Sân vận động",
            "Bảo tàng", "Công viên", "Trường Đại học", "Khách sạn Grand"
        )

        val CAPACITIES = listOf(120, 300, 500, 800, 1500, 3000)

        val USER_NAMES = listOf(
            "Trần Bảo Châu", "Nguyễn Khánh Linh", "Lê Quang Dũng", "Phạm Thu Trang",
            "Hoàng Minh Tuấn", "Vũ Thị Mai", "Đặng Hồng Nhung", "Bùi Anh Khoa",
            "Đỗ Thanh Hà", "Ngô Gia Bảo", "Trịnh Diễm Quỳnh", "Lý Hoàng Nam",
            "Phan Ngọc Ánh", "Cao Đức Thắng", "Tạ Thuỳ Dương", "Mai Xuân Trường",
            "Đinh Bích Ngọc", "Hồ Nhật Minh", "Dương Thảo Vy", "Lâm Tuấn Kiệt",
            "Chu Khánh An", "Tô Minh Quân", "Võ Hải Yến", "Trương Gia Hân"
        )

        val TEMPLATES = listOf(
            Template("Đêm nhạc Acoustic", "Đêm nhạc mộc mạc với ban nhạc acoustic.",
                "Không gian ấm cúng, dàn nhạc acoustic và khách mời đặc biệt. Phù hợp cho người yêu nhạc nhẹ.", "LIVE_MUSIC"),
            Template("Liveshow Indie", "Sân khấu indie với nhiều ban nhạc trẻ.",
                "Line-up gồm các ban nhạc indie nổi bật, kéo dài 3 giờ với phần giao lưu cùng khán giả.", "LIVE_MUSIC"),
            Template("Hoà nhạc Giao hưởng", "Dàn nhạc giao hưởng biểu diễn các tác phẩm kinh điển.",
                "Chương trình gồm các tác phẩm giao hưởng nổi tiếng, chỉ huy bởi nhạc trưởng khách mời.", "CLASSICAL"),
            Template("Hội thảo Chuyển đổi số", "Hội thảo về chuyển đổi số cho doanh nghiệp.",
                "Các phiên tham luận về dữ liệu, tự động hoá và chiến lược chuyển đổi số.", "CONFERENCE"),
            Template("Tech Talk", "Chuỗi chia sẻ kỹ thuật từ các kỹ sư.",
                "Talks về kiến trúc hệ thống, AI ứng dụng và kinh nghiệm vận hành sản phẩm.", "TECH"),
            Template("Workshop Kỹ năng", "Workshop thực hành kỹ năng mềm.",
                "Buổi thực hành nhóm, có hướng dẫn viên và tài liệu phát tay.", "WORKSHOP"),
            Template("Giải chạy", "Giải chạy phong trào với nhiều cự ly.",
                "Cự ly 5km, 10km và 21km. Có áo đua, huy chương và trạm tiếp nước.", "SPORTS"),
            Template("Triển lãm Nghệ thuật", "Triển lãm tác phẩm của các nghệ sĩ trẻ.",
                "Trưng bày hơn 60 tác phẩm, có khu vực tương tác và góc chụp ảnh.", "EXHIBITION"),
            Template("Ngày hội Cộng đồng", "Ngày hội vui chơi cho cả gia đình.",
                "Gian hàng ẩm thực, trò chơi thiếu nhi và hoạt động thiện nguyện.", "COMMUNITY"),
            Template("Mini Show", "Chương trình ca nhạc quy mô nhỏ.",
                "Không gian thân mật, thời lượng 90 phút, phù hợp khán giả trẻ.", "LIVE_MUSIC"),
            Template("Workshop Nhiếp ảnh", "Thực hành nhiếp ảnh ngoài trời.",
                "Hướng dẫn bố cục, ánh sáng và hậu kỳ cơ bản cùng nhiếp ảnh gia khách mời.", "PHOTO"),
            Template("Hội thảo Khởi nghiệp", "Chia sẻ kinh nghiệm gọi vốn và xây dựng đội ngũ.",
                "Gồm phần pitch của các startup và phần phản biện từ nhà đầu tư.", "CONFERENCE"),
            Template("Triển lãm Nghệ thuật số", "Không gian sắp đặt kỹ thuật số.",
                "Tác phẩm sắp đặt ánh sáng và thực tế ảo, có khu vực trải nghiệm.", "DIGITAL_ART"),
            Template("Giải bóng đá Phong trào", "Giải bóng đá giao hữu giữa các câu lạc bộ.",
                "Thi đấu vòng bảng và chung kết trong hai ngày, có trọng tài chuyên nghiệp.", "FOOTBALL")
        )

        val TAGS_BY_CATEGORY = mapOf(
            "LIVE_MUSIC" to listOf("ACOUSTIC", "INDIE", "JAZZ"),
            "MUSIC" to listOf("CLASSICAL", "ACOUSTIC"),
            "CLASSICAL" to listOf("CLASSICAL"),
            "CONFERENCE" to listOf("TECH", "KHOINGHIEP"),
            "TECH" to listOf("TECH", "AI"),
            "WORKSHOP" to listOf("KYNANG", "SINHVIEN"),
            "PHOTO" to listOf("NHIEPANH", "KYNANG"),
            "SPORTS" to listOf("MARATHON", "SUCKHOE"),
            "EXHIBITION" to listOf("NGHEHTHUAT", "GIOTRE"),
            "DIGITAL_ART" to listOf("DIGITALART", "NGHEHTHUAT"),
            "COMMUNITY" to listOf("THIENNGUYEN", "GIADINH"),
            "FOOTBALL" to listOf("SUCKHOE")
        )

        val REVIEW_TEXTS_5 = listOf(
            "Sự kiện tổ chức rất chuyên nghiệp, âm thanh ánh sáng ổn định. Sẽ quay lại lần sau.",
            "Trải nghiệm tuyệt vời, ban tổ chức chu đáo từ khâu check-in đến lúc kết thúc.",
            "Nội dung chất lượng, thời lượng hợp lý, rất đáng tiền.",
            "Không gian đẹp, nhân viên hỗ trợ nhiệt tình. Gia đình mình rất hài lòng."
        )
        val REVIEW_TEXTS_4 = listOf(
            "Chương trình tốt, chỉ hơi đông nên khâu gửi xe mất thời gian.",
            "Nội dung ổn, phần âm thanh ở khu vực cuối hội trường hơi nhỏ.",
            "Đáng tham gia, mong ban tổ chức bố trí thêm cổng check-in."
        )
        val REVIEW_TEXTS_3 = listOf(
            "Tạm ổn, một số phần bị chậm so với lịch công bố.",
            "Nội dung ổn nhưng địa điểm hơi khó tìm.",
            "Trung bình, giá vé hơi cao so với trải nghiệm."
        )
        val REVIEW_TEXTS_2 = listOf(
            "Thời gian chờ vào cửa khá lâu, ban tổ chức nên cải thiện.",
            "Nội dung chưa đúng như mô tả, hơi thất vọng."
        )
        val REVIEW_TEXTS_1 = listOf(
            "Chương trình bắt đầu trễ gần một tiếng và không có thông báo.",
            "Không đúng như quảng cáo, mình không hài lòng."
        )
        val PENDING_TEXTS = listOf(
            "Ban tổ chức quá tệ, ai cần liên hệ gấp thì gọi 0901234567 nhé.",
            "Mọi người xem thêm ở http://spam-link.example.com để nhận ưu đãi.",
            "Nội dung này chứa số điện thoại 0987654321 nên bị giữ lại để kiểm duyệt."
        )
        val REPORT_REASONS = listOf("SPAM", "OFFENSIVE", "FAKE_REVIEW", "OFF_TOPIC")
    }
}
