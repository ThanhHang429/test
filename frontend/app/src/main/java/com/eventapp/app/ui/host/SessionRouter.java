package com.eventapp.app.ui.host;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.eventapp.app.core.session.AuthSession;

/** Điều hướng host theo vai trò của phiên đăng nhập thật (JWT). */
public final class SessionRouter {

    private SessionRouter() {
    }

    @NonNull
    public static Class<?> hostFor(@NonNull AuthSession session) {
        String role = session.primaryRole();
        if ("ADMIN".equals(role)) {
            return AdminHostActivity.class;
        }
        if ("ORGANIZER".equals(role)) {
            return OrganizerHostActivity.class;
        }
        return UserHostActivity.class;
    }

    public static void openHost(@NonNull Context context, @NonNull AuthSession session) {
        Intent intent = new Intent(context, hostFor(session));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
