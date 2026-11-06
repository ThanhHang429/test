package com.eventapp.organizer.dashboard

import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.enums.TicketTypeStatus
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.identity.repository.OrganizerProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * FR-O03 — Dashboard Organizer.
 *
 * Nguyên tắc:
 *  - O03-R01: chỉ số liệu của Organizer đang đăng nhập, không đọc organizerId từ request.
 *  - O03-R02/BR-022: đơn chờ thanh toán không bao giờ cộng vào doanh thu.
 *  - O03-R04: phân biệt trạng thái bằng cả `review_status` và `lifecycle_status`.
 *  - O03-R05: mỗi khối độc lập; một khối lỗi không làm hỏng cả endpoint (ở đây các khối
 *    đọc cùng DB nên lỗi sẽ được bắt riêng và trả status = ERROR cho khối đó).
 *  - O03-R03: mọi tổng số tiền do backend trả, client không tự cộng.
 */
@Service
class OrganizerDashboardService(
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val eventRepository: EventRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val dashboardRepository: OrganizerDashboardRepository
) {

    @Transactional(readOnly = true)
    fun dashboard(userId: Long, range: String, fromRaw: String?, toRaw: String?): OrganizerDashboardDto {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        // PENDING/REJECTED/MORE_INFO không được dùng dashboard (SRS điều kiện trước).
        if (profile.status != OrganizerStatus.APPROVED && profile.status != OrganizerStatus.SUSPENDED) {
            throw ForbiddenException(
                ErrorCode.FORBIDDEN,
                "Hồ sơ Organizer đang ở trạng thái ${profile.status.name}, chưa dùng được dashboard"
            )
        }

        val (from, to, normalizedRange) = resolveRange(range, fromRaw, toRaw)
        val now = Instant.now()
        val events = eventRepository.findByOrganizerProfileIdOrderByStartAtDesc(profile.organizerProfileId!!)
        val eventIds = events.mapNotNull { it.eventId }

        val eventBlock = buildEventBlock(events, now)

        val registrationStats = runCatching { dashboardRepository.registrations(eventIds, from, to) }.getOrNull()
        val financeStats = runCatching { dashboardRepository.finance(eventIds, from, to) }.getOrNull()
        val ticketStats = runCatching { dashboardRepository.tickets(eventIds) }.getOrNull()

        val registrationBlock = runCatching {
            val stats = registrationStats ?: error("registrations unavailable")
            val tickets = ticketStats ?: error("tickets unavailable")
            RegistrationBlockDto(
                confirmed = stats.confirmed,
                cancelled = stats.cancelled,
                waitlisted = stats.waitlisted,
                ticketsIssued = tickets.issued,
                ticketsCancelled = tickets.cancelled,
                meta = BlockMetaDto("OK", now)
            )
        }.getOrElse {
            RegistrationBlockDto(0, 0, 0, 0, 0, BlockMetaDto("ERROR", now, "Không tải được số liệu đăng ký"))
        }

        val financeBlock = runCatching {
            val stats = financeStats ?: error("finance unavailable")
            val net = stats.verifiedAmount - stats.refundedAmount - stats.feeAmount
            FinanceBlockDto(
                verifiedAmount = stats.verifiedAmount.setScale(2, RoundingMode.HALF_UP),
                pendingAmount = stats.pendingAmount.setScale(2, RoundingMode.HALF_UP),
                refundedAmount = stats.refundedAmount.setScale(2, RoundingMode.HALF_UP),
                feeAmount = stats.feeAmount.setScale(2, RoundingMode.HALF_UP),
                netAmount = net.setScale(2, RoundingMode.HALF_UP),
                currency = CURRENCY,
                meta = BlockMetaDto("OK", now)
            )
        }.getOrElse {
            FinanceBlockDto(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, CURRENCY,
                BlockMetaDto("ERROR", now, "Không tải được số liệu tài chính, các khối khác vẫn hiển thị")
            )
        }

        val checkinBlock = runCatching {
            val tickets = ticketStats ?: error("tickets unavailable")
            val checkedIn = dashboardRepository.checkInCount(eventIds)
            CheckinBlockDto(
                checkedIn = checkedIn,
                ticketsIssued = tickets.issued,
                checkinRate = if (tickets.issued > 0) round4(checkedIn.toDouble() / tickets.issued) else 0.0,
                meta = BlockMetaDto("OK", now)
            )
        }.getOrElse {
            CheckinBlockDto(0, 0, 0.0, BlockMetaDto("ERROR", now, "Không tải được số liệu check-in"))
        }

        val reviewBlock = runCatching {
            val (avg, newCount) = dashboardRepository.reviews(eventIds, from, to)
            ReviewBlockDto(
                averageRating = round4(avg),
                totalVisible = dashboardRepository.countVisibleReviews(eventIds),
                newInRange = newCount,
                meta = BlockMetaDto("OK", now)
            )
        }.getOrElse {
            ReviewBlockDto(0.0, 0, 0, BlockMetaDto("ERROR", now, "Không tải được số liệu đánh giá"))
        }

        val verifiedByEvent = financeStats?.byEventVerified ?: emptyMap()
        val registrationsByEvent = registrationStats?.byEvent ?: emptyMap()
        val topEvents = events
            .map { event ->
                TopEventDto(
                    eventId = event.eventId!!,
                    title = event.title,
                    verifiedAmount = (verifiedByEvent[event.eventId] ?: BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP),
                    registrations = registrationsByEvent[event.eventId] ?: 0L
                )
            }
            .sortedWith(compareByDescending<TopEventDto> { it.verifiedAmount }.thenByDescending { it.registrations })
            .take(5)

        return OrganizerDashboardDto(
            range = normalizedRange,
            from = from,
            to = to,
            events = eventBlock,
            registrations = registrationBlock,
            finance = financeBlock,
            checkin = checkinBlock,
            reviews = reviewBlock,
            tasks = buildTasks(events, now),
            topEvents = topEvents,
            asOf = now
        )
    }

    // ------------------------------------------------------------------
    // Khối tình trạng sự kiện
    // ------------------------------------------------------------------

    private fun buildEventBlock(events: List<Event>, now: Instant): EventStatusBlockDto {
        val counts = linkedMapOf(
            "DRAFT" to 0L,
            "PENDING_REVIEW" to 0L,
            "REVISION_REQUIRED" to 0L,
            "REJECTED" to 0L,
            "APPROVED_UNPUBLISHED" to 0L,
            "ON_SALE" to 0L,
            "UPCOMING" to 0L,
            "COMPLETED" to 0L,
            "SUSPENDED" to 0L,
            "CANCELLED" to 0L
        )

        events.forEach { event ->
            val key = statusKey(event, now)
            counts[key] = (counts[key] ?: 0L) + 1
        }
        return EventStatusBlockDto(counts, events.size.toLong(), BlockMetaDto("OK", now))
    }

    private fun statusKey(event: Event, now: Instant): String = when {
        event.lifecycleStatus == EventLifecycleStatus.SUSPENDED -> "SUSPENDED"
        event.lifecycleStatus == EventLifecycleStatus.CANCELLED -> "CANCELLED"
        event.lifecycleStatus == EventLifecycleStatus.COMPLETED -> "COMPLETED"
        event.reviewStatus == EventReviewStatus.DRAFT -> "DRAFT"
        event.reviewStatus == EventReviewStatus.PENDING_REVIEW -> "PENDING_REVIEW"
        event.reviewStatus == EventReviewStatus.REVISION_REQUIRED -> "REVISION_REQUIRED"
        event.reviewStatus == EventReviewStatus.REJECTED -> "REJECTED"
        event.reviewStatus == EventReviewStatus.APPROVED && event.lifecycleStatus == EventLifecycleStatus.UNPUBLISHED ->
            "APPROVED_UNPUBLISHED"
        event.reviewStatus == EventReviewStatus.APPROVED && event.startAt.isBefore(now) -> "ON_SALE"
        else -> "UPCOMING"
    }

    // ------------------------------------------------------------------
    // Việc cần xử lý (O03-R07: sắp theo mức khẩn)
    // ------------------------------------------------------------------

    private fun buildTasks(events: List<Event>, now: Instant): List<DashboardTaskDto> {
        val tasks = mutableListOf<DashboardTaskDto>()

        events.filter { it.lifecycleStatus == EventLifecycleStatus.PUBLISHED }
            .filter { it.startAt.isAfter(now) && it.startAt.isBefore(now.plus(Duration.ofHours(48))) }
            .forEach { event ->
                tasks += DashboardTaskDto(
                    type = "EVENT_UPCOMING_48H",
                    eventId = event.eventId!!,
                    title = event.title,
                    detail = "Sự kiện sẽ diễn ra trong 48 giờ tới",
                    dueAt = event.startAt,
                    priority = 1
                )
            }

        events.filter { it.reviewStatus == EventReviewStatus.REVISION_REQUIRED }
            .forEach { event ->
                tasks += DashboardTaskDto(
                    type = "EVENT_REVISION_REQUIRED",
                    eventId = event.eventId!!,
                    title = event.title,
                    detail = "Sự kiện cần chỉnh sửa theo yêu cầu duyệt",
                    dueAt = null,
                    priority = 2
                )
            }

        events.filter { it.reviewStatus == EventReviewStatus.REJECTED }
            .forEach { event ->
                tasks += DashboardTaskDto(
                    type = "EVENT_REJECTED",
                    eventId = event.eventId!!,
                    title = event.title,
                    detail = "Sự kiện bị từ chối duyệt",
                    dueAt = null,
                    priority = 3
                )
            }

        events.filter { it.lifecycleStatus == EventLifecycleStatus.PUBLISHED && it.endAt.isAfter(now) }
            .forEach { event ->
                ticketTypeRepository.findByEventIdOrderByPriceAsc(event.eventId!!)
                    .filter { it.status == TicketTypeStatus.ACTIVE }
                    .filter { it.saleEndAt != null && it.saleEndAt!!.isAfter(now) && it.saleEndAt!!.isBefore(now.plus(Duration.ofHours(48))) }
                    .forEach { type ->
                        tasks += DashboardTaskDto(
                            type = "TICKET_TYPE_ENDING_SOON",
                            eventId = event.eventId!!,
                            title = event.title,
                            detail = "Loại vé \"${type.ticketTypeName}\" sắp kết thúc bán",
                            dueAt = type.saleEndAt,
                            priority = 4
                        )
                    }
            }

        return tasks.sortedWith(
            compareBy<DashboardTaskDto> { it.priority }.thenBy { it.dueAt ?: Instant.MAX }
        ).take(20)
    }

    // ------------------------------------------------------------------
    // Khoảng thời gian
    // ------------------------------------------------------------------

    private data class Range(val from: Instant, val to: Instant, val label: String)

    private fun resolveRange(range: String, fromRaw: String?, toRaw: String?): Range {
        val now = Instant.now()
        return when (range.lowercase()) {
            "7d", "7" -> Range(now.minus(Duration.ofDays(7)), now, "7d")
            "30d", "30", "" -> Range(now.minus(Duration.ofDays(30)), now, "30d")
            "custom" -> {
                if (fromRaw.isNullOrBlank() || toRaw.isNullOrBlank()) {
                    throw validationError("from", "range=custom cần truyền from và to (yyyy-MM-dd)")
                }
                val from = parseDate(fromRaw, "from")
                val to = parseDate(toRaw, "to")
                if (to.isBefore(from)) throw validationError("to", "Ngày kết thúc phải sau ngày bắt đầu")
                if (Duration.between(from, to).toDays() > MAX_RANGE_DAYS) {
                    throw validationError("to", "Khoảng thời gian tối đa $MAX_RANGE_DAYS ngày")
                }
                Range(from, to, "custom")
            }
            else -> throw validationError("range", "range phải là 7d | 30d | custom")
        }
    }

    private fun parseDate(raw: String, field: String): Instant = try {
        LocalDate.parse(raw.trim()).atStartOfDay(ZoneOffset.UTC).toInstant()
    } catch (_: Exception) {
        throw validationError(field, "Định dạng ngày phải là yyyy-MM-dd")
    }

    private fun round4(value: Double): Double =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toDouble()

    companion object {
        private const val MAX_RANGE_DAYS = 366L
        private const val CURRENCY = "VND"
    }
}
