package com.eventapp.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

/** Một đánh giá trong danh sách công khai (FR-U20). */
data class ReviewDto(
    val id: Long,
    val authorName: String,
    val rating: Int,
    val text: String?,
    /** PENDING (SRS gọi là HELD) | VISIBLE | HIDDEN | REMOVED */
    val status: String,
    val statusLabel: String,
    /** true nếu là đánh giá của chính người đang gọi API (U20-R03: ghim đầu danh sách). */
    val mine: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

/** Đánh giá của tôi — kèm ngữ cảnh sự kiện để màn «Đánh giá của tôi» không phải gọi thêm API. */
data class MyReviewDto(
    val id: Long,
    val eventId: Long,
    val eventTitle: String?,
    val eventCoverUrl: String?,
    val rating: Int,
    val text: String?,
    val status: String,
    val statusLabel: String,
    /** Lý do chung khi bị ẩn (A14-R01), không lộ chi tiết nội bộ. */
    val moderationReason: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val editCount: Int,
    val canEdit: Boolean,
    /** Số ngày còn lại được sửa (0 nếu đã hết hạn). */
    val editDaysLeft: Long
)

/** Kết quả sau khi tạo/sửa đánh giá. */
data class ReviewMutationDto(
    val review: MyReviewDto,
    /** Thông điệp cho app: đã hiển thị hay đang chờ kiểm duyệt. */
    val notice: String
)

data class CreateReviewRequest(
    @field:NotNull(message = "Vui lòng chọn số sao")
    @field:Min(1, message = "Số sao tối thiểu là 1")
    @field:Max(5, message = "Số sao tối đa là 5")
    val rating: Int?,

    @field:Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    val text: String? = null
)

data class UpdateReviewRequest(
    @field:Min(1, message = "Số sao tối thiểu là 1")
    @field:Max(5, message = "Số sao tối đa là 5")
    val rating: Int? = null,

    @field:Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự")
    val text: String? = null,

    /** Khoá lạc quan: chính là `updatedAt` dạng epoch millis của đánh giá hiện tại. */
    val version: Long? = null
)

data class ReportReviewRequest(
    @field:NotBlank(message = "Vui lòng chọn lý do báo cáo")
    val reasonCode: String,

    @field:Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    val note: String? = null
)

/** Một mục trong hàng chờ kiểm duyệt đánh giá (A14). */
data class AdminReviewDto(
    val id: Long,
    val eventId: Long,
    val eventTitle: String?,
    val authorName: String,
    val authorEmail: String?,
    val rating: Int,
    val text: String?,
    val status: String,
    val statusLabel: String,
    val moderationReason: String?,
    val flaggedCodes: List<String>,
    val reportCount: Long,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val moderatedAt: Instant?
)

/** Một "cờ kiểm duyệt" (A07). Suy ra từ reviews + audit_logs vì schema không có `moderation_flags`. */
data class ModerationFlagDto(
    val id: String,
    val targetType: String,
    val targetId: Long,
    val eventId: Long?,
    val eventTitle: String?,
    val reasonCode: String,
    val reasonLabel: String,
    val evidence: String?,
    val severity: String,
    val status: String,
    val createdAt: Instant?
)

data class ModerationActionRequest(
    @field:NotBlank(message = "Vui lòng chọn mã lý do")
    val reasonCode: String,

    @field:Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    val note: String? = null
)

data class RescanRequest(
    /** REVIEW (mặc định) — hiện chỉ đánh giá có nội dung văn bản để quét. */
    val targetType: String? = null,
    /** Chỉ quét nội dung tạo trong N ngày gần nhất; null = toàn bộ. */
    @field:Min(1, message = "Số ngày tối thiểu là 1")
    @field:Max(366, message = "Số ngày tối đa là 366")
    val sinceDays: Int? = null
)

data class RescanResultDto(
    val scanned: Int,
    val flagged: Int,
    val flagIds: List<String>
)
