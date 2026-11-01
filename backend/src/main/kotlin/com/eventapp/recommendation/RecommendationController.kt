package com.eventapp.recommendation

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.recommendation.dto.RecommendationListDto
import com.eventapp.recommendation.dto.RecommendationPreviewRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Gợi ý AI", description = "FR-U07 — API-10")
@RestController
@RequestMapping("/api/v1/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) {

    @Operation(summary = "API-10 — Gợi ý cho tôi (cache 15 phút, refresh=true để làm mới)")
    @GetMapping
    fun forMe(
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @RequestParam(required = false, defaultValue = "false") refresh: Boolean
    ): ApiResponse<RecommendationListDto> =
        ApiResponse.ok(
            recommendationService.forUser(
                userId = CurrentUser.requireUserId(),
                limit = limit.coerceIn(1, 20),
                refresh = refresh
            )
        )

    @Operation(summary = "API-10 — Xem trước gợi ý cho khách (sở thích tạm thời, không cần đăng nhập)")
    @PostMapping("/preview")
    fun preview(@Valid @RequestBody request: RecommendationPreviewRequest): ApiResponse<RecommendationListDto> =
        ApiResponse.ok(recommendationService.preview(request))

    @Operation(summary = "API-10 — «Không quan tâm» một gợi ý (loại trong 30 ngày)")
    @PostMapping("/{eventId}/dismiss")
    fun dismiss(@PathVariable eventId: Long): ApiResponse<Map<String, Any>> {
        recommendationService.dismiss(CurrentUser.requireUserId(), eventId)
        return ApiResponse.ok(mapOf("eventId" to eventId, "dismissed" to true))
    }
}
