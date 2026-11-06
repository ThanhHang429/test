package com.eventapp.recommendation.dto

import com.eventapp.discovery.dto.EventCardDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant

data class RecommendationItemDto(
    val event: EventCardDto,
    /** Tối đa 140 ký tự, không chứa số tiền/ngày ngoài dữ liệu (U07). */
    val reason: String,
    /** RULE hoặc AI — dùng để hiển thị nhãn «Gợi ý bởi AI» / «Phù hợp sở thích». */
    val reasonSource: String
)

data class RecommendationMetaDto(
    val title: String,
    val generatedAt: Instant,
    val isFallback: Boolean,
    val personalized: Boolean,
    val engineVersion: String,
    val modelCode: String?,
    val aiRequestId: Long?,
    val correlationId: String?
)

data class RecommendationListDto(
    val items: List<RecommendationItemDto>,
    val meta: RecommendationMetaDto
)

/** Sở thích tạm thời cho khách (POST /recommendations/preview). */
data class RecommendationPreviewRequest(
    @field:Size(max = 10, message = "Chọn tối đa 10 danh mục")
    val categoryIds: List<Long> = emptyList(),

    @field:Size(max = 120, message = "Tên thành phố tối đa 120 ký tự")
    val city: String? = null,

    /** ANY | FREE_ONLY | PAID_ONLY */
    val priceMode: String? = null,

    @field:Min(0, message = "Ngân sách không được âm")
    @field:Max(100_000_000, message = "Ngân sách tối đa 100.000.000")
    val maxBudgetVnd: Long? = null,

    /** ANY | ONSITE | ONLINE */
    val eventFormat: String? = null,

    val preferredDays: List<String> = emptyList(),

    @field:Min(1, message = "Số lượng tối thiểu là 1")
    @field:Max(20, message = "Số lượng tối đa là 20")
    val limit: Int? = null
)
