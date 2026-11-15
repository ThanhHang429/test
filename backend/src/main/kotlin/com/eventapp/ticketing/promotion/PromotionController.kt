package com.eventapp.ticketing.promotion

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Mã giảm giá", description = "FR-O18 — Tạo và áp dụng mã giảm giá")
@RestController
@RequestMapping("/api/v1")
class PromotionController(
    private val promotionService: PromotionService
) {
    @Operation(summary = "Kiểm tra và tính số tiền giảm khi áp dụng mã giảm giá")
    @PostMapping("/promotions/apply")
    fun apply(@RequestBody request: ApplyCouponRequest): ApiResponse<CouponResponseDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(promotionService.previewApply(userId, request))
    }

    @Operation(summary = "Organizer xem danh sách mã giảm giá theo sự kiện")
    @GetMapping("/organizer/events/{eventId}/promotions")
    fun listEventCoupons(@PathVariable eventId: Long): ApiResponse<List<OrganizerCouponDto>> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(promotionService.listEventCoupons(userId, eventId))
    }

    @Operation(summary = "Organizer tạo mã giảm giá mới cho sự kiện")
    @PostMapping("/organizer/events/{eventId}/promotions")
    fun createCoupon(
        @PathVariable eventId: Long,
        @RequestBody request: CreateCouponRequest
    ): ApiResponse<OrganizerCouponDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(promotionService.createCoupon(userId, eventId, request))
    }

    @Operation(summary = "Organizer bật/tạm dừng mã giảm giá")
    @PatchMapping("/organizer/promotions/{couponId}/status")
    fun updateStatus(
        @PathVariable couponId: Long,
        @RequestBody request: UpdateCouponStatusRequest
    ): ApiResponse<OrganizerCouponDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(promotionService.updateStatus(userId, couponId, request))
    }
}