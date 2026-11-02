package com.eventapp.app.core.network;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Khuôn phản hồi của backend (SRS §3.2–3.3):
 *
 * <pre>
 * { "success": true,  "data": {...}, "meta": {...}, "correlationId": "..." }
 * { "success": false, "error": { "code": "...", "message": "...", "details": [], "extra": {} },
 *   "correlationId": "..." }
 * </pre>
 */
public final class ResponseWrapper {

    private ResponseWrapper() {
    }

    /** Thân phản hồi thành công. */
    public static final class Envelope<T> {
        public boolean success;
        public T data;
        public JsonElement meta;
        public String correlationId;
    }

    /** Thân phản hồi lỗi — dùng khi HTTP status là 4xx/5xx. */
    public static final class ErrorBody {
        public boolean success;
        public ApiError error;
        public String correlationId;
    }

    public static final class ApiError {
        public String code;
        public String message;
        public List<FieldDetail> details;
        public JsonObject extra;
    }

    /** Một lỗi ở cấp trường, ví dụ {@code {"field":"rating","message":"..."}}. */
    public static final class FieldDetail {
        public String field;
        public String message;
    }

    /** {@code meta} của các API phân trang. */
    public static final class PageMeta {
        public int page;
        public int size;
        public long totalItems;
        public boolean hasNext;
    }

    @Nullable
    public static PageMeta asPageMeta(@Nullable JsonElement meta) {
        if (meta == null || !meta.isJsonObject()) {
            return null;
        }
        JsonObject object = meta.getAsJsonObject();
        PageMeta page = new PageMeta();
        page.page = optInt(object, "page", 0);
        page.size = optInt(object, "size", 0);
        page.totalItems = optLong(object, "totalItems", 0L);
        page.hasNext = object.has("hasNext") && !object.get("hasNext").isJsonNull()
                && object.get("hasNext").getAsBoolean();
        return page;
    }

    /** Danh sách thông điệp lỗi theo trường, đã gộp thành chuỗi để hiển thị. */
    public static List<String> detailMessages(@Nullable List<FieldDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> messages = new ArrayList<>(details.size());
        for (FieldDetail detail : details) {
            if (detail == null || detail.message == null) {
                continue;
            }
            messages.add(detail.field == null ? detail.message : detail.field + ": " + detail.message);
        }
        return messages;
    }

    /** Đọc một khoá số trong {@code error.extra} (ví dụ {@code retryAfterSeconds}). */
    public static long extraLong(@Nullable JsonObject extra, String key, long fallback) {
        if (extra == null || !extra.has(key) || extra.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return extra.get(key).getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    @Nullable
    public static JsonArray extraArray(@Nullable JsonObject extra, String key) {
        if (extra == null || !extra.has(key) || !extra.get(key).isJsonArray()) {
            return null;
        }
        return extra.getAsJsonArray(key);
    }

    private static int optInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long optLong(JsonObject object, String key, long fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
