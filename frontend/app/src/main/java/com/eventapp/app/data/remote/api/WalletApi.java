package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.payment.DevWebhookRequest;
import com.eventapp.app.data.remote.dto.wallet.PayWalletRequest;
import com.eventapp.app.data.remote.dto.wallet.SetPinRequest;
import com.eventapp.app.data.remote.dto.wallet.TopupIntentResponse;
import com.eventapp.app.data.remote.dto.wallet.TopupRequest;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletTransactionDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** API Quản lý Ví ETicket, mã PIN bảo mật & Nạp tiền qua Webhook kết nối tới Backend Spring Boot. */
public interface WalletApi {

    /** Lấy thông tin ví và số dư của người dùng hiện tại */
    @GET("wallets/me")
    Call<ResponseWrapper.Envelope<WalletResponse>> getMyWallet();

    /** Kích hoạt / Tạo ví mới (số dư ban đầu 0 đ) */
    @POST("wallets")
    Call<ResponseWrapper.Envelope<WalletResponse>> createWallet();

    /** Thiết lập hoặc đổi mã PIN 6 số lưu băm BCrypt trên Backend (FR-U13) */
    @POST("wallets/pin")
    Call<ResponseWrapper.Envelope<WalletResponse>> setPin(@Body SetPinRequest request);

    /** Nạp tiền trực tiếp (dành cho Thẻ quốc tế đã xác thực hoặc chế độ cũ) */
    @POST("wallets/topup")
    Call<ResponseWrapper.Envelope<WalletResponse>> topup(@Body TopupRequest request);

    /** Tạo yêu cầu nạp ví VietQR (TopupIntent PENDING) kèm nội dung CK ETNAP... để chờ Webhook + SSE */
    @POST("wallets/topups")
    Call<ResponseWrapper.Envelope<TopupIntentResponse>> createTopupIntent(@Body TopupRequest request);

    /** Kiểm tra trạng thái yêu cầu nạp ví (Fallback Polling / onResume) */
    @GET("wallets/topups/{topupId}")
    Call<ResponseWrapper.Envelope<TopupIntentResponse>> getTopupIntent(@Path("topupId") long topupId);

    /** Giả lập Webhook ngân hàng cho giao dịch nạp ví ở môi trường Dev */
    @POST("webhooks/payments/dev-simulate")
    Call<ResponseWrapper.Envelope<TopupIntentResponse>> simulateTopupWebhook(
            @Body DevWebhookRequest request
    );

    /** Thanh toán đơn hàng bằng số dư Ví ETicket (có xác thực mã PIN trên Backend) */
    @POST("wallets/pay")
    Call<ResponseWrapper.Envelope<WalletTransactionDto>> pay(@Body PayWalletRequest request);

    /** Lấy danh sách lịch sử biến động số dư ví */
    @GET("wallets/transactions")
    Call<ResponseWrapper.Envelope<List<WalletTransactionDto>>> getTransactions();
}
