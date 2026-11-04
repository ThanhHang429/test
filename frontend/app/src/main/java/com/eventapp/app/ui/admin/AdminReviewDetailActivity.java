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
import com.eventapp.app.databinding.ActivityAdminReviewDetailBinding;
import com.google.android.material.snackbar.Snackbar;

/** Hồ sơ kiểm duyệt: pháp lý, tín hiệu rủi ro và quyết định duyệt/từ chối. */
public class AdminReviewDetailActivity extends AppCompatActivity {

    private ActivityAdminReviewDetailBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminReviewDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyInsets();

        binding.btnReviewBack.setOnClickListener(v -> finish());
        binding.btnReviewApproveDetail.setOnClickListener(v -> decide(true));
        binding.btnReviewRejectDetail.setOnClickListener(v -> decide(false));
    }

    private void decide(boolean approved) {
        Snackbar.make(binding.getRoot(),
                approved ? R.string.admin_review_approved : R.string.admin_review_rejected,
                Snackbar.LENGTH_SHORT).show();
        binding.getRoot().postDelayed(this::finish, 700);
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.reviewRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams params = binding.reviewTopBar.getLayoutParams();
            int height = dp(64) + bars.top;
            if (params.height != height) {
                params.height = height;
                binding.reviewTopBar.setLayoutParams(params);
            }
            binding.reviewTopBar.setPaddingRelative(binding.reviewTopBar.getPaddingStart(),
                    bars.top, binding.reviewTopBar.getPaddingEnd(), 0);

            binding.reviewBottomBar.setPadding(binding.reviewBottomBar.getPaddingLeft(),
                    binding.reviewBottomBar.getPaddingTop(),
                    binding.reviewBottomBar.getPaddingRight(),
                    dp(12) + bars.bottom);
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
