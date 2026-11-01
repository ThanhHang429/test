package com.eventapp.favorite

import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.discovery.EventDiscoveryService
import com.eventapp.discovery.EventSearchRepository
import com.eventapp.event.repository.EventRepository
import com.eventapp.favorite.dto.FavoriteItemDto
import com.eventapp.interaction.entity.Favorite
import com.eventapp.interaction.entity.FavoriteId
import com.eventapp.interaction.repository.FavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.eventapp.recommendation.RecommendationCache
import java.time.Instant

@Service
class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val eventRepository: EventRepository,
    private val eventSearchRepository: EventSearchRepository,
    private val discoveryService: EventDiscoveryService,
    private val recommendationCache: RecommendationCache
) {

    /** PUT /me/favorites/{eventId} — idempotent (SRS U06). */
    @Transactional
    fun add(userId: Long, eventId: Long): Boolean {
        if (favoriteRepository.existsByIdUserIdAndIdEventId(userId, eventId)) return true

        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Sự kiện không còn khả dụng")
        // U06-R02: không thể lưu sự kiện đang ẩn/chưa công khai
        if (event.deletedAt != null ||
            event.reviewStatus != EventReviewStatus.APPROVED ||
            event.lifecycleStatus != EventLifecycleStatus.PUBLISHED
        ) {
            throw NotFoundException(message = "Sự kiện không còn khả dụng")
        }

        if (favoriteRepository.countByIdUserId(userId) >= MAX_FAVORITES) {
            throw ConflictException(ErrorCode.FAVORITE_LIMIT_REACHED)
        }
        favoriteRepository.save(Favorite(FavoriteId(userId, eventId)))
        recommendationCache.invalidate(userId)
        return true
    }

    /** DELETE /me/favorites/{eventId} — idempotent. */
    @Transactional
    fun remove(userId: Long, eventId: Long): Boolean {
        favoriteRepository.deleteByIdUserIdAndIdEventId(userId, eventId)
        recommendationCache.invalidate(userId)
        return false
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, filter: String, page: Int, size: Int): Pair<List<FavoriteItemDto>, Long> {
        val normalized = filter.uppercase()
        if (normalized !in FILTERS) throw validationError("filter", "Giá trị phải là ALL, UPCOMING hoặc PAST")

        val all = favoriteRepository.findByIdUserIdOrderByCreatedAtDesc(userId)
        if (all.isEmpty()) return emptyList<FavoriteItemDto>() to 0L

        val eventIds = all.map { it.id.eventId }
        val events = eventRepository.findAllById(eventIds).associateBy { it.eventId!! }
        val cards = discoveryService
            .buildCards(eventSearchRepository.findRowsByIds(eventIds), userId)
            .associateBy { it.id }

        val now = Instant.now()
        val filtered = all.filter { fav ->
            val event = events[fav.id.eventId] ?: return@filter normalized == "ALL"
            when (normalized) {
                "UPCOMING" -> !event.endAt.isBefore(now)
                "PAST" -> event.endAt.isBefore(now)
                else -> true
            }
        }

        val total = filtered.size.toLong()
        val pageItems = filtered.drop(page * size).take(size).map { fav ->
            val event = events[fav.id.eventId]
            val visible = event != null &&
                event.deletedAt == null &&
                event.reviewStatus == EventReviewStatus.APPROVED &&
                event.lifecycleStatus !in setOf(EventLifecycleStatus.SUSPENDED, EventLifecycleStatus.UNPUBLISHED)

            if (!visible) {
                return@map FavoriteItemDto(
                    eventId = fav.id.eventId,
                    available = false,
                    statusLabel = "Sự kiện không còn khả dụng",
                    favoritedAt = fav.createdAt,
                    event = null
                )
            }

            val label = when (event!!.lifecycleStatus) {
                EventLifecycleStatus.CANCELLED -> "Đã huỷ"
                EventLifecycleStatus.COMPLETED -> "Đã kết thúc"
                else -> cards[fav.id.eventId]?.statusLabel ?: ""
            }
            FavoriteItemDto(
                eventId = fav.id.eventId,
                available = true,
                statusLabel = label,
                favoritedAt = fav.createdAt,
                event = cards[fav.id.eventId]
            )
        }
        return pageItems to total
    }

    companion object {
        const val MAX_FAVORITES = 500L
        private val FILTERS = setOf("ALL", "UPCOMING", "PAST")
    }
}
