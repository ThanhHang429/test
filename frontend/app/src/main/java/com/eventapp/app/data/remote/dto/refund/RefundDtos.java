package com.eventapp.app.data.remote.dto.refund;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO Quản lý và xử lý hoàn tiền (FR-O25, FR-A12).
 */
public class RefundDtos {

    public static class CreateRefundRequest implements Serializable {
        @SerializedName("orderId")
        public long orderId;

        @SerializedName("reason")
        public String reason;

        public CreateRefundRequest(long orderId, String reason) {
            this.orderId = orderId;
            this.reason = reason;
        }
    }

    public static class RefundResponse implements Serializable {
        @SerializedName("refundId")
        public Long refundId;

        @SerializedName("orderId")
        public Long orderId;

        @SerializedName("orderCode")
        public String orderCode;

        @SerializedName("eventTitle")
        public String eventTitle;

        @SerializedName("eventId")
        public Long eventId;

        @SerializedName("userId")
        public Long userId;

        @SerializedName("buyerName")
        public String buyerName;

        @SerializedName("amount")
        public BigDecimal amount;

        @SerializedName("reason")
        public String reason;

        @SerializedName("status")
        public String status; // PENDING | APPROVED | REJECTED

        @SerializedName("rejectReason")
        public String rejectReason;

        @SerializedName("processedBy")
        public Long processedBy;

        @SerializedName("walletBalanceAfter")
        public BigDecimal walletBalanceAfter;

        @SerializedName("createdAt")
        public String createdAt;

        @SerializedName("processedAt")
        public String processedAt;
    }

    public static class ProcessRefundRequest implements Serializable {
        @SerializedName("approved")
        public boolean approved;

        @SerializedName("rejectReason")
        public String rejectReason;

        public ProcessRefundRequest(boolean approved, String rejectReason) {
            this.approved = approved;
            this.rejectReason = rejectReason;
        }
    }
}
