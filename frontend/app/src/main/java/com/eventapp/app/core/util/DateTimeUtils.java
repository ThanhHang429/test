package com.eventapp.app.core.util;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Chuyển mốc thời gian ISO-8601 của backend sang mili giây.
 *
 * <p>Backend (Jackson) luôn trả dạng UTC có hậu tố {@code Z}, ví dụ
 * {@code 2026-11-01T05:51:14.433884743Z}. Phần giây lẻ tới nano nên chỉ lấy 19 ký tự đầu
 * ({@code yyyy-MM-dd'T'HH:mm:ss}) để parse — đủ chính xác cho hiển thị.</p>
 */
public final class DateTimeUtils {

    private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    /** Độ dài phần "yyyy-MM-ddTHH:mm:ss" — KHÔNG dùng độ dài pattern vì pattern còn 2 dấu nháy. */
    private static final int PARSE_LENGTH = 19;

    private static final ThreadLocal<SimpleDateFormat> FORMATTER = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };

    private DateTimeUtils() {
    }

    /** ISO-8601 → epoch millis; trả 0 nếu không parse được. */
    public static long toMillis(@Nullable String iso) {
        if (iso == null || iso.length() < PARSE_LENGTH) {
            return 0L;
        }
        String head = iso.substring(0, PARSE_LENGTH);
        try {
            Date date = FORMATTER.get().parse(head);
            return date == null ? 0L : date.getTime();
        } catch (ParseException | RuntimeException ignored) {
            return 0L;
        }
    }

    /** epoch millis → ISO-8601 UTC (dùng khi cần gửi lại cho backend). */
    @Nullable
    public static String toIso(long millis) {
        if (millis <= 0) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(PATTERN, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis)) + "Z";
    }

    public static boolean isPast(long millis) {
        return millis > 0 && millis < System.currentTimeMillis();
    }

    public static boolean isFuture(long millis) {
        return millis > System.currentTimeMillis();
    }
}
