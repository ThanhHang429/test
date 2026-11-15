package com.eventapp.ticketing.refund

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.CreateRefundRequest
import com.eventapp.ticketing.dto.ProcessRefundRequest
import com.eventapp.ticketing.dto.RefundResponseDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Hoàn tiền", description = "FR-O25, FR-A12 — Yêu cầu & duyệt hoàn tiền tự động về Ví ETicket")
@RestController
@RequestMapping("/api/v1")
class RefundController(
    private val refundService: RefundService
) {
    @Operation(summary = "Tạo yêu cầu hoàn tiền cho đơn đã thanh toán")
    @PostMapping("/refunds")
    fun requestRefund(@RequestBody request: CreateRefundRequest): ApiResponse<RefundResponseDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(refundService.requestRefund(userId, request))
    }

    @Operation(summary = "Danh sách yêu cầu hoàn tiền (Admin xem tất cả, User xem của mình)")
    @GetMapping("/refunds")
    fun list(): ApiResponse<List<RefundResponseDto>> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(refundService.listForCaller(userId))
    }

    @Operation(summary = "Organizer xem danh sách yêu cầu hoàn tiền theo sự kiện")
    @GetMapping("/organizer/refunds")
    fun listForOrganizer(@RequestParam(required = false) eventId: Long?): ApiResponse<List<RefundResponseDto>> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(refundService.listForOrganizer(userId, eventId))
    }

    @Operation(summary = "Admin duyệt hoặc từ chối hoàn tiền (tự động hoàn về Ví ETicket khi duyệt)")
    @PostMapping("/refunds/{refundId}/process")
    fun process(
        @PathVariable refundId: Long,
        @RequestBody request: ProcessRefundRequest
    ): ApiResponse<RefundResponseDto> {
        val adminUserId = CurrentUser.requireUserId()
        return ApiResponse.ok(refundService.process(adminUserId, refundId, request))
    }
}