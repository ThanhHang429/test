package com.eventapp.recommendation

import com.eventapp.ai.gateway.AiGateway
import com.eventapp.ai.gateway.AiTask
import com.eventapp.ai.gateway.PromptBuilder
import com.eventapp.catalog.repository.EventCategoryMapRepository
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.validationError
import com.eventapp.discovery.EventDiscoveryService
import com.eventapp.discovery.EventSearchRepository
import com.eventapp.interaction.entity.UserPreference
import com.eventapp.interaction.repository.FavoriteRepository
import com.eventapp.interaction.repository.UserPreferenceRepository
import com.eventapp.recommendation.RecommendationRepository.CandidateRow
import com.eventapp.recommendation.dto.RecommendationItemDto
import com.eventapp.recommendation.dto.RecommendationListDto
import com.eventapp.recommendation.dto.RecommendationMetaDto
import com.eventapp.recommendation.dto.RecommendationPreviewRequest
import com.eventapp.ticketing.repository.RegistrationRepository
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.exp

/**
 * FR-U07 — AI gợi ý sự kiện.
 *
 * Quy trình 4 bước đúng SRS:
 *  1. Sinh tối đa 100 ứng viên (một truy vấn, đã loại sự kiện đã đăng ký/có vé).
 *  2. Chấm điểm bằng luật → giữ 30. Đây là kết quả dự phòng hoàn chỉnh.
 *  3. AI xếp hạng lại + viết lý do (tùy chọn, qua AI Gateway).
 *  4. Hậu kiểm: loại id lạ, loại trùng, thay mọi thông tin bằng dữ liệu chính thức.
 *
 * Cache 15 phút mỗi User (U07-R06), ghi vết vào `event_recommendations` để phục vụ
 * thao tác «Không quan tâm» và phân tích.
 */
@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val categoryMapRepository: EventCategoryMapRepository,
    private val categoryRepository: EventCategoryRepository,
    private val preferenceRepository: UserPreferenceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val registrationRepository: RegistrationRepository,
    private val engine: RecommendationEngine,
    private val eventSearchRepository: EventSearchRepository,
    private val discoveryService: EventDiscoveryService,
    private val aiGateway: AiGateway,
    private val eventRecommendationRepository: com.eventapp.ai.repository.EventRecommendationRepository,
    private val recommendationCache: RecommendationCache
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ------------------------------------------------------------------
    // API-10
    // ------------------------------------------------------------------

    @Transactional
    fun forUser(userId: Long, limit: Int, refresh: Boolean): RecommendationListDto {
        val cached = recommendationCache.get(userId)
        val now = Instant.now()
        if (!refresh && cached != null) {
            return cached.list.copy(items = cached.list.items.take(limit))
        }
        if (refresh && cached != null && Duration.between(cached.generatedAt, now) < REFRESH_COOLDOWN) {
            throw ConflictException(
                ErrorCode.RATE_LIMITED,
                "Chỉ được làm mới gợi ý 1 lần mỗi phút, vui lòng thử lại sau"
            )
        }

        val result = generate(userId, limit, buildSignals(userId))
        recommendationCache.put(userId, result, now)
        return result
    }

    /** Khách: sở thích tạm thời trong body, không cache, không gọi AI (tiết kiệm quota). */
    @Transactional(readOnly = true)
    fun preview(request: RecommendationPreviewRequest): RecommendationListDto {
        val priceMode = request.priceMode?.uppercase() ?: "ANY"
        if (priceMode !in PRICE_MODES) throw validationError("priceMode", "priceMode phải là ANY | FREE_ONLY | PAID_ONLY")
        val format = request.eventFormat?.uppercase() ?: "ANY"
        if (format !in FORMATS) throw validationError("eventFormat", "eventFormat phải là ANY | ONSITE | ONLINE")

        val signals = RecommendationEngine.Signals(
            categoryIds = request.categoryIds.toSet(),
            city = request.city?.trim()?.takeIf { it.isNotEmpty() },
            acceptsOnline = format != "ONSITE",
            priceMode = priceMode,
            maxBudgetVnd = request.maxBudgetVnd,
            preferredDays = request.preferredDays.map { it.uppercase() }.toSet(),
            personalized = request.categoryIds.isNotEmpty() || !request.city.isNullOrBlank()
        )
        return generate(null, (request.limit ?: 10).coerceIn(1, 20), signals, useAi = false)
    }

    @Transactional
    fun dismiss(userId: Long, eventId: Long) {
        val existing = eventRecommendationRepository.findTopByUserIdAndEventIdOrderByShownAtDesc(userId, eventId)
        if (existing != null) {
            existing.dismissedAt = Instant.now()
            eventRecommendationRepository.save(existing)
        } else {
            eventRecommendationRepository.save(
                com.eventapp.ai.entity.EventRecommendation(
                    userId = userId,
                    eventId = eventId,
                    recommendationReason = "Người dùng bỏ qua",
                    positionNo = null,
                    dismissedAt = Instant.now()
                )
            )
        }
        recommendationCache.invalidate(userId)
    }

    /** U07-R06: vô hiệu cache khi đổi sở thích / yêu thích / đăng ký. */
    fun invalidate(userId: Long) {
        recommendationCache.invalidate(userId)
    }

    // ------------------------------------------------------------------
    // Sinh gợi ý
    // ------------------------------------------------------------------

    private fun generate(
        userId: Long?,
        limit: Int,
        signals: RecommendationEngine.Signals,
        useAi: Boolean = true
    ): RecommendationListDto {
        val now = Instant.now()
        val candidates = recommendationRepository.candidates(userId, MAX_CANDIDATES)
            .filter { isAvailable(it) }
            .filterNot { userId != null && dismissedRecently(userId, it.eventId, now) }

        if (candidates.isEmpty()) {
            return RecommendationListDto(
                items = emptyList(),
                meta = RecommendationMetaDto(
                    title = if (signals.personalized) "Dành cho bạn" else "Phổ biến sắp diễn ra",
                    generatedAt = now,
                    isFallback = false,
                    personalized = signals.personalized,
                    engineVersion = RecommendationEngine.ENGINE_VERSION,
                    modelCode = null,
                    aiRequestId = null,
                    correlationId = null
                )
            )
        }

        val index = loadCategoryIndex(candidates.map { it.eventId })
        val ranked = engine.score(candidates, index, signals, now)
        val shortlist = ranked.take(AI_SHORTLIST)
        val diversified = engine.diversify(shortlist, limit)

        var reasonSource = "RULE"
        var isFallback = false
        var aiRequestId: Long? = null
        var correlationId: String? = null
        var modelCode: String? = null
        var ordered: List<RecommendationEngine.Scored> = diversified
        var aiReasons: Map<Long, String> = emptyMap()

        if (useAi && signals.personalized && aiGateway.isAvailable(AiTask.RECOMMEND)) {
            try {
                val result = aiGateway.execute(
                    task = AiTask.RECOMMEND,
                    userId = userId,
                    eventId = null,
                    fields = buildAiFields(shortlist, index, signals),
                    inputSummary = "recommend:${shortlist.size} ứng viên"
                )
                aiRequestId = result.aiRequestId
                correlationId = result.correlationId
                modelCode = result.modelCode
                val applied = applyAiRanking(result.data, shortlist, diversified, limit)
                if (applied.items.isNotEmpty()) {
                    ordered = applied.items
                    aiReasons = applied.reasons
                    reasonSource = "AI"
                } else {
                    isFallback = true
                }
            } catch (ex: Exception) {
                // NFR-03: AI lỗi không được làm hỏng gợi ý.
                isFallback = true
                log.info("Gợi ý dùng fallback luật: {}", ex.message)
            }
        } else if (useAi && signals.personalized) {
            isFallback = true
        }

        val cards = loadCards(ordered.map { it.candidate.eventId }, userId)
        val items = ordered.mapNotNull { scored ->
            val card = cards[scored.candidate.eventId] ?: return@mapNotNull null
            val aiReason = aiReasons[scored.candidate.eventId]
            RecommendationItemDto(
                event = card,
                reason = (aiReason ?: scored.reason).take(REASON_MAX),
                reasonSource = if (aiReason != null) "AI" else "RULE"
            )
        }

        if (userId != null && items.isNotEmpty()) persist(userId, items, aiRequestId)

        return RecommendationListDto(
            items = items,
            meta = RecommendationMetaDto(
                title = if (signals.personalized) "Dành cho bạn" else "Phổ biến sắp diễn ra",
                generatedAt = now,
                isFallback = isFallback,
                personalized = signals.personalized,
                engineVersion = RecommendationEngine.ENGINE_VERSION,
                modelCode = modelCode,
                aiRequestId = aiRequestId,
                correlationId = correlationId
            )
        )
    }

    private fun isAvailable(candidate: CandidateRow): Boolean {
        val soldOut = candidate.onSaleCount > 0 && (candidate.availableQuantity ?: 0L) <= 0L
        return !soldOut
    }

    private fun dismissedRecently(userId: Long, eventId: Long, now: Instant): Boolean {
        val row = eventRecommendationRepository.findTopByUserIdAndEventIdOrderByShownAtDesc(userId, eventId)
        val dismissed = row?.dismissedAt ?: return false
        return Duration.between(dismissed, now) < DISMISS_WINDOW
    }

    // ------------------------------------------------------------------
    // Hậu kiểm AI (bước 4)
    // ------------------------------------------------------------------

    private data class AiApplied(val items: List<RecommendationEngine.Scored>, val reasons: Map<Long, String>)

    private fun applyAiRanking(
        data: JsonNode,
        shortlist: List<RecommendationEngine.Scored>,
        fallbackOrder: List<RecommendationEngine.Scored>,
        limit: Int
    ): AiApplied {
        val byId = shortlist.associateBy { it.candidate.eventId }
        val reasons = LinkedHashMap<Long, String>()
        val picked = mutableListOf<RecommendationEngine.Scored>()

        data.path("items").forEach { node ->
            if (picked.size >= limit) return@forEach
            val id = node.path("id").asLong(-1)
            val candidate = byId[id] ?: return@forEach // id lạ → loại
            if (picked.any { it.candidate.eventId == id }) return@forEach // trùng → loại
            val reason = sanitizeReason(node.path("reason").asText(""), candidate)
            picked += candidate
            reasons[id] = reason ?: candidate.reason
        }

        // Bổ sung từ danh sách luật nếu AI trả thiếu, giữ đa dạng.
        if (picked.size < limit) {
            fallbackOrder.forEach { item ->
                if (picked.size >= limit) return@forEach
                if (picked.none { it.candidate.eventId == item.candidate.eventId }) picked += item
            }
        }
        return AiApplied(picked, reasons)
    }

    /**
     * Lý do do AI viết phải: ≤140 ký tự và không chứa số (giá/ngày) — mọi con số hiển thị
     * lấy từ dữ liệu chính thức, không từ văn bản AI (U07, O27-R01).
     */
    private fun sanitizeReason(raw: String, scored: RecommendationEngine.Scored): String? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (text.length > REASON_MAX) return text.take(REASON_MAX)
        if (DIGIT_PATTERN.containsMatchIn(text)) return null
        return text
    }

    // ------------------------------------------------------------------
    // Dữ liệu đầu vào
    // ------------------------------------------------------------------

    private fun buildSignals(userId: Long): RecommendationEngine.Signals {
        val preference = preferenceRepository.findById(userId).orElse(null)
        if (preference != null && !preference.personalizationEnabled) {
            return RecommendationEngine.Signals(personalized = false)
        }
        val settings = preference?.settings
        val categoryIds = preference?.preferredCategories.orEmpty().toSet()
        val affinity = buildAffinity(userId)
        val personalized = categoryIds.isNotEmpty() || affinity.isNotEmpty()

        return RecommendationEngine.Signals(
            categoryIds = categoryIds,
            city = preference?.preferredCity ?: settings?.preferredCities?.firstOrNull(),
            acceptsOnline = acceptsOnline(preference),
            priceMode = settings?.priceMode ?: "ANY",
            maxBudgetVnd = settings?.maxBudgetVnd,
            preferredDays = settings?.preferredDays.orEmpty().map { it.uppercase() }.toSet(),
            affinity = affinity,
            personalized = personalized
        )
    }

    private fun acceptsOnline(preference: UserPreference?): Boolean {
        val formats = preference?.preferredEventFormats.orEmpty().map { it.uppercase() }
        if (formats.isEmpty()) return true
        return formats.any { it == "ONLINE" || it == "HYBRID" }
    }

    /** Ái lực danh mục: Σ(trọng số hành động × e^(−tuổi_ngày/30)), chuẩn hoá 0..1. */
    private fun buildAffinity(userId: Long): Map<Long, Double> {
        val now = Instant.now()
        val raw = mutableMapOf<Long, Double>()

        val favorites = favoriteRepository.findByIdUserIdOrderByCreatedAtDesc(userId)
        if (favorites.isNotEmpty()) {
            val categoryByEvent = categoryMapOf(favorites.map { it.id.eventId })
            favorites.forEach { favorite ->
                val age = favorite.createdAt?.let { Duration.between(it, now).toDays().toDouble() } ?: 0.0
                val decay = exp(-age / AFFINITY_HALF_LIFE_DAYS)
                categoryByEvent[favorite.id.eventId].orEmpty().forEach { categoryId ->
                    raw[categoryId] = (raw[categoryId] ?: 0.0) + WEIGHT_FAVORITE * decay
                }
            }
        }

        val registrations = registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId)
        if (registrations.isNotEmpty()) {
            val categoryByEvent = categoryMapOf(registrations.map { it.eventId })
            registrations.forEach { registration ->
                val age = registration.registeredAt?.let { Duration.between(it, now).toDays().toDouble() } ?: 0.0
                val decay = exp(-age / AFFINITY_HALF_LIFE_DAYS)
                categoryByEvent[registration.eventId].orEmpty().forEach { categoryId ->
                    raw[categoryId] = (raw[categoryId] ?: 0.0) + WEIGHT_REGISTRATION * decay
                }
            }
        }

        if (raw.isEmpty()) return emptyMap()
        val max = raw.values.max()
        if (max <= 0.0) return emptyMap()
        return raw.mapValues { it.value / max }
    }

    private fun categoryMapOf(eventIds: Collection<Long>): Map<Long, List<Long>> {
        if (eventIds.isEmpty()) return emptyMap()
        return categoryMapRepository.findByIdEventIdIn(eventIds.toSet())
            .groupBy({ it.id.eventId }, { it.id.categoryId })
    }

    private fun loadCategoryIndex(eventIds: Collection<Long>): RecommendationEngine.CategoryIndex {
        val byEvent = categoryMapOf(eventIds)
        val allCategories = categoryRepository.findAllByOrderBySortOrderAscCategoryIdAsc()
        return RecommendationEngine.CategoryIndex(
            byEvent = byEvent,
            parentOf = allCategories.associate { it.categoryId!! to it.parentCategoryId },
            nameOf = allCategories.associate { it.categoryId!! to it.categoryName }
        )
    }

    private fun loadCards(
        eventIds: List<Long>,
        userId: Long?
    ): Map<Long, com.eventapp.discovery.dto.EventCardDto> {
        if (eventIds.isEmpty()) return emptyMap()
        val rows: List<EventSearchRepository.Row> = eventSearchRepository.findRowsByIds(eventIds)
        return discoveryService.buildCards(rows, userId).associateBy { it.id }
    }

    private fun buildAiFields(
        shortlist: List<RecommendationEngine.Scored>,
        index: RecommendationEngine.CategoryIndex,
        signals: RecommendationEngine.Signals
    ): Map<String, Any?> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
        val candidates = shortlist.map { scored ->
            val row = scored.candidate
            mapOf(
                "id" to row.eventId,
                "title" to PromptBuilder.clip(row.title, 120),
                "categories" to index.of(row.eventId).mapNotNull { index.name(it) },
                "city" to (row.city ?: ""),
                "format" to row.eventFormat,
                "startDate" to formatter.format(row.startAt),
                "free" to (row.freeCount > 0)
            )
        }
        return mapOf(
            "preferences" to mapOf(
                "categories" to signals.categoryIds.mapNotNull { index.name(it) },
                "city" to (signals.city ?: ""),
                "budget" to (signals.maxBudgetVnd ?: 0),
                "formats" to if (signals.acceptsOnline) listOf("ONSITE", "ONLINE") else listOf("ONSITE"),
                "days" to signals.preferredDays.toList()
            ),
            "candidates" to candidates
        )
    }

    private fun persist(userId: Long, items: List<RecommendationItemDto>, aiRequestId: Long?) {
        val rows = items.mapIndexed { index, item ->
            com.eventapp.ai.entity.EventRecommendation(
                userId = userId,
                eventId = item.event.id,
                aiRequestId = aiRequestId,
                recommendationReason = item.reason.take(1000),
                recommendationScore = null,
                positionNo = (index + 1).toShort()
            )
        }
        eventRecommendationRepository.saveAll(rows)
    }

    companion object {
        const val MAX_CANDIDATES = 100
        const val AI_SHORTLIST = 30
        const val REASON_MAX = 140

        private val REFRESH_COOLDOWN: Duration = Duration.ofMinutes(1)
        private val DISMISS_WINDOW: Duration = Duration.ofDays(30)

        private const val WEIGHT_REGISTRATION = 3.0
        private const val WEIGHT_FAVORITE = 2.0
        private const val AFFINITY_HALF_LIFE_DAYS = 30.0

        private val DIGIT_PATTERN = Regex("\\d")
        private val PRICE_MODES = setOf("ANY", "FREE_ONLY", "PAID_ONLY")
        private val FORMATS = setOf("ANY", "ONSITE", "ONLINE")
    }
}
