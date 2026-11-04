package com.eventapp.app.ui.organizer.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Định dạng tiền tệ rút gọn cho các thẻ KPI của Organizer. */
public final class OrgFormat {

    private static final DecimalFormat GROUPED =
            new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

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
