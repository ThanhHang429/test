package com.eventapp.app.data.remote.dto.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** DTO đơn hàng — khớp `CreateOrderRequest` / `OrderDto` của backend. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public static class CreateItem {
        public long ticketTypeId;
        public int quantity;

        public CreateItem() {
        }

        public CreateItem(long ticketTypeId, int quantity) {
            this.ticketTypeId = ticketTypeId;
            this.quantity = quantity;
        }
    }

    public static class CreateRequest {
        public long eventId;
        public List<CreateItem> items = new ArrayList<>();
        public String customerNote;
        public String idempotencyKey;
        public Long couponId;
        public String couponCode;
    }

    public static class Item {
        public long orderItemId;
        public long ticketTypeId;
        public String ticketName;
        public int quantity;
        public BigDecimal unitPrice;
        public BigDecimal lineTotal;
    }

    public static class PaymentAttempt {
        public long paymentId;
        public String paymentReference;
        public String providerCode;
        public String method;
        public BigDecimal amount;
        public String currency;
        /** INITIATED | PENDING | SUCCESS | FAILED | REVERSED */
        public String status;
        public String failureCode;
        public String failureMessage;
        public String verificationSource;
        public String initiatedAt;
        public String verifiedAt;
        public String failedAt;
    }

    public static class Order {
        public long orderId;
        public String orderCode;
        /** PENDING_PAYMENT | PAID | EXPIRED | CANCELLED | REFUND_PENDING | PARTIALLY_REFUNDED | REFUNDED */
        public String status;
        public long eventId;
        public String eventTitle;
        public String eventStartAt;
        public String eventEndAt;
        public String eventCoverUrl;
        public String venueName;
        public String city;
        public String currency;
        public BigDecimal subtotalAmount;
        public BigDecimal discountAmount;
        public BigDecimal platformFeeAmount;
        public BigDecimal totalAmount;
        public String paymentDueAt;
        public String paidAt;
        public String createdAt;
        public String customerNote;
        public String buyerName;
        public String buyerEmail;
        public String paymentMethod;
        /** Trạng thái giao dịch thanh toán tách biệt với trạng thái đơn hàng (FR-U16) */
        public String paymentStatus;
        public String ticketSummary;
        public List<Item> items = new ArrayList<>();
        public List<PaymentAttempt> payments = new ArrayList<>();
        public int ticketsIssued;
    }
}
