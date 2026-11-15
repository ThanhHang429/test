package com.eventapp.ticketing.order

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.OrderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Đơn hàng Organizer", description = "FR-O17 — Organizer quản lý đơn hàng của sự kiện")
@RestController
@RequestMapping("/api/v1/organizer/orders")
class OrganizerOrderController(
    private val orderService: OrderService
) {
    @Operation(summary = "Danh sách đơn hàng thuộc các sự kiện của Organizer")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) query: String?
    ): ApiResponse<List<OrderDto>> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(orderService.listForOrganizer(userId, status, query))
    }

    @Operation(summary = "Chi tiết đơn hàng theo mã đơn cho Organizer")
    @GetMapping("/{orderCode}")
    fun getByCode(@PathVariable orderCode: String): ApiResponse<OrderDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(orderService.getForOrganizerByCode(userId, orderCode))
    }
}