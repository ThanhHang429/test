package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Yêu cầu thanh toán đơn hàng bằng số dư Ví ETicket. */
public class PayWalletRequest {
    @SerializedName("orderId")
    public Long orderId;

    @SerializedName("amount")
    public Double amount;

    @SerializedName("note")
    public String note;

    @SerializedName("pin")
    public String pin;

    public PayWalletRequest() {}

    public PayWalletRequest(Long orderId) {
        this.orderId = orderId;
    }

    public PayWalletRequest(Long orderId, Double amount, String note) {
        this.orderId = orderId;
        this.amount = amount;
        this.note = note;
    }

    public PayWalletRequest(Long orderId, Double amount, String note, String pin) {
        this.orderId = orderId;
        this.amount = amount;
        this.note = note;
        this.pin = pin;
    }
}
