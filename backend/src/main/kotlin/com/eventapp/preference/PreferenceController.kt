package com.eventapp.preference

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.preference.dto.PreferenceDto
import com.eventapp.preference.dto.PreferenceUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Sở thích", description = "FR-U03 — API-01")
@RestController
@RequestMapping("/api/v1/me/preferences")
class PreferenceController(
    private val service: PreferenceService
) {

    @Operation(summary = "API-01 — Lấy sở thích của tôi")
    @GetMapping
    fun get(): ApiResponse<PreferenceDto> = ApiResponse.ok(service.get(CurrentUser.requireUserId()))

    @Operation(summary = "API-01 — Lưu sở thích (idempotent)")
    @PutMapping
    fun put(@Valid @RequestBody request: PreferenceUpdateRequest): ApiResponse<PreferenceDto> =
        ApiResponse.ok(service.put(CurrentUser.requireUserId(), request))

    @Operation(summary = "API-01 — Đặt lại sở thích về mặc định")
    @DeleteMapping
    fun reset(): ApiResponse<PreferenceDto> = ApiResponse.ok(service.reset(CurrentUser.requireUserId()))
}
