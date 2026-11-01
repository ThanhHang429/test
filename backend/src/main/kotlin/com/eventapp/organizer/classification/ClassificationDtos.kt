package com.eventapp.organizer.classification

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

data class ClassificationRequest(
    @field:NotNull(message = "Vui lòng chọn danh mục chính")
    val primaryCategoryId: Long?,

    @field:Size(max = 2, message = "Tối đa 2 danh mục phụ")
    val secondaryCategoryIds: List<Long> = emptyList(),

    @field:Size(max = 5, message = "Tối đa 5 nhãn phụ")
    val tags: List<String> = emptyList()
)

data class CategoryBriefDto(
    val id: Long,
    val code: String,
    val name: String,
    val slug: String,
    /** false nếu danh mục đã bị Admin ẩn (vẫn hiển thị trên sự kiện cũ). */
    val active: Boolean
)

data class ClassificationDto(
    val eventId: Long,
    val primaryCategory: CategoryBriefDto?,
    val secondaryCategories: List<CategoryBriefDto>,
    val tags: List<String>,
    /**
     * O09-R03: đổi danh mục chính của sự kiện đã duyệt là thay đổi quan trọng —
     * backend chỉ báo cờ, việc duyệt lại do module sự kiện quyết định.
     */
    val requiresReReview: Boolean,
    val updatedAt: Instant?
)

data class TagSuggestionDto(
    val id: Long,
    val name: String,
    val slug: String,
    val usageCount: Long
)
