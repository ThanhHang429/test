package com.eventapp.app.core.network;

import androidx.annotation.NonNull;

/** Callback cho các endpoint trả về ảnh nhị phân (ví dụ QR PNG). */
public interface ImageCallback {

    void onSuccess(@NonNull byte[] png);

    void onError();
}
