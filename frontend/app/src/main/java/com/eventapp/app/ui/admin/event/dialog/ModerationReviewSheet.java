package com.eventapp.app.ui.admin.event.dialog;


import com.eventapp.app.ui.admin.event.ContentModerationViewModel;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.SeverityChip;
import com.eventapp.app.ui.common.view.StarRatingView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * FR-A14 review detail bottom sheet. It only collects the chosen action; the parent fragment owns
 * the reason dialog and the queue update so both lanes share one Snackbar/undo path.
 */
public class ModerationReviewSheet extends BottomSheetDialogFragment {

    public static final String REQUEST_KEY = "moderation_review_request";
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_REVIEW_ID = "review_id";

    public static final String ACTION_HIDE = "hide";
    public static final String ACTION_KEEP = "keep";
    public static final String ACTION_REQUEST_EDIT = "request_edit";

    private static final String ARG_REVIEW_ID = "arg_review_id";

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));

    public static ModerationReviewSheet newInstance(long reviewId) {
        ModerationReviewSheet sheet = new ModerationReviewSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_REVIEW_ID, reviewId);
        sheet.setArguments(args);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(ignored -> {
            FrameLayout sheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) {
                return;
            }
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_moderation_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long reviewId = getArguments() == null ? 0L : getArguments().getLong(ARG_REVIEW_ID);
        ContentModerationViewModel viewModel =
                new ViewModelProvider(requireParentFragment())
                        .get(ContentModerationViewModel.class);
        ReviewUiModel review = viewModel.findReview(reviewId);
        if (review == null) {
            dismiss();
            return;
        }
        bind(view, review);
    }

    private void bind(View view, ReviewUiModel review) {
        TextView avatar = view.findViewById(R.id.tvA14Avatar);
        TextView author = view.findViewById(R.id.tvA14Author);
        TextView authorCode = view.findViewById(R.id.tvA14AuthorCode);
        TextView event = view.findViewById(R.id.tvA14Event);
        StarRatingView stars = view.findViewById(R.id.ratingA14Stars);
        TextView date = view.findViewById(R.id.tvA14Date);
        StatusChip status = view.findViewById(R.id.chipA14Status);
        StatusChip source = view.findViewById(R.id.chipA14Source);
        StatusChip severity = view.findViewById(R.id.chipA14Severity);
        TextView content = view.findViewById(R.id.tvA14Content);
        View flagBlock = view.findViewById(R.id.layoutA14Flag);
        TextView flagTitle = view.findViewById(R.id.tvA14FlagTitle);
        TextView flagReason = view.findViewById(R.id.tvA14FlagReason);
        TextView evidence = view.findViewById(R.id.tvA14Evidence);
        TextView scores = view.findViewById(R.id.tvA14Scores);

        avatar.setText(initials(review.authorName));
        author.setText(shortAuthorName(review.authorName));
        boolean hasCode = !TextUtils.isEmpty(review.authorCode);
        authorCode.setVisibility(hasCode ? View.VISIBLE : View.GONE);
        authorCode.setText(hasCode
                ? getString(R.string.hc_review_author_code, review.authorCode)
                : null);

        event.setText(getString(R.string.hc_mod_a14_event, review.eventTitle, review.eventId));
        stars.setStarSize(16);
        stars.setRating(review.rating);
        date.setText(dateFormat.format(new Date(review.createdAt)));

        bindStatus(status, review);
        bindSource(source, review);
        SeverityChip.bind(severity, review.severity);

        boolean hasComment = !TextUtils.isEmpty(review.comment);
        content.setText(hasComment ? review.comment : getString(R.string.hc_review_no_comment));
        content.setTextColor(ContextCompat.getColor(requireContext(),
                hasComment ? R.color.ds_text : R.color.ds_text_muted));

        boolean hasFlag = !TextUtils.isEmpty(review.flagReason)
                || !TextUtils.isEmpty(review.evidence);
        flagBlock.setVisibility(hasFlag ? View.VISIBLE : View.GONE);
        if (hasFlag) {
            flagTitle.setText(getString(R.string.hc_review_flag_title,
                    TextUtils.isEmpty(review.severity) ? "LOW" : review.severity));
            flagReason.setText(review.flagReason);
            boolean hasEvidence = !TextUtils.isEmpty(review.evidence);
            evidence.setVisibility(hasEvidence ? View.VISIBLE : View.GONE);
            evidence.setText(hasEvidence
                    ? getString(R.string.hc_review_evidence, review.evidence)
                    : null);
            boolean hasScores = review.spamScore != null || review.toxicityScore != null;
            scores.setVisibility(hasScores ? View.VISIBLE : View.GONE);
            if (hasScores) {
                scores.setText(getString(R.string.hc_review_scores,
                        review.spamScore == null ? 0 : review.spamScore,
                        review.toxicityScore == null ? 0 : review.toxicityScore));
            }
        }

        view.findViewById(R.id.btnA14Close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnA14Hide).setOnClickListener(v ->
                sendAction(ACTION_HIDE, review.id));
        view.findViewById(R.id.btnA14Keep).setOnClickListener(v ->
                sendAction(ACTION_KEEP, review.id));
        view.findViewById(R.id.btnA14RequestEdit).setOnClickListener(v ->
                sendAction(ACTION_REQUEST_EDIT, review.id));
    }

    private void bindStatus(StatusChip chip, ReviewUiModel review) {
        switch (review.status) {
            case HELD:
                chip.setStatus(getString(R.string.hc_review_status_held),
                        StatusChip.Tone.WARNING);
                break;
            case HIDDEN:
                chip.setStatus(getString(R.string.hc_review_status_hidden),
                        StatusChip.Tone.DANGER);
                break;
            case VISIBLE:
            default:
                chip.setStatus(getString(R.string.hc_review_status_visible),
                        StatusChip.Tone.SUCCESS);
                break;
        }
    }

    private void bindSource(StatusChip chip, ReviewUiModel review) {
        switch (review.source) {
            case HELD:
                chip.setStatus(getString(R.string.hc_mod_source_held),
                        StatusChip.Tone.WARNING);
                break;
            case FLAGGED:
                chip.setStatus(getString(R.string.hc_mod_source_flagged),
                        StatusChip.Tone.DANGER);
                break;
            case REPORTED:
                chip.setStatus(getString(R.string.hc_mod_source_reported),
                        StatusChip.Tone.INFO);
                break;
            case USER:
            default:
                chip.setVisibility(View.GONE);
                break;
        }
    }

    private void sendAction(String action, long reviewId) {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, action);
        result.putLong(RESULT_REVIEW_ID, reviewId);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private String shortAuthorName(String name) {
        if (TextUtils.isEmpty(name)) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String last = parts[parts.length - 1];
        return parts[0] + " " + Character.toUpperCase(last.charAt(0)) + ".";
    }

    private String initials(String name) {
        if (TextUtils.isEmpty(name)) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length && builder.length() < 2; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return builder.length() == 0 ? "?" : builder.toString();
    }
}
