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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.NotificationUiModel;
import com.eventapp.app.databinding.ActivityUserNotificationsBinding;
import com.eventapp.app.ui.user.adapter.NotificationAdapter;
import com.eventapp.app.ui.user.model.DemoNotification;
import com.eventapp.app.ui.user.viewmodel.NotificationsViewModel;
import com.eventapp.app.ui.common.AppDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/** Trung tâm thông báo của người dùng — đọc thông báo organizer gửi (FR-O28). */
public class NotificationsActivity extends AppCompatActivity {

    private ActivityUserNotificationsBinding binding;
    private NotificationAdapter adapter;
    private NotificationsViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        adapter = new NotificationAdapter(this::open);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);

        // Backend chưa có trạng thái đã đọc nên ẩn hành động này.
        binding.btnMarkRead.setVisibility(View.GONE);
        binding.btnBack.setOnClickListener(v -> finish());

        observe();
        viewModel.reload();
    }

    private void observe() {
        viewModel.getItems().observe(this, this::bind);
        viewModel.getError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            Snackbar.make(binding.getRoot(), error.getUserMessage(),
                    Snackbar.LENGTH_LONG).show();
        });
    }

    private void bind(List<NotificationUiModel> data) {
        List<DemoNotification> items = new ArrayList<>(data.size());
        for (NotificationUiModel item : data) {
            items.add(new DemoNotification(
                    String.valueOf(item.id),
                    item.title,
                    item.body,
                    item.sentAtMillis,
                    R.drawable.ic_user_bell,
                    R.color.user_primary_soft,
                    R.color.user_primary,
                    false));
        }
        adapter.setItems(items);
        binding.tvSub.setText(getString(R.string.user_notifications_sub_total, items.size()));
        boolean empty = items.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void open(DemoNotification notification) {
        AppDialog.confirm(this,
                R.drawable.ic_user_bell,
                notification.title,
                notification.body,
                getString(R.string.user_close),
                null,
                AppDialog.Tone.NEUTRAL,
                () -> {
                });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
