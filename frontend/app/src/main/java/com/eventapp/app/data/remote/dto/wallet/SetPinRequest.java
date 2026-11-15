package com.eventapp.app.data.remote.dto.wallet;

import com.google.gson.annotations.SerializedName;

/** Yêu cầu thiết lập hoặc đổi mã PIN 6 số cho Ví ETicket trên Backend. */
public class SetPinRequest {
    @SerializedName("oldPin")
    public String oldPin;

    @SerializedName("newPin")
    public String newPin;

    public SetPinRequest() {}

    public SetPinRequest(String oldPin, String newPin) {
        this.oldPin = oldPin;
        this.newPin = newPin;
    }
}
