package com.eventapp.app.data.remote.dto.report;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO Báo cáo tài chính và hiệu quả sự kiện (FR-A16, FR-A17, FR-O26, FR-A10).
 */
public class ReportDtos {

    public static class AdminFinanceSummary implements Serializable {
        @SerializedName("totalGmv")
        public BigDecimal totalGmv;

        @SerializedName("platformFeeCollected")
        public BigDecimal platformFeeCollected;

        @SerializedName("platformFeeOutstanding")
        public BigDecimal platformFeeOutstanding;

        @SerializedName("platformFeeRate")
        public double platformFeeRate; // Ví dụ 5.0%

        @SerializedName("settlementCycle")
        public String settlementCycle;

        @SerializedName("totalOrders")
        public int totalOrders;

        @SerializedName("paidOrders")
        public int paidOrders;

        @SerializedName("totalEvents")
        public int totalEvents;

        @SerializedName("totalOrganizers")
        public int totalOrganizers;

        @SerializedName("reconciliations")
        public List<OrganizerReconItem> reconciliations;
    }

    public static class OrganizerReconItem implements Serializable {
        @SerializedName("organizerId")
        public long organizerId;

        @SerializedName("organizerName")
        public String organizerName;

        @SerializedName("gmv")
        public BigDecimal gmv;

        @SerializedName("fee")
        public BigDecimal fee;

        @SerializedName("status")
        public String status; // PAID | PENDING
    }

    public static class UpdateFeeRateRequest implements Serializable {
        @SerializedName("feeRate")
        public double feeRate;

        @SerializedName("effectiveFrom")
        public String effectiveFrom;

        @SerializedName("description")
        public String description;

        public UpdateFeeRateRequest(double feeRate) {
            this.feeRate = feeRate;
        }

        public UpdateFeeRateRequest(double feeRate, String effectiveFrom, String description) {
            this.feeRate = feeRate;
            this.effectiveFrom = effectiveFrom;
            this.description = description;
        }
    }

    /** FR-O26: Báo cáo tài chính chi tiết của một sự kiện dành cho Organizer. */
    public static class EventFinanceReport implements Serializable {
        @SerializedName("eventId")
        public long eventId;

        @SerializedName("eventTitle")
        public String eventTitle;

        @SerializedName("ticketsSold")
        public int ticketsSold;

        @SerializedName("capacityTotal")
        public int capacityTotal;

        @SerializedName("ticketsIssued")
        public int ticketsIssued;

        @SerializedName("checkedInCount")
        public int checkedInCount;

        @SerializedName("grossRevenue")
        public BigDecimal grossRevenue;

        @SerializedName("discountTotal")
        public BigDecimal discountTotal;

        @SerializedName("platformFeeRate")
        public double platformFeeRate;

        @SerializedName("platformFeeAmount")
        public BigDecimal platformFeeAmount;

        @SerializedName("refundedAmount")
        public BigDecimal refundedAmount;

        @SerializedName("netRevenue")
        public BigDecimal netRevenue;

        @SerializedName("paidOrdersCount")
        public int paidOrdersCount;

        @SerializedName("refundedOrdersCount")
        public int refundedOrdersCount;

        @SerializedName("pendingRefundsCount")
        public int pendingRefundsCount;
    }

    /** FR-A11: Nhật ký Webhook ngân hàng SePay / PayOS / Cổng thanh toán. */
    public static class WebhookLogItem implements Serializable {
        @SerializedName("logId")
        public long logId;

        @SerializedName("providerCode")
        public String providerCode;

        @SerializedName("eventType")
        public String eventType;

        @SerializedName("externalTransactionId")
        public String externalTransactionId;

        @SerializedName("transferContent")
        public String transferContent;

        @SerializedName("amount")
        public BigDecimal amount;

        @SerializedName("status")
        public String status;

        @SerializedName("errorMessage")
        public String errorMessage;

        @SerializedName("receivedAt")
        public String receivedAt;
    }
}
