package com.eventapp.admin.category

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CategoryCreateRequest(
    @field:NotBlank(message = "Mã danh mục không được để trống")
    @field:Size(max = 60, message = "Mã danh mục tối đa 60 ký tự")
    val categoryCode: String,

    @field:NotBlank(message = "Tên danh mục không được để trống")
    @field:Size(max = 120, message = "Tên danh mục tối đa 120 ký tự")
    val categoryName: String,

    @field:Size(max = 160, message = "Slug tối đa 160 ký tự")
    val slug: String? = null,

    @field:Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    val description: String? = null,

    val parentCategoryId: Long? = null,
    val sortOrder: Int? = null
)

data class CategoryUpdateRequest(
    @field:Size(max = 120, message = "Tên danh mục tối đa 120 ký tự")
    val categoryName: String? = null,

    @field:Size(max = 160, message = "Slug tối đa 160 ký tự")
    val slug: String? = null,

    @field:Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    val description: String? = null,

    val sortOrder: Int? = null,

    /** Khoá lạc quan = `updatedAt` dạng epoch millis. */
    val version: Long? = null
)

data class CategoryHideRequest(
    @field:NotBlank(message = "Bắt buộc ghi lý do khi ẩn danh mục")
    @field:Size(max = 500, message = "Lý do tối đa 500 ký tự")
    val reason: String? = null
)

data class CategoryOrderItem(
    val categoryId: Long,
    val sortOrder: Int
)

data class CategoryOrderRequest(
    @field:Size(min = 1, max = 200, message = "Danh sách sắp xếp phải từ 1 đến 200 mục")
    val items: List<CategoryOrderItem>
)

data class AdminCategoryDto(
    val id: Long,
    val code: String,
    val name: String,
    val slug: String,
    val description: String?,
    val parentCategoryId: Long?,
    val active: Boolean,
    val sortOrder: Int,
    val eventCount: Long,
    val children: List<AdminCategoryDto>,
    val updatedAt: Instant?
)
