package com.eventapp.app.ui.host;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.databinding.ActivityAdminHostBinding;
import com.eventapp.app.ui.admin.AdminAccountsFragment;
import com.eventapp.app.ui.admin.AdminBroadcastFragment;
import com.eventapp.app.ui.admin.AdminCategoriesFragment;
import com.eventapp.app.ui.admin.AdminDashboardFragment;
import com.eventapp.app.ui.admin.AdminEventsFragment;
import com.eventapp.app.ui.admin.AdminFinanceFragment;
import com.eventapp.app.ui.admin.AdminNotificationsFragment;
import com.eventapp.app.ui.admin.AdminSystemFragment;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.common.AppDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Khu Quản trị sau khi đăng nhập: menu điều hướng bên trái + vùng nội dung.
 *
 * <p>Quản trị có nhiều màn chức năng nên dùng navigation drawer thay cho dock dưới.
 * Mỗi màn là một fragment giữ theo tag, chuyển qua lại không mất trạng thái cuộn.</p>
 */
public class AdminHostActivity extends AppCompatActivity {

    public static final int SCREEN_DASHBOARD = 0;
    public static final int SCREEN_EVENTS = 1;
    public static final int SCREEN_CATEGORIES = 2;
    public static final int SCREEN_ACCOUNTS = 3;
    public static final int SCREEN_NOTIFICATIONS = 4;
    public static final int SCREEN_BROADCAST = 5;
    public static final int SCREEN_FINANCE = 6;
    public static final int SCREEN_SYSTEM = 7;

    private static final int SCREEN_COUNT = 8;
    private static final String STATE_SCREEN = "state_admin_screen";

    private static final int[] ICONS = {
            R.drawable.ic_org_dashboard, R.drawable.ic_user_calendar,
            R.drawable.ic_admin_category, R.drawable.ic_ds_users,
            R.drawable.ic_user_bell, R.drawable.ic_ds_edit,
            R.drawable.ic_org_receipt, R.drawable.ic_org_settings
    };
    private static final int[] TITLES = {
            R.string.admin_screen_dashboard, R.string.admin_screen_events,
            R.string.admin_screen_categories, R.string.admin_screen_accounts,
            R.string.admin_screen_notifications, R.string.admin_screen_broadcast,
            R.string.admin_screen_finance, R.string.admin_screen_system
    };
    private static final int[] BADGES = {0, 0, 0, 5, 3, 0, 0, 0};
    private static final int[] SECTIONS = {
            R.string.admin_drawer_section_ops, 0, 0,
            R.string.admin_drawer_section_manage, 0, 0,
            R.string.admin_drawer_section_finance, 0
    };

    private ActivityAdminHostBinding binding;
    private final List<View> drawerItems = new ArrayList<>(SCREEN_COUNT);
    private int selectedScreen = SCREEN_DASHBOARD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        buildDrawer();
        applyInsets();

        binding.btnAdminMenu.setOnClickListener(v -> binding.adminDrawer.openDrawer(
                GravityCompat.START));
        binding.btnAdminAvatar.setOnClickListener(v -> binding.adminDrawer.openDrawer(
                GravityCompat.START));
        binding.btnAdminNotifications.setOnClickListener(v -> selectScreen(SCREEN_NOTIFICATIONS));
        binding.btnDrawerLogout.setOnClickListener(v -> confirmLogout());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.adminDrawer.isDrawerOpen(GravityCompat.START)) {
                    binding.adminDrawer.closeDrawers();
                    return;
                }
                if (selectedScreen != SCREEN_DASHBOARD) {
                    selectScreen(SCREEN_DASHBOARD);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState != null) {
            selectedScreen = savedInstanceState.getInt(STATE_SCREEN, SCREEN_DASHBOARD);
        }
        showScreen(selectedScreen, false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SCREEN, selectedScreen);
    }

    /** Mở một màn từ fragment con (hàng đợi ở Tổng quan, hành động ở màn khác…). */
    public void openScreen(int screen) {
        selectScreen(screen);
    }

    /** Ẩn huy hiệu thông báo sau khi người quản trị đã đọc hết. */
    public void markNotificationsRead() {
        binding.viewAdminUnreadDot.setVisibility(View.GONE);
        if (drawerItems.size() > SCREEN_NOTIFICATIONS) {
            View badge = drawerItems.get(SCREEN_NOTIFICATIONS).findViewById(R.id.drawerBadge);
            badge.setVisibility(View.GONE);
        }
    }

    private void buildDrawer() {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < SCREEN_COUNT; i++) {
            if (SECTIONS[i] != 0) {
                TextView section = new TextView(this);
                section.setText(SECTIONS[i]);
                section.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                section.setLetterSpacing(0.14f);
                section.setTextColor(ContextCompat.getColor(this, R.color.admin_drawer_faint));
                section.setTypeface(section.getTypeface(), android.graphics.Typeface.BOLD);
                section.setPadding(dp(22), dp(18), 0, dp(6));
                binding.adminDrawerItems.addView(section);
            }

            View item = inflater.inflate(R.layout.item_admin_drawer, binding.adminDrawerItems, false);
            ImageView icon = item.findViewById(R.id.drawerIcon);
            TextView label = item.findViewById(R.id.drawerLabel);
            TextView badge = item.findViewById(R.id.drawerBadge);

            icon.setImageResource(ICONS[i]);
            label.setText(TITLES[i]);
            if (BADGES[i] > 0) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(String.valueOf(BADGES[i]));
            }

            final int screen = i;
            item.setOnClickListener(v -> {
                binding.adminDrawer.closeDrawers();
                selectScreen(screen);
            });

            binding.adminDrawerItems.addView(item);
            drawerItems.add(item);
        }
    }

    private void selectScreen(int screen) {
        if (screen == selectedScreen && getSupportFragmentManager().findFragmentByTag(tagOf(screen)) != null) {
            return;
        }
        showScreen(screen, true);
    }

    private void showScreen(int screen, boolean animate) {
        selectedScreen = screen;

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (animate) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        }

        for (int i = 0; i < SCREEN_COUNT; i++) {
            Fragment existing = manager.findFragmentByTag(tagOf(i));
            if (existing == null) {
                continue;
            }
            if (i == screen) {
                transaction.show(existing);
            } else {
                transaction.hide(existing);
            }
        }
        if (manager.findFragmentByTag(tagOf(screen)) == null) {
            transaction.add(R.id.adminContent, createScreen(screen), tagOf(screen));
        }
        transaction.commit();

        binding.tvAdminScreenTitle.setText(TITLES[screen]);
        applyDrawerSelection(screen);
    }

    private void applyDrawerSelection(int screen) {
        TypedValue ripple = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);

        for (int i = 0; i < drawerItems.size(); i++) {
            View item = drawerItems.get(i);
            boolean active = i == screen;
            ImageView icon = item.findViewById(R.id.drawerIcon);
            TextView label = item.findViewById(R.id.drawerLabel);
            View indicator = item.findViewById(R.id.drawerIndicator);

            item.setBackgroundResource(active
                    ? R.drawable.bg_admin_drawer_item_selected : ripple.resourceId);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    active ? R.color.admin_accent_bright : R.color.admin_drawer_muted)));
            label.setTextColor(ContextCompat.getColor(this,
                    active ? R.color.white : R.color.admin_drawer_text));
            label.setTypeface(label.getTypeface(), active
                    ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private Fragment createScreen(int screen) {
        switch (screen) {
            case SCREEN_EVENTS:
                return new AdminEventsFragment();
            case SCREEN_CATEGORIES:
                return new AdminCategoriesFragment();
            case SCREEN_ACCOUNTS:
                return new AdminAccountsFragment();
            case SCREEN_NOTIFICATIONS:
                return new AdminNotificationsFragment();
            case SCREEN_BROADCAST:
                return new AdminBroadcastFragment();
            case SCREEN_FINANCE:
                return new AdminFinanceFragment();
            case SCREEN_SYSTEM:
                return new AdminSystemFragment();
            case SCREEN_DASHBOARD:
            default:
                return new AdminDashboardFragment();
        }
    }

    private void confirmLogout() {
        binding.adminDrawer.closeDrawers();
        AppDialog.confirm(this,
                R.drawable.ic_user_logout,
                getString(R.string.admin_logout_confirm_title),
                getString(R.string.admin_logout_confirm_body),
                getString(R.string.admin_drawer_logout),
                getString(R.string.admin_stay),
                AppDialog.Tone.DANGER,
                this::logout);
    }

    private void logout() {
        SessionManager.get().clear();
        ServiceLocator.reset();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams barParams = binding.adminTopBar.getLayoutParams();
            int barHeight = dp(64) + bars.top;
            if (barParams.height != barHeight) {
                barParams.height = barHeight;
                binding.adminTopBar.setLayoutParams(barParams);
            }
            binding.adminTopBar.setPaddingRelative(binding.adminTopBar.getPaddingStart(), bars.top,
                    binding.adminTopBar.getPaddingEnd(), 0);

            binding.adminContent.setPadding(0, barHeight, 0, 0);
            binding.adminDrawerPanel.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
    }

    private static String tagOf(int screen) {
        return "admin_screen_" + screen;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
