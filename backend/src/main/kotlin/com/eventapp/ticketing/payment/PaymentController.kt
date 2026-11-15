package com.eventapp.ticketing.payment

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.CreatePaymentRequest
import com.eventapp.ticketing.dto.PaymentDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
// [MỚI] Bổ sung 2 import bị thiếu gây lỗi biên dịch SSE
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Thanh toán", description = "FR-U11 — Thanh toán chuyển khoản QR, xác nhận và SSE real-time")
@RestController
@RequestMapping("/api/v1")
class PaymentController(
    private val paymentService: PaymentService,
    private val paymentSseHub: PaymentSseHub
) {

    @Operation(summary = "Khởi tạo giao dịch thanh toán cho đơn hàng")
    @PostMapping("/orders/{orderId}/payments")
    fun initiate(
        @PathVariable orderId: Long,
        @RequestBody(required = false) request: CreatePaymentRequest?
    ): ApiResponse<PaymentDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(paymentService.initiate(userId, orderId, request ?: CreatePaymentRequest()))
    }

    @Operation(summary = "Lấy trạng thái giao dịch thanh toán")
    @GetMapping("/payments/{paymentId}")
    fun get(@PathVariable paymentId: Long): ApiResponse<PaymentDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(paymentService.get(userId, paymentId))
    }

    @Operation(summary = "Lắng nghe SSE trạng thái giao dịch thanh toán theo thời gian thực")
    @GetMapping("/payments/{paymentId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamPaymentEvents(@PathVariable paymentId: Long): SseEmitter {
        val userId = CurrentUser.requireUserId()
        val current = paymentService.get(userId, paymentId)
        return paymentSseHub.subscribePayment(paymentId, current)
    }

    @Operation(summary = "Xác nhận thanh toán (dùng cho thẻ quốc tế hoặc mô phỏng dev)")
    @PostMapping("/payments/{paymentId}/confirm")
    fun confirm(@PathVariable paymentId: Long): ApiResponse<PaymentDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(paymentService.confirm(userId, paymentId))
    }
}