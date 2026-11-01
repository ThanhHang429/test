package com.eventapp.preference

import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.validationError
import com.eventapp.interaction.entity.PreferenceSettings
import com.eventapp.interaction.entity.UserPreference
import com.eventapp.interaction.repository.UserPreferenceRepository
import com.eventapp.preference.dto.PreferenceDto
import com.eventapp.preference.dto.PreferenceUpdateRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.eventapp.recommendation.RecommendationCache

@Service
class PreferenceService(
    private val preferenceRepository: UserPreferenceRepository,
    private val categoryRepository: EventCategoryRepository,
    private val recommendationCache: RecommendationCache
) {

    @Transactional(readOnly = true)
    fun get(userId: Long): PreferenceDto = toDto(userId, preferenceRepository.findById(userId).orElse(null))

    /**
     * PUT idempotent (U03-R01): cùng nội dung gửi lặp không tăng version.
     */
    @Transactional
    fun put(userId: Long, request: PreferenceUpdateRequest): PreferenceDto {
        val current = preferenceRepository.findById(userId).orElse(null)
        val currentDto = toDto(userId, current)

        if (request.version != null && request.version != currentDto.version) {
            throw ConflictException(
                message = "Dữ liệu sở thích đã thay đổi ở thiết bị khác (version ${currentDto.version})"
            )
        }

        val categoryIds = request.categoryIds.distinct()
        if (categoryIds.size > MAX_CATEGORIES) {
            throw validationError("categoryIds", "Chọn tối đa $MAX_CATEGORIES danh mục")
        }
        if (categoryIds.isNotEmpty()) {
            val found = categoryRepository.findAllById(categoryIds)
            if (found.size != categoryIds.size) {
                val missing = categoryIds - found.map { it.categoryId!! }.toSet()
                throw validationError("categoryIds", "Danh mục không tồn tại: $missing")
            }
            val hidden = found.filter { !it.isActive }.map { it.categoryId!! }
            if (hidden.isNotEmpty()) {
                throw validationError("categoryIds", "Danh mục đã bị ẩn: $hidden")
            }
        }

        val format = request.eventFormat.uppercase()
        if (format !in FORMATS) throw validationError("eventFormat", "Giá trị phải là ANY, ONSITE hoặc ONLINE")

        val priceMode = request.priceMode.uppercase()
        if (priceMode !in PRICE_MODES) throw validationError("priceMode", "Giá trị phải là ANY, FREE_ONLY hoặc PAID_ONLY")

        val cities = request.preferredCities.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cities.size > MAX_CITIES) throw validationError("preferredCities", "Chọn tối đa $MAX_CITIES thành phố")

        val days = request.preferredDays.map { it.uppercase() }.filter { it.isNotEmpty() }.distinct()
        if (days.any { it !in DAYS }) throw validationError("preferredDays", "Giá trị phải là WEEKDAY hoặc WEEKEND")

        // FREE_ONLY thì bỏ qua ngân sách (SRS FR-U03)
        val budget = if (priceMode == "FREE_ONLY") null else request.maxBudgetVnd
        if (budget != null && (budget < 0 || budget > MAX_BUDGET)) {
            throw validationError("maxBudgetVnd", "Ngân sách phải trong khoảng 0..$MAX_BUDGET")
        }

        val newSettings = PreferenceSettings(
            priceMode = priceMode,
            maxBudgetVnd = budget,
            preferredDays = days.ifEmpty { listOf("WEEKDAY", "WEEKEND") },
            preferredCities = cities,
            onboardingCompleted = request.onboardingCompleted ?: currentDto.onboardingCompleted,
            version = currentDto.version
        )

        val unchanged = categoryIds.sorted() == currentDto.categoryIds.sorted() &&
            format == currentDto.eventFormat &&
            priceMode == currentDto.priceMode &&
            budget == currentDto.maxBudgetVnd &&
            cities == currentDto.preferredCities &&
            days.sorted() == currentDto.preferredDays.sorted() &&
            newSettings.onboardingCompleted == currentDto.onboardingCompleted &&
            (request.personalizationEnabled ?: currentDto.personalizationEnabled) == currentDto.personalizationEnabled

        val entity = current ?: UserPreference(userId = userId)
        entity.preferredCategories = categoryIds
        entity.preferredEventFormats = when (format) {
            "ONSITE" -> listOf("OFFLINE")
            "ONLINE" -> listOf("ONLINE")
            else -> listOf("OFFLINE", "ONLINE", "HYBRID")
        }
        entity.preferredCity = cities.firstOrNull()
        entity.personalizationEnabled = request.personalizationEnabled ?: currentDto.personalizationEnabled
        entity.settings = if (unchanged) current?.settings ?: newSettings
        else newSettings.copy(version = currentDto.version + 1)

        preferenceRepository.save(entity)
        recommendationCache.invalidate(userId)
        return get(userId)
    }

    /** DELETE: đặt về mặc định, giữ nguyên cờ onboarding (SRS FR-U03). */
    @Transactional
    fun reset(userId: Long): PreferenceDto {
        val current = preferenceRepository.findById(userId).orElse(null)
        val onboarding = toDto(userId, current).onboardingCompleted
        val entity = current ?: UserPreference(userId = userId)
        entity.preferredCategories = emptyList()
        entity.preferredEventFormats = listOf("OFFLINE", "ONLINE", "HYBRID")
        entity.preferredCity = null
        entity.settings = PreferenceSettings(onboardingCompleted = onboarding, version = 1)
        preferenceRepository.save(entity)
        recommendationCache.invalidate(userId)
        return get(userId)
    }

    private fun toDto(userId: Long, entity: UserPreference?): PreferenceDto {
        if (entity == null) {
            return PreferenceDto(
                categoryIds = emptyList(),
                eventFormat = "ANY",
                priceMode = "ANY",
                maxBudgetVnd = null,
                preferredCities = emptyList(),
                preferredDays = listOf("WEEKDAY", "WEEKEND"),
                onboardingCompleted = false,
                personalizationEnabled = true,
                version = 0
            )
        }
        val settings = entity.settings ?: PreferenceSettings()
        val formats = entity.preferredEventFormats ?: listOf("OFFLINE", "ONLINE", "HYBRID")
        val eventFormat = when {
            formats.size >= 3 -> "ANY"
            formats.contains("OFFLINE") -> "ONSITE"
            formats.contains("ONLINE") -> "ONLINE"
            else -> "ANY"
        }
        val savedCategories = entity.preferredCategories ?: emptyList()
        val activeIds = if (savedCategories.isEmpty()) emptySet()
        else categoryRepository.findAllById(savedCategories).filter { it.isActive }.map { it.categoryId!! }.toSet()

        val cities = settings.preferredCities.ifEmpty { listOfNotNull(entity.preferredCity) }

        return PreferenceDto(
            categoryIds = savedCategories,
            eventFormat = eventFormat,
            priceMode = settings.priceMode,
            maxBudgetVnd = settings.maxBudgetVnd,
            preferredCities = cities,
            preferredDays = settings.preferredDays,
            onboardingCompleted = settings.onboardingCompleted,
            personalizationEnabled = entity.personalizationEnabled,
            version = settings.version,
            removedCategoryIds = savedCategories.filter { it !in activeIds }
        )
    }

    companion object {
        const val MAX_CATEGORIES = 10
        const val MAX_CITIES = 3
        const val MAX_BUDGET = 100_000_000L
        private val FORMATS = setOf("ANY", "ONSITE", "ONLINE")
        private val PRICE_MODES = setOf("ANY", "FREE_ONLY", "PAID_ONLY")
        private val DAYS = setOf("WEEKDAY", "WEEKEND")
    }
}
