package com.eventapp.favorite

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.response.PageMeta
import com.eventapp.common.security.CurrentUser
import com.eventapp.favorite.dto.FavoriteItemDto
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Yêu thích", description = "FR-U06 — API-05, API-06")
@RestController
@RequestMapping("/api/v1/me/favorites")
class FavoriteController(
    private val service: FavoriteService
) {

    @Operation(summary = "API-05 — Thêm sự kiện vào yêu thích (idempotent)")
    @PutMapping("/{eventId}")
    fun add(@PathVariable eventId: Long): ApiResponse<Map<String, Any>> {
        val isFavorite = service.add(CurrentUser.requireUserId(), eventId)
        return ApiResponse.ok(mapOf("eventId" to eventId, "isFavorite" to isFavorite))
    }

    @Operation(summary = "API-05 — Bỏ yêu thích (idempotent)")
    @DeleteMapping("/{eventId}")
    fun remove(@PathVariable eventId: Long): ApiResponse<Map<String, Any>> {
        val isFavorite = service.remove(CurrentUser.requireUserId(), eventId)
        return ApiResponse.ok(mapOf("eventId" to eventId, "isFavorite" to isFavorite))
    }

    @Operation(summary = "API-06 — Danh sách đã lưu (ALL | UPCOMING | PAST)")
    @GetMapping
    fun list(
        @RequestParam(required = false, defaultValue = "ALL") filter: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ApiResponse<List<FavoriteItemDto>> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 50)
        val (items, total) = service.list(CurrentUser.requireUserId(), filter, safePage, safeSize)
        return ApiResponse.ok(items, PageMeta.of(safePage, safeSize, total))
    }
}
