package com.eventapp.app.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.common.AppLogger;

import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Gắn {@code X-Correlation-Id} cho mỗi request để đối chiếu với log backend
 * ({@code ai_requests.correlation_id}, {@code audit_logs.correlation_id}).
 */
public final class RequestIdProvider implements Interceptor {

    public static final String HEADER = "X-Correlation-Id";

    /** Sinh mã mới cho một request. */
    @NonNull
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws java.io.IOException {
        Request request = chain.request();
        String id = request.header(HEADER);
        if (id == null || id.isEmpty()) {
            id = newId();
            request = request.newBuilder().header(HEADER, id).build();
        }
        AppLogger.d("→ " + request.method() + " " + request.url().encodedPath() + " [" + id + "]");
        return chain.proceed(request);
    }

    /** Correlation id của phản hồi — dùng khi cần đối chiếu lỗi. */
    @Nullable
    public static String fromResponse(@Nullable Response response) {
        return response == null ? null : response.header(HEADER);
    }
}
