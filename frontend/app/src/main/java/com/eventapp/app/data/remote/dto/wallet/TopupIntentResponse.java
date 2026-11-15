package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Yêu cầu nạp tiền vào Ví ETicket chờ đối soát qua Webhook + SSE. */
public class TopupIntentResponse {
    @SerializedName("topupId")
    public long topupId;

    @SerializedName("topupCode")
    public String topupCode;

    @SerializedName("amount")
    public double amount;

    @SerializedName("currency")
    public String currency;

    @SerializedName("paymentMethod")
    public String paymentMethod;

    /** PENDING | SUCCESS | EXPIRED | FAILED */
    @SerializedName("status")
    public String status;

    @SerializedName("bankCode")
    public String bankCode;

    @SerializedName("bankAccount")
    public String bankAccount;

    @SerializedName("accountName")
    public String accountName;

    @SerializedName("transferContent")
    public String transferContent;

    @SerializedName("qrPayload")
    public String qrPayload;

    @SerializedName("expiresAt")
    public String expiresAt;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("completedAt")
    public String completedAt;

    @SerializedName("providerTransactionId")
    public String providerTransactionId;

    @SerializedName("failureMessage")
    public String failureMessage;
}
