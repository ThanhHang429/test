package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Thông tin ví của người dùng trả về từ API backend. */
public class WalletResponse {
    @SerializedName("walletId")
    public long walletId;

    @SerializedName("userId")
    public long userId;

    @SerializedName("currency")
    public String currency;

    @SerializedName("balance")
    public double balance;

    @SerializedName("status")
    public String status;

    @SerializedName("hasPin")
    public boolean hasPin;

    @SerializedName("createdAt")
    public String createdAt;

    @SerializedName("updatedAt")
    public String updatedAt;
}
