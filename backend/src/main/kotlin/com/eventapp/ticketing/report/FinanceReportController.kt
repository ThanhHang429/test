package com.eventapp.ticketing.report

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.CurrentUser
import com.eventapp.ticketing.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Báo cáo Tài chính & Đối soát", description = "FR-O26, FR-A10, FR-A11, FR-A16, FR-A17")
@RestController
@RequestMapping("/api/v1")
class FinanceReportController(
    private val financeReportService: FinanceReportService
) {
    @Operation(summary = "FR-O26: Báo cáo tài chính sự kiện cho Organizer")
    @GetMapping("/organizer/events/{eventId}/finance-report")
    fun eventFinanceReport(@PathVariable eventId: Long): ApiResponse<EventFinanceReportDto> {
        val userId = CurrentUser.requireUserId()
        return ApiResponse.ok(financeReportService.getEventFinanceReport(userId, eventId))
    }

    @Operation(summary = "FR-A17: Báo cáo tài chính toàn nền tảng cho Admin")
    @GetMapping("/admin/reports/finance")
    fun adminFinanceSummary(): ApiResponse<AdminFinanceSummaryDto> {
        return ApiResponse.ok(financeReportService.getAdminFinanceSummary())
    }

    @Operation(summary = "FR-A16: Admin cập nhật tỷ lệ phí nền tảng")
    @PostMapping("/admin/reports/finance/fee-rate")
    fun updateFeeRate(@RequestBody request: UpdateFeeRateRequest): ApiResponse<AdminFinanceSummaryDto> {
        val adminUserId = CurrentUser.requireUserId()
        return ApiResponse.ok(financeReportService.updateFeeRate(adminUserId, request))
    }

    @Operation(summary = "FR-A10: Admin tra cứu đơn hàng theo mã đơn")
    @GetMapping("/admin/orders/lookup")
    fun lookupOrder(@RequestParam query: String): ApiResponse<OrderDto> {
        return ApiResponse.ok(financeReportService.lookupOrder(query))
    }

    @Operation(summary = "FR-A11: Admin xem nhật ký Webhook cổng thanh toán")
    @GetMapping("/admin/webhooks/logs")
    fun getWebhookLogs(@RequestParam(defaultValue = "20") limit: Int): ApiResponse<List<WebhookLogItemDto>> {
        return ApiResponse.ok(financeReportService.getWebhookLogs(limit))
    }
}