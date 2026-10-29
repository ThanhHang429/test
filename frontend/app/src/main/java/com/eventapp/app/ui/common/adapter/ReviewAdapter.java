package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StarRatingView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.user.review.ReviewUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared review row for event detail, the public review list and admin moderation (FR-U20/A14). */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public enum Mode {
        COMPACT,
        FULL,
        MODERATION
    }

    public interface OnReviewActionListener {
        void onEditReview(ReviewUiModel review);

        void onReportReview(ReviewUiModel review);

        void onModerateReview(ReviewUiModel review);
    }

    private final List<ReviewUiModel> items = new ArrayList<>();
    private final Mode mode;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
    private OnReviewActionListener listener;
    private boolean showEventName;

    public ReviewAdapter() {
        this(Mode.FULL);
    }

    public ReviewAdapter(Mode mode) {
        this.mode = mode == null ? Mode.FULL : mode;
    }

    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.listener = listener;
    }

    public void setShowEventName(boolean showEventName) {
        this.showEventName = showEventName;
    }

    public void submitList(List<ReviewUiModel> reviews) {
        items.clear();
        if (reviews != null) {
            items.addAll(reviews);
        }
        notifyDataSetChanged();
    }

    public List<ReviewUiModel> getItems() {
        return new ArrayList<>(items);
    }

    public ReviewUiModel getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewUiModel review = items.get(position);
        Context context = holder.itemView.getContext();

        holder.avatar.setText(initials(review.authorName));
        holder.author.setText(review.authorName);
        holder.stars.setStarSize(16);
        holder.stars.setRating(review.rating);
        holder.date.setText(dateFormat.format(new Date(review.createdAt)));

        boolean edited = review.editCount > 0 || review.editedAt > 0L;
        holder.edited.setVisibility(edited ? View.VISIBLE : View.GONE);

        boolean showCode = mode == Mode.MODERATION && !TextUtils.isEmpty(review.authorCode);
        holder.authorCode.setVisibility(showCode ? View.VISIBLE : View.GONE);
        if (showCode) {
            holder.authorCode.setText(context.getString(R.string.hc_review_author_code,
                    review.authorCode));
        }

        boolean showEvent = (showEventName || mode == Mode.MODERATION)
                && !TextUtils.isEmpty(review.eventTitle);
        holder.event.setVisibility(showEvent ? View.VISIBLE : View.GONE);
        holder.event.setText(review.eventTitle);

        if (TextUtils.isEmpty(review.comment)) {
            holder.comment.setText(R.string.hc_review_no_comment);
            holder.comment.setTextColor(ContextCompat.getColor(context, R.color.ds_text_muted));
        } else {
            holder.comment.setText(review.comment);
            holder.comment.setTextColor(ContextCompat.getColor(context, R.color.ds_text));
        }

        bindStatus(holder.status, review);
        bindSource(holder.source, review);
        bindFlag(holder, review);
        bindActions(holder, review);
    }

    private void bindStatus(StatusChip chip, ReviewUiModel review) {
        if (chip == null) {
            return;
        }
        boolean show = mode == Mode.MODERATION || review.mine
                || review.status != ReviewUiModel.Status.VISIBLE;
        chip.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        switch (review.status) {
            case HELD:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_status_held),
                        StatusChip.Tone.WARNING);
                break;
            case HIDDEN:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_status_hidden),
                        StatusChip.Tone.DANGER);
                break;
            case VISIBLE:
            default:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_status_visible),
                        StatusChip.Tone.SUCCESS);
                break;
        }
    }

    private void bindSource(StatusChip chip, ReviewUiModel review) {
        if (chip == null) {
            return;
        }
        boolean show = mode == Mode.MODERATION && review.source != ReviewUiModel.Source.USER;
        chip.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        switch (review.source) {
            case HELD:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_source_held),
                        StatusChip.Tone.WARNING);
                break;
            case FLAGGED:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_source_flagged),
                        StatusChip.Tone.DANGER);
                break;
            case REPORTED:
                chip.setStatus(chip.getContext().getString(R.string.hc_review_source_reported),
                        StatusChip.Tone.INFO);
                break;
            case USER:
            default:
                chip.setVisibility(View.GONE);
                break;
        }
    }

    private void bindFlag(ReviewViewHolder holder, ReviewUiModel review) {
        boolean show = mode == Mode.MODERATION
                && (!TextUtils.isEmpty(review.flagReason) || !TextUtils.isEmpty(review.evidence));
        holder.flag.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        String severity = TextUtils.isEmpty(review.severity) ? "LOW" : review.severity;
        holder.flagTitle.setText(holder.itemView.getContext().getString(
                R.string.hc_review_flag_title, severity));
        holder.flagReason.setText(review.flagReason);
        boolean hasEvidence = !TextUtils.isEmpty(review.evidence);
        holder.evidence.setVisibility(hasEvidence ? View.VISIBLE : View.GONE);
        holder.evidence.setText(hasEvidence
                ? holder.itemView.getContext().getString(R.string.hc_review_evidence,
                review.evidence)
                : null);

        boolean hasScores = review.spamScore != null || review.toxicityScore != null;
        holder.scores.setVisibility(hasScores ? View.VISIBLE : View.GONE);
        if (hasScores) {
            holder.scores.setText(holder.itemView.getContext().getString(
                    R.string.hc_review_scores,
                    review.spamScore == null ? 0 : review.spamScore,
                    review.toxicityScore == null ? 0 : review.toxicityScore));
        }
    }

    private void bindActions(ReviewViewHolder holder, ReviewUiModel review) {
        boolean moderation = mode == Mode.MODERATION;
        boolean showActions = mode != Mode.COMPACT;
        holder.moderate.setVisibility(moderation ? View.VISIBLE : View.GONE);
        holder.edit.setVisibility(showActions && !moderation && review.mine
                && review.status != ReviewUiModel.Status.HIDDEN
                ? View.VISIBLE : View.GONE);
        holder.report.setVisibility(showActions && !moderation && !review.mine
                ? View.VISIBLE : View.GONE);

        holder.edit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditReview(review);
            }
        });
        holder.report.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReportReview(review);
            }
        });
        holder.moderate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModerateReview(review);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        final TextView avatar;
        final TextView author;
        final TextView authorCode;
        final TextView event;
        final StarRatingView stars;
        final TextView date;
        final TextView edited;
        final TextView comment;
        final StatusChip status;
        final StatusChip source;
        final View flag;
        final TextView flagTitle;
        final TextView flagReason;
        final TextView evidence;
        final TextView scores;
        final View edit;
        final View report;
        final View moderate;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tvReviewAvatar);
            author = itemView.findViewById(R.id.tvReviewAuthor);
            authorCode = itemView.findViewById(R.id.tvReviewAuthorCode);
            event = itemView.findViewById(R.id.tvReviewEvent);
            stars = itemView.findViewById(R.id.ratingReviewStars);
            date = itemView.findViewById(R.id.tvReviewDate);
            edited = itemView.findViewById(R.id.tvReviewEdited);
            comment = itemView.findViewById(R.id.tvReviewComment);
            status = itemView.findViewById(R.id.chipReviewStatus);
            source = itemView.findViewById(R.id.chipReviewSource);
            flag = itemView.findViewById(R.id.layoutReviewFlag);
            flagTitle = itemView.findViewById(R.id.tvReviewFlagTitle);
            flagReason = itemView.findViewById(R.id.tvReviewFlagReason);
            evidence = itemView.findViewById(R.id.tvReviewEvidence);
            scores = itemView.findViewById(R.id.tvReviewScores);
            edit = itemView.findViewById(R.id.btnReviewEdit);
            report = itemView.findViewById(R.id.btnReviewReport);
            moderate = itemView.findViewById(R.id.btnReviewModerate);
        }
    }
}
