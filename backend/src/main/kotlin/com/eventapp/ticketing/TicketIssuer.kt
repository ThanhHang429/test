package com.eventapp.ticketing

import com.eventapp.common.enums.TicketStatus
import com.eventapp.ticketing.entity.Ticket
import com.eventapp.ticketing.repository.TicketRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Phát hành vé cho đơn đã thanh toán hoặc đăng ký vé miễn phí.
 *
 * Vé chỉ được phát hành sau khi tiền đã xác nhận (BR-017) — nơi gọi phải đảm bảo điều đó.
 */
@Component
class TicketIssuer(
    private val ticketRepository: TicketRepository,
    private val codes: TicketingCodes
) {

    fun issue(
        userId: Long,
        eventId: Long,
        ticketTypeId: Long,
        quantity: Int,
        registrationId: Long? = null,
        orderItemId: Long? = null
    ): List<Ticket> {
        if (quantity <= 0) return emptyList()
        val now = Instant.now()
        val tickets = (1..quantity).map {
            val code = codes.newTicketCode(now)
            Ticket(
                ticketCode = code,
                qrTokenHash = codes.hashToken(codes.qrTokenFor(code)),
                userId = userId,
                eventId = eventId,
                ticketTypeId = ticketTypeId,
                registrationId = registrationId,
                orderItemId = orderItemId,
                status = TicketStatus.VALID
            )
        }
        return ticketRepository.saveAll(tickets)
    }
}
