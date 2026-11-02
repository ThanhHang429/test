package com.eventapp.app.core.util;

import androidx.annotation.NonNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Định dạng tiền VND cho toàn app. */
public final class MoneyUtils {

    private static final DecimalFormat GROUPED =
            new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    private MoneyUtils() {
    }

    /** 200000 → "200.000 ₫". */
    @NonNull
    public static String vnd(long amount) {
        return GROUPED.format(Math.max(0L, amount)).replace(',', '.') + " ₫";
    }

    /** 1200000 → "1,2 tr". Dùng cho thẻ số liệu nhỏ. */
    @NonNull
    public static String compact(long amount) {
        long value = Math.max(0L, amount);
        if (value >= 1_000_000_000L) {
            return trim(value / 1_000_000_000.0) + " tỷ";
        }
        if (value >= 1_000_000L) {
            return trim(value / 1_000_000.0) + " tr";
        }
        if (value >= 1_000L) {
            return trim(value / 1_000.0) + " ng";
        }
        return String.valueOf(value);
    }

    @NonNull
    private static String trim(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.valueOf(Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }
}
