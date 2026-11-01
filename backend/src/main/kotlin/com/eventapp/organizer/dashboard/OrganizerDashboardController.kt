package com.eventapp.organizer.dashboard

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Dashboard Organizer", description = "FR-O03 — API-17")
@RestController
@RequestMapping("/api/v1/organizer")
class OrganizerDashboardController(
    private val dashboardService: OrganizerDashboardService
) {

    @Operation(summary = "API-17 — Tổng hợp số liệu cho Organizer (range=7d | 30d | custom)")
    @GetMapping("/dashboard")
    fun dashboard(
        @RequestParam(required = false, defaultValue = "30d") range: String,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?
    ): ApiResponse<OrganizerDashboardDto> =
        ApiResponse.ok(dashboardService.dashboard(CurrentUser.requireUserId(), range, from, to))
}
