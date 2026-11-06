package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityEventReviewsBinding;
import com.eventapp.app.databinding.DialogReviewEditorBinding;
import com.eventapp.app.databinding.ItemEventReviewBinding;
import com.eventapp.app.databinding.ItemReviewDistributionBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.model.ReviewSummaryUiModel;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.eventapp.app.ui.user.util.StarText;
import com.eventapp.app.ui.user.util.UserFmt;
import com.eventapp.app.ui.user.viewmodel.EventReviewsViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Danh sách đánh giá của một sự kiện: tổng hợp điểm, viết/sửa đánh giá và báo cáo (FR-U20). */
public class EventReviewsActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_EVENT_TITLE = "event_title";

    private static final String[] REPORT_CODES = {
            "SPAM", "OFFENSIVE", "PERSONAL_DATA", "FAKE_REVIEW", "OFF_TOPIC", "OTHER"
    };

    private ActivityEventReviewsBinding binding;
    private EventReviewsViewModel viewModel;
    private long eventId;
    private String eventTitle;

    private ReviewUiModel mine;
    private AlertDialog editorDialog;

    public static void start(@NonNull Context context, long eventId, @Nullable String eventTitle) {
        Intent intent = new Intent(context, EventReviewsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_EVENT_TITLE, eventTitle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEventReviewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        eventTitle = getIntent().getStringExtra(EXTRA_EVENT_TITLE);
        if (eventId <= 0L) {
            finish();
            return;
        }

        binding.tvEventName.setText(eventTitle == null ? "" : eventTitle);
        binding.btnBack.setOnClickListener(v -> finish());
        binding.chipNewest.setOnClickListener(v -> selectSort("NEWEST"));
        binding.chipHigh.setOnClickListener(v -> selectSort("RATING_DESC"));
        binding.chipLow.setOnClickListener(v -> selectSort("RATING_ASC"));
        binding.btnWrite.setOnClickListener(v -> openEditor());
        binding.btnEditMine.setOnClickListener(v -> openEditor());
        binding.btnLoadMore.setOnClickListener(v -> viewModel.loadMore());

        viewModel = new ViewModelProvider(this).get(EventReviewsViewModel.class);
        observe();
        viewModel.init(eventId);
        selectSort("NEWEST");
    }

    private void observe() {
        viewModel.getSummary().observe(this, summary -> {
            if (summary != null && binding != null) {
                bindSummary(summary);
            }
        });
        viewModel.getReviews().observe(this, data -> {
            if (binding != null) {
                renderReviews();
            }
        });
        viewModel.getLoading().observe(this, loading -> {
            if (binding != null) {
                binding.progress.setVisibility(Boolean.TRUE.equals(loading)
                        ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.getMine().observe(this, data -> {
            if (binding != null) {
                mine = data;
                bindMine();
            }
        });
        viewModel.getError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
            renderReviews();
        });
        viewModel.getSubmitNotice().observe(this, notice -> {
            if (notice == null) {
                return;
            }
            viewModel.consumeSubmitNotice();
            dismissEditor();
            String message = !notice.isEmpty()
                    ? notice : getString(R.string.user_reviews_saved);
            if (binding != null) {
                SnackPill.show(binding.getRoot(), message, dp(96));
            }
        });
        viewModel.getSubmitError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeSubmitError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
        });
        viewModel.getReportSent().observe(this, sent -> {
            if (!Boolean.TRUE.equals(sent)) {
                return;
            }
            viewModel.consumeReportSent();
            if (binding != null) {
                SnackPill.show(binding.getRoot(),
                        getString(R.string.user_reviews_report_sent), dp(96));
            }
        });
        viewModel.getReportError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeReportError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
        });
    }

    private void selectSort(String value) {
        binding.chipNewest.setSelected("NEWEST".equals(value));
        binding.chipHigh.setSelected("RATING_DESC".equals(value));
        binding.chipLow.setSelected("RATING_ASC".equals(value));
        viewModel.selectSort(value);
    }

    private void dismissEditor() {
        if (editorDialog != null) {
            if (editorDialog.isShowing()) {
                editorDialog.dismiss();
            }
            editorDialog = null;
        }
    }

    // ------------------------------------------------------------------ tổng hợp

    private void bindSummary(ReviewSummaryUiModel summary) {
        binding.tvAverage.setText(String.format(Locale.US, "%.1f", summary.averageRating)
                .replace('.', ','));
        StarText.apply(binding.tvAverageStars, (float) summary.averageRating);
        binding.tvTotal.setText(getString(R.string.user_reviews_total, summary.totalReviews));

        binding.distributionList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int star = 5; star >= 1; star--) {
            long count = 0L;
            if (summary.distribution != null) {
                Long value = summary.distribution.get(String.valueOf(star));
                count = value == null ? 0L : value;
            }
            int percent = summary.totalReviews <= 0
                    ? 0 : (int) Math.round(count * 100d / summary.totalReviews);
            ItemReviewDistributionBinding row = ItemReviewDistributionBinding.inflate(
                    inflater, binding.distributionList, false);
            row.tvStar.setText(String.valueOf(star));
            row.bar.setProgress(percent);
            row.tvCount.setText(String.valueOf(count));
            binding.distributionList.addView(row.getRoot());
        }
    }

    // -------------------------------------------------------------------- danh sách

    private void renderReviews() {
        List<ReviewUiModel> reviews = viewModel.getReviews().getValue();
        if (reviews == null) {
            reviews = new ArrayList<>();
        }
        binding.reviewList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (ReviewUiModel review : reviews) {
            ItemEventReviewBinding row = ItemEventReviewBinding.inflate(
                    inflater, binding.reviewList, false);
            bindReview(row, review);
            binding.reviewList.addView(row.getRoot());
        }
        boolean empty = reviews.isEmpty();
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        boolean hasNext = Boolean.TRUE.equals(viewModel.getHasNext().getValue());
        binding.btnLoadMore.setVisibility(hasNext ? View.VISIBLE : View.GONE);
    }

    private void bindReview(ItemEventReviewBinding row, ReviewUiModel review) {
        row.tvAvatar.setText(initials(review.authorName));
        row.tvAuthor.setText(review.authorName);
        row.tvDate.setText(UserFmt.relative(review.createdAt));
        StarText.apply(row.tvStars, review.rating);

        boolean hasText = review.comment != null && !review.comment.trim().isEmpty();
        row.tvText.setText(hasText ? review.comment : "");
        row.tvText.setVisibility(hasText ? View.VISIBLE : View.GONE);

        String note = "";
        if (review.mine) {
            note = getString(R.string.user_reviews_mine_title);
        } else if (review.editedAt > review.createdAt) {
            note = getString(R.string.user_reviews_edited);
        }
        row.tvNote.setText(note);
        row.tvNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);

        row.btnReport.setVisibility(review.mine ? View.GONE : View.VISIBLE);
        row.btnReport.setOnClickListener(v -> showReportDialog(review));
    }

    // ---------------------------------------------------------------- đánh giá của tôi

    private void bindMine() {
        boolean has = mine != null;
        binding.mineCard.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.btnWrite.setText(has ? R.string.user_reviews_edit : R.string.user_reviews_write);
        if (!has) {
            return;
        }
        StarText.apply(binding.tvMineStars, mine.rating);
        boolean hasText = !mine.comment.trim().isEmpty();
        binding.tvMineText.setText(hasText ? mine.comment : "");
        binding.tvMineText.setVisibility(hasText ? View.VISIBLE : View.GONE);

        switch (mine.status) {
            case HELD:
                binding.tvMineStatus.setText(R.string.user_reviews_status_pending);
                binding.tvMineStatus.setBackgroundResource(R.drawable.bg_user_badge_warning);
                binding.tvMineStatus.setTextColor(getColor(R.color.user_warning_text));
                break;
            case HIDDEN:
                binding.tvMineStatus.setText(R.string.user_reviews_status_hidden);
                binding.tvMineStatus.setBackgroundResource(R.drawable.bg_user_badge_danger);
                binding.tvMineStatus.setTextColor(getColor(R.color.user_danger_text));
                break;
            default:
                binding.tvMineStatus.setText(R.string.user_reviews_status_visible);
                binding.tvMineStatus.setBackgroundResource(R.drawable.bg_user_badge_success);
                binding.tvMineStatus.setTextColor(getColor(R.color.user_success_text));
                break;
        }
    }

    // ------------------------------------------------------------------- soạn thảo

    private void openEditor() {
        DialogReviewEditorBinding dialogBinding = DialogReviewEditorBinding.inflate(
                getLayoutInflater());
        boolean editing = mine != null;
        dialogBinding.tvTitle.setText(editing
                ? R.string.user_reviews_edit : R.string.user_reviews_write);

        int[] selected = {editing ? Math.round(mine.rating) : 0};
        renderStarPicker(dialogBinding, selected[0]);
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            starView(dialogBinding, i).setOnClickListener(v -> {
                selected[0] = value;
                renderStarPicker(dialogBinding, value);
            });
        }

        if (editing && !mine.comment.isEmpty()) {
            dialogBinding.etReview.setText(mine.comment);
            dialogBinding.etReview.setSelection(dialogBinding.etReview.getText().length());
        }
        updateCounter(dialogBinding);
        dialogBinding.etReview.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateCounter(dialogBinding);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.user_close, null)
                .setPositiveButton(R.string.user_send, null)
                .create();
        editorDialog = dialog;
        dialog.setOnDismissListener(d -> {
            if (editorDialog == dialog) {
                editorDialog = null;
            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (selected[0] <= 0) {
                dialogBinding.tvError.setText(R.string.user_reviews_rating_required);
                dialogBinding.tvError.setVisibility(View.VISIBLE);
                return;
            }
            dialogBinding.tvError.setVisibility(View.GONE);
            submitReview(selected[0], dialogBinding.etReview.getText() == null
                    ? "" : dialogBinding.etReview.getText().toString().trim());
        });
    }

    private View starView(DialogReviewEditorBinding binding, int index) {
        switch (index) {
            case 0:
                return binding.star1;
            case 1:
                return binding.star2;
            case 2:
                return binding.star3;
            case 3:
                return binding.star4;
            default:
                return binding.star5;
        }
    }

    private void renderStarPicker(DialogReviewEditorBinding binding, int rating) {
        for (int i = 0; i < 5; i++) {
            android.widget.ImageView star = (android.widget.ImageView) starView(binding, i);
            star.setImageResource(i < rating ? R.drawable.ic_ds_star_filled : R.drawable.ic_ds_star);
            star.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getColor(i < rating ? R.color.user_warning : R.color.user_border_strong)));
        }
    }

    private void updateCounter(DialogReviewEditorBinding binding) {
        int length = binding.etReview.getText() == null ? 0 : binding.etReview.getText().length();
        binding.tvCounter.setText(getString(R.string.user_reviews_counter, length));
    }

    private void submitReview(int rating, String text) {
        viewModel.submit(rating, text);
    }

    // ---------------------------------------------------------------------- báo cáo

    private void showReportDialog(ReviewUiModel review) {
        String[] labels = {
                getString(R.string.user_reviews_report_spam),
                getString(R.string.user_reviews_report_offensive),
                getString(R.string.user_reviews_report_personal),
                getString(R.string.user_reviews_report_fake),
                getString(R.string.user_reviews_report_off_topic),
                getString(R.string.user_reviews_report_other)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.user_reviews_report_title)
                .setItems(labels, (dialog, which) -> report(review.id, REPORT_CODES[which]))
                .setNegativeButton(R.string.user_close, null)
                .show();
    }

    private void report(long reviewId, String reasonCode) {
        viewModel.report(reviewId, reasonCode);
    }

    private static String initials(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            builder.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return builder.toString();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) {
            viewModel.refreshDetails();
        }
    }
}
