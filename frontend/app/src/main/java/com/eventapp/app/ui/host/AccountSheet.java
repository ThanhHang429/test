package com.eventapp.app.ui.host;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.eventapp.app.R;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

/** Sheet tài khoản dùng chung: hiển thị phiên hiện tại và đăng xuất. */
public final class AccountSheet {

    private AccountSheet() {
    }

    public static void show(@NonNull AppCompatActivity activity) {
        AuthSession session = SessionManager.get().getSession();
        if (session == null) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_account, null);
        dialog.setContentView(content);

        TextView name = content.findViewById(R.id.tvAccountName);
        TextView email = content.findViewById(R.id.tvAccountEmail);
        ChipGroup roles = content.findViewById(R.id.chipGroupAccountRoles);
        View myReviews = content.findViewById(R.id.rowAccountMyReviews);
        View logout = content.findViewById(R.id.rowAccountLogout);

        String displayName = session.fullName == null || session.fullName.trim().isEmpty()
                ? "Người dùng ETicket" : session.fullName.trim();
        name.setText(displayName);
        email.setText(session.email == null ? "Không có email" : session.email);

        roles.removeAllViews();
        for (String role : session.roles) {
            Chip chip = new Chip(activity);
            chip.setText(roleLabel(role));
            chip.setChipBackgroundColorResource(R.color.ds_primary_soft);
            chip.setTextColor(activity.getColor(R.color.ds_primary_dark));
            chip.setChipStrokeWidth(0f);
            chip.setClickable(false);
            chip.setCheckable(false);
            roles.addView(chip);
        }

        myReviews.setVisibility(session.hasRole("USER") ? View.VISIBLE : View.GONE);
        myReviews.setOnClickListener(v -> {
            dialog.dismiss();
            navigateTo(activity, R.id.my_reviewsFragment);
        });

        logout.setOnClickListener(v -> {
            dialog.dismiss();
            logout(activity);
        });

        dialog.show();
    }

    private static void navigateTo(@NonNull AppCompatActivity activity, int destination) {
        try {
            NavHostFragment host = (NavHostFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (host == null) {
                return;
            }
            NavController controller = host.getNavController();
            if (controller.getCurrentDestination() != null
                    && controller.getCurrentDestination().getId() != destination) {
                controller.navigate(destination);
            }
        } catch (RuntimeException ignored) {
            Snackbar.make(activity.findViewById(android.R.id.content),
                    "Không mở được màn hình này", Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Xoá phiên, xoá cache repository rồi quay về màn đăng nhập. */
    public static void logout(@NonNull FragmentActivity activity) {
        SessionManager.get().clear();
        ServiceLocator.reset();
        SessionRouter.openAuth(activity);
        activity.finish();
    }

    @NonNull
    private static String roleLabel(@Nullable String role) {
        if (role == null) {
            return "";
        }
        switch (role.toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN":
                return "Quản trị";
            case "ORGANIZER":
                return "Nhà tổ chức";
            default:
                return "Người dùng";
        }
    }
}
