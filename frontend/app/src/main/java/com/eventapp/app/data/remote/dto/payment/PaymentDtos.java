package com.eventapp.app.data.remote.dto.payment;

import java.math.BigDecimal;

/** DTO thanh toán — khớp `PaymentDto` của backend (có bổ sung trường thực tế tương thích ngược). */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    public static class CreateRequest {
        /** BANK_TRANSFER_QR | WALLET | CARD | OTHER */
        public String method;

        public CreateRequest() {
        }

        public CreateRequest(String method) {
            this.method = method;
        }
    }

    public static class Payment {
        public long paymentId;
        public String paymentReference;
        public long orderId;
        public String orderCode;
        public String providerCode;
        public String method;
        public BigDecimal amount;
        public String currency;
        /** INITIATED | PENDING | SUCCESS | FAILED | REVERSED */
        public String status;
        public String qrPayload;
        public String transferContent;
        public String bankCode;
        public String bankAccount;
        public String expiresAt;
        public String initiatedAt;
        public String verifiedAt;

        // 3 trường nullable bổ sung chuẩn Production (tương thích ngược với backend cũ)
        public String accountName;
        public String providerTransactionId;
        public String failureMessage;
    }
}
