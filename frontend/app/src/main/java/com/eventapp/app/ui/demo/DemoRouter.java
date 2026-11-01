package com.eventapp.app.ui.demo;

import android.content.Context;
import android.content.Intent;

import com.eventapp.app.core.enums.Role;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.host.AdminHostActivity;
import com.eventapp.app.ui.host.OrganizerHostActivity;
import com.eventapp.app.ui.host.UserHostActivity;

/** Điều hướng host theo vai trò của phiên demo. */
public final class DemoRouter {

    private DemoRouter() {
    }

    public static Class<?> hostFor(Role role) {
        if (role == Role.ORGANIZER) {
            return OrganizerHostActivity.class;
        }
        if (role == Role.ADMIN) {
            return AdminHostActivity.class;
        }
        return UserHostActivity.class;
    }

    public static void openHost(Context context, Role role) {
        Intent intent = new Intent(context, hostFor(role));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public static void openAuth(Context context) {
        Intent intent = new Intent(context, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
