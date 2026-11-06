package com.eventapp.ticketing.ticket

import com.eventapp.common.enums.TicketStatus
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.EventBriefLoader
import com.eventapp.ticketing.TicketingCodes
import com.eventapp.ticketing.dto.TicketDto
import com.eventapp.ticketing.entity.Ticket
import com.eventapp.ticketing.repository.CheckInRepository
import com.eventapp.ticketing.repository.TicketRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * FR-U12 — ví vé của người dùng.
 *
 * Mã QR được sinh lại từ mã vé mỗi lần đọc (`TicketingCodes.qrTokenFor`), DB chỉ giữ bản băm nên
 * không có bản rõ nào bị lộ nếu mất database.
 */
@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val checkInRepository: CheckInRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val briefs: EventBriefLoader,
    private val codes: TicketingCodes
) {

    @Transactional(readOnly = true)
    fun list(userId: Long, status: String?, page: Int, size: Int): Pair<List<TicketDto>, Long> {
        val pageable = PageRequest.of(page, size)
        val normalized = status?.trim()?.uppercase()?.takeIf { it.isNotEmpty() && it != "ALL" }
        val result = if (normalized == null) {
            ticketRepository.findByUserIdOrderByIssuedAtDesc(userId, pageable)
        } else {
            val parsed = runCatching { TicketStatus.valueOf(normalized) }.getOrNull()
                ?: throw validationError("status", "Trạng thái vé không hợp lệ")
            ticketRepository.findByUserIdAndStatusOrderByIssuedAtDesc(userId, parsed, pageable)
        }
        return toDtos(result.content) to result.totalElements
    }

    @Transactional(readOnly = true)
    fun get(userId: Long, ticketId: Long): TicketDto {
        val ticket = ticketRepository.findById(ticketId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy vé")
        if (ticket.userId != userId) throw ForbiddenException(ErrorCode.FORBIDDEN, "Vé không thuộc về bạn")
        return toDtos(listOf(ticket)).first()
    }

    fun toDtos(tickets: List<Ticket>): List<TicketDto> {
        if (tickets.isEmpty()) return emptyList()
        val briefsById = briefs.load(tickets.map { it.eventId }.distinct())
        val typeNames = ticketTypeRepository.findByEventIdIn(tickets.map { it.eventId }.distinct())
            .associate { it.ticketTypeId!! to it.ticketTypeName }
        val checkIns = checkInRepository.findByTicketIdIn(tickets.mapNotNull { it.ticketId })
            .associate { it.ticketId to it.checkedInAt }

        return tickets.map { ticket ->
            val brief = briefsById[ticket.eventId]
            TicketDto(
                ticketId = ticket.ticketId!!,
                ticketCode = ticket.ticketCode,
                status = ticket.status.name,
                eventId = ticket.eventId,
                eventTitle = brief?.title ?: "",
                eventStartAt = brief?.startAt,
                eventEndAt = brief?.endAt,
                venueName = brief?.venueName,
                city = brief?.city,
                coverUrl = brief?.coverUrl,
                ticketTypeName = typeNames[ticket.ticketTypeId] ?: "",
                issuedAt = ticket.issuedAt,
                checkedInAt = checkIns[ticket.ticketId!!],
                qrToken = codes.qrTokenFor(ticket.ticketCode)
            )
        }
    }
}
