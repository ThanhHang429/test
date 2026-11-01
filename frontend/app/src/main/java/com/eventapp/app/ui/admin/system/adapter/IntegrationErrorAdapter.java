package com.eventapp.app.ui.admin.system.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.IntegrationErrorUiModel;

import java.util.ArrayList;
import java.util.List;

/** FR-A20 recent error row: time, task, error code, latency and a truncated correlation id. */
public class IntegrationErrorAdapter
        extends RecyclerView.Adapter<IntegrationErrorAdapter.ErrorViewHolder> {

    private final List<IntegrationErrorUiModel> items = new ArrayList<>();

    public void submitList(List<IntegrationErrorUiModel> next) {
        List<IntegrationErrorUiModel> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return incoming.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return items.get(oldPosition).correlationId
                        .equals(incoming.get(newPosition).correlationId);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                IntegrationErrorUiModel oldItem = items.get(oldPosition);
                IntegrationErrorUiModel newItem = incoming.get(newPosition);
                return oldItem.timeLabel.equals(newItem.timeLabel)
                        && oldItem.taskType.equals(newItem.taskType)
                        && oldItem.errorCode.equals(newItem.errorCode)
                        && oldItem.latencyMs == newItem.latencyMs;
            }
        });
        items.clear();
        items.addAll(incoming);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ErrorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_integration_error, parent, false);
        return new ErrorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ErrorViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {

        private final TextView time;
        private final TextView task;
        private final TextView meta;

        ErrorViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.tvErrorTime);
            task = itemView.findViewById(R.id.tvErrorTask);
            meta = itemView.findViewById(R.id.tvErrorMeta);
        }

        void bind(IntegrationErrorUiModel item) {
            time.setText(item.timeLabel);
            task.setText(item.taskType);
            String shortId = item.correlationId.length() > 8
                    ? item.correlationId.substring(0, 8)
                    : item.correlationId;
            meta.setText(itemView.getContext().getString(R.string.hc_health_error_meta,
                    item.errorCode, item.latencyMs, shortId));
        }
    }
}
