package com.eventapp.app.ui.user.review;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StarRatingView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** FR-U20 write/edit review with stars, optional comment and moderation feedback. */
public class WriteReviewFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_review_event_id";
    public static final String ARG_EVENT_TITLE = "arg_review_event_title";
    public static final String ARG_EDIT_MODE = "arg_review_edit_mode";
    public static final String ARG_EVENT_END_AT = "arg_review_event_end_at";
    public static final String ARG_CAN_REVIEW = "arg_review_can_review";
    public static final String ARG_COMMENT = "arg_review_comment";
    public static final String ARG_RATING = "arg_review_rating";
    public static final String ARG_EDIT_COUNT = "arg_review_edit_count";

    private static final long DAY = 24L * 60L * 60L * 1000L;

    private WriteReviewViewModel viewModel;
    private StarRatingView stars;
    private TextView ratingLabel;
    private TextInputLayout commentLayout;
    private TextInputEditText commentInput;
    private TextView errorView;
    private TextView eligibility;
    private TextView eventTitle;
    private TextView eventTime;
    private StatusChip status;
    private View submit;
    private View editNote;

    private long eventId;
    private String title = "";
    private long eventEndAt;
    private boolean editMode;
    private boolean canReview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_review_write_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        readArguments();
        viewModel = new ViewModelProvider(this).get(WriteReviewViewModel.class);
        bindViews(view);
        setupForm();
        setupState();
        setupObservers();
        evaluateEligibility();
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        eventId = args.getLong(ARG_EVENT_ID, -1L);
        title = args.getString(ARG_EVENT_TITLE, "");
        eventEndAt = args.getLong(ARG_EVENT_END_AT, 0L);
        editMode = args.getBoolean(ARG_EDIT_MODE, false);
        canReview = args.getBoolean(ARG_CAN_REVIEW, true);
    }

    private void bindViews(View view) {
        stars = view.findViewById(R.id.ratingWriteStars);
        ratingLabel = view.findViewById(R.id.tvWriteRatingLabel);
        commentLayout = view.findViewById(R.id.tilWriteComment);
        commentInput = view.findViewById(R.id.etWriteComment);
        errorView = view.findViewById(R.id.tvWriteError);
        eligibility = view.findViewById(R.id.cardWriteEligibility);
        eventTitle = view.findViewById(R.id.tvWriteEventTitle);
        eventTime = view.findViewById(R.id.tvWriteEventTime);
        status = view.findViewById(R.id.chipWriteStatus);
        submit = view.findViewById(R.id.btnWriteSubmit);
        editNote = view.findViewById(R.id.tvWriteEditNote);
        view.findViewById(R.id.btnWriteBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupForm() {
        eventTitle.setText(TextUtils.isEmpty(title)
                ? getString(R.string.hc_reviews_title) : title);
        eventTime.setText(eventEndAt > 0L
                ? getString(R.string.hc_detail_time_range, formatDate(eventEndAt),
                formatTime(eventEndAt))
                : getString(R.string.hc_reviews_write_note));

        stars.setEditable(true);
        stars.setStarSize(40);
        stars.setRating(editMode ? 5f : 0f);
        stars.setOnRatingChangedListener(value -> {
            ratingLabel.setText(ratingLabel(value));
            errorView.setVisibility(View.GONE);
        });
        ratingLabel.setText(ratingLabel(stars.getRating()));

        commentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s == null ? "" : s.toString();
                if (text.trim().length() > 0 && text.trim().length() < 10) {
                    commentLayout.setHelperText(getString(R.string.hc_write_comment_min));
                } else {
                    commentLayout.setHelperText(null);
                }
            }
        });

        if (editMode) {
            ((TextView) requireView().findViewById(R.id.tvWriteTitle))
                    .setText(R.string.hc_write_title);
            ((TextView) submit).setText(R.string.hc_write_update);
            editNote.setVisibility(View.VISIBLE);
            Bundle args = getArguments();
            if (args != null) {
                float rating = args.getFloat(ARG_RATING, 5f);
                stars.setRating(rating);
                commentInput.setText(args.getString(ARG_COMMENT, ""));
                ratingLabel.setText(ratingLabel(rating));
            }
        }

        submit.setOnClickListener(v -> {
            if (!canReview) {
                return;
            }
            viewModel.submit(stars.getRating(), textOf(commentInput), editMode);
        });
    }

    private void setupState() {
        status.setStatus(getString(canReview ? R.string.hc_review_status_visible
                : R.string.hc_review_status_hidden), canReview
                ? StatusChip.Tone.SUCCESS : StatusChip.Tone.MUTED);
        strandEligibility();
    }

    private void strandEligibility() {
        // Kept separate for readability of the eligibility UI in this dummy data slice.
        if (canReview) {
            status.setStatus(getString(R.string.hc_detail_write_review),
                    StatusChip.Tone.SUCCESS);
        }
    }

    private void evaluateEligibility() {
        long now = System.currentTimeMillis();
        String reason = null;
        if (!canReview) {
            if (eventEndAt > now) {
                reason = getString(R.string.hc_write_event_not_ended);
            } else if (eventEndAt > 0L && now - eventEndAt > 30L * DAY) {
                reason = getString(R.string.hc_write_window_closed);
            } else {
                reason = getString(R.string.hc_write_not_attended);
            }
        }
        boolean blocked = reason != null;
        eligibility.setVisibility(blocked ? View.VISIBLE : View.GONE);
        eligibility.setText(reason);
        submit.setEnabled(!blocked);
        submit.setAlpha(blocked ? 0.55f : 1f);
        stars.setEnabled(!blocked);
        commentInput.setEnabled(!blocked);
        if (blocked) {
            status.setStatus(getString(R.string.hc_review_status_hidden),
                    StatusChip.Tone.MUTED);
        }
    }

    private void setupObservers() {
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), submitting -> {
            boolean busy = Boolean.TRUE.equals(submitting);
            submit.setEnabled(!busy && canReview);
            submit.setAlpha(busy ? 0.6f : (canReview ? 1f : 0.55f));
            ((TextView) submit).setText(busy ? getString(R.string.hc_write_sending)
                    : getString(editMode ? R.string.hc_write_update : R.string.hc_write_submit));
        });

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (TextUtils.isEmpty(message)) {
                errorView.setVisibility(View.GONE);
                return;
            }
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null || !result.success || !isAdded()) {
                return;
            }
            showResultDialog(result.held);
        });
    }

    private void showResultDialog(boolean held) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(held ? R.string.hc_write_held : R.string.hc_write_success)
                .setMessage(held
                        ? R.string.hc_write_held_message
                        : R.string.hc_write_success_message)
                .setPositiveButton(R.string.hc_action_done, (dialog, which) -> {
                    viewModel.resetResult();
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .setCancelable(false)
                .show();
    }

    private String ratingLabel(float rating) {
        int value = Math.round(rating);
        switch (value) {
            case 1:
                return getString(R.string.hc_write_rating_1);
            case 2:
                return getString(R.string.hc_write_rating_2);
            case 3:
                return getString(R.string.hc_write_rating_3);
            case 4:
                return getString(R.string.hc_write_rating_4);
            case 5:
                return getString(R.string.hc_write_rating_5);
            default:
                return getString(R.string.hc_write_stars_hint);
        }
    }

    private String textOf(TextView view) {
        return view == null || view.getText() == null ? "" : view.getText().toString();
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"))
                .format(new Date(millis));
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm", new Locale("vi", "VN"))
                .format(new Date(millis));
    }
}
