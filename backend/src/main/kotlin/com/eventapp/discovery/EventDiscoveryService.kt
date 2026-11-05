package com.eventapp.discovery

import com.eventapp.catalog.repository.EventCategoryMapRepository
import com.eventapp.catalog.repository.EventCategoryRepository
import com.eventapp.catalog.repository.EventTagMapRepository
import com.eventapp.catalog.repository.EventTagRepository
import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.TicketTypeStatus
import com.eventapp.common.exception.NotFoundException
import com.eventapp.discovery.dto.DisabledReason
import com.eventapp.discovery.dto.EventAction
import com.eventapp.discovery.dto.EventCardDto
import com.eventapp.discovery.dto.EventDetailDto
import com.eventapp.discovery.dto.OrganizerBriefDto
import com.eventapp.discovery.dto.ReviewBriefDto
import com.eventapp.discovery.dto.ReviewSummaryDto
import com.eventapp.discovery.dto.TicketTypeDto
import com.eventapp.event.entity.Event
import com.eventapp.event.entity.TicketType
import com.eventapp.event.repository.EventMediaRepository
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.identity.repository.UserRepository
import com.eventapp.interaction.repository.FavoriteRepository
import com.eventapp.interaction.repository.ReviewRepository
import com.eventapp.ticketing.repository.RegistrationRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

@Service
@Transactional(readOnly = true)
class EventDiscoveryService(
    private val eventRepository: EventRepository,
    private val eventSearchRepository: EventSearchRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val mediaRepository: EventMediaRepository,
    private val categoryRepository: EventCategoryRepository,
    private val categoryMapRepository: EventCategoryMapRepository,
    private val tagRepository: EventTagRepository,
    private val tagMapRepository: EventTagMapRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val registrationRepository: RegistrationRepository,
    private val ticketRepository: TicketRepository,
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val userRepository: UserRepository
) {

    // ------------------------------------------------------------------
    // FR-U04 — danh sách, tìm kiếm, lọc
    // ------------------------------------------------------------------

    fun search(criteria: EventSearchRepository.Criteria, currentUserId: Long?): Pair<List<EventCardDto>, Long> {
        val result = eventSearchRepository.search(criteria)
        if (result.rows.isEmpty()) return emptyList<EventCardDto>() to result.totalItems
        return buildCards(result.rows, currentUserId) to result.totalItems
    }

    /** Thành phố có sự kiện công khai — dùng cho bộ lọc địa điểm của người dùng. */
    fun cities(): List<String> = eventSearchRepository.cities()

    /** Chuyển các dòng SQL thành thẻ sự kiện, nạp danh mục/nhãn/yêu thích theo lô. */
    fun buildCards(rows: List<EventSearchRepository.Row>, currentUserId: Long?): List<EventCardDto> {
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.eventId }
        val categoryNames = loadCategoryNames(ids)
        val tagNames = loadTagNames(ids)
        val favorites = currentUserId?.let {
            favoriteRepository.findByIdUserIdAndIdEventIdIn(it, ids).map { f -> f.id.eventId }.toSet()
        } ?: emptySet()

        val now = Instant.now()

        return rows.map { row ->
            EventCardDto(
                id = row.eventId,
                code = row.eventCode,
                title = row.title,
                slug = row.slug,
                shortDescription = row.shortDescription,
                coverUrl = row.coverUrl,
                startAt = row.startAt,
                endAt = row.endAt,
                timezone = row.timezone,
                city = row.city,
                venueName = row.venueName,
                eventFormat = row.eventFormat,
                categoryIds = categoryNames[row.eventId]?.first ?: emptyList(),
                categoryNames = categoryNames[row.eventId]?.second ?: emptyList(),
                tags = tagNames[row.eventId] ?: emptyList(),
                minPrice = row.minPrice,
                maxPrice = row.maxPrice,
                isFree = row.minPrice != null && row.minPrice.compareTo(BigDecimal.ZERO) == 0,
                available = if (row.activeCount == 0) null else (row.availableQuantity ?: 0L) > 0,
                availabilityUnknown = row.activeCount == 0,
                saleStatus = saleStatus(row, now),
                statusLabel = statusLabel(row, now),
                isFavorite = if (currentUserId == null) null else favorites.contains(row.eventId)
            )
        }
    }

    // ------------------------------------------------------------------
    // FR-U05 — chi tiết sự kiện
    // ------------------------------------------------------------------

    fun detail(eventId: Long, currentUserId: Long?): EventDetailDto {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")

        // Bảng trạng thái SRS FR-U05: nháp/chờ duyệt/từ chối/đình chỉ/chưa công khai -> 404
        val viewable = event.deletedAt == null &&
            event.reviewStatus == EventReviewStatus.APPROVED &&
            event.lifecycleStatus in setOf(
                EventLifecycleStatus.PUBLISHED,
                EventLifecycleStatus.CANCELLED,
                EventLifecycleStatus.COMPLETED
            )
        if (!viewable) throw NotFoundException(message = "Không tìm thấy sự kiện")

        val now = Instant.now()
        val media = mediaRepository.findByEventIdOrderBySortOrderAsc(eventId)
        val ticketTypes = ticketTypeRepository.findByEventIdOrderByPriceAsc(eventId)
            .filter { it.status != TicketTypeStatus.ARCHIVED }

        val categoryIds = categoryMapRepository.findByIdEventId(eventId).map { it.id.categoryId }
        val categoryNames = if (categoryIds.isEmpty()) emptyList()
        else categoryRepository.findAllById(categoryIds).sortedBy { it.sortOrder }.map { it.categoryName }

        val tagIds = tagMapRepository.findByIdEventId(eventId).map { it.id.tagId }
        val tagNames = if (tagIds.isEmpty()) emptyList()
        else tagRepository.findAllById(tagIds).map { it.tagName }

        val organizer = organizerProfileRepository.findById(event.organizerProfileId).orElse(null)
            ?.let {
                OrganizerBriefDto(
                    id = it.organizerProfileId!!,
                    publicName = it.publicName ?: it.organizationName,
                    description = it.description,
                    logoUrl = null,
                    organizedEventCount = eventRepository.findByOrganizerProfileIdOrderByStartAtDesc(it.organizerProfileId!!)
                        .count { e -> e.lifecycleStatus == EventLifecycleStatus.PUBLISHED }.toLong()
                )
            }

        val ticketDtos = ticketTypes.map { toTicketTypeDto(it, now) }
        val onSale = ticketDtos.filter { it.saleStatus == SALE_ON_SALE }
        val minPrice = onSale.mapNotNull { it.price }.minOrNull()
        val isFree = minPrice != null && minPrice.compareTo(BigDecimal.ZERO) == 0

        val isFavorite = currentUserId?.let { favoriteRepository.existsByIdUserIdAndIdEventId(it, eventId) } ?: false
        val registrationStatus = currentUserId
            ?.let { registrationRepository.findByUserIdAndEventId(it, eventId)?.status?.name }
            ?: "NONE"
        val canReview = currentUserId != null && canReview(currentUserId, event, now)

        val (actions, reason) = allowedActions(
            event = event,
            currentUserId = currentUserId,
            isFavorite = isFavorite,
            ticketTypes = ticketDtos,
            registrationStatus = registrationStatus,
            canReview = canReview,
            now = now
        )

        return EventDetailDto(
            id = event.eventId!!,
            code = event.eventCode,
            title = event.title,
            slug = event.slug,
            shortDescription = event.shortDescription,
            description = event.description,
            eventFormat = event.eventFormat.name,
            lifecycleStatus = event.lifecycleStatus.name,
            startAt = event.startAt,
            endAt = event.endAt,
            timezone = event.timezone,
            registrationStartAt = event.registrationStartAt,
            registrationEndAt = event.registrationEndAt,
            venueName = event.venueName,
            addressLine = event.addressLine,
            city = event.city,
            province = event.province,
            onlineUrl = event.onlineUrl,
            capacityLimit = event.capacityLimit,
            participationConditions = event.participationConditions,
            cancellationPolicy = event.cancellationPolicy,
            coverUrl = media.firstOrNull { it.isCover }?.storageUrl ?: media.firstOrNull()?.storageUrl,
            mediaUrls = media.map { it.storageUrl },
            categoryIds = categoryIds,
            categoryNames = categoryNames,
            tags = tagNames,
            organizer = organizer,
            ticketTypes = ticketDtos,
            minPrice = minPrice,
            isFree = isFree,
            reviewSummary = reviewSummary(eventId),
            isFavorite = isFavorite,
            registrationStatus = registrationStatus,
            canReview = canReview,
            allowedActions = actions.map { it.name },
            disabledReason = reason?.name,
            serverTime = now,
            fetchedAt = now
        )
    }

    fun reviewSummary(eventId: Long): ReviewSummaryDto {
        val avg = reviewRepository.averageRating(eventId) ?: 0.0
        val total = reviewRepository.countByEventIdAndStatus(eventId, com.eventapp.common.enums.ReviewStatus.VISIBLE)
        val distribution = (1..5).associate { star ->
            star.toString() to (reviewRepository.ratingDistribution(eventId)
                .firstOrNull { (it[0] as Number).toInt() == star }
                ?.let { (it[1] as Number).toLong() } ?: 0L)
        }
        val highlights = reviewRepository
            .findByEventIdAndStatusOrderByCreatedAtDesc(
                eventId,
                com.eventapp.common.enums.ReviewStatus.VISIBLE,
                org.springframework.data.domain.PageRequest.of(0, 3)
            )
            .content
            .map { r ->
                ReviewBriefDto(
                    id = r.reviewId!!,
                    authorName = displayName(r.userId),
                    rating = r.rating.toInt(),
                    text = r.reviewText,
                    createdAt = r.createdAt
                )
            }
        return ReviewSummaryDto(
            averageRating = BigDecimal(avg).setScale(1, RoundingMode.HALF_UP).toDouble(),
            totalReviews = total,
            distribution = distribution,
            highlights = highlights
        )
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    fun canReview(userId: Long, event: Event, now: Instant): Boolean {
        if (event.endAt.isAfter(now)) return false
        if (now.isAfter(event.endAt.plus(Duration.ofDays(REVIEW_WINDOW_DAYS)))) return false
        if (ticketRepository.countCheckedIn(userId, event.eventId!!) == 0L) return false
        return reviewRepository.findByUserIdAndEventId(userId, event.eventId!!) == null
    }

    /** U20-R04: chỉ hiển thị họ + tên, che tên đệm. "Lê Hoàng Anh" -> "Lê Anh". */
    fun displayName(userId: Long): String {
        val user = userRepository.findById(userId).orElse(null) ?: return "Người dùng"
        val parts = user.fullName.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "Người dùng"
            parts.size == 1 -> parts[0]
            else -> parts.first() + " " + parts.last()
        }
    }

    fun toTicketTypeDto(t: TicketType, now: Instant): TicketTypeDto {
        val remaining = t.remaining()
        val onSale = t.status == TicketTypeStatus.ACTIVE &&
            (t.saleStartAt == null || !t.saleStartAt!!.isAfter(now)) &&
            (t.saleEndAt == null || t.saleEndAt!!.isAfter(now))
        val saleStatus = when {
            !onSale && t.saleStartAt != null && t.saleStartAt!!.isAfter(now) -> SALE_NOT_STARTED
            !onSale && t.status == TicketTypeStatus.ACTIVE -> SALE_ENDED
            !onSale -> SALE_ENDED
            remaining != null && remaining <= 0 -> SALE_SOLD_OUT
            else -> SALE_ON_SALE
        }
        return TicketTypeDto(
            id = t.ticketTypeId!!,
            name = t.ticketTypeName,
            description = t.description,
            kind = t.ticketKind.name,
            price = t.price,
            currency = t.currency,
            saleStatus = saleStatus,
            saleStartAt = t.saleStartAt,
            saleEndAt = t.saleEndAt,
            remaining = remaining,
            remainingLabel = remainingLabel(remaining, t.quantityTotal),
            minPerOrder = t.minPerOrder.toInt(),
            maxPerUser = t.maxPerUser.toInt()
        )
    }

    private fun remainingLabel(remaining: Int?, total: Int?): String? {
        if (remaining == null || total == null || total <= 0) return null
        if (remaining <= 0) return "Hết vé"
        return if (remaining * 10 <= total) "Còn ít vé" else null
    }

    private fun saleStatus(row: EventSearchRepository.Row, now: Instant): String {
        // Sự kiện đã qua giờ kết thúc thì không còn bán vé, bất kể cửa sổ bán của loại vé.
        if (!row.endAt.isAfter(now)) return SALE_ENDED
        return when {
            row.activeCount == 0 -> SALE_ENDED
            row.onSaleCount > 0 && (row.availableQuantity ?: 0) > 0 -> SALE_ON_SALE
            row.onSaleCount > 0 -> SALE_SOLD_OUT
            row.futureCount > 0 -> SALE_NOT_STARTED
            else -> SALE_ENDED
        }
    }

    private fun statusLabel(row: EventSearchRepository.Row, now: Instant): String {
        // U04: sự kiện đã kết thúc phải hiện «Đã kết thúc», kể cả khi lifecycle chưa kịp
        // chuyển sang COMPLETED mà loại vé vẫn còn trong cửa sổ bán.
        if (!row.endAt.isAfter(now)) return "Đã kết thúc"
        return when (saleStatus(row, now)) {
            SALE_ON_SALE -> "Đang mở bán"
            SALE_NOT_STARTED -> "Sắp mở bán"
            SALE_SOLD_OUT -> "Hết vé"
            else -> "Đã kết thúc bán"
        }
    }

    private fun allowedActions(
        event: Event,
        currentUserId: Long?,
        isFavorite: Boolean,
        ticketTypes: List<TicketTypeDto>,
        registrationStatus: String,
        canReview: Boolean,
        now: Instant
    ): Pair<List<EventAction>, DisabledReason?> {
        if (event.lifecycleStatus == EventLifecycleStatus.CANCELLED) {
            return emptyList<EventAction>() to DisabledReason.EVENT_CANCELLED
        }
        if (event.lifecycleStatus == EventLifecycleStatus.COMPLETED) {
            val actions = buildList {
                if (canReview) add(EventAction.WRITE_REVIEW)
                if (currentUserId != null && isFavorite) add(EventAction.UNFAVORITE)
                if (currentUserId != null && !isFavorite) add(EventAction.FAVORITE)
            }
            return actions to DisabledReason.EVENT_COMPLETED
        }

        val actions = mutableListOf<EventAction>()
        var reason: DisabledReason? = null

        if (currentUserId == null) {
            reason = DisabledReason.LOGIN_REQUIRED
        } else {
            if (isFavorite) actions += EventAction.UNFAVORITE else actions += EventAction.FAVORITE
            if (registrationStatus != "NONE" && registrationStatus != "CANCELLED") {
                actions += EventAction.VIEW_MY_TICKET
                reason = DisabledReason.ALREADY_REGISTERED
            }
        }

        val onSale = ticketTypes.filter { it.saleStatus == SALE_ON_SALE && (it.remaining == null || it.remaining > 0) }
        val hasFree = onSale.any { it.kind == "FREE" }
        val hasPaid = onSale.any { it.kind == "PAID" }

        if (registrationStatus == "NONE" || registrationStatus == "CANCELLED") {
            if (hasFree) actions += EventAction.REGISTER_FREE
            if (hasPaid) actions += EventAction.BUY_TICKET

            if (!hasFree && !hasPaid) {
                reason = when {
                    ticketTypes.any { it.saleStatus == SALE_NOT_STARTED } -> DisabledReason.SALES_NOT_STARTED
                    ticketTypes.any { it.saleStatus == SALE_SOLD_OUT } -> DisabledReason.SOLD_OUT
                    else -> DisabledReason.SALES_ENDED
                }
            }
        }

        if (canReview) actions += EventAction.WRITE_REVIEW
        return actions to reason
    }

    private fun loadCategoryNames(eventIds: List<Long>): Map<Long, Pair<List<Long>, List<String>>> {
        val maps = categoryMapRepository.findByIdEventIdIn(eventIds)
        if (maps.isEmpty()) return emptyMap()
        val categories = categoryRepository.findAllById(maps.map { it.id.categoryId }).associateBy { it.categoryId!! }
        return maps.groupBy { it.id.eventId }.mapValues { (_, v) ->
            val ids = v.map { it.id.categoryId }.sorted()
            ids to ids.mapNotNull { categories[it]?.categoryName }
        }
    }

    private fun loadTagNames(eventIds: List<Long>): Map<Long, List<String>> {
        val maps = tagMapRepository.findByIdEventIdIn(eventIds)
        if (maps.isEmpty()) return emptyMap()
        val tags = tagRepository.findAllById(maps.map { it.id.tagId }).associateBy { it.tagId!! }
        return maps.groupBy { it.id.eventId }.mapValues { (_, v) -> v.mapNotNull { tags[it.id.tagId]?.tagName } }
    }

    companion object {
        const val SALE_NOT_STARTED = "NOT_STARTED"
        const val SALE_ON_SALE = "ON_SALE"
        const val SALE_ENDED = "ENDED"
        const val SALE_SOLD_OUT = "SOLD_OUT"
        const val REVIEW_WINDOW_DAYS = 30L
    }
}
