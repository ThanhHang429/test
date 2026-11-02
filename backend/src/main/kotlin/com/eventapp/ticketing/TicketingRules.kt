package com.eventapp.ticketing

import com.eventapp.common.enums.EventLifecycleStatus
import com.eventapp.common.enums.EventReviewStatus
import com.eventapp.common.enums.TicketTypeStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.NotFoundException
import com.eventapp.event.entity.Event
import com.eventapp.event.entity.TicketType
import java.time.Instant

/**
 * Điều kiện mua/đăng ký dùng chung cho đơn hàng và đăng ký vé miễn phí.
 *
 * BR-017: mọi quyết định về việc có được đặt vé hay không đều nằm ở backend.
 */
object TicketingRules {

    /** Sự kiện phải đã duyệt, đã công bố, chưa xoá và chưa kết thúc. */
    fun requireOrderable(event: Event?, now: Instant = Instant.now()): Event {
        if (event == null || event.deletedAt != null) {
            throw NotFoundException(ErrorCode.EVENT_NOT_AVAILABLE)
        }
        if (event.reviewStatus != EventReviewStatus.APPROVED) {
            throw ConflictException(ErrorCode.EVENT_NOT_APPROVED)
        }
        if (event.lifecycleStatus != EventLifecycleStatus.PUBLISHED) {
            throw NotFoundException(ErrorCode.EVENT_NOT_AVAILABLE)
        }
        if (event.endAt.isBefore(now)) {
            throw ConflictException(ErrorCode.CONFLICT, "Sự kiện đã kết thúc")
        }
        return event
    }

    /** NOT_STARTED | ON_SALE | ENDED | SOLD_OUT — cùng cách tính với trang chi tiết sự kiện. */
    fun saleStatus(type: TicketType, now: Instant = Instant.now()): String {
        if (type.status != TicketTypeStatus.ACTIVE) return "ENDED"
        val remaining = type.remaining()
        if (remaining != null && remaining <= 0) return "SOLD_OUT"
        if (type.saleStartAt != null && now.isBefore(type.saleStartAt)) return "NOT_STARTED"
        if (type.saleEndAt != null && now.isAfter(type.saleEndAt)) return "ENDED"
        return "ON_SALE"
    }

    fun requireOnSale(type: TicketType, now: Instant = Instant.now()) {
        when (saleStatus(type, now)) {
            "ON_SALE" -> Unit
            "SOLD_OUT" -> throw ConflictException(ErrorCode.TICKET_SOLD_OUT, "Loại vé \"${type.ticketTypeName}\" đã hết")
            "NOT_STARTED" -> throw ConflictException(
                ErrorCode.CONFLICT, "Loại vé \"${type.ticketTypeName}\" chưa mở bán"
            )
            else -> throw ConflictException(
                ErrorCode.CONFLICT, "Loại vé \"${type.ticketTypeName}\" đã ngừng bán"
            )
        }
    }
}
