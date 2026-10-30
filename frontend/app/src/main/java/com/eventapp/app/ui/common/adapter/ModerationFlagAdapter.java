package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.event.ModerationUiModel;
import com.eventapp.app.ui.common.view.SeverityChip;
import com.eventapp.app.ui.common.view.StatusChip;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** FR-A07 flag row: target, severity, source/confidence, evidence and confirm/dismiss/rescan. */
public class ModerationFlagAdapter
        extends RecyclerView.Adapter<ModerationFlagAdapter.FlagViewHolder> {

    public interface OnFlagActionListener {
        void onConfirm(ModerationUiModel flag);

        void onDismiss(ModerationUiModel flag);

        void onRescan(ModerationUiModel flag);
    }

    private final List<ModerationUiModel> items = new ArrayList<>();
    private OnFlagActionListener listener;

    public void setOnFlagActionListener(OnFlagActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ModerationUiModel> flags) {
        List<ModerationUiModel> next = flags == null ? new ArrayList<>() : new ArrayList<>(flags);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return items.get(oldPosition).id == next.get(newPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return sameContents(items.get(oldPosition), next.get(newPosition));
            }
        });
        items.clear();
        items.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    public ModerationUiModel getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    @NonNull
    @Override
    public FlagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_moderation_flag, parent, false);
        return new FlagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FlagViewHolder holder, int position) {
        ModerationUiModel flag = items.get(position);
        Context context = holder.itemView.getContext();

        holder.target.setText(flag.targetName);
        holder.meta.setText(context.getString(R.string.hc_mod_meta,
                targetTypeLabel(context, flag.targetType), relativeTime(context, flag.createdAt)));
        holder.icon.setImageResource(targetIcon(flag.targetType));
        SeverityChip.bind(holder.severity, flag.severity.name());

        bindSource(holder, flag);
        bindNotes(holder, flag);
        bindStatus(holder, flag);

        boolean hasEvidence = !TextUtils.isEmpty(flag.reason)
                || !TextUtils.isEmpty(flag.evidence);
        holder.evidenceBlock.setVisibility(hasEvidence ? View.VISIBLE : View.GONE);
        holder.reason.setText(flag.reason);
        holder.evidence.setText(TextUtils.isEmpty(flag.evidence)
                ? null
                : context.getString(R.string.hc_review_evidence, flag.evidence));
        holder.evidence.setVisibility(TextUtils.isEmpty(flag.evidence)
                ? View.GONE : View.VISIBLE);

        boolean open = flag.isOpen();
        holder.actions.setVisibility(open ? View.VISIBLE : View.GONE);
        holder.confirm.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirm(flag);
            }
        });
        holder.dismiss.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDismiss(flag);
            }
        });
        holder.rescan.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRescan(flag);
            }
        });
    }

    private void bindSource(FlagViewHolder holder, ModerationUiModel flag) {
        Context context = holder.itemView.getContext();
        boolean ai = flag.source == ModerationUiModel.Source.AI;
        holder.source.setStatus(context.getString(ai
                        ? R.string.hc_mod_source_ai : R.string.hc_mod_source_rule),
                ai ? StatusChip.Tone.PRIMARY : StatusChip.Tone.MUTED);
        boolean hasConfidence = flag.confidence > 0;
        holder.confidence.setVisibility(hasConfidence ? View.VISIBLE : View.GONE);
        holder.confidence.setText(hasConfidence
                ? context.getString(R.string.hc_mod_confidence, flag.confidence)
                : null);
    }

    private void bindNotes(FlagViewHolder holder, ModerationUiModel flag) {
        boolean hasNotes = flag.unchecked || flag.contentChanged;
        holder.notes.setVisibility(hasNotes ? View.VISIBLE : View.GONE);
        holder.unchecked.setVisibility(flag.unchecked ? View.VISIBLE : View.GONE);
        holder.changed.setVisibility(flag.contentChanged ? View.VISIBLE : View.GONE);
    }

    private void bindStatus(FlagViewHolder holder, ModerationUiModel flag) {
        Context context = holder.itemView.getContext();
        switch (flag.status) {
            case CONFIRMED:
                holder.status.setStatus(context.getString(R.string.hc_mod_status_confirmed),
                        StatusChip.Tone.DANGER);
                holder.status.setVisibility(View.VISIBLE);
                break;
            case DISMISSED:
                holder.status.setStatus(context.getString(R.string.hc_mod_status_dismissed),
                        StatusChip.Tone.MUTED);
                holder.status.setVisibility(View.VISIBLE);
                break;
            case OPEN:
            default:
                holder.status.setVisibility(View.GONE);
                break;
        }
    }

    private String targetTypeLabel(Context context, ModerationUiModel.TargetType type) {
        switch (type) {
            case EVENT_IMAGE:
                return context.getString(R.string.hc_mod_target_image);
            case TAG:
                return context.getString(R.string.hc_mod_target_tag);
            case REVIEW:
                return context.getString(R.string.hc_mod_target_review);
            case EVENT_TEXT:
            default:
                return context.getString(R.string.hc_mod_target_event);
        }
    }

    private int targetIcon(ModerationUiModel.TargetType type) {
        switch (type) {
            case EVENT_IMAGE:
                return R.drawable.ic_ds_image;
            case TAG:
                return R.drawable.ic_ds_flag;
            case REVIEW:
                return R.drawable.ic_review;
            case EVENT_TEXT:
            default:
                return R.drawable.ic_ds_event;
        }
    }

    private String relativeTime(Context context, long createdAt) {
        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 60_000L) {
            return context.getString(R.string.hc_mod_time_now);
        }
        if (diff < 3_600_000L) {
            return context.getString(R.string.hc_mod_time_minutes, diff / 60_000L);
        }
        if (diff < 86_400_000L) {
            return context.getString(R.string.hc_mod_time_hours, diff / 3_600_000L);
        }
        return context.getString(R.string.hc_mod_time_days, diff / 86_400_000L);
    }

    private boolean sameContents(ModerationUiModel left, ModerationUiModel right) {
        return left.status == right.status
                && left.source == right.source
                && left.confidence == right.confidence
                && left.unchecked == right.unchecked
                && left.contentChanged == right.contentChanged
                && left.reviewedAt == right.reviewedAt
                && Objects.equals(left.reason, right.reason)
                && Objects.equals(left.evidence, right.evidence)
                && Objects.equals(left.reviewNote, right.reviewNote)
                && Objects.equals(left.targetName, right.targetName);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FlagViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView target;
        final TextView meta;
        final StatusChip severity;
        final StatusChip status;
        final StatusChip source;
        final TextView confidence;
        final View notes;
        final StatusChip unchecked;
        final StatusChip changed;
        final View evidenceBlock;
        final TextView reason;
        final TextView evidence;
        final View actions;
        final View confirm;
        final View dismiss;
        final View rescan;

        FlagViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivFlagIcon);
            target = itemView.findViewById(R.id.tvFlagTarget);
            meta = itemView.findViewById(R.id.tvFlagMeta);
            severity = itemView.findViewById(R.id.chipFlagSeverity);
            status = itemView.findViewById(R.id.chipFlagStatus);
            source = itemView.findViewById(R.id.chipFlagSource);
            confidence = itemView.findViewById(R.id.tvFlagConfidence);
            notes = itemView.findViewById(R.id.layoutFlagNotes);
            unchecked = itemView.findViewById(R.id.chipFlagUnchecked);
            changed = itemView.findViewById(R.id.chipFlagChanged);
            evidenceBlock = itemView.findViewById(R.id.layoutFlagEvidence);
            reason = itemView.findViewById(R.id.tvFlagReason);
            evidence = itemView.findViewById(R.id.tvFlagEvidence);
            actions = itemView.findViewById(R.id.layoutFlagActions);
            confirm = itemView.findViewById(R.id.btnFlagConfirm);
            dismiss = itemView.findViewById(R.id.btnFlagDismiss);
            rescan = itemView.findViewById(R.id.btnFlagRescan);
        }
    }
}
