package com.eventapp.organizer.event

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Sự kiện của Organizer", description = "FR-O02 — danh sách và chi tiết sự kiện do mình quản lý")
@RestController
@RequestMapping("/api/v1/organizer/events")
class OrganizerEventController(
    private val service: OrganizerEventService
) {

    @Operation(summary = "Danh sách sự kiện của tôi (lọc theo trạng thái duyệt hoặc vòng đời)")
    @GetMapping
    fun list(
        @RequestParam(required = false, defaultValue = "ALL") status: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<OrganizerEventDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val (items, total) = service.list(CurrentUser.requireUserId(), status, safePage, safeSize)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }

    @Operation(summary = "Chi tiết một sự kiện của tôi")
    @GetMapping("/{eventId}")
    fun detail(@PathVariable eventId: Long): ApiResponse<OrganizerEventDto> =
        ApiResponse.ok(service.detail(CurrentUser.requireUserId(), eventId))
}
