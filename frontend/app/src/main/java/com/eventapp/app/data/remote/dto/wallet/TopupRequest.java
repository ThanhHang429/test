package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Yêu cầu nạp tiền vào ví qua VietQR hoặc Thẻ. */
public class TopupRequest {
    @SerializedName("amount")
    public double amount;

    @SerializedName("paymentMethod")
    public String paymentMethod;

    @SerializedName("note")
    public String note;

    public TopupRequest(double amount, String paymentMethod, String note) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.note = note;
    }
}
