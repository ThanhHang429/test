package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.organizer.dashboard.DashboardUiModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/** Top events block of FR-O03; the bar shows each event's share of the highest revenue. */
public class TopEventAdapter extends RecyclerView.Adapter<TopEventAdapter.TopEventViewHolder> {

    public interface OnTopEventClickListener {
        void onTopEventClick(DashboardUiModel.TopEvent event);
    }

    private final List<DashboardUiModel.TopEvent> items = new ArrayList<>();
    private OnTopEventClickListener listener;

    public void setOnTopEventClickListener(OnTopEventClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DashboardUiModel.TopEvent> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TopEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_top_event, parent, false);
        return new TopEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopEventViewHolder holder, int position) {
        DashboardUiModel.TopEvent event = items.get(position);
        Context context = holder.itemView.getContext();
        long max = maxRevenue();
        int share = max <= 0L ? 0 : Math.round(event.verifiedRevenue * 100f / max);

        holder.rank.setText(String.valueOf(position + 1));
        holder.title.setText(event.title);
        holder.revenue.setPrice(event.verifiedRevenue);
        holder.revenue.setTextColor(ContextCompat.getColor(context,
                position == 0 ? R.color.ds_success_dark : R.color.ds_text));
        holder.registrations.setText(
                context.getString(R.string.hc_odash_top_registrations, event.registrations));
        holder.lifecycle.setStatus(
                DashboardStatusLabels.lifecycleLabel(context, event.lifecycleStage),
                DashboardStatusLabels.lifecycleTone(event.lifecycleStage));
        holder.progress.setProgressCompat(share, false);
        holder.progress.setIndicatorColor(ContextCompat.getColor(context,
                position == 0 ? R.color.ds_primary : R.color.ds_chart_bar_alt));
        holder.itemView.setContentDescription(event.title + ". "
                + PriceTextView.formatVnd(event.verifiedRevenue) + ". "
                + context.getString(R.string.hc_odash_top_registrations, event.registrations));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTopEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private long maxRevenue() {
        long max = 0L;
        for (DashboardUiModel.TopEvent event : items) {
            max = Math.max(max, event.verifiedRevenue);
        }
        return max;
    }

    static class TopEventViewHolder extends RecyclerView.ViewHolder {
        final TextView rank;
        final TextView title;
        final PriceTextView revenue;
        final TextView registrations;
        final StatusChip lifecycle;
        final LinearProgressIndicator progress;

        TopEventViewHolder(@NonNull View itemView) {
            super(itemView);
            rank = itemView.findViewById(R.id.tvDashTopRank);
            title = itemView.findViewById(R.id.tvDashTopTitle);
            revenue = itemView.findViewById(R.id.tvDashTopRevenue);
            registrations = itemView.findViewById(R.id.tvDashTopRegistrations);
            lifecycle = itemView.findViewById(R.id.chipDashTopLifecycle);
            progress = itemView.findViewById(R.id.progressDashTop);
        }
    }
}
