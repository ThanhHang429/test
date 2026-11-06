package com.eventapp.app.core.network;

import androidx.annotation.NonNull;

import com.eventapp.app.core.session.SessionManager;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Gắn {@code Authorization: Bearer <jwt>} cho mọi request khi đã có phiên đăng nhập.
 *
 * <p>Token do {@link SessionManager} giữ trong bộ nhớ — không ghi xuống đĩa, nên tắt app là
 * phải chọn lại vai trò (yêu cầu bỏ "remember").</p>
 */
public final class AuthInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws java.io.IOException {
        Request request = chain.request();
        String token = SessionManager.get().getToken();
        if (token == null || token.isEmpty() || request.header("Authorization") != null) {
            return chain.proceed(request);
        }
        return chain.proceed(request.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build());
    }
}
