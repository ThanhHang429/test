package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.RefundApi;
import com.eventapp.app.data.remote.dto.refund.RefundDtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository xử lý hoàn tiền (FR-O25, FR-A12) — gọi trực tiếp Backend thật qua {@link RefundApi}.
 */
public class RefundRepository {

    private final RefundApi api;

    public RefundRepository(@NonNull RefundApi api) {
        this.api = api;
    }

    /** Khách hàng gửi yêu cầu hoàn tiền cho đơn hàng đã thanh toán (FR-O25). */
    public void requestRefund(long orderId, String orderCode, String eventTitle, BigDecimal amount, String reason,
                              @NonNull ApiCallback<RefundDtos.RefundResponse> callback) {
        RefundDtos.CreateRefundRequest req = new RefundDtos.CreateRefundRequest(orderId, reason);
        ApiCallback.enqueue(api.requestRefund(req), callback);
    }

    /** Lấy danh sách yêu cầu hoàn tiền từ Backend cho Admin / User (FR-A12, FR-O25). */
    public void getRefunds(@NonNull ApiCallback<List<RefundDtos.RefundResponse>> callback) {
        ApiCallback.enqueue(api.getRefunds(), callback);
    }

    /** Lấy danh sách yêu cầu hoàn tiền thuộc sự kiện do Organizer tổ chức (FR-O25). */
    public void getOrganizerRefunds(@Nullable Long eventId,
                                    @NonNull ApiCallback<List<RefundDtos.RefundResponse>> callback) {
        ApiCallback.enqueue(api.getOrganizerRefunds(eventId), callback);
    }

    /** Admin duyệt (hoàn tiền vào Ví ETicket) hoặc từ chối yêu cầu hoàn tiền (FR-A12). */
    public void processRefund(long refundId, boolean approve, String rejectReason,
                              @NonNull ApiCallback<RefundDtos.RefundResponse> callback) {
        RefundDtos.ProcessRefundRequest req = new RefundDtos.ProcessRefundRequest(approve, rejectReason);
        ApiCallback.enqueue(api.processRefund(refundId, req), callback);
    }
}
