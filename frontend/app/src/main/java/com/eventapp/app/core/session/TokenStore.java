package com.eventapp.app.core.session;

import androidx.annotation.Nullable;

/**
 * Giữ token trong bộ nhớ.
 *
 * <p>Cố ý <b>không</b> dùng SharedPreferences: app phải quên phiên khi bị đóng để mỗi lần mở lại
 * đều chọn được vai trò khác (yêu cầu bỏ "remember").</p>
 */
public final class TokenStore {

    private volatile String token;

    @Nullable
    public String get() {
        return token;
    }

    public void set(@Nullable String value) {
        token = value;
    }

    public void clear() {
        token = null;
    }
}
