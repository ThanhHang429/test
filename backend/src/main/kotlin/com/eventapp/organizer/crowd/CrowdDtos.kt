package com.eventapp.organizer.crowd

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// ---------------------------------------------------------------------------
// Phòng dự phòng
// ---------------------------------------------------------------------------

data class OverflowRoomDto(
    val id: Long,
    val roomName: String,
    val capacity: Int,
    val note: String?,
    /** ACTIVE | CLOSED */
    val status: String,
    val createdAt: Instant?
)

data class CreateOverflowRoomRequest(
    @field:NotBlank(message = "Tên phòng không được để trống")
    @field:Size(max = 120, message = "Tên phòng tối đa 120 ký tự")
    val roomName: String,

    @field:Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @field:Max(value = 100_000, message = "Sức chứa tối đa 100.000")
    val capacity: Int,

    @field:Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    val note: String? = null
)

// ---------------------------------------------------------------------------
// Cảnh báo đông phòng
// ---------------------------------------------------------------------------

data class CrowdStatusDto(
    val eventId: Long,
    val eventTitle: String,
    val capacityLimit: Int,
    val registered: Long,
    val checkedIn: Long,
    val notCheckedIn: Long,
    /** checkedIn / capacityLimit (0…1+) */
    val occupancyRate: Double,
    /** checkedIn / registered (0…1) */
    val checkinRate: Double,
    /** NORMAL | WATCH | CROWDED | FULL */
    val level: String,
    val warning: String?,
    val overflowRooms: List<OverflowRoomDto>,
    val suggestedRoom: OverflowRoomDto?,
    val suggestion: String?
)

// ---------------------------------------------------------------------------
// Thông báo
// ---------------------------------------------------------------------------

data class SendNotificationRequest(
    @field:NotBlank(message = "Tiêu đề không được để trống")
    @field:Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    val title: String,

    @field:NotBlank(message = "Nội dung không được để trống")
    @field:Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
    val body: String,

    /** REGISTERED_NOT_CHECKED_IN (mặc định) | ALL_REGISTERED */
    val audience: String? = null,

    /** AI | MANUAL */
    val source: String? = null
)

data class NotificationDto(
    val id: Long,
    val eventId: Long,
    val eventTitle: String?,
    val title: String,
    val body: String,
    val audience: String,
    val recipientCount: Int,
    val source: String,
    val sentAt: Instant?
)

data class SendNotificationResponse(
    val notification: NotificationDto,
    val recipientCount: Int
)
