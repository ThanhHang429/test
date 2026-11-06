package com.eventapp.organizer.classification

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Phân loại sự kiện", description = "FR-O09 — API-12, API-13")
@RestController
@RequestMapping("/api/v1")
class ClassificationController(
    private val classificationService: ClassificationService
) {

    @Operation(summary = "API-12 — Gắn danh mục chính/phụ và nhãn phụ cho sự kiện")
    @PutMapping("/organizer/events/{eventId}/classification")
    fun update(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: ClassificationRequest
    ): ApiResponse<ClassificationDto> =
        ApiResponse.ok(classificationService.update(CurrentUser.requireUserId(), eventId, request))

    @Operation(summary = "API-12 — Xem phân loại hiện tại của sự kiện")
    @GetMapping("/organizer/events/{eventId}/classification")
    fun get(@PathVariable eventId: Long): ApiResponse<ClassificationDto> =
        ApiResponse.ok(classificationService.get(CurrentUser.requireUserId(), eventId))

    @Operation(summary = "API-13 — Gợi ý nhãn phụ đang được dùng nhiều")
    @GetMapping("/tags/suggest")
    fun suggestTags(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false, defaultValue = "10") limit: Int
    ): ApiResponse<List<TagSuggestionDto>> =
        ApiResponse.ok(classificationService.suggestTags(q, limit))
}
