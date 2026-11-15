package com.eventapp.ticketing.tickettype

import com.eventapp.common.enums.TicketKind
import com.eventapp.common.enums.TicketTypeStatus
import com.eventapp.common.exception.ConflictException
import com.eventapp.common.exception.ErrorCode
import com.eventapp.common.exception.ForbiddenException
import com.eventapp.common.exception.NotFoundException
import com.eventapp.common.exception.validationError
import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.event.entity.TicketType
import com.eventapp.event.repository.EventRepository
import com.eventapp.event.repository.TicketTypeRepository
import com.eventapp.ticketing.TicketingRules
import com.eventapp.ticketing.dto.CreateTicketTypeRequest
import com.eventapp.ticketing.dto.TicketTypeDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@Tag(name = "Quản lý loại vé", description = "FR-O10, FR-O11, FR-O12 — Quản lý loại vé cho Organizer")
@RestController
@RequestMapping("/api/v1")
class TicketTypeController(
    private val ticketTypeRepository: TicketTypeRepository,
    private val eventRepository: EventRepository
) {

    @Operation(summary = "Lấy danh sách loại vé của một sự kiện (sắp xếp theo giá)")
    @GetMapping("/events/{eventId}/ticket-types")
    @Transactional(readOnly = true)
    fun listByEvent(@PathVariable eventId: Long): ApiResponse<List<TicketTypeDto>> {
        val types = ticketTypeRepository.findByEventIdOrderByPriceAsc(eventId)
        return ApiResponse.ok(types.map { toDto(it) })
    }

    @Operation(summary = "FR-O10 & FR-O11: Organizer tạo loại vé mới cho sự kiện")
    @PostMapping("/organizer/events/{eventId}/ticket-types")
    @Transactional
    fun createTicketType(
        @PathVariable eventId: Long,
        @RequestBody request: CreateTicketTypeRequest
    ): ApiResponse<TicketTypeDto> {
        val userId = CurrentUser.requireUserId()

        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.createdByUserId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không phải chủ sở hữu sự kiện này")
        }

        if (request.ticketTypeName.isBlank()) {
            throw ConflictException(ErrorCode.CONFLICT, "Tên loại vé không được để trống")
        }
        if (request.price < BigDecimal.ZERO) {
            throw ConflictException(ErrorCode.CONFLICT, "Giá vé không được nhỏ hơn 0")
        }
        if (request.quantityTotal != null && request.quantityTotal <= 0) {
            throw ConflictException(ErrorCode.CONFLICT, "Tổng số lượng vé phát hành phải lớn hơn 0")
        }
        if (request.saleStartAt != null && request.saleEndAt != null && !request.saleEndAt.isAfter(request.saleStartAt)) {
            throw validationError("saleEndAt", "Thời gian kết thúc bán phải sau thời gian mở bán")
        }

        // 🌟 KIỂM TRA CHỐNG VƯỢT QUÁ SỨC CHỨA SỰ KIỆN (FR-O11)
        val capacity = event.capacityLimit
        if (capacity != null && capacity > 0 && request.quantityTotal != null) {
            val existingTickets = ticketTypeRepository.findByEventIdOrderByPriceAsc(eventId)
                .mapNotNull { it.quantityTotal }
                .sum()

            if (existingTickets + request.quantityTotal > capacity) {
                val remainingSlots = (capacity - existingTickets).coerceAtLeast(0)
                throw ConflictException(
                    ErrorCode.CONFLICT,
                    "Vượt quá sức chứa sự kiện ($capacity vé)! Hiện đã phát hành $existingTickets vé, chỉ có thể tạo thêm tối đa $remainingSlots vé."
                )
            }
        }

        // Tự động phân loại: giá = 0 là FREE, giá > 0 là PAID theo đúng enum TicketKind của nhóm
        val kind = if (request.price.compareTo(BigDecimal.ZERO) == 0) TicketKind.FREE else TicketKind.PAID

        val ticketType = TicketType(
            eventId = eventId,
            ticketTypeName = request.ticketTypeName.trim(),
            description = request.description?.trim(),
            ticketKind = kind,
            price = request.price,
            currency = request.currency.trim().ifBlank { "VND" },
            quantityTotal = request.quantityTotal,
            quantityReserved = 0,
            quantitySold = 0,
            minPerOrder = request.minPerOrder,
            maxPerUser = request.maxPerUser,
            saleStartAt = request.saleStartAt,
            saleEndAt = request.saleEndAt,
            status = TicketTypeStatus.ACTIVE
        )
        val saved = ticketTypeRepository.save(ticketType)
        return ApiResponse.ok(toDto(saved))
    }

    @Operation(summary = "Organizer cập nhật thông tin loại vé")
    @PutMapping("/organizer/ticket-types/{ticketTypeId}")
    @Transactional
    fun updateTicketType(
        @PathVariable ticketTypeId: Long,
        @RequestBody request: CreateTicketTypeRequest
    ): ApiResponse<TicketTypeDto> {
        val userId = CurrentUser.requireUserId()
        val ticketType = ticketTypeRepository.findById(ticketTypeId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy loại vé")

        val event = eventRepository.findById(ticketType.eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.createdByUserId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không phải chủ sở hữu sự kiện này")
        }

        if (request.ticketTypeName.isBlank()) {
            throw ConflictException(ErrorCode.CONFLICT, "Tên loại vé không được để trống")
        }
        if (request.price < BigDecimal.ZERO) {
            throw ConflictException(ErrorCode.CONFLICT, "Giá vé không được nhỏ hơn 0")
        }
        if (request.quantityTotal != null && request.quantityTotal <= 0) {
            throw ConflictException(ErrorCode.CONFLICT, "Tổng số lượng vé phát hành phải lớn hơn 0")
        }
        if (request.saleStartAt != null && request.saleEndAt != null && !request.saleEndAt.isAfter(request.saleStartAt)) {
            throw validationError("saleEndAt", "Thời gian kết thúc bán phải sau thời gian mở bán")
        }

        // Ràng buộc 1: Chống giảm tổng vé thấp hơn số vé đã bán + đang giữ chỗ (FR-O11)
        val committed = ticketType.quantitySold + ticketType.quantityReserved
        if (request.quantityTotal != null && request.quantityTotal < committed) {
            throw ConflictException(
                ErrorCode.CONFLICT,
                "Không thể giảm tổng số vé (${request.quantityTotal}) thấp hơn số vé đã bán/đang giữ chỗ ($committed)"
            )
        }

        // 🌟 Ràng buộc 2: Chống cập nhật tổng vé vượt quá sức chứa sự kiện (FR-O11)
        val capacity = event.capacityLimit
        if (capacity != null && capacity > 0 && request.quantityTotal != null) {
            // Lấy tổng vé của các hạng vé khác (không tính hạng vé đang sửa)
            val otherTickets = ticketTypeRepository.findByEventIdOrderByPriceAsc(ticketType.eventId)
                .filter { it.ticketTypeId != ticketTypeId }
                .mapNotNull { it.quantityTotal }
                .sum()

            if (otherTickets + request.quantityTotal > capacity) {
                val remainingSlots = (capacity - otherTickets).coerceAtLeast(0)
                throw ConflictException(
                    ErrorCode.CONFLICT,
                    "Vượt quá sức chứa sự kiện ($capacity vé)! Các hạng vé khác đã chiếm $otherTickets vé, chỉ có thể đặt tối đa $remainingSlots vé cho hạng vé này."
                )
            }
        }

        val kind = if (request.price.compareTo(BigDecimal.ZERO) == 0) TicketKind.FREE else TicketKind.PAID

        ticketType.ticketTypeName = request.ticketTypeName.trim()
        ticketType.description = request.description?.trim()
        ticketType.ticketKind = kind
        ticketType.price = request.price
        ticketType.quantityTotal = request.quantityTotal
        ticketType.minPerOrder = request.minPerOrder
        ticketType.maxPerUser = request.maxPerUser
        ticketType.saleStartAt = request.saleStartAt
        ticketType.saleEndAt = request.saleEndAt

        val updated = ticketTypeRepository.save(ticketType)
        return ApiResponse.ok(toDto(updated))
    }

    @Operation(summary = "Organizer xóa loại vé (chỉ xóa được khi chưa bán vé nào)")
    @DeleteMapping("/organizer/ticket-types/{ticketTypeId}")
    @Transactional
    fun deleteTicketType(@PathVariable ticketTypeId: Long): ApiResponse<Boolean> {
        val userId = CurrentUser.requireUserId()
        val ticketType = ticketTypeRepository.findById(ticketTypeId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy loại vé")

        val event = eventRepository.findById(ticketType.eventId).orElse(null)
            ?: throw NotFoundException(message = "Không tìm thấy sự kiện")
        if (event.createdByUserId != userId) {
            throw ForbiddenException(ErrorCode.FORBIDDEN, "Bạn không phải chủ sở hữu sự kiện này")
        }

        if (ticketType.quantitySold > 0 || ticketType.quantityReserved > 0) {
            throw ConflictException(ErrorCode.CONFLICT, "Loại vé này đã có người mua hoặc đang giữ chỗ, không thể xóa!")
        }

        ticketTypeRepository.delete(ticketType)
        return ApiResponse.ok(true)
    }

    private fun toDto(type: TicketType): TicketTypeDto {
        return TicketTypeDto(
            ticketTypeId = type.ticketTypeId!!,
            eventId = type.eventId,
            ticketTypeName = type.ticketTypeName,
            description = type.description,
            ticketKind = type.ticketKind.name,
            price = type.price,
            currency = type.currency,
            quantityTotal = type.quantityTotal,
            quantityReserved = type.quantityReserved,
            quantitySold = type.quantitySold,
            remainingQuantity = type.remaining(),
            minPerOrder = type.minPerOrder,
            maxPerUser = type.maxPerUser,
            saleStartAt = type.saleStartAt,
            saleEndAt = type.saleEndAt,
            status = TicketingRules.saleStatus(type)
        )
    }
}