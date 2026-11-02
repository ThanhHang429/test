package com.eventapp.app.core.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Truy vấn vai trò của phiên hiện tại để điều hướng và ẩn/hiện hành động. */
public final class RoleStore {

    private RoleStore() {
    }

    public static boolean hasRole(@Nullable String role) {
        AuthSession session = SessionManager.get().getSession();
        return session != null && session.hasRole(role);
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isOrganizer() {
        return hasRole("ORGANIZER");
    }

    public static boolean isUser() {
        return hasRole("USER");
    }

    /** Vai trò ưu tiên của phiên, null khi chưa đăng nhập. */
    @Nullable
    public static String primaryRole() {
        AuthSession session = SessionManager.get().getSession();
        return session == null ? null : session.primaryRole();
    }

    @NonNull
    public static String describe(@Nullable AuthSession session) {
        return session == null ? "Chưa đăng nhập" : session.displayLabel();
    }
}
