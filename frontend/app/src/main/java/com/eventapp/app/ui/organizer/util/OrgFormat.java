package com.eventapp.app.ui.organizer.util;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Định dạng tiền tệ, số lượng và thời gian cho các màn hình Organizer. */
public final class OrgFormat {

    private static final DecimalFormat GROUPED =
            new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    private static final ThreadLocal<SimpleDateFormat> DATE =
            formatter("dd/MM/yyyy");
    private static final ThreadLocal<SimpleDateFormat> TIME =
            formatter("HH:mm");

    private OrgFormat() {
    }

    public static String money(long value) {
        if (value <= 0) {
            return "0 đ";
        }
        return GROUPED.format(value).replace(',', '.') + " đ";
    }

    /** Số lượng vé/đơn: 1720 → "1.720". */
    public static String count(int value) {
        return GROUPED.format(value).replace(',', '.');
    }

    public static String count(long value) {
        return GROUPED.format(value).replace(',', '.');
    }

    public static String compactMoney(long value) {
        if (value <= 0) {
            return "0 đ";
        }
        if (value >= 1_000_000_000L) {
            return trim(value / 1_000_000_000d) + " tỷ đ";
        }
        if (value >= 1_000_000L) {
            return Math.round(value / 1_000_000d) + " tr đ";
        }
        if (value >= 1_000L) {
            return Math.round(value / 1_000d) + "k đ";
        }
        return money(value);
    }

    /** "15/11/2025 · 09:00" — trả "—" nếu chưa có mốc thời gian. */
    public static String dateTime(long millis) {
        if (millis <= 0) {
            return "—";
        }
        return DATE.get().format(new Date(millis)) + " · " + TIME.get().format(new Date(millis));
    }

    /** Khoảng diễn ra: cùng ngày thì gộp giờ, khác ngày thì hiện cả hai đầu. */
    public static String dateRange(long startAt, long endAt) {
        if (startAt <= 0) {
            return "—";
        }
        String start = dateTime(startAt);
        if (endAt <= 0 || endAt <= startAt) {
            return start;
        }
        if (sameDay(startAt, endAt)) {
            return start + "–" + TIME.get().format(new Date(endAt));
        }
        return start + " → " + dateTime(endAt);
    }

    /** "SECC, Quận 7 · TP.HCM" — bỏ phần rỗng. */
    public static String location(@Nullable String venue, @Nullable String city) {
        String left = venue == null ? "" : venue.trim();
        String right = city == null ? "" : city.trim();
        if (left.isEmpty()) {
            return right.isEmpty() ? "—" : right;
        }
        return right.isEmpty() ? left : left + " · " + right;
    }

    private static boolean sameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(first);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static ThreadLocal<SimpleDateFormat> formatter(String pattern) {
        return new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(pattern, Locale.US);
            }
        };
    }

    private static String trim(double value) {
        String text = String.format(Locale.US, "%.2f", value);
        if (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.replace('.', ',');
    }
}
