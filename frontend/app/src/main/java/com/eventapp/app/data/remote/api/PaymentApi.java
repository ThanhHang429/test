package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.payment.DevWebhookRequest;
import com.eventapp.app.data.remote.dto.payment.PaymentDtos;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API Thanh toán đơn hàng & Webhook Dev Test (FR-U11):
 * Khớp với PaymentController & PaymentWebhookController ở Backend Spring Boot.
 */
public interface PaymentApi {

    /** Khởi tạo giao dịch thanh toán cho một đơn hàng đang PENDING_PAYMENT. */
    @POST("orders/{orderId}/payments")
    Call<ResponseWrapper.Envelope<PaymentDtos.Payment>> initiate(
            @Path("orderId") long orderId,
            @Body PaymentDtos.CreateRequest request
    );

    /** Lấy trạng thái giao dịch thanh toán (dùng cho Fallback Polling / onResume). */
    @GET("payments/{paymentId}")
    Call<ResponseWrapper.Envelope<PaymentDtos.Payment>> get(@Path("paymentId") long paymentId);

    /** Xác nhận thanh toán thẻ (luồng thẻ hoặc mô phỏng dev). */
    @POST("payments/{paymentId}/confirm")
    Call<ResponseWrapper.Envelope<PaymentDtos.Payment>> confirm(@Path("paymentId") long paymentId);

    /** Giả lập Webhook ngân hàng (SePay/PayOS) ở môi trường Dev để test luồng Webhook + SSE thật. */
    @POST("webhooks/payments/dev-simulate")
    Call<ResponseWrapper.Envelope<PaymentDtos.Payment>> simulateWebhook(
            @Body DevWebhookRequest request
    );
}
