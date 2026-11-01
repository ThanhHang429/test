package com.eventapp.recommendation

import com.eventapp.recommendation.RecommendationRepository.CandidateRow
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.ln

/**
 * Recommendation Engine thuần luật (SRS 5.2) — phương án dự phòng hoàn đầy đủ khi AI lỗi.
 *
 * score = Σ wᵢ·sᵢ với sᵢ ∈ [0,1]. Với người mới (chưa có sở thích/hành vi) chỉ dùng
 * phổ biến + gần thời gian + mức khẩn và chuẩn hoá lại trọng số, đúng như SRS mô tả.
 *
 * Kết quả xác định: cùng đầu vào + cùng thời điểm → cùng thứ tự; tie-break theo startAt rồi id (RE-R02).
 *
 * Diễn giải "danh mục chính/phụ" khi schema chỉ có `event_category_map` (không có cờ is_primary):
 *  - khớp đúng danh mục của sự kiện với sở thích → 1,0 (danh mục chính)
 *  - khớp danh mục cha của nó → 0,5 (danh mục phụ)
 */
@Component
class RecommendationEngine {

    /** Chỉ mục danh mục nạp sẵn theo lô để không truy vấn trong vòng lặp. */
    data class CategoryIndex(
        val byEvent: Map<Long, List<Long>>,
        val parentOf: Map<Long, Long?>,
        val nameOf: Map<Long, String>
    ) {
        fun of(eventId: Long): List<Long> = byEvent[eventId].orEmpty()
        fun name(categoryId: Long?): String? = categoryId?.let { nameOf[it] }
    }

    data class Signals(
        val categoryIds: Set<Long> = emptySet(),
        val city: String? = null,
        val acceptsOnline: Boolean = true,
        /** ANY | FREE_ONLY | PAID_ONLY */
        val priceMode: String = "ANY",
        val maxBudgetVnd: Long? = null,
        val preferredDays: Set<String> = emptySet(),
        /** categoryId → ái lực đã chuẩn hoá 0..1 */
        val affinity: Map<Long, Double> = emptyMap(),
        val personalized: Boolean = false
    )

    data class Scored(
        val candidate: CandidateRow,
        val score: Double,
        val reason: String,
        val categoryIds: List<Long>
    )

    fun score(
        candidates: List<CandidateRow>,
        index: CategoryIndex,
        signals: Signals,
        now: Instant
    ): List<Scored> {
        if (candidates.isEmpty()) return emptyList()

        val maxPopularity = candidates.maxOf { it.popularity7d }
        val maxLog = ln(1.0 + maxPopularity).takeIf { it > 0.0 } ?: 1.0

        return candidates.map { candidate ->
            val categories = index.of(candidate.eventId)
            val parts = LinkedHashMap<String, Double>()

            if (signals.personalized) {
                parts["category"] = W_CATEGORY * categoryScore(categories, index, signals)
                parts["price"] = W_PRICE * priceScore(candidate, signals)
                parts["location"] = W_LOCATION * locationScore(candidate, signals)
                parts["time"] = W_TIME * timeScore(candidate, signals, now)
                parts["affinity"] = W_AFFINITY * affinityScore(categories, signals)
                parts["popularity"] = W_POPULARITY * popularityScore(candidate, maxLog)
                parts["urgency"] = W_URGENCY * urgencyScore(candidate, now)
            } else {
                val scale = W_POPULARITY + W_TIME + W_URGENCY
                parts["popularity"] = W_POPULARITY / scale * popularityScore(candidate, maxLog)
                parts["time"] = W_TIME / scale * timeScore(candidate, signals, now)
                parts["urgency"] = W_URGENCY / scale * urgencyScore(candidate, now)
            }

            Scored(
                candidate = candidate,
                score = parts.values.sum(),
                reason = reasonFor(candidate, categories, index, signals, parts),
                categoryIds = categories
            )
        }.sortedWith(
            compareByDescending<Scored> { it.score }
                .thenBy { it.candidate.startAt }
                .thenBy { it.candidate.eventId }
        )
    }

    /**
     * U07-R05: top 10 có tối đa 3 sự kiện cùng danh mục và tối đa 2 cùng Organizer.
     * Đồng thời áp hình phạt của SRS 5.2 (từ sự kiện thứ 3 cùng Organizer trừ 0,1).
     */
    fun diversify(ranked: List<Scored>, limit: Int = 10): List<Scored> {
        val picked = mutableListOf<Scored>()
        val perOrganizer = mutableMapOf<Long, Int>()
        val perCategory = mutableMapOf<Long, Int>()

        for (item in ranked) {
            if (picked.size >= limit) break
            val organizerId = item.candidate.organizerProfileId
            val primaryCategory = item.categoryIds.firstOrNull() ?: 0L

            if ((perOrganizer[organizerId] ?: 0) >= MAX_PER_ORGANIZER) continue
            if (primaryCategory != 0L && (perCategory[primaryCategory] ?: 0) >= MAX_PER_CATEGORY) continue

            picked += item
            perOrganizer[organizerId] = (perOrganizer[organizerId] ?: 0) + 1
            if (primaryCategory != 0L) perCategory[primaryCategory] = (perCategory[primaryCategory] ?: 0) + 1
        }
        return picked
    }

    // ------------------------------------------------------------------
    // Thành phần điểm
    // ------------------------------------------------------------------

    private fun categoryScore(categories: List<Long>, index: CategoryIndex, signals: Signals): Double {
        if (signals.categoryIds.isEmpty() || categories.isEmpty()) return 0.0
        if (categories.any { signals.categoryIds.contains(it) }) return 1.0
        val matchesParent = categories.any { categoryId ->
            index.parentOf[categoryId]?.let { signals.categoryIds.contains(it) } == true
        }
        return if (matchesParent) 0.5 else 0.0
    }

    private fun priceScore(candidate: CandidateRow, signals: Signals): Double {
        val minPrice = candidate.minPrice?.toLong()
        val isFree = candidate.freeCount > 0 && (minPrice == null || minPrice == 0L)
        return when (signals.priceMode.uppercase()) {
            "FREE_ONLY" -> if (isFree) 1.0 else 0.0
            "ANY" -> 0.5
            else -> {
                val budget = signals.maxBudgetVnd ?: return 0.5
                if (minPrice == null) return 0.5
                when {
                    minPrice <= budget -> 1.0
                    minPrice >= budget * 2 -> 0.0
                    else -> 1.0 - (minPrice - budget).toDouble() / budget.toDouble()
                }
            }
        }
    }

    private fun locationScore(candidate: CandidateRow, signals: Signals): Double {
        val online = candidate.eventFormat == "ONLINE"
        if (online && signals.acceptsOnline) return 1.0
        val city = signals.city
        if (city.isNullOrBlank()) return 0.5
        return if (candidate.city?.trim().equals(city.trim(), ignoreCase = true)) 1.0 else 0.0
    }

    private fun timeScore(candidate: CandidateRow, signals: Signals, now: Instant): Double {
        val days = Duration.between(now, candidate.startAt).toDays().toDouble().coerceAtLeast(0.0)
        var score = when {
            days <= NEAR_DAYS -> 1.0
            days >= FAR_DAYS -> 0.0
            else -> 1.0 - (days - NEAR_DAYS) / (FAR_DAYS - NEAR_DAYS)
        }
        if (signals.preferredDays.isNotEmpty()) {
            val isWeekend = isWeekend(candidate.startAt)
            val wantsWeekend = "WEEKEND" in signals.preferredDays
            val wantsWeekday = "WEEKDAY" in signals.preferredDays
            if ((isWeekend && wantsWeekend) || (!isWeekend && wantsWeekday)) {
                score = (score + DAY_BONUS).coerceAtMost(1.0)
            }
        }
        return score
    }

    private fun affinityScore(categories: List<Long>, signals: Signals): Double {
        if (signals.affinity.isEmpty() || categories.isEmpty()) return 0.0
        return categories.maxOf { signals.affinity[it] ?: 0.0 }.coerceIn(0.0, 1.0)
    }

    private fun popularityScore(candidate: CandidateRow, maxLog: Double): Double =
        (ln(1.0 + candidate.popularity7d) / maxLog).coerceIn(0.0, 1.0)

    private fun urgencyScore(candidate: CandidateRow, now: Instant): Double {
        val saleOpenedRecently = candidate.registrationStartAt?.let {
            Duration.between(it, now).toDays() in 0..7
        } ?: false
        val lowStock = candidate.onSaleCount > 0 &&
            (candidate.availableQuantity ?: Long.MAX_VALUE) < LOW_STOCK_THRESHOLD
        return if (saleOpenedRecently || lowStock) 1.0 else 0.0
    }

    // ------------------------------------------------------------------
    // Lý do mẫu (SRS 5.2) — chọn theo thành phần đóng góp lớn nhất
    // ------------------------------------------------------------------

    private fun reasonFor(
        candidate: CandidateRow,
        categories: List<Long>,
        index: CategoryIndex,
        signals: Signals,
        parts: Map<String, Double>
    ): String {
        val categoryName = index.name(categories.firstOrNull())
        if (!signals.personalized) {
            return if (candidate.popularity7d > 0) "Đang được nhiều người quan tâm"
            else "Sự kiện sắp diễn ra"
        }
        return when (parts.maxByOrNull { it.value }?.key) {
            "category" -> if (categoryName != null) "Phù hợp với sở thích $categoryName của bạn"
            else "Phù hợp với sở thích của bạn"
            "affinity" -> if (categoryName != null) "Bạn đã lưu các sự kiện $categoryName"
            else "Dựa trên hoạt động gần đây của bạn"
            "price" -> when {
                candidate.freeCount > 0 && !candidate.city.isNullOrBlank() -> "Miễn phí và diễn ra ở ${candidate.city}"
                candidate.freeCount > 0 -> "Miễn phí và phù hợp ngân sách của bạn"
                else -> "Phù hợp với ngân sách của bạn"
            }
            "location" -> if (!candidate.city.isNullOrBlank()) "Diễn ra ở ${candidate.city}"
            else "Hình thức phù hợp với bạn"
            "popularity" -> "Đang được nhiều người quan tâm"
            "urgency" -> "Sắp hết chỗ hoặc vừa mở bán"
            else -> "Sự kiện sắp diễn ra, phù hợp với bạn"
        }
    }

    private fun isWeekend(instant: Instant): Boolean {
        val local = ZonedDateTime.ofInstant(instant, ZoneId.of("Asia/Ho_Chi_Minh"))
        return local.dayOfWeek.value >= 6
    }

    companion object {
        // Trọng số SRS 5.2 (RE-R01: hiện là hằng số có tên, đổi được mà không sửa logic).
        const val W_CATEGORY = 0.30
        const val W_PRICE = 0.15
        const val W_LOCATION = 0.15
        const val W_TIME = 0.10
        const val W_AFFINITY = 0.15
        const val W_POPULARITY = 0.10
        const val W_URGENCY = 0.05

        const val ENGINE_VERSION = "rule-1.0.0"
        const val MAX_PER_ORGANIZER = 2
        const val MAX_PER_CATEGORY = 3
        const val LOW_STOCK_THRESHOLD = 20L
        const val NEAR_DAYS = 14.0
        const val FAR_DAYS = 90.0
        const val DAY_BONUS = 0.1
    }
}
