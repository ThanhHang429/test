package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Lịch sử biến động số dư trong ví (Nạp tiền, Mua vé, Hoàn tiền). */
public class WalletTransactionDto {
    @SerializedName("walletTransactionId")
    public long walletTransactionId;

    @SerializedName("walletId")
    public long walletId;

    @SerializedName("transactionType")
    public String transactionType;

    @SerializedName("direction")
    public String direction;

    @SerializedName("amount")
    public double amount;

    @SerializedName("balanceAfter")
    public double balanceAfter;

    @SerializedName("referenceType")
    public String referenceType;

    @SerializedName("referenceId")
    public String referenceId;

    @SerializedName("status")
    public String status;

    @SerializedName("note")
    public String note;

    @SerializedName("createdAt")
    public String createdAt;
}
