package com.eventapp.admin.category

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Quản lý danh mục", description = "FR-A09 — API-15 (quyền ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
class CategoryAdminController(
    private val categoryAdminService: CategoryAdminService
) {

    @Operation(summary = "API-15 — Cây danh mục đầy đủ (kèm danh mục đã ẩn)")
    @GetMapping
    fun tree(
        @RequestParam(required = false, defaultValue = "true") includeInactive: Boolean
    ): ApiResponse<List<AdminCategoryDto>> =
        ApiResponse.ok(categoryAdminService.tree(includeInactive))

    @Operation(summary = "API-15 — Tạo danh mục (tối đa 2 cấp)")
    @PostMapping
    fun create(@Valid @RequestBody request: CategoryCreateRequest): ApiResponse<AdminCategoryDto> =
        ApiResponse.ok(categoryAdminService.create(CurrentUser.requireUserId(), request))

    @Operation(summary = "API-15 — Sửa danh mục (kèm version để tránh ghi đè)")
    @PutMapping("/{categoryId}")
    fun update(
        @PathVariable categoryId: Long,
        @Valid @RequestBody request: CategoryUpdateRequest
    ): ApiResponse<AdminCategoryDto> =
        ApiResponse.ok(categoryAdminService.update(CurrentUser.requireUserId(), categoryId, request))

    @Operation(summary = "API-15 — Ẩn danh mục (bắt buộc lý do, không xoá cứng)")
    @PostMapping("/{categoryId}/hide")
    fun hide(
        @PathVariable categoryId: Long,
        @Valid @RequestBody request: CategoryHideRequest
    ): ApiResponse<AdminCategoryDto> =
        ApiResponse.ok(categoryAdminService.hide(CurrentUser.requireUserId(), categoryId, request.reason.orEmpty()))

    @Operation(summary = "API-15 — Bỏ ẩn danh mục")
    @PostMapping("/{categoryId}/unhide")
    fun unhide(@PathVariable categoryId: Long): ApiResponse<AdminCategoryDto> =
        ApiResponse.ok(categoryAdminService.unhide(CurrentUser.requireUserId(), categoryId))

    @Operation(summary = "API-15 — Sắp xếp lại thứ tự danh mục (kéo-thả)")
    @PutMapping("/order")
    fun reorder(@Valid @RequestBody request: CategoryOrderRequest): ApiResponse<List<AdminCategoryDto>> =
        ApiResponse.ok(categoryAdminService.reorder(CurrentUser.requireUserId(), request.items))
}
