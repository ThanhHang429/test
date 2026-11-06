package com.eventapp.app.core.common;

import android.util.Log;

import com.eventapp.app.BuildConfig;

/**
 * Log có tiền tố chung. Ở bản release chỉ giữ mức warning trở lên để không lộ dữ liệu.
 */
public final class AppLogger {

    private AppLogger() {
    }

    public static void d(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(AppConstants.LOG_TAG, message);
        }
    }

    public static void d(String message, Throwable throwable) {
        if (BuildConfig.DEBUG) {
            Log.d(AppConstants.LOG_TAG, message, throwable);
        }
    }

    public static void w(String message, Throwable throwable) {
        Log.w(AppConstants.LOG_TAG, message, throwable);
    }

    public static void e(String message, Throwable throwable) {
        Log.e(AppConstants.LOG_TAG, message, throwable);
    }
}
