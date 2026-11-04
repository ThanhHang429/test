package com.eventapp.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminModerationBinding;
import com.eventapp.app.databinding.ItemAdminReviewBinding;
import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminReview;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Tab Kiểm duyệt: hàng đợi hồ sơ, bộ lọc và thao tác duyệt nhanh. */
public class AdminModerationFragment extends Fragment {

    private FragmentAdminModerationBinding binding;
    private List<AdminReview> reviews;
    private int filterTone = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminModerationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        reviews = AdminDemo.reviews();

        binding.chipAll.setOnClickListener(v -> selectFilter(-1));
        binding.chipPriority.setOnClickListener(v -> selectFilter(AdminReview.TONE_PRIORITY));
        binding.chipWarning.setOnClickListener(v -> selectFilter(AdminReview.TONE_WARNING));
        binding.chipOverdue.setOnClickListener(v -> selectFilter(AdminReview.TONE_OVERDUE));

        selectFilter(-1);
    }

    private void selectFilter(int tone) {
        filterTone = tone;
        setChipSelected(binding.chipAll, tone == -1);
        setChipSelected(binding.chipPriority, tone == AdminReview.TONE_PRIORITY);
        setChipSelected(binding.chipWarning, tone == AdminReview.TONE_WARNING);
        setChipSelected(binding.chipOverdue, tone == AdminReview.TONE_OVERDUE);
        render();
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setSelected(selected);
    }

    private void render() {
        binding.listReviews.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (AdminReview review : reviews) {
            if (filterTone != -1 && review.tone != filterTone) {
                continue;
            }
            ItemAdminReviewBinding item =
                    ItemAdminReviewBinding.inflate(inflater, binding.listReviews, false);
            bindItem(item, review);
            binding.listReviews.addView(item.getRoot());
        }
    }

    private void bindItem(ItemAdminReviewBinding item, AdminReview review) {
        item.tvReviewTitle.setText(review.title);
        item.tvReviewOrg.setText(review.organizer);
        item.tvReviewCategory.setText(review.category);
        item.tvReviewCapacity.setText(review.capacity);
        item.tvReviewPrice.setText(review.price);
        item.tvReviewDeadline.setText(review.deadline);
        item.tvReviewer.setText(review.reviewer);
        item.tvReviewerInitials.setText(review.initials);
        item.coverReview.setBackgroundResource(review.coverRes);

        switch (review.tone) {
            case AdminReview.TONE_WARNING:
                item.tvReviewStatus.setText(R.string.admin_mod_warning_pill);
                item.tvReviewStatus.setBackgroundResource(R.drawable.bg_admin_pill_danger);
                item.tvReviewStatus.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                item.tvReviewDeadline.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                break;
            case AdminReview.TONE_OVERDUE:
                item.tvReviewStatus.setText(R.string.admin_mod_overdue_pill);
                item.tvReviewStatus.setBackgroundResource(R.drawable.bg_admin_pill_neutral);
                item.tvReviewStatus.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                item.tvReviewDeadline.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                break;
            case AdminReview.TONE_PRIORITY:
            default:
                item.tvReviewStatus.setText(R.string.admin_mod_priority_pill);
                item.tvReviewStatus.setBackgroundResource(R.drawable.bg_admin_pill_warning);
                item.tvReviewStatus.setTextColor(
                        requireContext().getColor(R.color.admin_warning_text));
                item.tvReviewDeadline.setTextColor(
                        requireContext().getColor(R.color.admin_warning_text));
                break;
        }

        item.getRoot().setOnClickListener(v -> openDetail());
        item.btnReviewReject.setOnClickListener(v -> openDetail());
        item.btnReviewApprove.setOnClickListener(v -> approve(item.getRoot(), review));
    }

    private void approve(View card, AdminReview review) {
        reviews.remove(review);
        card.animate()
                .alpha(0f)
                .translationX(card.getWidth() * 0.15f)
                .setDuration(180)
                .withEndAction(() -> {
                    if (binding != null) {
                        binding.listReviews.removeView(card);
                    }
                })
                .start();
        if (binding != null) {
            Snackbar.make(binding.getRoot(), R.string.admin_review_approved, Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void openDetail() {
        startActivity(new Intent(requireContext(), AdminReviewDetailActivity.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
