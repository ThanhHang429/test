package com.eventapp.admin.integration

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Tích hợp", description = "FR-A20 — API-21, API-22 (quyền ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/integrations")
@PreAuthorize("hasRole('ADMIN')")
class IntegrationController(
    private val monitorService: IntegrationMonitorService
) {

    @Operation(summary = "API-21 — Trạng thái 3 dịch vụ ngoài (AI, thanh toán, FCM)")
    @GetMapping
    fun health(): ApiResponse<List<IntegrationHealthDto>> =
        ApiResponse.ok(monitorService.health())

    @Operation(summary = "API-21 — Số liệu sử dụng AI theo cửa sổ 1h | 24h | 7d")
    @GetMapping("/ai/requests")
    fun aiUsage(@RequestParam(required = false, defaultValue = "24h") range: String): ApiResponse<AiUsageSummaryDto> =
        ApiResponse.ok(monitorService.aiUsage(range))

    @Operation(summary = "API-21 — Danh sách lỗi AI gần đây")
    @GetMapping("/ai/requests/errors")
    fun aiErrors(@RequestParam(required = false, defaultValue = "20") limit: Int): ApiResponse<List<AiErrorItemDto>> =
        ApiResponse.ok(monitorService.recentErrors(limit))

    @Operation(summary = "API-21 — Tra một yêu cầu AI theo correlationId")
    @GetMapping("/ai/requests/{correlationId}")
    fun aiRequest(@PathVariable correlationId: String): ApiResponse<List<AiRequestDetailDto>> =
        ApiResponse.ok(monitorService.requestsByCorrelation(correlationId))

    @Operation(summary = "API-22 — Bật/tắt khẩn một tác vụ AI (bắt buộc lý do)")
    @PutMapping("/ai/features/{taskType}")
    fun toggleFeature(
        @PathVariable taskType: String,
        @Valid @RequestBody request: AiFeatureToggleRequest
    ): ApiResponse<AiFeatureToggleResponse> =
        ApiResponse.ok(
            monitorService.toggleFeature(
                taskType = taskType,
                enabled = request.enabled!!,
                reason = request.reason.orEmpty(),
                actorUserId = CurrentUser.requireUserId()
            )
        )
}
