package com.eventapp.organizer.event

import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.OrderStatus
import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventMediaRepository
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.ticketing.repository.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * FR-O02 — danh sách sự kiện của Organizer đang đăng nhập.
 *
 * O02-R01: không nhận organizerId từ client, luôn suy ra từ token.
 * O03-R04: trả cả `reviewStatus` và `lifecycleStatus` vì hai vòng đời độc lập.
 * BR-022: doanh thu chỉ cộng đơn PAID, đơn PENDING_PAYMENT không được tính.
 */
@Service
class OrganizerEventService(
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val eventRepository: EventRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val eventMediaRepository: EventMediaRepository,
    private val orderRepository: OrderRepository
) {

    @Transactional(readOnly = true)
    fun list(userId: Long, status: String, page: Int, size: Int): Pair<List<OrganizerEventDto>, Long> {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        if (profile.status != OrganizerStatus.APPROVED && profile.status != OrganizerStatus.SUSPENDED) {
            throw ForbiddenException(
                ErrorCode.FORBIDDEN,
                "Hồ sơ Organizer đang ở trạng thái ${profile.status.name}, chưa xem được danh sách sự kiện"
            )
        }

        val normalized = status.trim().uppercase()
        if (normalized != "ALL" && !FILTERS.contains(normalized)) {
            throw validationError("status", "Giá trị phải là ALL hoặc một trạng thái sự kiện hợp lệ")
        }

        val events = eventRepository.findByOrganizerProfileIdOrderByStartAtDesc(profile.organizerProfileId!!)
            .filter { it.deletedAt == null }
            .filter { matches(it, normalized) }

        val total = events.size.toLong()
        val pageItems = events.drop(page * size).take(size)
        return build(pageItems) to total
    }

    @Transactional(readOnly = true)
    fun detail(userId: Long, eventId: Long): OrganizerEventDto {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        val event = eventRepository.findById(eventId).orElse(null)
        if (event == null || event.deletedAt != null) {
            throw NotFoundException(message = "Không tìm thấy sự kiện")
        }
        if (event.organizerProfileId != profile.organizerProfileId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Sự kiện không thuộc quyền quản lý của bạn")
        }
        return build(listOf(event)).first()
    }

    /** Lọc theo đúng một trong hai vòng đời; giá trị lạ đã bị chặn ở [list]. */
    private fun matches(event: Event, filter: String): Boolean = when (filter) {
        "ALL" -> true
        "DRAFT", "PENDING_REVIEW", "REVISION_REQUIRED", "REJECTED", "APPROVED" ->
            event.reviewStatus.name == filter
        else -> event.lifecycleStatus.name == filter
    }

    private fun build(events: List<Event>): List<OrganizerEventDto> {
        if (events.isEmpty()) return emptyList()
        val ids = events.mapNotNull { it.eventId }
        val ticketTypes = ticketTypeRepository.findByEventIdIn(ids).groupBy { it.eventId }
        val covers = eventMediaRepository.findByEventIdInOrderBySortOrderAsc(ids)
            .filter { it.isCover }
            .associate { it.eventId to it.storageUrl }

        return events.map { event ->
            val id = event.eventId!!
            val types = (ticketTypes[id] ?: emptyList()).sortedBy { it.price }
            val sold = types.sumOf { it.quantitySold }
            OrganizerEventDto(
                id = id,
                code = event.eventCode,
                title = event.title,
                slug = event.slug,
                coverUrl = covers[id],
                reviewStatus = event.reviewStatus.name,
                lifecycleStatus = event.lifecycleStatus.name,
                startAt = event.startAt,
                endAt = event.endAt,
                timezone = event.timezone,
                city = event.city,
                venueName = event.venueName,
                capacityLimit = event.capacityLimit,
                ticketTypes = types.map { type ->
                    OrganizerTicketTypeDto(
                        id = type.ticketTypeId!!,
                        name = type.ticketTypeName,
                        kind = type.ticketKind.name,
                        price = type.price,
                        currency = type.currency.trim(),
                        quantityTotal = type.quantityTotal,
                        quantitySold = type.quantitySold,
                        quantityReserved = type.quantityReserved,
                        remaining = type.remaining(),
                        status = type.status.name,
                        saleStartAt = type.saleStartAt,
                        saleEndAt = type.saleEndAt
                    )
                },
                ticketsSold = sold,
                revenue = orderRepository.sumTotalByEventAndStatus(id, OrderStatus.PAID) ?: BigDecimal.ZERO,
                currency = types.firstOrNull()?.currency?.trim() ?: "VND"
            )
        }
    }

    companion object {
        /** Trạng thái duyệt + trạng thái vòng đời đều dùng chung một tham số `status`. */
        private val FILTERS = buildSet {
            EventReviewStatus.entries.forEach { add(it.name) }
            com.eventapp.common.enums.EventLifecycleStatus.entries.forEach { add(it.name) }
        }
    }
}
