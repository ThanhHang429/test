package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.ReportApi;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.report.ReportDtos;

import java.util.List;

/**
 * Repository báo cáo tài chính, tra cứu đơn hàng và cấu hình phí (FR-A10, FR-A11, FR-A16, FR-A17, FR-O26).
 * Toàn bộ số liệu lấy trực tiếp từ Backend qua {@link ReportApi}, không tự bịa số liệu cục bộ.
 */
public class FinanceRepository {

    private final ReportApi api;

    public FinanceRepository(@NonNull ReportApi api) {
        this.api = api;
    }

    /** Lấy tóm tắt báo cáo tài chính toàn nền tảng (FR-A17). */
    public void getAdminFinanceSummary(@NonNull ApiCallback<ReportDtos.AdminFinanceSummary> callback) {
        ApiCallback.enqueue(api.getAdminFinanceSummary(), callback);
    }

    /** Cập nhật tỷ lệ phí nền tảng (FR-A16). */
    public void updateFeeRate(double newRate, @NonNull ApiCallback<ReportDtos.AdminFinanceSummary> callback) {
        updateFeeRate(newRate, null, null, callback);
    }

    /** Cập nhật tỷ lệ phí nền tảng có phiên bản và thời điểm hiệu lực (FR-A16). */
    public void updateFeeRate(double newRate,
                              @androidx.annotation.Nullable String effectiveFrom,
                              @androidx.annotation.Nullable String description,
                              @NonNull ApiCallback<ReportDtos.AdminFinanceSummary> callback) {
        ReportDtos.UpdateFeeRateRequest req = new ReportDtos.UpdateFeeRateRequest(newRate, effectiveFrom, description);
        ApiCallback.enqueue(api.updateFeeRate(req), callback);
    }

    /** Tra cứu đơn hàng theo mã hoặc từ khóa (FR-A10). */
    public void lookupOrder(String query, @NonNull ApiCallback<OrderDtos.Order> callback) {
        ApiCallback.enqueue(api.lookupOrder(query), callback);
    }

    /** Lấy nhật ký Webhook thanh toán (FR-A11). */
    public void getWebhookLogs(int limit, @NonNull ApiCallback<List<ReportDtos.WebhookLogItem>> callback) {
        ApiCallback.enqueue(api.getWebhookLogs(limit), callback);
    }

    /** Báo cáo tài chính chi tiết sự kiện cho Organizer (FR-O26). */
    public void getEventFinanceReport(long eventId, @NonNull ApiCallback<ReportDtos.EventFinanceReport> callback) {
        ApiCallback.enqueue(api.getEventFinanceReport(eventId), callback);
    }
}
