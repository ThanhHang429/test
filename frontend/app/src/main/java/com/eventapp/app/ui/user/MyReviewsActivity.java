package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.databinding.ActivityMyReviewsBinding;
import com.eventapp.app.databinding.ItemMyReviewBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.util.StarText;

/** Đánh giá của tôi ở mọi trạng thái duyệt (FR-U20). */
public class MyReviewsActivity extends AppCompatActivity {

    private ActivityMyReviewsBinding binding;

    public static void start(@NonNull Context context) {
        context.startActivity(new Intent(context, MyReviewsActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        load();
    }

    private void load() {
        binding.progress.setVisibility(View.VISIBLE);
        ServiceLocator.get(ReviewRepository.class).mine(0, 50,
                new ApiCallback<Paged<ReviewDtos.MyReview>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        binding.reviewList.removeAllViews();
                        LayoutInflater inflater = getLayoutInflater();
                        int count = 0;
                        if (data != null) {
                            for (ReviewDtos.MyReview dto : data.items) {
                                ItemMyReviewBinding row = ItemMyReviewBinding.inflate(
                                        inflater, binding.reviewList, false);
                                bindRow(row, dto);
                                binding.reviewList.addView(row.getRoot());
                                count++;
                            }
                        }
                        binding.tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
                    }
                });
    }

    private void bindRow(ItemMyReviewBinding row, ReviewDtos.MyReview review) {
        row.tvEventTitle.setText(review.eventTitle);
        StarText.apply(row.tvStars, review.rating);

        boolean hasText = review.text != null && !review.text.trim().isEmpty();
        row.tvText.setText(hasText ? review.text : "");
        row.tvText.setVisibility(hasText ? View.VISIBLE : View.GONE);

        switch (review.status == null ? "" : review.status) {
            case "PENDING":
                row.tvStatus.setText(R.string.user_reviews_status_pending);
                row.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_warning);
                row.tvStatus.setTextColor(getColor(R.color.user_warning_text));
                break;
            case "HIDDEN":
            case "REMOVED":
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
