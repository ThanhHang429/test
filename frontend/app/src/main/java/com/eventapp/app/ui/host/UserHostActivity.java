package com.eventapp.app.ui.host;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.eventapp.app.databinding.ActivityUserHostBinding;
import com.eventapp.app.ui.user.AccountFragment;
import com.eventapp.app.ui.user.CalendarFragment;
import com.eventapp.app.ui.user.DiscoveryFragment;
import com.eventapp.app.ui.user.NotificationsActivity;
import com.eventapp.app.ui.user.SavedFragment;
import com.eventapp.app.ui.user.TicketsFragment;
import com.eventapp.app.ui.user.UserBottomNavView;
import com.eventapp.app.ui.user.data.DemoContent;

/**
 * Khu người dùng sau khi đăng nhập: thanh trên cùng + dock điều hướng nổi 5 tab.
 *
 * <p>Mỗi tab là một fragment được giữ lại theo tag nên chuyển qua lại không mất trạng thái cuộn.
 * Nội dung đang dùng dữ liệu mẫu trong {@link DemoContent}; khi nối API thật chỉ cần thay nguồn
 * dữ liệu bên trong các fragment.</p>
 */
public class UserHostActivity extends AppCompatActivity {

    private static final String STATE_TAB = "state_tab";

    private static final int TAB_COUNT = 5;

    private ActivityUserHostBinding binding;
    private int selectedTab = UserBottomNavView.TAB_DISCOVERY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserHostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        applyInsets();

        binding.userBottomNav.setOnTabSelectedListener(this::showTab);
        binding.btnNotifications.setOnClickListener(v -> startActivity(
                new Intent(this, NotificationsActivity.class)));
        binding.btnAvatar.setOnClickListener(v ->
                binding.userBottomNav.selectTab(UserBottomNavView.TAB_ACCOUNT));
        binding.tvAvatarInitials.setText("TL");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.userBottomNav.getSelectedIndex() != UserBottomNavView.TAB_DISCOVERY) {
                    binding.userBottomNav.selectTab(UserBottomNavView.TAB_DISCOVERY);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_TAB, UserBottomNavView.TAB_DISCOVERY);
        }
        binding.userBottomNav.selectTab(selectedTab, false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, selectedTab);
    }

    /** Hiện tab tương ứng; tab đã tạo trước đó chỉ bị ẩn/hiện để giữ trạng thái. */
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
            transaction.add(R.id.userContent, createTab(index), tagOf(index));
        }
        transaction.commit();
    }

    /** Chuyển sang tab khác từ bên trong một fragment (ví dụ nút "Khám phá ngay"). */
    public void openTab(int index) {
        binding.userBottomNav.selectTab(index);
    }

    private Fragment createTab(int index) {
        switch (index) {
            case UserBottomNavView.TAB_SAVED:
                return new SavedFragment();
            case UserBottomNavView.TAB_TICKETS:
                return new TicketsFragment();
            case UserBottomNavView.TAB_CALENDAR:
                return new CalendarFragment();
            case UserBottomNavView.TAB_ACCOUNT:
                return new AccountFragment();
            case UserBottomNavView.TAB_DISCOVERY:
            default:
                return new DiscoveryFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.viewUnreadDot.setVisibility(
                DemoContent.get().unreadNotifications() > 0 ? View.VISIBLE : View.GONE);
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams barParams = binding.userTopBar.getLayoutParams();
            int barHeight = dp(64) + bars.top;
            if (barParams.height != barHeight) {
                barParams.height = barHeight;
                binding.userTopBar.setLayoutParams(barParams);
            }
            binding.userTopBar.setPaddingRelative(binding.userTopBar.getPaddingStart(), bars.top,
                    binding.userTopBar.getPaddingEnd(), 0);

            ViewGroup.MarginLayoutParams navParams =
                    (ViewGroup.MarginLayoutParams) binding.userBottomNav.getLayoutParams();
            int navMargin = dp(12) + bars.bottom;
            if (navParams.bottomMargin != navMargin) {
                navParams.bottomMargin = navMargin;
                binding.userBottomNav.setLayoutParams(navParams);
            }

            binding.userContent.setPadding(0, barHeight, 0,
                    dp(68) + dp(12) + bars.bottom + dp(8));
            return insets;
        });
    }

    private static String tagOf(int index) {
        return "user_tab_" + index;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
