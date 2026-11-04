package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityAdminEventDetailBinding;
import com.google.android.material.snackbar.Snackbar;

/** Giám sát một sự kiện: sức chứa, doanh thu, cờ rủi ro và hành động kiểm soát. */
public class AdminEventDetailActivity extends AppCompatActivity {

    private ActivityAdminEventDetailBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyInsets();

        binding.btnEventBack.setOnClickListener(v -> finish());
        binding.btnPauseSale.setOnClickListener(v -> snack(R.string.admin_event_paused));
        binding.btnSendAlert.setOnClickListener(v -> snack(R.string.admin_event_alerted));
    }

    private void snack(int message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.eventRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams params = binding.eventTopBar.getLayoutParams();
            int height = dp(64) + bars.top;
            if (params.height != height) {
                params.height = height;
                binding.eventTopBar.setLayoutParams(params);
            }
            binding.eventTopBar.setPaddingRelative(binding.eventTopBar.getPaddingStart(),
                    bars.top, binding.eventTopBar.getPaddingEnd(), 0);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
