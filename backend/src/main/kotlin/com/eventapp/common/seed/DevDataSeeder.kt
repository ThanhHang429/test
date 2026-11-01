package com.eventapp.common.seed

import com.eventapp.ai.entity.IntegrationHealthLog
import com.eventapp.ai.entity.PlatformSetting
import com.eventapp.ai.repository.IntegrationHealthLogRepository
import com.eventapp.ai.repository.PlatformSettingRepository
import com.eventapp.catalog.entity.EventCategory
import com.eventapp.catalog.entity.EventCategoryMap
import com.eventapp.catalog.entity.EventCategoryMapId
import com.eventapp.catalog.entity.EventTag
import com.eventapp.catalog.entity.EventTagMap
import com.eventapp.catalog.entity.EventTagMapId
import com.eventapp.catalog.repository.EventCategoryMapRepository
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.catalog.repository.EventTagMapRepository
import com.eventapp.catalog.repository.EventTagRepository
import com.eventapp.common.enums.AiFeatureCode
import com.eventapp.common.enums.CheckinMethod
import com.eventapp.common.enums.EventFormat
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.EventVisibility
import com.eventapp.common.enums.IntegrationResultStatus
import com.eventapp.common.enums.IntegrationServiceCode
import com.eventapp.common.enums.MediaType
import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.enums.PaymentMethod
import com.eventapp.common.enums.PaymentStatus
import com.eventapp.common.enums.PaymentVerificationSource
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.ReviewStatus
import com.eventapp.common.enums.TicketKind
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.enums.TicketTypeStatus
import com.eventapp.common.enums.UserStatus
import com.eventapp.event.entity.Event
import com.eventapp.event.entity.EventMedia
import com.eventapp.event.entity.TicketType
import com.eventapp.event.repository.EventMediaRepository
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.identity.entity.OrganizerProfile
import com.eventapp.identity.entity.User
import com.eventapp.identity.entity.UserRole
import com.eventapp.identity.entity.UserRoleId
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.identity.repository.RoleRepository
import com.eventapp.identity.repository.UserRepository
import com.eventapp.identity.repository.UserRoleRepository
import com.eventapp.interaction.entity.Favorite
import com.eventapp.interaction.entity.FavoriteId
import com.eventapp.interaction.entity.PreferenceSettings
import com.eventapp.interaction.entity.Review
import com.eventapp.interaction.entity.UserPreference
import com.eventapp.interaction.repository.FavoriteRepository
import com.eventapp.interaction.repository.ReviewRepository
import com.eventapp.interaction.repository.UserPreferenceRepository
import com.eventapp.ticketing.entity.CheckIn
import com.eventapp.ticketing.entity.Order
import com.eventapp.ticketing.entity.OrderItem
import com.eventapp.ticketing.entity.Payment
import com.eventapp.ticketing.entity.Registration
import com.eventapp.ticketing.entity.Ticket
import com.eventapp.ticketing.repository.CheckInRepository
import com.eventapp.ticketing.repository.OrderItemRepository
import com.eventapp.ticketing.repository.OrderRepository
import com.eventapp.ticketing.repository.PaymentRepository
import com.eventapp.ticketing.repository.RegistrationRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tạo dữ liệu mẫu để test API ở môi trường dev.
 * Chỉ chạy khi: profile = dev, app.seed.enabled = true, và bảng users đang rỗng.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = ["app.seed.enabled"], havingValue = "true")
class DevDataSeeder(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val categoryRepository: EventCategoryRepository,
    private val categoryMapRepository: EventCategoryMapRepository,
    private val tagRepository: EventTagRepository,
    private val tagMapRepository: EventTagMapRepository,
    private val eventRepository: EventRepository,
    private val mediaRepository: EventMediaRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val registrationRepository: RegistrationRepository,
    private val ticketRepository: TicketRepository,
    private val checkInRepository: CheckInRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val paymentRepository: PaymentRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val preferenceRepository: UserPreferenceRepository,
    private val platformSettingRepository: PlatformSettingRepository,
    private val integrationHealthLogRepository: IntegrationHealthLogRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val now: Instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)

    @Transactional
    override fun run(vararg args: String) {
        if (userRepository.count() > 0) {
            log.info("[seed] Bảng users đã có dữ liệu — bỏ qua seed.")
            return
        }
        log.info("[seed] Bắt đầu tạo dữ liệu mẫu...")

        val roles = roleRepository.findAll().associateBy { it.roleCode }
        require(roles.containsKey("USER") && roles.containsKey("ORGANIZER") && roles.containsKey("ADMIN")) {
            "Thiếu role hệ thống (USER/ORGANIZER/ADMIN) trong bảng roles"
        }

        // ---------- Users ----------
        val password = passwordEncoder.encode(DEFAULT_PASSWORD)
        val admin = userRepository.save(
            User(email = "admin@eticket.local", passwordHash = password, fullName = "Quản trị viên",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )
        val organizer1 = userRepository.save(
            User(email = "organizer1@eticket.local", passwordHash = password, fullName = "Nguyễn Minh Châu",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )
        val organizer2 = userRepository.save(
            User(email = "organizer2@eticket.local", passwordHash = password, fullName = "Trần Hữu Cảnh",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )
        val user1 = userRepository.save(
            User(email = "user1@eticket.local", passwordHash = password, fullName = "Lê Hoàng Anh",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )
        val user2 = userRepository.save(
            User(email = "user2@eticket.local", passwordHash = password, fullName = "Phạm Thu Hằng",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )
        val user3 = userRepository.save(
            User(email = "user3@eticket.local", passwordHash = password, fullName = "Võ Quốc Anh",
                status = UserStatus.ACTIVE, emailVerifiedAt = now)
        )

        userRoleRepository.saveAll(
            listOf(
                UserRole(UserRoleId(admin.userId!!, roles["ADMIN"]!!.roleId!!)),
                UserRole(UserRoleId(organizer1.userId!!, roles["ORGANIZER"]!!.roleId!!)),
                UserRole(UserRoleId(organizer1.userId!!, roles["USER"]!!.roleId!!)),
                UserRole(UserRoleId(organizer2.userId!!, roles["ORGANIZER"]!!.roleId!!)),
                UserRole(UserRoleId(organizer2.userId!!, roles["USER"]!!.roleId!!)),
                UserRole(UserRoleId(user1.userId!!, roles["USER"]!!.roleId!!)),
                UserRole(UserRoleId(user2.userId!!, roles["USER"]!!.roleId!!)),
                UserRole(UserRoleId(user3.userId!!, roles["USER"]!!.roleId!!))
            )
        )

        // ---------- Organizer profiles ----------
        val orgProfile1 = organizerProfileRepository.save(
            OrganizerProfile(
                userId = organizer1.userId!!, organizationName = "Châu Entertainment",
                publicName = "Châu Entertainment", description = "Đơn vị tổ chức sự kiện âm nhạc.",
                contactEmail = "contact@chau-ent.local", status = OrganizerStatus.APPROVED,
                submittedAt = now.minus(60, ChronoUnit.DAYS), reviewedByUserId = admin.userId,
                reviewedAt = now.minus(58, ChronoUnit.DAYS)
            )
        )
        val orgProfile2 = organizerProfileRepository.save(
            OrganizerProfile(
                userId = organizer2.userId!!, organizationName = "Cảnh Studio",
                publicName = "Cảnh Studio", description = "Workshop và hội thảo công nghệ.",
                contactEmail = "contact@canh-studio.local", status = OrganizerStatus.PENDING,
                submittedAt = now.minus(3, ChronoUnit.DAYS)
            )
        )

        // ---------- Categories ----------
        val catMusic = categoryRepository.save(category("MUSIC", "Âm nhạc", "am-nhac", 1, "Sự kiện âm nhạc, liveshow"))
        val catConference = categoryRepository.save(category("CONFERENCE", "Hội thảo", "hoi-thao", 2, "Hội thảo, seminar"))
        val catSports = categoryRepository.save(category("SPORTS", "Thể thao", "the-thao", 3, "Giải chạy, thi đấu"))
        val catWorkshop = categoryRepository.save(category("WORKSHOP", "Workshop", "workshop", 4, "Lớp học kỹ năng"))
        val catExhibition = categoryRepository.save(category("EXHIBITION", "Triển lãm", "trien-lam", 5, "Triển lãm nghệ thuật"))
        val catCommunity = categoryRepository.save(category("COMMUNITY", "Cộng đồng", "cong-dong", 6, "Hoạt động cộng đồng"))
        val catLiveMusic = categoryRepository.save(
            category("LIVE_MUSIC", "Nhạc sống", "nhac-song", 1, "Liveshow, acoustic", catMusic.categoryId)
        )
        val catTech = categoryRepository.save(
            category("TECH", "Công nghệ", "cong-nghe", 1, "Sự kiện công nghệ", catConference.categoryId)
        )

        // ---------- Tags ----------
        val tagAcoustic = tagRepository.save(tag("ACOUSTIC", "Acoustic", "acoustic", organizer1.userId))
        val tagIndie = tagRepository.save(tag("INDIE", "Indie", "indie", organizer1.userId))
        val tagStartup = tagRepository.save(tag("STARTUP", "Startup", "startup", organizer2.userId))
        val tagAi = tagRepository.save(tag("AI", "AI", "ai", organizer2.userId))
        val tagMarathon = tagRepository.save(tag("MARATHON", "Marathon", "marathon", organizer1.userId))
        val tagVegan = tagRepository.save(tag("VEGAN", "Vegan", "vegan", organizer2.userId))

        // ---------- Events ----------
        val published = mutableListOf<Event>()
        published += saveEvent(
            orgProfile1, organizer1, "Đêm nhạc Acoustic Sài Gòn", "dem-nhac-acoustic-sai-gon",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 7, durationHours = 3, city = "Hồ Chí Minh", venue = "Nhà hát Bến Thành",
            category = catLiveMusic, tags = listOf(tagAcoustic, tagIndie)
        )
        published += saveEvent(
            orgProfile1, organizer1, "Hội nhạc Indie Hà Nội", "hoi-nhac-indie-ha-noi",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 14, durationHours = 4, city = "Hà Nội", venue = "Cung Văn hoá Hữu nghị",
            category = catMusic, tags = listOf(tagIndie)
        )
        published += saveEvent(
            orgProfile2, organizer2, "Hội thảo AI ứng dụng 2026", "hoi-thao-ai-ung-dung-2026",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.ONLINE,
            startIn = 5, durationHours = 6, city = null, venue = null,
            category = catTech, tags = listOf(tagAi, tagStartup), onlineUrl = "https://meet.example.com/ai-2026"
        )
        published += saveEvent(
            orgProfile2, organizer2, "Workshop Startup gọi vốn", "workshop-startup-goi-von",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 21, durationHours = 3, city = "Đà Nẵng", venue = "Trung tâm Hội nghị Đà Nẵng",
            category = catWorkshop, tags = listOf(tagStartup)
        )
        published += saveEvent(
            orgProfile1, organizer1, "Giải chạy Marathon Biên Hoà", "giai-chay-marathon-bien-hoa",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 30, durationHours = 8, city = "Đồng Nai", venue = "Công viên Biên Hùng",
            category = catSports, tags = listOf(tagMarathon)
        )
        published += saveEvent(
            orgProfile2, organizer2, "Triển lãm Nghệ thuật Số", "trien-lam-nghe-thuat-so",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 11, durationHours = 10, city = "Hồ Chí Minh", venue = "Bảo tàng Mỹ thuật",
            category = catExhibition, tags = emptyList()
        )
        published += saveEvent(
            orgProfile1, organizer1, "Ngày hội Cộng đồng Xanh", "ngay-hoi-cong-dong-xanh",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.OFFLINE,
            startIn = 3, durationHours = 6, city = "Hồ Chí Minh", venue = "Công viên Tao Đàn",
            category = catCommunity, tags = listOf(tagVegan)
        )
        published += saveEvent(
            orgProfile2, organizer2, "Liveshow Mùa Thu", "liveshow-mua-thu",
            EventReviewStatus.APPROVED, EventLifecycleStatus.PUBLISHED, EventFormat.HYBRID,
            startIn = 45, durationHours = 3, city = "Hà Nội", venue = "Nhà hát Lớn",
            category = catLiveMusic, tags = listOf(tagAcoustic)
        )

        // Các trạng thái KHÔNG được xuất hiện ở API khám phá
        val draftEvent = saveEvent(
            orgProfile1, organizer1, "Sự kiện nháp chưa công bố", "su-kien-nhap-chua-cong-bo",
            EventReviewStatus.DRAFT, EventLifecycleStatus.UNPUBLISHED, EventFormat.OFFLINE,
            startIn = 20, durationHours = 2, city = "Hồ Chí Minh", venue = "Địa điểm nháp",
            category = catMusic, tags = emptyList(), publish = false
        )
        val pendingEvent = saveEvent(
            orgProfile1, organizer1, "Sự kiện đang chờ duyệt", "su-kien-dang-cho-duyet",
            EventReviewStatus.PENDING_REVIEW, EventLifecycleStatus.UNPUBLISHED, EventFormat.OFFLINE,
            startIn = 25, durationHours = 2, city = "Hồ Chí Minh", venue = "Địa điểm chờ duyệt",
            category = catMusic, tags = emptyList(), publish = false
        )
        val rejectedEvent = saveEvent(
            orgProfile1, organizer1, "Sự kiện bị từ chối", "su-kien-bi-tu-choi",
            EventReviewStatus.REJECTED, EventLifecycleStatus.UNPUBLISHED, EventFormat.OFFLINE,
            startIn = 28, durationHours = 2, city = "Hồ Chí Minh", venue = "Địa điểm bị từ chối",
            category = catMusic, tags = emptyList(), publish = false
        )
        val suspendedEvent = saveEvent(
            orgProfile1, organizer1, "Sự kiện bị đình chỉ", "su-kien-bi-dinh-chi",
            EventReviewStatus.APPROVED, EventLifecycleStatus.SUSPENDED, EventFormat.OFFLINE,
            startIn = 12, durationHours = 2, city = "Hồ Chí Minh", venue = "Địa điểm bị đình chỉ",
            category = catMusic, tags = emptyList(), publish = false
        )
        val cancelledEvent = saveEvent(
            orgProfile1, organizer1, "Sự kiện đã huỷ (vẫn mở được bằng id)", "su-kien-da-huy",
            EventReviewStatus.APPROVED, EventLifecycleStatus.CANCELLED, EventFormat.OFFLINE,
            startIn = 9, durationHours = 2, city = "Hồ Chí Minh", venue = "Địa điểm đã huỷ",
            category = catMusic, tags = emptyList()
        ).also { it.cancelledAt = now.minus(2, ChronoUnit.DAYS); eventRepository.save(it) }
        val completedEvent = saveEvent(
            orgProfile1, organizer1, "Đêm nhạc đã kết thúc (dùng để test đánh giá)", "dem-nhac-da-ket-thuc",
            EventReviewStatus.APPROVED, EventLifecycleStatus.COMPLETED, EventFormat.OFFLINE,
            startIn = -10, durationHours = 3, city = "Hồ Chí Minh", venue = "Nhà hát Hoà Bình",
            category = catLiveMusic, tags = listOf(tagAcoustic)
        )

        // ---------- Ticket types ----------
        published.forEachIndexed { index, event ->
            val isFree = index % 4 == 0
            if (isFree) {
                saveTicketType(event, "Vé miễn phí", TicketKind.FREE, BigDecimal.ZERO, null, TicketTypeStatus.ACTIVE)
            } else {
                saveTicketType(event, "Vé Standard", TicketKind.PAID, BigDecimal("150000"), 200, TicketTypeStatus.ACTIVE)
                saveTicketType(event, "Vé VIP", TicketKind.PAID, BigDecimal("450000"), 50, TicketTypeStatus.ACTIVE)
            }
        }
        // Event đã bán hết
        val soldOutEvent = published[6]
        ticketTypeRepository.findByEventIdOrderByPriceAsc(soldOutEvent.eventId!!).forEach {
            it.quantityTotal = 100
            it.quantitySold = 100
            ticketTypeRepository.save(it)
        }
        // Event chưa mở bán
        val notStarted = saveTicketType(published[7], "Vé mở bán sau", TicketKind.PAID, BigDecimal("300000"), 100, TicketTypeStatus.ACTIVE)
        notStarted.saleStartAt = now.plus(10, ChronoUnit.DAYS)
        notStarted.saleEndAt = now.plus(40, ChronoUnit.DAYS)
        ticketTypeRepository.save(notStarted)
        // Vé cho event đã kết thúc (điều kiện đánh giá)
        val completedTicketType = saveTicketType(
            completedEvent, "Vé Standard", TicketKind.PAID, BigDecimal("200000"), 300, TicketTypeStatus.ACTIVE
        )
        // Vé cho event nháp (để kiểm tra không lộ)
        saveTicketType(draftEvent, "Vé nháp", TicketKind.PAID, BigDecimal("100000"), 50, TicketTypeStatus.ACTIVE)
        saveTicketType(cancelledEvent, "Vé đã huỷ", TicketKind.PAID, BigDecimal("120000"), 50, TicketTypeStatus.ACTIVE)
        saveTicketType(suspendedEvent, "Vé bị đình chỉ", TicketKind.PAID, BigDecimal("120000"), 50, TicketTypeStatus.ACTIVE)

        // ---------- Đăng ký / vé / check-in cho event đã kết thúc ----------
        seedAttendance(completedEvent, completedTicketType, user1)
        seedAttendance(completedEvent, completedTicketType, user2)
        // user3 đã check-in nhưng chưa đánh giá: dùng để test POST /events/{id}/reviews.
        seedAttendance(completedEvent, completedTicketType, user3)

        // ---------- Đơn hàng / thanh toán (cho dashboard) ----------
        seedOrders(published[0], user1)
        seedOrders(published[1], user2)
        seedOrders(published[2], user3)

        // ---------- Yêu thích ----------
        favoriteRepository.saveAll(
            listOf(
                Favorite(FavoriteId(user1.userId!!, published[0].eventId!!)),
                Favorite(FavoriteId(user1.userId!!, published[2].eventId!!)),
                Favorite(FavoriteId(user2.userId!!, published[1].eventId!!)),
                Favorite(FavoriteId(user2.userId!!, published[0].eventId!!)),
                Favorite(FavoriteId(user3.userId!!, published[3].eventId!!))
            )
        )

        // ---------- Sở thích ----------
        preferenceRepository.save(
            UserPreference(
                userId = user1.userId!!,
                preferredCategories = listOf(catMusic.categoryId!!, catLiveMusic.categoryId!!),
                preferredCity = "Hồ Chí Minh",
                preferredEventFormats = listOf("OFFLINE"),
                personalizationEnabled = true,
                settings = PreferenceSettings(
                    priceMode = "ANY", maxBudgetVnd = 500_000,
                    preferredDays = listOf("WEEKEND"),
                    preferredCities = listOf("Hồ Chí Minh", "Hà Nội"),
                    onboardingCompleted = true, version = 1
                )
            )
        )

        // ---------- Đánh giá ----------
        reviewRepository.saveAll(
            listOf(
                Review(userId = user1.userId!!, eventId = completedEvent.eventId!!, rating = 5,
                    reviewText = "Đêm nhạc rất hay, âm thanh tốt và ban tổ chức chu đáo.", status = ReviewStatus.VISIBLE),
                Review(userId = user2.userId!!, eventId = completedEvent.eventId!!, rating = 4,
                    reviewText = "Không gian đẹp, chỉ hơi đông. Nhìn chung đáng tiền.", status = ReviewStatus.VISIBLE),
                Review(userId = user3.userId!!, eventId = published[0].eventId!!, rating = 3,
                    reviewText = "Liên hệ số 0901234567 để mua vé nhanh.", status = ReviewStatus.PENDING,
                    moderationReason = "Chứa số điện thoại")
            )
        )

        // ---------- Cấu hình AI (kill switch) ----------
        platformSettingRepository.save(
            PlatformSetting(
                settingKey = AI_FLAGS_KEY, versionNo = 1,
                settingValue = com.eventapp.ai.gateway.AiTask.entries.associate { it.code to true },
                description = "Bật/tắt từng tác vụ AI (kill switch)",
                changedByUserId = admin.userId
            )
        )
        platformSettingRepository.save(
            PlatformSetting(
                settingKey = "content.blocked_keywords", versionNo = 1,
                settingValue = mapOf(
                    "terms" to listOf("lừa đảo", "scam", "cá độ", "casino", "vay nóng")
                ),
                description = "Từ khoá bị chặn khi kiểm duyệt nội dung",
                changedByUserId = admin.userId
            )
        )

        // ---------- Log tích hợp (cho A20) ----------
        integrationHealthLogRepository.saveAll(
            listOf(
                IntegrationHealthLog(
                    serviceCode = IntegrationServiceCode.AI, operationCode = "AI_GATEWAY_CALL",
                    resultStatus = IntegrationResultStatus.SUCCESS, latencyMs = 420,
                    message = "Mock provider trả kết quả hợp lệ", correlationId = "seed-corr-1"
                ),
                IntegrationHealthLog(
                    serviceCode = IntegrationServiceCode.AI, operationCode = "AI_GATEWAY_CALL",
                    resultStatus = IntegrationResultStatus.WARNING, latencyMs = 4800,
                    errorCode = "AI_TIMEOUT", message = "Vượt ngưỡng 4s (mock)", correlationId = "seed-corr-2"
                )
            )
        )

        log.info(
            "[seed] Xong. Đăng nhập test: admin@eticket.local / organizer1@eticket.local / user1@eticket.local, mật khẩu {}",
            DEFAULT_PASSWORD
        )
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun category(code: String, name: String, slug: String, order: Int, desc: String, parent: Long? = null) =
        EventCategory(
            categoryCode = code, categoryName = name, slug = slug, sortOrder = order,
            description = desc, parentCategoryId = parent, isActive = true
        )

    private fun tag(code: String, name: String, slug: String, createdBy: Long?) =
        EventTag(tagCode = code, tagName = name, slug = slug, isActive = true, createdByUserId = createdBy)

    private fun saveEvent(
        organizer: OrganizerProfile,
        owner: User,
        title: String,
        slug: String,
        reviewStatus: EventReviewStatus,
        lifecycle: EventLifecycleStatus,
        format: EventFormat,
        startIn: Long,
        durationHours: Long,
        city: String?,
        venue: String?,
        category: EventCategory,
        tags: List<EventTag>,
        onlineUrl: String? = null,
        publish: Boolean = true
    ): Event {
        val start = now.plus(startIn, ChronoUnit.DAYS)
        val event = eventRepository.save(
            Event(
                eventCode = "EVT-" + slug.uppercase().replace('-', '_').take(30),
                organizerProfileId = organizer.organizerProfileId!!,
                createdByUserId = owner.userId!!,
                title = title,
                slug = slug,
                shortDescription = "Sự kiện mẫu: $title",
                description = "Mô tả chi tiết cho sự kiện $title. Đây là dữ liệu mẫu dùng cho kiểm thử API.",
                eventFormat = format,
                reviewStatus = reviewStatus,
                lifecycleStatus = lifecycle,
                visibility = EventVisibility.PUBLIC,
                startAt = start,
                endAt = start.plus(durationHours, ChronoUnit.HOURS),
                registrationStartAt = now.minus(20, ChronoUnit.DAYS),
                registrationEndAt = start.minus(1, ChronoUnit.HOURS),
                venueName = venue,
                addressLine = venue?.let { "Số 1, $it" },
                city = city,
                province = city,
                countryCode = "VN",
                onlineUrl = onlineUrl,
                capacityLimit = 500,
                participationConditions = "Mang theo vé điện tử khi tham dự.",
                cancellationPolicy = "Huỷ trước 48 giờ được hoàn 100%.",
                publishedAt = if (publish) now.minus(5, ChronoUnit.DAYS) else null
            )
        )
        categoryMapRepository.save(EventCategoryMap(EventCategoryMapId(event.eventId!!, category.categoryId!!)))
        tags.forEach { tag ->
            tagMapRepository.save(EventTagMap(EventTagMapId(event.eventId!!, tag.tagId!!), owner.userId!!))
        }
        mediaRepository.save(
            EventMedia(
                eventId = event.eventId!!,
                mediaType = MediaType.IMAGE,
                storageUrl = "https://picsum.photos/seed/${event.slug}/1200/675",
                originalFileName = "cover.jpg", mimeType = "image/jpeg",
                altText = title, sortOrder = 0, isCover = true, uploadedByUserId = owner.userId!!
            )
        )
        return event
    }

    private fun saveTicketType(
        event: Event,
        name: String,
        kind: TicketKind,
        price: BigDecimal,
        total: Int?,
        status: TicketTypeStatus
    ): TicketType =
        ticketTypeRepository.save(
            TicketType(
                eventId = event.eventId!!, ticketTypeName = name, ticketKind = kind, price = price,
                currency = "VND", quantityTotal = total, quantitySold = 0, quantityReserved = 0,
                minPerOrder = 1, maxPerUser = if (kind == TicketKind.FREE) 2 else 4,
                saleStartAt = now.minus(5, ChronoUnit.DAYS), saleEndAt = now.plus(60, ChronoUnit.DAYS),
                status = status
            )
        )

    private fun seedAttendance(event: Event, ticketType: TicketType, attendee: User) {
        val registration = registrationRepository.save(
            Registration(
                registrationCode = "REG-" + event.eventId + "-" + attendee.userId,
                userId = attendee.userId!!, eventId = event.eventId!!, ticketTypeId = ticketType.ticketTypeId!!,
                quantity = 1, status = RegistrationStatus.CONFIRMED,
                idempotencyKey = "seed-reg-" + event.eventId + "-" + attendee.userId
            )
        )
        val ticket = ticketRepository.save(
            Ticket(
                ticketCode = "TKT-" + event.eventId + "-" + attendee.userId,
                qrTokenHash = ("seedqr" + event.eventId + attendee.userId).padEnd(64, '0').take(64),
                userId = attendee.userId!!, eventId = event.eventId!!, ticketTypeId = ticketType.ticketTypeId!!,
                registrationId = registration.registrationId, status = TicketStatus.CHECKED_IN
            )
        )
        checkInRepository.save(
            CheckIn(
                ticketId = ticket.ticketId!!, eventId = event.eventId!!, actorUserId = attendee.userId!!,
                checkInMethod = CheckinMethod.SELF_SCAN, note = "Dữ liệu mẫu"
            )
        )
    }

    private fun seedOrders(event: Event, buyer: User) {
        val paidOrder = orderRepository.save(
            Order(
                orderCode = "ORD-" + event.eventId + "-1", userId = buyer.userId!!, eventId = event.eventId!!,
                status = OrderStatus.PAID, subtotalAmount = BigDecimal("300000"),
                totalAmount = BigDecimal("300000"), idempotencyKey = "seed-order-1-" + event.eventId,
                paidAt = now.minus(2, ChronoUnit.DAYS)
            )
        )
        orderItemRepository.save(
            OrderItem(
                orderId = paidOrder.orderId!!, ticketTypeId = ticketTypeRepository
                    .findByEventIdOrderByPriceAsc(event.eventId!!).first().ticketTypeId!!,
                ticketNameSnapshot = "Vé Standard", quantity = 2,
                unitPrice = BigDecimal("150000"), lineTotal = BigDecimal("300000")
            )
        )
        paymentRepository.save(
            Payment(
                paymentReference = "PAY-" + event.eventId + "-1", orderId = paidOrder.orderId!!,
                providerCode = "MOCK_BANK", paymentMethod = PaymentMethod.BANK_TRANSFER_QR,
                amount = BigDecimal("300000"), status = PaymentStatus.SUCCESS,
                verifiedAt = now.minus(2, ChronoUnit.DAYS),
                verificationSource = PaymentVerificationSource.MANUAL_RECONCILIATION,
                idempotencyKey = "seed-pay-1-" + event.eventId
            )
        )

        val pendingOrder = orderRepository.save(
            Order(
                orderCode = "ORD-" + event.eventId + "-2", userId = buyer.userId!!, eventId = event.eventId!!,
                status = OrderStatus.PENDING_PAYMENT, subtotalAmount = BigDecimal("150000"),
                totalAmount = BigDecimal("150000"), idempotencyKey = "seed-order-2-" + event.eventId,
                paymentDueAt = now.plus(1, ChronoUnit.DAYS)
            )
        )
        orderItemRepository.save(
            OrderItem(
                orderId = pendingOrder.orderId!!, ticketTypeId = ticketTypeRepository
                    .findByEventIdOrderByPriceAsc(event.eventId!!).first().ticketTypeId!!,
                ticketNameSnapshot = "Vé Standard", quantity = 1,
                unitPrice = BigDecimal("150000"), lineTotal = BigDecimal("150000")
            )
        )
        paymentRepository.save(
            Payment(
                paymentReference = "PAY-" + event.eventId + "-2", orderId = pendingOrder.orderId!!,
                providerCode = "MOCK_BANK", paymentMethod = PaymentMethod.BANK_TRANSFER_QR,
                amount = BigDecimal("150000"), status = PaymentStatus.PENDING,
                idempotencyKey = "seed-pay-2-" + event.eventId
            )
        )
    }

    companion object {
        const val DEFAULT_PASSWORD = "Password123!"
        const val AI_FLAGS_KEY = "ai.feature.flags"
    }
}
