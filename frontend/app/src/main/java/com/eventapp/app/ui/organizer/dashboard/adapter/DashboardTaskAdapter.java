package com.eventapp.app.ui.organizer.dashboard.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.DashboardUiModel;

import java.util.ArrayList;
import java.util.List;

/** "Việc cần xử lý" rows for FR-O03, ordered by urgency by the ViewModel. */
public class DashboardTaskAdapter
        extends RecyclerView.Adapter<DashboardTaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(DashboardUiModel.Task task);
    }

    private final List<DashboardUiModel.Task> items = new ArrayList<>();
    private OnTaskClickListener listener;

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DashboardUiModel.Task> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        DashboardUiModel.Task task = items.get(position);
        Context context = holder.itemView.getContext();
        holder.title.setText(task.title);
        holder.subtitle.setText(task.subtitle);
        holder.urgency.setStatus(urgencyLabel(context, task.urgency), urgencyTone(task.urgency));
        holder.icon.setImageResource(iconFor(task.kind));
        holder.icon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, tintFor(task.urgency))));
        holder.itemView.setContentDescription(task.title + ". " + task.subtitle);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String urgencyLabel(Context context, DashboardUiModel.Urgency urgency) {
        switch (urgency) {
            case NOW:
                return context.getString(R.string.hc_odash_urgency_now);
            case SOON:
                return context.getString(R.string.hc_odash_urgency_soon);
            case LATER:
            default:
                return context.getString(R.string.hc_odash_urgency_later);
        }
    }

    private static StatusChip.Tone urgencyTone(DashboardUiModel.Urgency urgency) {
        switch (urgency) {
            case NOW:
                return StatusChip.Tone.DANGER;
            case SOON:
                return StatusChip.Tone.WARNING;
            case LATER:
            default:
                return StatusChip.Tone.MUTED;
        }
    }

    private static int tintFor(DashboardUiModel.Urgency urgency) {
        switch (urgency) {
            case NOW:
                return R.color.ds_danger;
            case SOON:
                return R.color.ds_warning_dark;
            case LATER:
            default:
                return R.color.ds_text_muted;
        }
    }

    private static int iconFor(DashboardUiModel.TaskKind kind) {
        switch (kind) {
            case REFUND_PENDING:
                return R.drawable.ic_ds_payment;
            case EVENT_REVISION:
                return R.drawable.ic_ds_edit;
            case EVENT_REJECTED:
                return R.drawable.ic_ds_error;
            case TICKET_SALE_ENDING:
                return R.drawable.ic_ds_ticket;
            case EVENT_STARTING_SOON:
            default:
                return R.drawable.ic_ds_clock;
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final StatusChip urgency;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivDashTaskIcon);
            title = itemView.findViewById(R.id.tvDashTaskTitle);
            subtitle = itemView.findViewById(R.id.tvDashTaskSubtitle);
            urgency = itemView.findViewById(R.id.chipDashTaskUrgency);
        }
    }
}
