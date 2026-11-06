package com.eventapp.app.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

/**
 * Lỗi API đã chuẩn hoá: giữ cả HTTP status, {@code error.code} của backend và correlationId
 * để màn hình vừa hiển thị được thông điệp, vừa phân nhánh được theo mã lỗi.
 */
public class ApiException extends RuntimeException {

    /** Mã lỗi phía client, dùng khi không có phản hồi từ server. */
    public static final String CODE_NETWORK = "NETWORK_ERROR";
    public static final String CODE_TIMEOUT = "TIMEOUT";
    public static final String CODE_MALFORMED = "MALFORMED_RESPONSE";
    public static final String CODE_UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String CODE_FORBIDDEN = "FORBIDDEN";
    public static final String CODE_NOT_FOUND = "NOT_FOUND";
    public static final String CODE_CONFLICT = "CONFLICT";
    public static final String CODE_RATE_LIMITED = "RATE_LIMITED";
    public static final String CODE_SERVER_ERROR = "SERVER_ERROR";

    private static final Gson GSON = new Gson();

    private final int httpCode;
    private final String errorCode;
    private final List<String> details;
    private final String correlationId;
    private final JsonObject extra;

    private ApiException(int httpCode,
                         @NonNull String errorCode,
                         @Nullable String message,
                         @Nullable List<String> details,
                         @Nullable String correlationId,
                         @Nullable JsonObject extra,
                         @Nullable Throwable cause) {
        super(message == null || message.isEmpty() ? defaultMessage(errorCode, httpCode) : message, cause);
        this.httpCode = httpCode;
        this.errorCode = errorCode;
        this.details = details == null ? Collections.emptyList() : details;
        this.correlationId = correlationId;
        this.extra = extra;
    }

    // ------------------------------------------------------------------ factory

    /** Dựng ngoại lệ từ phản hồi lỗi của backend. */
    public static ApiException from(@NonNull Response<?> response) {
        int code = response.code();
        String raw = readErrorBody(response);
        ResponseWrapper.ErrorBody body = null;
        if (raw != null) {
            try {
                body = GSON.fromJson(raw, ResponseWrapper.ErrorBody.class);
            } catch (RuntimeException ignored) {
                // Body không đúng khuôn JSON — rơi xuống nhánh mặc định bên dưới.
            }
        }
        if (body != null && body.error != null && body.error.code != null) {
            return new ApiException(code, body.error.code, body.error.message,
                    ResponseWrapper.detailMessages(body.error.details),
                    body.correlationId != null ? body.correlationId : response.headers().get("X-Correlation-Id"),
                    body.error.extra, null);
        }
        return new ApiException(code, codeForStatus(code), null, null,
                response.headers().get("X-Correlation-Id"), null, null);
    }

    public static ApiException network(@NonNull Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return new ApiException(0, CODE_TIMEOUT,
                    "Máy chủ phản hồi quá chậm, vui lòng thử lại.", null, null, null, throwable);
        }
        if (throwable instanceof UnknownHostException) {
            return new ApiException(0, CODE_NETWORK,
                    "Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa.", null, null, null, throwable);
        }
        if (throwable instanceof IOException) {
            return new ApiException(0, CODE_NETWORK,
                    "Mất kết nối mạng, vui lòng thử lại.", null, null, null, throwable);
        }
        return new ApiException(0, CODE_NETWORK, throwable.getMessage(), null, null, null, throwable);
    }

    public static ApiException malformed(@Nullable Throwable cause) {
        return new ApiException(0, CODE_MALFORMED,
                "Phản hồi từ máy chủ không đúng định dạng.", null, null, null, cause);
    }

    /** Lỗi do phía app tự phát hiện (ví dụ không tìm thấy tài khoản phù hợp). */
    public static ApiException of(int httpCode, @NonNull String errorCode, @Nullable String message) {
        return new ApiException(httpCode, errorCode, message, null, null, null, null);
    }

    // ------------------------------------------------------------------ accessor

    public int getHttpCode() {
        return httpCode;
    }

    @NonNull
    public String getErrorCode() {
        return errorCode;
    }

    @NonNull
    public List<String> getDetails() {
        return details;
    }

    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    @Nullable
    public JsonObject getExtra() {
        return extra;
    }

    /** Số giây phải chờ khi bị giới hạn tần suất (AI quota), -1 nếu không có. */
    public long getRetryAfterSeconds() {
        return ResponseWrapper.extraLong(extra, "retryAfterSeconds", -1L);
    }

    public boolean isUnauthenticated() {
        return httpCode == 401;
    }

    public boolean isForbidden() {
        return httpCode == 403;
    }

    public boolean isNotFound() {
        return httpCode == 404;
    }

    public boolean isConflict() {
        return httpCode == 409;
    }

    public boolean isValidation() {
        return httpCode == 400 || "VALIDATION_ERROR".equals(errorCode);
    }

    public boolean isRateLimited() {
        return httpCode == 429;
    }

    public boolean isNetworkIssue() {
        return httpCode == 0;
    }

    /** Thông điệp đầy đủ để hiển thị trên ErrorStateView / Snackbar. */
    @NonNull
    public String getUserMessage() {
        String base = getMessage() == null ? "" : getMessage();
        if (!details.isEmpty()) {
            return base + " (" + details.get(0) + ")";
        }
        return base;
    }

    // ------------------------------------------------------------------ helper

    @Nullable
    private static String readErrorBody(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return response.errorBody().string();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @NonNull
    private static String codeForStatus(int httpCode) {
        switch (httpCode) {
            case 400:
                return "VALIDATION_ERROR";
            case 401:
                return CODE_UNAUTHENTICATED;
            case 403:
                return CODE_FORBIDDEN;
            case 404:
                return CODE_NOT_FOUND;
            case 409:
                return CODE_CONFLICT;
            case 429:
                return CODE_RATE_LIMITED;
            default:
                return httpCode >= 500 ? CODE_SERVER_ERROR : CODE_NETWORK;
        }
    }

    @NonNull
    private static String defaultMessage(@NonNull String errorCode, int httpCode) {
        switch (errorCode) {
            case CODE_UNAUTHENTICATED:
                return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";
            case CODE_FORBIDDEN:
                return "Bạn không có quyền thực hiện thao tác này.";
            case CODE_NOT_FOUND:
                return "Không tìm thấy dữ liệu.";
            case CODE_CONFLICT:
                return "Dữ liệu đã thay đổi, vui lòng tải lại.";
            case CODE_RATE_LIMITED:
                return "Bạn thao tác quá nhanh, vui lòng thử lại sau.";
            case CODE_TIMEOUT:
                return "Máy chủ phản hồi quá chậm, vui lòng thử lại.";
            case CODE_NETWORK:
                return "Không kết nối được máy chủ.";
            case CODE_MALFORMED:
                return "Phản hồi từ máy chủ không đúng định dạng.";
            default:
                return httpCode >= 500
                        ? "Máy chủ đang gặp sự cố, vui lòng thử lại sau."
                        : "Đã xảy ra lỗi, vui lòng thử lại.";
        }
    }
}
