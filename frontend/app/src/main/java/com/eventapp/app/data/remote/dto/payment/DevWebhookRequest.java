package com.eventapp.app.data.remote.dto.payment;

import java.math.BigDecimal;

/**
 * Payload gửi tới endpoint giả lập Webhook ngân hàng (`dev` profile) khi test Local/Ngrok.
 * Tách thành file riêng để giữ nguyên vẹn cấu trúc `PaymentDtos` dùng chung của nhóm.
 */
public class DevWebhookRequest {
    public String transferContent;
    public BigDecimal amount;
    public String providerCode;
    public String providerTransactionId;
    public DevWebhookRequest() {
    }
    public DevWebhookRequest(String transferContent, long amount, String providerTransactionId) {
        this.transferContent = transferContent;
        this.amount = BigDecimal.valueOf(amount);
        this.providerCode = "SEPAY";
        this.providerTransactionId = providerTransactionId;
    }
}
