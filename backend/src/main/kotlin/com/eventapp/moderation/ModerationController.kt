package com.eventapp.moderation

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import com.eventapp.review.dto.AdminReviewDto
import com.eventapp.review.dto.ModerationActionRequest
import com.eventapp.review.dto.ModerationFlagDto
import com.eventapp.review.dto.RescanRequest
import com.eventapp.review.dto.RescanResultDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Kiểm duyệt", description = "FR-A14 / FR-A07 — API-19, API-20 (quyền ADMIN)")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
class ModerationController(
    private val moderationService: ModerationService
) {

    // ---------------- API-20: quản lý đánh giá (A14) ----------------

    @Operation(summary = "API-20 — Hàng chờ đánh giá (lọc status / eventId / rating)")
    @GetMapping("/reviews")
    fun listReviews(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) eventId: Long?,
        @RequestParam(required = false) rating: Int?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<AdminReviewDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val (items, total) = moderationService.listReviews(status, eventId, rating, safePage, safeSize)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "API-20 — Duyệt hiển thị đánh giá (PENDING → VISIBLE)")
    @PostMapping("/reviews/{reviewId}/approve")
    fun approve(
        @PathVariable reviewId: Long,
        @Valid @RequestBody request: ModerationActionRequest
    ): ApiResponse<AdminReviewDto> =
        ApiResponse.ok(moderationService.approve(reviewId, CurrentUser.requireUserId(), request))

    @Operation(summary = "API-20 — Ẩn đánh giá (bắt buộc mã lý do)")
    @PostMapping("/reviews/{reviewId}/hide")
    fun hide(
        @PathVariable reviewId: Long,
        @Valid @RequestBody request: ModerationActionRequest
    ): ApiResponse<AdminReviewDto> =
        ApiResponse.ok(moderationService.hide(reviewId, CurrentUser.requireUserId(), request))

    @Operation(summary = "API-20 — Khôi phục đánh giá đã ẩn (HIDDEN → VISIBLE)")
    @PostMapping("/reviews/{reviewId}/restore")
    fun restore(
        @PathVariable reviewId: Long,
        @Valid @RequestBody request: ModerationActionRequest
    ): ApiResponse<AdminReviewDto> =
        ApiResponse.ok(moderationService.restore(reviewId, CurrentUser.requireUserId(), request))

    // ---------------- API-19: cờ kiểm duyệt (A07) ----------------

    @Operation(summary = "API-19 — Hàng chờ cờ kiểm duyệt (ALL | HELD | REPORTED)")
    @GetMapping("/moderation/flags")
    fun listFlags(
        @RequestParam(required = false, defaultValue = "ALL") source: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<ModerationFlagDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val (items, total) = moderationService.listFlags(source, safePage, safeSize)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "API-19 — Xác nhận vi phạm (ẩn nội dung bị gắn cờ)")
    @PostMapping("/moderation/flags/{flagId}/confirm")
    fun confirmFlag(
        @PathVariable flagId: String,
        @Valid @RequestBody request: ModerationActionRequest
    ): ApiResponse<AdminReviewDto> =
        ApiResponse.ok(moderationService.confirmFlag(flagId, CurrentUser.requireUserId(), request))

    @Operation(summary = "API-19 — Bỏ qua cờ (cho hiển thị lại)")
    @PostMapping("/moderation/flags/{flagId}/dismiss")
    fun dismissFlag(
        @PathVariable flagId: String,
        @Valid @RequestBody request: ModerationActionRequest
    ): ApiResponse<AdminReviewDto> =
        ApiResponse.ok(moderationService.dismissFlag(flagId, CurrentUser.requireUserId(), request))

    @Operation(summary = "API-19 — Quét lại nội dung đánh giá (idempotent theo targetType)")
    @PostMapping("/moderation/rescan")
    fun rescan(@Valid @RequestBody(required = false) request: RescanRequest?): ApiResponse<RescanResultDto> =
        ApiResponse.ok(moderationService.rescan(CurrentUser.requireUserId(), request ?: RescanRequest()))
}
