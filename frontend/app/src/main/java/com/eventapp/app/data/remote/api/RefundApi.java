package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.refund.RefundDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Quản lý và xử lý hoàn tiền (FR-O25, FR-A12).
 */
public interface RefundApi {

    /** Khách hàng yêu cầu hoàn tiền cho đơn hàng (FR-O25). */
    @POST("refunds")
    Call<ResponseWrapper.Envelope<RefundDtos.RefundResponse>> requestRefund(
            @Body RefundDtos.CreateRefundRequest request
    );

    /** Lấy danh sách yêu cầu hoàn tiền cho Admin / User (FR-A12, FR-O25). */
    @GET("refunds")
    Call<ResponseWrapper.Envelope<List<RefundDtos.RefundResponse>>> getRefunds();

    /** Organizer xem danh sách yêu cầu hoàn tiền thuộc sự kiện mình tổ chức (FR-O25). */
    @GET("organizer/refunds")
    Call<ResponseWrapper.Envelope<List<RefundDtos.RefundResponse>>> getOrganizerRefunds(
            @Query("eventId") Long eventId
    );

    /** Admin duyệt hoặc từ chối hoàn tiền (FR-A12). */
    @POST("refunds/{refundId}/process")
    Call<ResponseWrapper.Envelope<RefundDtos.RefundResponse>> processRefund(
            @Path("refundId") long refundId,
            @Body RefundDtos.ProcessRefundRequest request
    );
}
