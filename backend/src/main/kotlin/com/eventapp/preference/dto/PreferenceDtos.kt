package com.eventapp.preference.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * Dữ liệu sở thích trả về (FR-U03). Trường [version] dùng cho khoá lạc quan (SRS 3.5).
 * [removedCategoryIds] là các danh mục đã bị Admin ẩn sau khi User lưu.
 */
data class PreferenceDto(
    val categoryIds: List<Long>,
    /** ANY | ONSITE | ONLINE */
    val eventFormat: String,
    /** ANY | FREE_ONLY | PAID_ONLY */
    val priceMode: String,
    val maxBudgetVnd: Long?,
    val preferredCities: List<String>,
    /** WEEKDAY | WEEKEND */
    val preferredDays: List<String>,
    val onboardingCompleted: Boolean,
    val personalizationEnabled: Boolean,
    val version: Int,
    val removedCategoryIds: List<Long> = emptyList()
)

/** Không nhận userId từ body (U03-R02) — luôn lấy từ token. */
data class PreferenceUpdateRequest(
    @field:NotNull(message = "categoryIds không được null")
    val categoryIds: List<Long> = emptyList(),

    val eventFormat: String = "ANY",
    val priceMode: String = "ANY",

    @field:Min(value = 0, message = "Ngân sách phải >= 0")
    @field:Max(value = 100_000_000, message = "Ngân sách tối đa 100.000.000")
    val maxBudgetVnd: Long? = null,

    val preferredCities: List<String> = emptyList(),
    val preferredDays: List<String> = listOf("WEEKDAY", "WEEKEND"),
    val onboardingCompleted: Boolean? = null,
    val personalizationEnabled: Boolean? = null,

    /** Nếu client gửi version và lệch với server -> 409 (khoá lạc quan). */
    val version: Int? = null
)
