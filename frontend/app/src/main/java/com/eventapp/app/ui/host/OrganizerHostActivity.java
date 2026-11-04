package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityOrgHostBinding;
import com.eventapp.app.ui.organizer.OrgBottomNavView;
import com.eventapp.app.ui.organizer.OrgCheckinFragment;
import com.eventapp.app.ui.organizer.OrgDashboardFragment;
import com.eventapp.app.ui.organizer.OrgEventsFragment;
import com.eventapp.app.ui.organizer.OrgOrdersFragment;
import com.eventapp.app.ui.organizer.OrgSettingsFragment;
import com.google.android.material.snackbar.Snackbar;

/**
 * Khu Nhà tổ chức sau khi đăng nhập: thanh trên cùng + dock 5 tab
 * (Tổng quan, Sự kiện, Check-in, Đơn hàng, Cài đặt).
 *
 * <p>Dữ liệu đang là mẫu trong {@code OrgDemo}; khi nối API chỉ cần thay nguồn dữ liệu
 * bên trong các fragment, không đổi cấu trúc điều hướng.</p>
 */
public class OrganizerHostActivity extends AppCompatActivity {

    private static final String STATE_TAB = "state_org_tab";
    private static final int TAB_COUNT = 5;

    private ActivityOrgHostBinding binding;
    private int selectedTab = OrgBottomNavView.TAB_DASHBOARD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getTheme().applyStyle(R.style.ThemeOverlay_EventApp_Organizer, true);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityOrgHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        applyInsets();

        binding.orgBottomNav.setOnTabSelectedListener(this::showTab);
        binding.btnOrgNotifications.setOnClickListener(v -> snack("Bạn có 3 thông báo mới"));
        binding.btnOrgAvatar.setOnClickListener(v ->
                binding.orgBottomNav.selectTab(OrgBottomNavView.TAB_SETTINGS));
        binding.tvOrgAvatarInitials.setText("NA");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.orgBottomNav.getSelectedIndex() != OrgBottomNavView.TAB_DASHBOARD) {
                    binding.orgBottomNav.selectTab(OrgBottomNavView.TAB_DASHBOARD);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_TAB, OrgBottomNavView.TAB_DASHBOARD);
        }
        binding.orgBottomNav.selectTab(selectedTab, false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab);
    }

    /** Mở tab từ bên trong fragment (ví dụ "Xem tất cả" ở Tổng quan). */
    public void openTab(int index) {
        binding.orgBottomNav.selectTab(index);
    }

    public void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showTab(int index) {
        selectedTab = index;

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        for (int i = 0; i < TAB_COUNT; i++) {
            Fragment existing = manager.findFragmentByTag(tagOf(i));
            if (existing == null) {
                continue;
            }
            if (i == index) {
                transaction.show(existing);
            } else {
                transaction.hide(existing);
            }
        }
        if (manager.findFragmentByTag(tagOf(index)) == null) {
            transaction.add(R.id.orgContent, createTab(index), tagOf(index));
        }
        transaction.commit();
    }

    private Fragment createTab(int index) {
        switch (index) {
            case OrgBottomNavView.TAB_EVENTS:
                return new OrgEventsFragment();
            case OrgBottomNavView.TAB_CHECKIN:
                return new OrgCheckinFragment();
            case OrgBottomNavView.TAB_ORDERS:
                return new OrgOrdersFragment();
            case OrgBottomNavView.TAB_SETTINGS:
                return new OrgSettingsFragment();
            case OrgBottomNavView.TAB_DASHBOARD:
            default:
                return new OrgDashboardFragment();
        }
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams barParams = binding.orgTopBar.getLayoutParams();
            int barHeight = dp(64) + bars.top;
            if (barParams.height != barHeight) {
                barParams.height = barHeight;
                binding.orgTopBar.setLayoutParams(barParams);
            }
            binding.orgTopBar.setPaddingRelative(binding.orgTopBar.getPaddingStart(), bars.top,
                    binding.orgTopBar.getPaddingEnd(), 0);

            ViewGroup.MarginLayoutParams navParams =
                    (ViewGroup.MarginLayoutParams) binding.orgBottomNav.getLayoutParams();
            int navMargin = dp(12) + bars.bottom;
            if (navParams.bottomMargin != navMargin) {
                navParams.bottomMargin = navMargin;
                binding.orgBottomNav.setLayoutParams(navParams);
            }

            binding.orgContent.setPadding(0, barHeight, 0,
                    dp(68) + dp(12) + bars.bottom + dp(8));
            return insets;
        });
    }

    private static String tagOf(int index) {
        return "org_tab_" + index;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
