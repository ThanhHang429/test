package com.eventapp.organizer.ai

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "AI cho Organizer", description = "FR-O06 — API-14, FR-O27 — API-18")
@RestController
@RequestMapping("/api/v1/organizer")
class OrganizerAiController(
    private val organizerAiService: OrganizerAiService
) {

    @Operation(summary = "API-14 — AI viết mô tả sự kiện (bản nháp, không tự lưu)")
    @PostMapping("/ai/event-description")
    fun describe(@Valid @RequestBody request: EventDescriptionRequest): ApiResponse<EventDescriptionResponse> =
        ApiResponse.ok(organizerAiService.describe(CurrentUser.requireUserId(), request))

    @Operation(summary = "API-14b — AI soạn thông báo gửi người tham dự (bản nháp, không tự gửi)")
    @PostMapping("/ai/announcement")
    fun composeAnnouncement(
        @Valid @RequestBody request: AnnouncementDraftRequest
    ): ApiResponse<AnnouncementDraftResponse> =
        ApiResponse.ok(organizerAiService.composeAnnouncement(CurrentUser.requireUserId(), request))

    @Operation(summary = "API-18 — AI phân tích và gợi ý vận hành cho một sự kiện")
    @PostMapping("/events/{eventId}/ai-analysis")
    fun analyze(@PathVariable eventId: Long): ApiResponse<EventAnalysisResponse> =
        ApiResponse.ok(organizerAiService.analyze(CurrentUser.requireUserId(), eventId))
}
