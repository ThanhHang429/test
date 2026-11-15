package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.WalletApi;
import com.eventapp.app.data.remote.dto.payment.DevWebhookRequest;
import com.eventapp.app.data.remote.dto.wallet.PayWalletRequest;
import com.eventapp.app.data.remote.dto.wallet.SetPinRequest;
import com.eventapp.app.data.remote.dto.wallet.TopupIntentResponse;
import com.eventapp.app.data.remote.dto.wallet.TopupRequest;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.data.remote.dto.wallet.WalletTransactionDto;

import java.util.List;
import java.util.UUID;

/**
 * Repository tầng dữ liệu cho Ví ETicket.
 * Hỗ trợ luồng TopupIntent + Webhook + SSE, PIN bảo mật trên Backend và thanh toán bằng số dư ví.
 */
public class WalletRepository {

    private final WalletApi api;

    public WalletRepository(@NonNull WalletApi api) {
        this.api = api;
    }

    /** Lấy thông tin ví và số dư */
    public void getMyWallet(@NonNull ApiCallback<WalletResponse> callback) {
        ApiCallback.enqueue(api.getMyWallet(), callback);
    }

    /** Tạo ví mới */
    public void createWallet(@NonNull ApiCallback<WalletResponse> callback) {
        ApiCallback.enqueue(api.createWallet(), callback);
    }

    /** Thiết lập hoặc đổi mã PIN ví 6 số trên Backend */
    public void setPin(String oldPin, @NonNull String newPin,
                       @NonNull ApiCallback<WalletResponse> callback) {
        ApiCallback.enqueue(api.setPin(new SetPinRequest(oldPin, newPin)), callback);
    }

    /** Nạp tiền vào ví trực tiếp (cho thẻ quốc tế đã xác thực 3DS) */
    public void topup(double amount, @NonNull String paymentMethod, String note,
                      @NonNull ApiCallback<WalletResponse> callback) {
        ApiCallback.enqueue(api.topup(new TopupRequest(amount, paymentMethod, note)), callback);
    }

    /** Tạo yêu cầu nạp ví VietQR (PENDING) chờ Webhook ngân hàng đối soát */
    public void createTopupIntent(double amount, @NonNull String paymentMethod, String note,
                                  @NonNull ApiCallback<TopupIntentResponse> callback) {
        ApiCallback.enqueue(api.createTopupIntent(new TopupRequest(amount, paymentMethod, note)), callback);
    }

    /** Lấy trạng thái yêu cầu nạp ví (Fallback Polling / onResume) */
    public void getTopupIntent(long topupId, @NonNull ApiCallback<TopupIntentResponse> callback) {
        ApiCallback.enqueue(api.getTopupIntent(topupId), callback);
    }

    /** Giả lập Webhook ngân hàng cho giao dịch nạp ví ở môi trường Dev */
    public void simulateTopupWebhook(@NonNull String transferContent, long amount,
                                     @NonNull ApiCallback<TopupIntentResponse> callback) {
        String providerTxId = "SIMNAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DevWebhookRequest req =
                new DevWebhookRequest(transferContent, amount, providerTxId);
        ApiCallback.enqueue(api.simulateTopupWebhook(req), callback);
    }

    /** Thanh toán đơn hàng bằng ví */
    public void pay(long orderId, @NonNull ApiCallback<WalletTransactionDto> callback) {
        ApiCallback.enqueue(api.pay(new PayWalletRequest(orderId)), callback);
    }

    /** Thanh toán vé bằng số dư ví (không truyền PIN - legacy) */
    public void pay(Long orderId, double amount, String note, @NonNull ApiCallback<WalletTransactionDto> callback) {
        ApiCallback.enqueue(api.pay(new PayWalletRequest(orderId, amount, note)), callback);
    }

    /** Thanh toán vé bằng số dư ví kèm mã PIN 6 số xác thực trên Backend */
    public void pay(Long orderId, double amount, String note, @NonNull String pin,
                    @NonNull ApiCallback<WalletTransactionDto> callback) {
        ApiCallback.enqueue(api.pay(new PayWalletRequest(orderId, amount, note, pin)), callback);
    }

    /** Lấy lịch sử biến động số dư */
    public void getTransactions(@NonNull ApiCallback<List<WalletTransactionDto>> callback) {
        ApiCallback.enqueue(api.getTransactions(), callback);
    }
}
