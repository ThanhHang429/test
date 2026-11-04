package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserNotificationsBinding;
import com.eventapp.app.ui.user.adapter.NotificationAdapter;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoNotification;

/** Trung tâm thông báo của người dùng. */
public class NotificationsActivity extends AppCompatActivity {

    private ActivityUserNotificationsBinding binding;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(binding.notificationRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) binding.topBar.getLayoutParams();
            params.height = dp(60) + bars.top;
            binding.topBar.setLayoutParams(params);
            binding.topBar.setPadding(dp(16), bars.top + dp(6), dp(16), dp(6));
            binding.rvNotifications.setPadding(dp(20), 0, dp(20), bars.bottom + dp(20));
            return insets;
        });

        adapter = new NotificationAdapter(this::open);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnMarkRead.setOnClickListener(v -> {
            DemoContent.get().markAllNotificationsRead();
            reload();
        });

        reload();
    }

    private void open(DemoNotification notification) {
        notification.unread = false;
        reload();
    }

    private void reload() {
        DemoContent content = DemoContent.get();
        adapter.setItems(content.notifications());
        binding.tvSub.setText(getString(R.string.user_notifications_sub,
                content.unreadNotifications()));
        boolean empty = content.notifications().isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.btnMarkRead.setVisibility(content.unreadNotifications() > 0
                ? View.VISIBLE : View.INVISIBLE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
