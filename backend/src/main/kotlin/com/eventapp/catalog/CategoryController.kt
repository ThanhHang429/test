package com.eventapp.catalog

import com.eventapp.common.response.ApiResponse
import com.eventapp.discovery.dto.CategoryNodeDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Danh mục", description = "API-02 — danh mục sự kiện công khai")
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val service: CategoryQueryService
) {

    @Operation(summary = "API-02 — Cây danh mục đang hoạt động")
    @GetMapping
    fun tree(): ApiResponse<List<CategoryNodeDto>> = ApiResponse.ok(service.activeTree())
}
