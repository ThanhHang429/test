package com.eventapp.organizer.crowd

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Thông báo của tôi", description = "FR-O28 — người tham dự đọc thông báo từ organizer")
@RestController
@RequestMapping("/api/v1/me")
class MyNotificationController(
    private val service: OrganizerCrowdService
) {

    @Operation(summary = "Danh sách thông báo gửi tới tôi")
    @GetMapping("/notifications")
    fun mine(): ApiResponse<List<NotificationDto>> =
        ApiResponse.ok(service.myNotifications(CurrentUser.requireUserId()))
}
