package com.eventapp.app.core.network;

import androidx.annotation.NonNull;

import com.eventapp.app.BuildConfig;

import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Log HTTP chỉ bật ở bản debug; bản release không ghi gì.
 *
 * <p>Dùng {@code BASIC} (method, URL, status, thời gian) thay vì {@code BODY}: ghi toàn bộ body
 * JSON của mọi request làm chậm đáng kể lúc tải danh sách lớn trên bản debug. Cần xem body thì
 * đổi tạm sang {@code BODY} ở đây.</p>
 */
public final class HttpLogger {

    private HttpLogger() {
    }

    @NonNull
    public static HttpLoggingInterceptor create() {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor(
                message -> com.eventapp.app.core.common.AppLogger.d(message));
        logger.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);
        return logger;
    }
}
