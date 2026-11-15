package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.OrderApi;
import com.eventapp.app.data.remote.api.PaymentApi;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.payment.DevWebhookRequest;
import com.eventapp.app.data.remote.dto.payment.PaymentDtos;

import java.util.List;
import java.util.UUID;

/**
 * Repository xử lý dữ liệu đơn hàng và thanh toán cho User và Organizer (FR-U10, FR-U11, FR-U16, FR-O17).
 */
public class OrderRepository {

    private final OrderApi api;
    private final PaymentApi paymentApi;

    public OrderRepository(@NonNull OrderApi api, @NonNull PaymentApi paymentApi) {
        this.api = api;
        this.paymentApi = paymentApi;
    }

    /** Lấy danh sách lịch sử đơn hàng của người dùng (FR-U16). */
    public void getMyOrders(@NonNull ApiCallback<List<OrderDtos.Order>> callback) {
        ApiCallback.enqueue(api.getMyOrders(), callback);
    }

    /** Lấy chi tiết đơn hàng (FR-U16). */
    public void getOrderDetail(long orderId, @NonNull ApiCallback<OrderDtos.Order> callback) {
        ApiCallback.enqueue(api.getOrderDetail(orderId), callback);
    }

    /** Tạo đơn hàng mới (FR-U10). */
    public void createOrder(@NonNull OrderDtos.CreateRequest request,
                            @NonNull ApiCallback<OrderDtos.Order> callback) {
        ApiCallback.enqueue(api.createOrder(request), callback);
    }

    /** Huỷ đơn hàng đang chờ thanh toán (trả lại tồn kho vé). */
    public void cancelOrder(long orderId, @NonNull ApiCallback<OrderDtos.Order> callback) {
        ApiCallback.enqueue(api.cancelOrder(orderId), callback);
    }

    /** Khởi tạo giao dịch thanh toán (FR-U11). */
    public void initiatePayment(long orderId, @NonNull PaymentDtos.CreateRequest request,
                                @NonNull ApiCallback<PaymentDtos.Payment> callback) {
        ApiCallback.enqueue(paymentApi.initiate(orderId, request), callback);
    }

    /** Kiểm tra trạng thái giao dịch thanh toán (Fallback Polling / onResume). */
    public void getPayment(long paymentId, @NonNull ApiCallback<PaymentDtos.Payment> callback) {
        ApiCallback.enqueue(paymentApi.get(paymentId), callback);
    }

    /** Xác nhận thanh toán cho luồng Thẻ quốc tế hoặc dev legacy (FR-U11). */
    public void confirmPayment(long paymentId, @NonNull ApiCallback<PaymentDtos.Payment> callback) {
        ApiCallback.enqueue(paymentApi.confirm(paymentId), callback);
    }

    /** Giả lập Webhook ngân hàng SePay/PayOS ở chế độ Dev để test toàn bộ pipeline Webhook + SSE. */
    public void simulateOrderWebhook(@NonNull String transferContent, long amount,
                                     @NonNull ApiCallback<PaymentDtos.Payment> callback) {
        String providerTxId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DevWebhookRequest req =
                new DevWebhookRequest(transferContent, amount, providerTxId);
        ApiCallback.enqueue(paymentApi.simulateWebhook(req), callback);
    }

    /** Lấy danh sách đơn hàng cho nhà tổ chức (FR-O17). */
    public void getOrganizerOrders(@NonNull ApiCallback<List<OrderDtos.Order>> callback) {
        getOrganizerOrders(null, null, callback);
    }

    /** Lấy danh sách đơn hàng cho nhà tổ chức có lọc trạng thái và từ khoá (FR-O17). */
    public void getOrganizerOrders(@Nullable String status,
                                   @Nullable String query,
                                   @NonNull ApiCallback<List<OrderDtos.Order>> callback) {
        ApiCallback.enqueue(api.getOrganizerOrders(status, query), callback);
    }

    /** Lấy chi tiết một đơn hàng của nhà tổ chức theo mã đơn (FR-O17). */
    public void getOrganizerOrderDetail(@NonNull String orderCode,
                                        @NonNull ApiCallback<OrderDtos.Order> callback) {
        ApiCallback.enqueue(api.getOrganizerOrderDetail(orderCode), callback);
    }
}
