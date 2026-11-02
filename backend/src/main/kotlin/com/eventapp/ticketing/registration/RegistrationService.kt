package com.eventapp.ticketing.registration

import com.eventapp.common.enums.RegistrationStatus
import com.eventapp.common.enums.TicketKind
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.EventBriefLoader
import com.eventapp.ticketing.TicketIssuer
import com.eventapp.ticketing.TicketingCodes
import com.eventapp.ticketing.TicketingRules
import com.eventapp.ticketing.dto.CreateRegistrationRequest
import com.eventapp.ticketing.dto.RegistrationDto
import com.eventapp.ticketing.entity.Registration
import com.eventapp.ticketing.repository.RegistrationRepository
import com.eventapp.ticketing.repository.TicketRepository
import com.eventapp.ticketing.ticket.TicketService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * FR-U09 — đăng ký vé miễn phí.
 *
 * Vé miễn phí không đi qua bước thanh toán nên đăng ký thành công là phát hành vé ngay trong cùng
 * transaction; tồn kho vẫn được chốt như đơn có phí để không bán vượt số chỗ.
 */
@Service
class RegistrationService(
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository,
    private val ticketTypeRepository: TicketTypeRepository,
    private val ticketRepository: TicketRepository,
    private val issuer: TicketIssuer,
    private val ticketService: TicketService,
    private val briefs: EventBriefLoader,
    private val codes: TicketingCodes
) {

    @Transactional
    fun create(userId: Long, eventId: Long, request: CreateRegistrationRequest): RegistrationDto {
        val now = Instant.now()
        val event = TicketingRules.requireOrderable(eventRepository.findById(eventId).orElse(null), now)

        val key = request.idempotencyKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            registrationRepository.findFirstByUserIdAndIdempotencyKey(userId, key)
                ?.let { return toDto(it) }
        }

        val quantity = request.quantity
        if (quantity <= 0) throw validationError("quantity", "Số lượng vé phải lớn hơn 0")

        val type = ticketTypeRepository.findByTicketTypeIdIn(listOf(request.ticketTypeId)).firstOrNull()
            ?: throw validationError("ticketTypeId", "Loại vé không tồn tại")
        if (type.eventId != event.eventId) {
            throw validationError("ticketTypeId", "Loại vé không thuộc sự kiện này")
        }
        if (type.ticketKind != TicketKind.FREE) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Loại vé \"${type.ticketTypeName}\" có thu phí, vui lòng đặt vé và thanh toán"
            )
        }
        TicketingRules.requireOnSale(type, now)
        if (quantity < type.minPerOrder) {
            throw validationError("quantity", "Loại vé này cần ít nhất ${type.minPerOrder} vé")
        }
        if (quantity > type.maxPerUser) {
            throw validationError("quantity", "Loại vé này tối đa ${type.maxPerUser} vé mỗi người")
        }
        val remaining = type.remaining()
        if (remaining != null && remaining < quantity) {
            throw ConflictException(ErrorCode.TICKET_SOLD_OUT, "Chỉ còn ${remaining.coerceAtLeast(0)} vé")
        }

        val existing = registrationRepository.findByUserIdAndEventId(userId, eventId)
        if (existing != null && existing.status == RegistrationStatus.CONFIRMED) {
            throw ConflictException(ErrorCode.CONFLICT, "Bạn đã đăng ký sự kiện này")
        }

        type.quantityReserved += quantity
        val registration = registrationRepository.save(
            Registration(
                registrationCode = codes.newRegistrationCode(now),
                userId = userId,
                eventId = eventId,
                ticketTypeId = type.ticketTypeId!!,
                quantity = quantity.toShort(),
                status = RegistrationStatus.CONFIRMED,
                idempotencyKey = key ?: codes.newRegistrationCode(now)
            )
        )
        // Đăng ký thành công là chốt luôn tồn kho, không cần bước thanh toán.
        type.quantityReserved = (type.quantityReserved - quantity).coerceAtLeast(0)
        type.quantitySold += quantity
        issuer.issue(
            userId = userId,
            eventId = eventId,
            ticketTypeId = type.ticketTypeId!!,
            quantity = quantity,
            registrationId = registration.registrationId
        )
        return toDto(registration)
    }

    @Transactional(readOnly = true)
    fun get(userId: Long, registrationId: Long): RegistrationDto {
        val registration = registrationRepository.findById(registrationId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy đăng ký")
        if (registration.userId != userId) {
            throw NotFoundException(message = "Không tìm thấy đăng ký")
        }
        return toDto(registration)
    }

    private fun toDto(registration: Registration): RegistrationDto {
        val brief = briefs.load(registration.eventId)
        val tickets = ticketRepository.findByRegistrationIdIn(listOf(registration.registrationId!!))
        return RegistrationDto(
            registrationId = registration.registrationId!!,
            registrationCode = registration.registrationCode,
            eventId = registration.eventId,
            eventTitle = brief?.title ?: "",
            status = registration.status.name,
            quantity = registration.quantity.toInt(),
            registeredAt = registration.registeredAt,
            tickets = ticketService.toDtos(tickets)
        )
    }
}
