package com.eventapp.app.core.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eventapp.app.core.common.AppLogger;

/**
 * Phiên đăng nhập hiện tại — nguồn sự thật duy nhất cho token và vai trò.
 *
 * <p>Phiên chỉ nằm trong bộ nhớ: đóng app là mất, nên mỗi lần mở lại người dùng phải chọn vai trò
 * (đã bỏ "remember" theo yêu cầu kiểm thử nhiều role).</p>
 */
public final class SessionManager implements TokenProvider {

    private static volatile SessionManager instance;

    private final TokenStore tokenStore = new TokenStore();
    private final MutableLiveData<AuthSession> liveSession = new MutableLiveData<>();

    private volatile AuthSession session;

    private SessionManager() {
    }

    @NonNull
    public static SessionManager get() {
        SessionManager local = instance;
        if (local == null) {
            synchronized (SessionManager.class) {
                local = instance;
                if (local == null) {
                    local = new SessionManager();
                    instance = local;
                }
            }
        }
        return local;
    }

    // ------------------------------------------------------------------ state

    @Nullable
    public AuthSession getSession() {
        return session;
    }

    /** LiveData để Activity/Fragment tự đóng khi phiên bị xoá (đăng xuất hoặc 401). */
    @NonNull
    public LiveData<AuthSession> observe() {
        return liveSession;
    }

    public boolean isLoggedIn() {
        return session != null;
    }

    @Nullable
    @Override
    public String getToken() {
        return tokenStore.get();
    }

    public long getUserId() {
        return session == null ? -1L : session.userId;
    }

    // ------------------------------------------------------------------ mutation

    /** Bắt đầu phiên mới sau khi lấy được token. */
    public void start(@NonNull AuthSession newSession) {
        session = newSession;
        tokenStore.set(newSession.token);
        liveSession.postValue(newSession);
        AppLogger.d("Phiên mới: " + newSession.displayLabel());
    }

    /** Đăng xuất / hết hạn token. */
    public void clear() {
        if (session == null) {
            return;
        }
        session = null;
        tokenStore.clear();
        liveSession.postValue(null);
        AppLogger.d("Đã xoá phiên đăng nhập");
    }
}
