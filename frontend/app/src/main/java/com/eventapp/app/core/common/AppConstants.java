package com.eventapp.app.core.common;

/** Hằng số dùng chung cho toàn app. */
public final class AppConstants {

    /** Tag log chung — lọc logcat bằng: adb logcat -s ETicket */
    public static final String LOG_TAG = "ETicket";

    /** Số bản ghi mỗi trang khi gọi danh sách (backend chặn tối đa 50). */
    public static final int PAGE_SIZE = 20;

    /** Backend chỉ chấp nhận size <= 50 (U04-R08). */
    public static final int MAX_PAGE_SIZE = 50;

    /** Timeout mạng, mili giây. */
    public static final long CONNECT_TIMEOUT_MS = 15_000L;
    public static final long READ_TIMEOUT_MS = 30_000L;
    public static final long WRITE_TIMEOUT_MS = 30_000L;

    private AppConstants() {
    }
}
