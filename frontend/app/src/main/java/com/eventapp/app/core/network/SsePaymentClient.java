package com.eventapp.app.core.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Client lắng nghe Server-Sent Events (SSE) thời gian thực bằng OkHttp thuần (KHÔNG cần thêm
 * dependency `okhttp-sse` vào `build.gradle`).
 *
 * <p>Dùng chung {@link RequestIdProvider} và {@link AuthInterceptor} (JWT) của app, đặt
 * {@code readTimeout = 0} để giữ mở kết nối {@code text/event-stream} chờ Backend push tín hiệu.</p>
 */
public final class SsePaymentClient {

    public interface Listener<T> {
        default void onConnected() {
        }

        void onEvent(@NonNull T payload);

        default void onDisconnected(@Nullable Throwable error) {
        }
    }

    public static final class Subscription {
        private volatile Call activeCall;
        private volatile BufferedSource activeSource;
        private volatile boolean closed;

        void attach(@NonNull Call call) {
            if (closed) {
                call.cancel();
            } else {
                this.activeCall = call;
            }
        }

        void attachSource(@NonNull BufferedSource source) {
            if (closed) {
                closeQuietly(source);
            } else {
                this.activeSource = source;
            }
        }

        public void close() {
            closed = true;
            Call call = activeCall;
            activeCall = null;
            if (call != null) {
                call.cancel();
            }
            BufferedSource source = activeSource;
            activeSource = null;
            if (source != null) {
                closeQuietly(source);
            }
        }

        private static void closeQuietly(@NonNull BufferedSource source) {
            try {
                source.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static volatile OkHttpClient sseClient;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SsePaymentClient() {
    }

    @NonNull
    private static OkHttpClient client() {
        OkHttpClient local = sseClient;
        if (local == null) {
            synchronized (SsePaymentClient.class) {
                local = sseClient;
                if (local == null) {
                    local = new OkHttpClient.Builder()
                            .connectTimeout(5000L, TimeUnit.MILLISECONDS)
                            .readTimeout(0L, TimeUnit.MILLISECONDS)
                            .retryOnConnectionFailure(true)
                            .addInterceptor(new RequestIdProvider())
                            .addInterceptor(new AuthInterceptor())
                            .build();
                    sseClient = local;
                }
            }
        }
        return local;
    }

    /**
     * Mở luồng SSE lắng nghe trạng thái giao dịch tại đường dẫn tương đối của {@code BASE_URL},
     * ví dụ {@code payments/123/events} hoặc {@code wallets/topups/456/events}.
     */
    @NonNull
    public static <T> Subscription subscribe(@NonNull String relativePath,
                                             @NonNull Class<T> payloadType,
                                             @NonNull Listener<T> listener) {
        String baseUrl = BuildConfig.BASE_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;

        Request request = new Request.Builder()
                .url(baseUrl + cleanPath)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build();

        Subscription subscription = new Subscription();
        Call call = client().newCall(request);
        subscription.attach(call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call c, @NonNull IOException e) {
                if (!subscription.closed) {
                    MAIN.post(() -> listener.onDisconnected(e));
                }
            }

            @Override
            public void onResponse(@NonNull Call c, @NonNull Response response) {
                try (Response res = response) {
                    ResponseBody body = res.body();
                    if (!res.isSuccessful() || body == null) {
                        if (!subscription.closed) {
                            MAIN.post(() -> listener.onDisconnected(
                                    new IOException("HTTP " + res.code())));
                        }
                        return;
                    }

                    if (!subscription.closed) {
                        MAIN.post(listener::onConnected);
                    }

                    BufferedSource source = body.source();
                    subscription.attachSource(source);

                    String eventType = "";
                    StringBuilder dataBuffer = new StringBuilder();
                    String line;
                    while (!subscription.closed && (line = source.readUtf8Line()) != null) {
                        if (line.isEmpty()) {
                            dispatchBufferedEvent(eventType, dataBuffer, payloadType, listener, subscription);
                            eventType = "";
                            dataBuffer.setLength(0);
                            continue;
                        }
                        if (line.startsWith(":")) {
                            // SSE comment / heartbeat
                            continue;
                        }
                        if (line.startsWith("event:")) {
                            eventType = line.substring(6).trim();
                        } else if (line.startsWith("data:")) {
                            String chunk = line.substring(5).trim();
                            if (dataBuffer.length() > 0) {
                                dataBuffer.append('\n');
                            }
                            dataBuffer.append(chunk);
                        }
                    }

                    // Flush gói cuối nếu server đóng stream ngay sau khi bắn data
                    dispatchBufferedEvent(eventType, dataBuffer, payloadType, listener, subscription);

                    if (!subscription.closed) {
                        MAIN.post(() -> listener.onDisconnected(null));
                    }
                } catch (Exception e) {
                    if (!subscription.closed) {
                        MAIN.post(() -> listener.onDisconnected(e));
                    }
                }
            }
        });

        return subscription;
    }

    private static <T> void dispatchBufferedEvent(@NonNull String eventType,
                                                  @NonNull StringBuilder dataBuffer,
                                                  @NonNull Class<T> payloadType,
                                                  @NonNull Listener<T> listener,
                                                  @NonNull Subscription subscription) {
        if (subscription.closed || dataBuffer.length() == 0) {
            return;
        }
        if ("ping".equalsIgnoreCase(eventType) || "heartbeat".equalsIgnoreCase(eventType)) {
            return;
        }
        String raw = dataBuffer.toString().trim();
        if (raw.isEmpty() || "connected".equalsIgnoreCase(raw) || "keepalive".equalsIgnoreCase(raw)) {
            return;
        }
        try {
            T parsed = ApiClient.gson().fromJson(raw, payloadType);
            if (parsed != null && !subscription.closed) {
                MAIN.post(() -> {
                    if (!subscription.closed) {
                        listener.onEvent(parsed);
                    }
                });
            }
        } catch (Exception ignored) {
            // Bỏ qua frame không phải JSON của DTO đích
        }
    }
}
