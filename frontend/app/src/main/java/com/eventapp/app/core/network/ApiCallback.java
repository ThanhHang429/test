package com.eventapp.app.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.common.AppLogger;
import com.eventapp.app.core.session.SessionManager;
import com.google.gson.JsonElement;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Cầu nối giữa {@link Call} của Retrofit và ViewModel.
 *
 * <p>Xử lý sẵn: bóc envelope {@code {success, data, meta}}, biến lỗi HTTP thành
 * {@link ApiException}, và tự xoá phiên khi backend trả 401.</p>
 *
 * <p>Callback luôn chạy trên main thread (Retrofit giao kết quả về main looper trên Android).</p>
 */
public abstract class ApiCallback<T> {

    /** Gọi khi {@code success = true}. {@code meta} có thể null. */
    public abstract void onSuccess(@Nullable T data, @Nullable JsonElement meta);

    /** Gọi với mọi lỗi: mạng, timeout, 4xx, 5xx. */
    public abstract void onError(@NonNull ApiException error);

    /** Hook tuỳ chọn — chạy trước khi gửi request (bật loading). */
    public void onStart() {
    }

    /** Hook tuỳ chọn — luôn chạy sau cùng, kể cả khi lỗi (tắt loading). */
    public void onFinish() {
    }

    /** Gửi request và tự bóc envelope. */
    public static <T> void enqueue(@NonNull Call<ResponseWrapper.Envelope<T>> call,
                                   @NonNull ApiCallback<T> callback) {
        callback.onStart();
        call.enqueue(new retrofit2.Callback<ResponseWrapper.Envelope<T>>() {
            @Override
            public void onResponse(@NonNull Call<ResponseWrapper.Envelope<T>> ignored,
                                   @NonNull Response<ResponseWrapper.Envelope<T>> response) {
                try {
                    ResponseWrapper.Envelope<T> body = response.body();
                    if (response.isSuccessful() && body != null && body.success) {
                        callback.onSuccess(body.data, body.meta);
                        return;
                    }
                    ApiException error = ApiException.from(response);
                    if (error.isUnauthenticated()) {
                        SessionManager.get().clear();
                    }
                    callback.onError(error);
                } catch (RuntimeException unexpected) {
                    AppLogger.e("Lỗi khi đọc phản hồi", unexpected);
                    callback.onError(ApiException.malformed(unexpected));
                } finally {
                    callback.onFinish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseWrapper.Envelope<T>> ignored,
                                  @NonNull Throwable throwable) {
                try {
                    callback.onError(ApiException.network(throwable));
                } finally {
                    callback.onFinish();
                }
            }
        });
    }

    /**
     * Callback bọc để biến đổi dữ liệu trả về nhưng vẫn giữ nguyên vòng đời của callback gốc.
     *
     * <p>Repository thường bọc {@code List<T>} thành {@code Paged<T>}; nếu không chuyển tiếp
     * {@link #onStart()} / {@link #onFinish()} thì màn hình bật loading mà không bao giờ tắt.</p>
     */
    public abstract static class Delegate<T> extends ApiCallback<T> {

        private final ApiCallback<?> target;

        protected Delegate(@NonNull ApiCallback<?> target) {
            this.target = target;
        }

        @Override
        public void onStart() {
            target.onStart();
        }

        @Override
        public void onFinish() {
            target.onFinish();
        }
    }

    /** Gửi request không cần đọc dữ liệu trả về (DELETE, một số POST). */
    public static void enqueueVoid(@NonNull Call<ResponseWrapper.Envelope<Object>> call,
                                   @NonNull ApiCallback<Object> callback) {
        enqueue(call, callback);
    }

    /** Huỷ an toàn. */
    public static void cancel(@Nullable Call<?> call) {
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    /** Tiện ích: chạy đồng bộ cho trường hợp hiếm (không dùng trên main thread). */
    @Nullable
    public static <T> T execute(@NonNull Call<ResponseWrapper.Envelope<T>> call) throws IOException {
        Response<ResponseWrapper.Envelope<T>> response = call.execute();
        if (!response.isSuccessful()) {
            throw ApiException.from(response);
        }
        ResponseWrapper.Envelope<T> body = response.body();
        return body == null ? null : body.data;
    }
}
