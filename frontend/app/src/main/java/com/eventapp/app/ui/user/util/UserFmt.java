package com.eventapp.app.ui.user.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/** Định dạng ngày giờ và tiền tệ dùng riêng cho khu người dùng. */
public final class UserFmt {

    private static final String[] DAY_FULL = {
            "Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"
    };
    private static final String[] DAY_SHORT = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
    private static final String[] MONTH_FULL = {
            "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
            "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    };

    private UserFmt() {
    }

    public static String dateShort(long time) {
        Calendar c = calendar(time);
        return DAY_SHORT[c.get(Calendar.DAY_OF_WEEK) - 1] + ", "
                + pad(c.get(Calendar.DAY_OF_MONTH)) + "/" + pad(c.get(Calendar.MONTH) + 1);
    }

    public static String dateLong(long time) {
        Calendar c = calendar(time);
        return DAY_FULL[c.get(Calendar.DAY_OF_WEEK) - 1] + ", "
                + pad(c.get(Calendar.DAY_OF_MONTH)) + "/" + pad(c.get(Calendar.MONTH) + 1)
                + "/" + c.get(Calendar.YEAR);
    }

    public static String dateTime(long time) {
        return dateShort(time) + " · " + time(time);
    }

    public static String time(long time) {
        Calendar c = calendar(time);
        return pad(c.get(Calendar.HOUR_OF_DAY)) + ":" + pad(c.get(Calendar.MINUTE));
    }

    public static String monthYear(long time) {
        Calendar c = calendar(time);
        return MONTH_FULL[c.get(Calendar.MONTH)] + ", " + c.get(Calendar.YEAR);
    }

    public static String dayOfWeekShort(long time) {
        return DAY_SHORT[calendar(time).get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String dayNumber(long time) {
        return pad(calendar(time).get(Calendar.DAY_OF_MONTH));
    }

    public static boolean isToday(long time) {
        Calendar today = Calendar.getInstance();
        Calendar target = calendar(time);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    public static String money(long amount) {
        if (amount <= 0L) {
            return "Miễn phí";
        }
        return amount(amount);
    }

    /** Số tiền luôn kèm hậu tố "đ", kể cả 0. */
    public static String amount(long value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(value) + " đ";
    }

    public static String relative(long time) {
        long diff = System.currentTimeMillis() - time;
        if (diff < 60_000L) {
            return "Vừa xong";
        }
        if (diff < 3600_000L) {
            return (diff / 60_000L) + " phút trước";
        }
        if (diff < 24 * 3600_000L) {
            return (diff / 3600_000L) + " giờ trước";
        }
        long days = diff / (24 * 3600_000L);
        if (days == 1) {
            return "Hôm qua";
        }
        if (days < 7) {
            return days + " ngày trước";
        }
        return dateShort(time);
    }

    /** Số ngày còn lại, làm tròn lên; 0 nếu đã bắt đầu. */
    public static int daysUntil(long time) {
        long diff = time - System.currentTimeMillis();
        if (diff <= 0) {
            return 0;
        }
        return (int) Math.ceil(diff / (double) (24 * 3600_000L));
    }

    public static String dayMonth(long time) {
        Calendar c = calendar(time);
        return pad(c.get(Calendar.DAY_OF_MONTH)) + "/" + pad(c.get(Calendar.MONTH) + 1);
    }

    private static Calendar calendar(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    private static String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
