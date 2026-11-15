package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.report.ReportDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Báo cáo tài chính, tra cứu đơn hàng và cấu hình phí (FR-A10, FR-A11, FR-A16, FR-A17, FR-O26).
 */
public interface ReportApi {

    /** FR-A17: Báo cáo tài chính toàn nền tảng cho Admin. */
    @GET("admin/reports/finance")
    Call<ResponseWrapper.Envelope<ReportDtos.AdminFinanceSummary>> getAdminFinanceSummary();

    /** FR-A16: Cập nhật tỷ lệ phí nền tảng. */
    @POST("admin/reports/finance/fee-rate")
    Call<ResponseWrapper.Envelope<ReportDtos.AdminFinanceSummary>> updateFeeRate(
            @Body ReportDtos.UpdateFeeRateRequest request
    );

    /** FR-A10: Admin tra cứu đơn hàng và hóa đơn đối soát theo mã đơn hoặc từ khóa. */
    @GET("admin/orders/lookup")
    Call<ResponseWrapper.Envelope<OrderDtos.Order>> lookupOrder(
            @Query("query") String query
    );

    /** FR-A11: Admin xem nhật ký webhook cổng thanh toán để đối soát. */
    @GET("admin/webhooks/logs")
    Call<ResponseWrapper.Envelope<List<ReportDtos.WebhookLogItem>>> getWebhookLogs(
            @Query("limit") int limit
    );

    /** FR-O26: Organizer xem báo cáo doanh thu, phí nền tảng, hoàn tiền, check-in theo sự kiện. */
    @GET("organizer/events/{eventId}/finance-report")
    Call<ResponseWrapper.Envelope<ReportDtos.EventFinanceReport>> getEventFinanceReport(
            @Path("eventId") long eventId
    );
}
