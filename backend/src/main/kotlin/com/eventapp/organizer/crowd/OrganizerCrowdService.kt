package com.eventapp.organizer.crowd

import com.eventapp.common.enums.OrganizerStatus
import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.entity.Event
import com.eventapp.event.repository.EventRepository
import com.eventapp.identity.entity.OrganizerProfile
import com.eventapp.identity.repository.OrganizerProfileRepository
import com.eventapp.ticketing.repository.RegistrationRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * FR-O28 — điều phối phòng khi quá tải.
 *
 * <p>Luồng nghiệp vụ: organizer theo dõi mức lấp đầy của phòng chính; khi vượt ngưỡng,
 * hệ thống cảnh báo và gợi ý phòng dự phòng đã khai báo; organizer soạn (AI hỗ trợ) rồi gửi
 * thông báo cho những người **đã đăng ký nhưng chưa check-in**.</p>
 *
 * <p>Ngưỡng tính trên `checkedIn / capacityLimit`:
 * 70% → `WATCH`, 90% → `CROWDED`, 100% → `FULL`.</p>
 */
@Service
class OrganizerCrowdService(
    private val organizerProfileRepository: OrganizerProfileRepository,
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val ticketRepository: TicketRepository,
    private val overflowRoomRepository: EventOverflowRoomRepository,
    private val notificationRepository: EventNotificationRepository,
    private val recipientRepository: EventNotificationRecipientRepository
) {

    // ------------------------------------------------------------------ cảnh báo

    @Transactional(readOnly = true)
    fun crowdStatus(userId: Long, eventId: Long): CrowdStatusDto {
        val event = requireOwnedEvent(userId, eventId)
        val capacity = event.capacityLimit ?: 0
        val registered = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)
        val checkedIn = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.CHECKED_IN)
        val notCheckedIn = (registered - checkedIn).coerceAtLeast(0)

        val occupancy = if (capacity > 0) checkedIn.toDouble() / capacity else 0.0
        val checkinRate = if (registered > 0) checkedIn.toDouble() / registered else 0.0
        val level = levelOf(capacity, occupancy)

        val rooms = overflowRoomRepository
            .findByEventIdAndStatusOrderByCreatedAtAsc(eventId, EventOverflowRoom.STATUS_ACTIVE)
            .map { it.toDto() }
        val suggested = rooms.maxByOrNull { it.capacity }

        return CrowdStatusDto(
            eventId = eventId,
            eventTitle = event.title,
            capacityLimit = capacity,
            registered = registered,
            checkedIn = checkedIn,
            notCheckedIn = notCheckedIn,
            occupancyRate = round4(occupancy),
            checkinRate = round4(checkinRate),
            level = level,
            warning = warningOf(level, checkedIn, capacity, occupancy),
            overflowRooms = rooms,
            suggestedRoom = suggested,
            suggestion = suggestionOf(level, suggested, notCheckedIn)
        )
    }

    private fun levelOf(capacity: Int, occupancy: Double): String = when {
        capacity <= 0 -> LEVEL_NORMAL
        occupancy >= 1.0 -> LEVEL_FULL
        occupancy >= 0.9 -> LEVEL_CROWDED
        occupancy >= 0.7 -> LEVEL_WATCH
        else -> LEVEL_NORMAL
    }

    private fun warningOf(level: String, checkedIn: Long, capacity: Int, occupancy: Double): String? =
        when (level) {
            LEVEL_FULL -> "Phòng chính đã đầy ($checkedIn/$capacity chỗ). Cần điều chuyển ngay."
            LEVEL_CROWDED -> "Phòng chính gần đầy (${percent(occupancy)}%). Nên mở phòng dự phòng."
            LEVEL_WATCH -> "Lượng check-in đang tăng nhanh (${percent(occupancy)}%)."
            else -> null
        }

    private fun suggestionOf(
        level: String,
        room: OverflowRoomDto?,
        notCheckedIn: Long
    ): String? {
        if (level == LEVEL_NORMAL) return null
        if (room == null) {
            return "Chưa khai báo phòng dự phòng. Thêm phòng dự phòng để có phương án điều chuyển."
        }
        return "Phòng dự phòng «${room.roomName}» còn ${room.capacity} chỗ. " +
            "Có $notCheckedIn người đã đăng ký nhưng chưa check-in — gửi thông báo để họ sang phòng này."
    }

    // ------------------------------------------------------------ phòng dự phòng

    @Transactional(readOnly = true)
    fun rooms(userId: Long, eventId: Long): List<OverflowRoomDto> {
        requireOwnedEvent(userId, eventId)
        return overflowRoomRepository.findByEventIdOrderByCreatedAtAsc(eventId).map { it.toDto() }
    }

    @Transactional
    fun addRoom(userId: Long, eventId: Long, request: CreateOverflowRoomRequest): OverflowRoomDto {
        requireOwnedEvent(userId, eventId)
        val name = request.roomName.trim()
        if (name.isEmpty()) throw validationError("roomName", "Tên phòng không được để trống")
        val room = EventOverflowRoom(
            eventId = eventId,
            roomName = name,
            capacity = request.capacity,
            note = request.note?.trim()?.takeIf { it.isNotEmpty() },
            status = EventOverflowRoom.STATUS_ACTIVE
        )
        return overflowRoomRepository.save(room).toDto()
    }

    /** Đóng phòng: giữ lại bản ghi để còn lịch sử điều phối. */
    @Transactional
    fun closeRoom(userId: Long, eventId: Long, roomId: Long): OverflowRoomDto {
        requireOwnedEvent(userId, eventId)
        val room = overflowRoomRepository.findById(roomId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy phòng dự phòng")
        if (room.eventId != eventId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Phòng dự phòng không thuộc sự kiện này")
        }
        room.status = EventOverflowRoom.STATUS_CLOSED
        return overflowRoomRepository.save(room).toDto()
    }

    // ----------------------------------------------------------------- thông báo

    @Transactional
    fun sendNotification(
        userId: Long,
        eventId: Long,
        request: SendNotificationRequest
    ): SendNotificationResponse {
        val event = requireOwnedEvent(userId, eventId)
        val audience = (request.audience ?: EventNotification.AUDIENCE_NOT_CHECKED_IN).uppercase()
        if (audience != EventNotification.AUDIENCE_NOT_CHECKED_IN &&
            audience != EventNotification.AUDIENCE_ALL_REGISTERED
        ) {
            throw validationError(
                "audience",
                "audience phải là REGISTERED_NOT_CHECKED_IN hoặc ALL_REGISTERED"
            )
        }
        val source = (request.source ?: EventNotification.SOURCE_MANUAL).uppercase()
        if (source != EventNotification.SOURCE_AI && source != EventNotification.SOURCE_MANUAL) {
            throw validationError("source", "source phải là AI hoặc MANUAL")
        }

        val recipients = resolveRecipients(eventId, audience)
        val saved = notificationRepository.save(
            EventNotification(
                eventId = eventId,
                createdByUserId = userId,
                title = request.title.trim(),
                body = request.body.trim(),
                audience = audience,
                source = source,
                recipientCount = recipients.size
            )
        )
        val notificationId = saved.notificationId!!
        recipients.forEach { recipientUserId ->
            recipientRepository.save(
                EventNotificationRecipient(notificationId = notificationId, userId = recipientUserId)
            )
        }
        return SendNotificationResponse(
            notification = saved.toDto(event.title),
            recipientCount = recipients.size
        )
    }

    @Transactional(readOnly = true)
    fun notifications(userId: Long, eventId: Long): List<NotificationDto> {
        val event = requireOwnedEvent(userId, eventId)
        return notificationRepository.findByEventIdOrderBySentAtDesc(eventId)
            .map { it.toDto(event.title) }
    }

    /** Thông báo gửi tới chính user đang đăng nhập (mọi sự kiện). */
    @Transactional(readOnly = true)
    fun myNotifications(userId: Long): List<NotificationDto> {
        val recipients = recipientRepository.findByUserIdOrderByDeliveredAtDesc(userId)
        if (recipients.isEmpty()) return emptyList()
        val notifications = notificationRepository.findAllById(recipients.map { it.notificationId })
            .associateBy { it.notificationId }
        val eventIds = notifications.values.map { it.eventId }.distinct()
        val titles = eventRepository.findAllById(eventIds).associate { it.eventId!! to it.title }
        return recipients.mapNotNull { notifications[it.notificationId] }
            .map { it.toDto(titles[it.eventId]) }
    }

    /** Người nhận: đăng ký CONFIRMED, trừ những user đã có vé CHECKED_IN. */
    private fun resolveRecipients(eventId: Long, audience: String): List<Long> {
        val registered = registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId)
            .filter { it.status == RegistrationStatus.CONFIRMED }
            .map { it.userId }
            .distinct()
        if (audience == EventNotification.AUDIENCE_ALL_REGISTERED) return registered
        val checkedIn = ticketRepository.findByEventIdAndStatus(eventId, TicketStatus.CHECKED_IN)
            .map { it.userId }
            .toSet()
        return registered.filter { it !in checkedIn }
    }

    // -------------------------------------------------------------------- helpers

    private fun requireApprovedOrganizer(userId: Long): OrganizerProfile {
        val profile = organizerProfileRepository.findByUserId(userId)
            ?: throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn chưa có hồ sơ Organizer")
        if (profile.status != OrganizerStatus.APPROVED) {
            throw ForbiddenException(
                ErrorCode.FORBIDDEN,
                "Hồ sơ Organizer đang ở trạng thái ${profile.status.name}, chưa dùng được tính năng điều phối"
            )
        }
        return profile
    }

    private fun requireOwnedEvent(userId: Long, eventId: Long): Event {
        val profile = requireApprovedOrganizer(userId)
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.deletedAt != null) throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.organizerProfileId != profile.organizerProfileId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Sự kiện không thuộc quyền quản lý của bạn")
        }
        return event
    }

    private fun percent(ratio: Double): Long = Math.round(ratio * 100)

    private fun round4(value: Double): Double =
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP).toDouble()

    private fun EventOverflowRoom.toDto() = OverflowRoomDto(
        id = overflowRoomId ?: 0L,
        roomName = roomName,
        capacity = capacity,
        note = note,
        status = status,
        createdAt = createdAt
    )

    private fun EventNotification.toDto(eventTitle: String?) = NotificationDto(
        id = notificationId ?: 0L,
        eventId = eventId,
        eventTitle = eventTitle,
        title = title,
        body = body,
        audience = audience,
        recipientCount = recipientCount,
        source = source,
        sentAt = sentAt
    )

    companion object {
        const val LEVEL_NORMAL = "NORMAL"
        const val LEVEL_WATCH = "WATCH"
        const val LEVEL_CROWDED = "CROWDED"
        const val LEVEL_FULL = "FULL"
    }
}
