package com.eventapp.ticketing.order

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.CreateOrderRequest
import com.eventapp.ticketing.dto.OrderDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Đơn hàng & Mua vé", description = "FR-U10, FR-U16 — Quản lý đặt vé và đơn hàng")
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService
) {

    @Operation(summary = "FR-U10: Tạo đơn hàng đặt vé mới")
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateOrderRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?
    ): ApiResponse<OrderDto> {
        // Lấy userId an toàn từ JWT Token của người dùng đang đăng nhập
        val userId = CurrentUser.requireUserId()
        val order = orderService.create(userId, request)
        return ApiResponse.ok(order)
    }

    @Operation(summary = "Xem chi tiết một đơn hàng theo orderId")
    @GetMapping("/{orderId}")
    fun get(@PathVariable orderId: Long): ApiResponse<OrderDto> {
        val userId = CurrentUser.requireUserId()
        val order = orderService.get(userId, orderId)
        return ApiResponse.ok(order)
    }

    @Operation(summary = "FR-U16: Lịch sử đơn hàng của tôi (phân trang)")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<List<OrderDto>> {
        val userId = CurrentUser.requireUserId()
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)

        val (orders, total) = orderService.list(userId, status, safePage, safeSize)
        return ApiResponse.ok(orders, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "Huỷ đơn hàng chưa thanh toán")
    @PostMapping("/{orderId}/cancel")
    fun cancel(@PathVariable orderId: Long): ApiResponse<OrderDto> {
        val userId = CurrentUser.requireUserId()
        val cancelledOrder = orderService.cancel(userId, orderId)
        return ApiResponse.ok(cancelledOrder)
    }
}