package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.eventapp.app.databinding.ActivityMyReviewsBinding;
import com.eventapp.app.databinding.ItemMyReviewBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.util.StarText;
import com.eventapp.app.ui.user.viewmodel.MyReviewsViewModel;

import java.util.List;

/** Đánh giá của tôi ở mọi trạng thái duyệt (FR-U20). */
public class MyReviewsActivity extends AppCompatActivity {

    private ActivityMyReviewsBinding binding;
    private MyReviewsViewModel viewModel;

    public static void start(@NonNull Context context) {
        context.startActivity(new Intent(context, MyReviewsActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(MyReviewsViewModel.class);
        observe();
        viewModel.load();
    }

    private void observe() {
        viewModel.getReviews().observe(this, this::render);
        viewModel.getLoading().observe(this, loading -> {
            if (binding != null) {
                binding.progress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.getError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            binding.tvEmpty.setVisibility(View.VISIBLE);
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
        });
    }

    private void render(List<ReviewUiModel> reviews) {
        if (binding == null) {
            return;
        }
        binding.reviewList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        if (reviews != null) {
            for (ReviewUiModel dto : reviews) {
                ItemMyReviewBinding row = ItemMyReviewBinding.inflate(
                        inflater, binding.reviewList, false);
                bindRow(row, dto);
                binding.reviewList.addView(row.getRoot());
            }
        }
        boolean empty = reviews == null || reviews.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void bindRow(ItemMyReviewBinding row, ReviewUiModel review) {
        row.tvEventTitle.setText(review.eventTitle);
        StarText.apply(row.tvStars, review.rating);

        boolean hasText = !review.comment.trim().isEmpty();
        row.tvText.setText(hasText ? review.comment : "");
        row.tvText.setVisibility(hasText ? View.VISIBLE : View.GONE);

        switch (review.status) {
            case HELD:
                row.tvStatus.setText(R.string.user_reviews_status_pending);
                row.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_warning);
                row.tvStatus.setTextColor(getColor(R.color.user_warning_text));
                break;
            case HIDDEN:
                row.tvStatus.setText(R.string.user_reviews_status_hidden);
                row.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_danger);
                row.tvStatus.setTextColor(getColor(R.color.user_danger_text));
                break;
            default:
                row.tvStatus.setText(R.string.user_reviews_status_visible);
                row.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_success);
                row.tvStatus.setTextColor(getColor(R.color.user_success_text));
                break;
        }

        row.tvMeta.setText(review.canEdit
                ? getString(R.string.user_reviews_meta_editable,
                review.editCount, review.editDaysLeft)
                : getString(R.string.user_reviews_meta_locked, review.editCount));

        row.getRoot().setOnClickListener(v ->
                EventReviewsActivity.start(this, review.eventId, review.eventTitle));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
