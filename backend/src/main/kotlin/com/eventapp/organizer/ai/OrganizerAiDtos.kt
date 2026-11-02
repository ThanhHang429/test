package com.eventapp.organizer.ai

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class EventDescriptionRequest(
    @field:NotBlank(message = "Tiêu đề không được để trống")
    @field:Size(min = 5, max = 120, message = "Tiêu đề phải từ 5 đến 120 ký tự")
    val title: String,

    @field:NotNull(message = "Vui lòng chọn danh mục")
    val categoryId: Long?,

    /** OFFLINE | ONLINE | HYBRID */
    val format: String? = null,
    val startTime: String? = null,
    val location: String? = null,
    val onlineInfo: String? = null,

    @field:Size(max = 200, message = "Đối tượng tối đa 200 ký tự")
    val audience: String? = null,

    @field:Size(min = 3, max = 10, message = "Cần từ 3 đến 10 ý chính")
    val keyPoints: List<@Size(min = 3, max = 200, message = "Mỗi ý chính từ 3 đến 200 ký tự") String>,

    /** PROFESSIONAL | FRIENDLY | FORMAL */
    val tone: String? = null,

    /** SHORT | MEDIUM | LONG */
    val length: String? = null,

    @field:Min(1, message = "Số phương án tối thiểu là 1")
    @field:Max(3, message = "Số phương án tối đa là 3")
    val variants: Int? = null,

    /** Nếu có: kiểm tra sự kiện thuộc Organizer đang gọi. */
    val eventId: Long? = null
)

data class DescriptionVariantDto(
    val text: String,
    val shortSummary: String
)

data class EventDescriptionResponse(
    val descriptions: List<DescriptionVariantDto>,
    /** Thông tin còn thiếu nên bổ sung, hiển thị dưới dạng gợi ý. */
    val missingInfo: List<String>,
    /** Cảnh báo hậu kiểm: số tiền/ngày/SĐT/URL không có trong dữ liệu nhập. */
    val warnings: List<String>,
    /** AI | RULE_UNAVAILABLE */
    val source: String,
    val aiRequestId: Long?,
    val correlationId: String?
)

/** FR-O06 — AI soạn bản nháp thông báo gửi người tham dự (không tự gửi/công khai). */
data class AnnouncementDraftRequest(
    @field:NotBlank(message = "Nội dung cần thông báo không được để trống")
    @field:Size(min = 5, max = 500, message = "Nội dung cần thông báo phải từ 5 đến 500 ký tự")
    val topic: String,

    /** Nếu có: kiểm tra sự kiện thuộc Organizer đang gọi. */
    val eventId: Long? = null,

    @field:Size(max = 120, message = "Tên sự kiện tối đa 120 ký tự")
    val eventTitle: String? = null,

    @field:Size(max = 200, message = "Đối tượng nhận tối đa 200 ký tự")
    val audience: String? = null,

    @field:Size(max = 5, message = "Tối đa 5 ý chính")
    val keyPoints: List<@Size(min = 3, max = 200, message = "Mỗi ý chính từ 3 đến 200 ký tự") String> = emptyList(),

    /** PROFESSIONAL | FRIENDLY | FORMAL */
    val tone: String? = null,

    /** SHORT | MEDIUM | LONG */
    val length: String? = null,

    @field:Min(1, message = "Số phương án tối thiểu là 1")
    @field:Max(3, message = "Số phương án tối đa là 3")
    val variants: Int? = null
)

data class AnnouncementDraftResponse(
    val variants: List<DescriptionVariantDto>,
    /** Thông tin còn thiếu nên bổ sung, hiển thị dưới dạng gợi ý. */
    val missingInfo: List<String>,
    /** Cảnh báo hậu kiểm: số tiền/ngày/SĐT/URL không có trong dữ liệu nhập. */
    val warnings: List<String>,
    /** AI | RULE_UNAVAILABLE */
    val source: String,
    val aiRequestId: Long?,
    val correlationId: String?
)

data class EventAnalysisResponse(
    val eventId: Long,
    val metrics: Map<String, Any?>,
    val summary: List<String>,
    val insights: List<InsightDto>,
    val disclaimer: String,
    val dataLimitations: List<String>,
    val insufficientData: Boolean,
    val source: String,
    val generatedAt: Instant,
    val aiRequestId: Long?,
    val correlationId: String?
)

data class InsightDto(
    /** OBSERVATION | SUGGESTION */
    val type: String,
    val text: String,
    val relatedMetric: String?,
    /** LOW | MEDIUM | HIGH */
    val confidence: String
)
