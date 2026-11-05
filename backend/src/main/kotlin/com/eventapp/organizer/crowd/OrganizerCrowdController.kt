package com.eventapp.organizer.crowd

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Điều phối phòng",
    description = "FR-O28 — cảnh báo đông phòng, phòng dự phòng và thông báo cho người chưa check-in"
)
@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}")
class OrganizerCrowdController(
    private val service: OrganizerCrowdService
) {

    @Operation(summary = "Mức lấp đầy phòng chính + cảnh báo + gợi ý phòng dự phòng")
    @GetMapping("/crowd-status")
    fun status(@PathVariable eventId: Long): ApiResponse<CrowdStatusDto> =
        ApiResponse.ok(service.crowdStatus(CurrentUser.requireUserId(), eventId))

    @Operation(summary = "Danh sách phòng dự phòng của sự kiện")
    @GetMapping("/overflow-rooms")
    fun rooms(@PathVariable eventId: Long): ApiResponse<List<OverflowRoomDto>> =
        ApiResponse.ok(service.rooms(CurrentUser.requireUserId(), eventId))

    @Operation(summary = "Thêm phòng dự phòng")
    @PostMapping("/overflow-rooms")
    fun addRoom(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: CreateOverflowRoomRequest
    ): ApiResponse<OverflowRoomDto> =
        ApiResponse.ok(service.addRoom(CurrentUser.requireUserId(), eventId, request))

    @Operation(summary = "Đóng phòng dự phòng (giữ lịch sử)")
    @DeleteMapping("/overflow-rooms/{roomId}")
    fun closeRoom(
        @PathVariable eventId: Long,
        @PathVariable roomId: Long
    ): ApiResponse<OverflowRoomDto> =
        ApiResponse.ok(service.closeRoom(CurrentUser.requireUserId(), eventId, roomId))

    @Operation(summary = "Lịch sử thông báo đã gửi của sự kiện")
    @GetMapping("/notifications")
    fun notifications(@PathVariable eventId: Long): ApiResponse<List<NotificationDto>> =
        ApiResponse.ok(service.notifications(CurrentUser.requireUserId(), eventId))

    @Operation(summary = "Gửi thông báo cho người đã đăng ký (mặc định: chưa check-in)")
    @PostMapping("/notifications")
    fun send(
        @PathVariable eventId: Long,
        @Valid @RequestBody request: SendNotificationRequest
    ): ApiResponse<SendNotificationResponse> =
        ApiResponse.ok(service.sendNotification(CurrentUser.requireUserId(), eventId, request))
}
