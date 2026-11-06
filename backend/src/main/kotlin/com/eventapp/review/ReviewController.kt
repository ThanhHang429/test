package com.eventapp.review

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import com.eventapp.discovery.EventDiscoveryService
import com.eventapp.discovery.dto.ReviewSummaryDto
import com.eventapp.review.dto.CreateReviewRequest
import com.eventapp.review.dto.MyReviewDto
import com.eventapp.review.dto.ReportReviewRequest
import com.eventapp.review.dto.ReviewDto
import com.eventapp.review.dto.ReviewMutationDto
import com.eventapp.review.dto.UpdateReviewRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Đánh giá", description = "FR-U20 — API-07, API-08, API-09")
@RestController
@RequestMapping("/api/v1")
class ReviewController(
    private val reviewService: ReviewService,
    private val discoveryService: EventDiscoveryService
) {

    @Operation(summary = "API-07 — Danh sách đánh giá của sự kiện (công khai, chỉ VISIBLE)")
    @GetMapping("/events/{eventId}/reviews")
    fun list(
        @PathVariable eventId: Long,
        @RequestParam(required = false, defaultValue = "NEWEST") sort: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<ReviewDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val safeSort = sort.uppercase().let {
            if (it in SORTS) it else ReviewService.SORT_NEWEST
        }
        val viewerId = CurrentUser.orNull()?.userId
        val (items, total) = reviewService.list(eventId, safeSort, safePage, safeSize, viewerId)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "API-07 — Tổng hợp điểm đánh giá (trung bình, phân bố, nổi bật)")
    @GetMapping("/events/{eventId}/reviews/summary")
    fun summary(@PathVariable eventId: Long): ApiResponse<ReviewSummaryDto> {
        reviewService.assertPublicEvent(eventId)
        return ApiResponse.ok(discoveryService.reviewSummary(eventId))
    }

    @Operation(summary = "API-08 — Viết đánh giá (Idempotency-Key tuỳ chọn)")
    @PostMapping("/events/{eventId}/reviews")
    fun create(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: CreateReviewRequest,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?
    ): ApiResponse<ReviewMutationDto> =
        ApiResponse.ok(reviewService.create(CurrentUser.requireUserId(), eventId, request, idempotencyKey))

    @Operation(summary = "API-08 — Sửa đánh giá của tôi (tối đa 3 lần trong 7 ngày)")
    @PutMapping("/events/{eventId}/reviews/me")
    fun update(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: UpdateReviewRequest
    ): ApiResponse<ReviewMutationDto> =
        ApiResponse.ok(reviewService.update(CurrentUser.requireUserId(), eventId, request))

    @Operation(summary = "API-08 — Đánh giá của tôi (mọi trạng thái)")
    @GetMapping("/me/reviews")
    fun mine(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<MyReviewDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val (items, total) = reviewService.myReviews(CurrentUser.requireUserId(), safePage, safeSize)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "API-09 — Báo cáo đánh giá vi phạm (1 lần/đánh giá, tối đa 10/ngày)")
    @PostMapping("/reviews/{reviewId}/report")
    fun report(
        @PathVariable reviewId: Long,
        @Valid @RequestBody request: ReportReviewRequest
    ): ApiResponse<Map<String, Any>> {
        reviewService.report(CurrentUser.requireUserId(), reviewId, request)
        return ApiResponse.ok(mapOf("reviewId" to reviewId, "reported" to true))
    }

    companion object {
        private val SORTS = setOf(
            ReviewService.SORT_NEWEST,
            ReviewService.SORT_RATING_DESC,
            ReviewService.SORT_RATING_ASC
        )
    }
}
