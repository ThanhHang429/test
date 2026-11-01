package com.eventapp.app.ui.admin.system.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.ServiceHealthUiModel;

import java.util.ArrayList;
import java.util.List;

/** FR-A20 service row: status dot, name, error rate and latency. */
public class ServiceHealthAdapter
        extends RecyclerView.Adapter<ServiceHealthAdapter.ServiceViewHolder> {

    private final List<ServiceHealthUiModel> items = new ArrayList<>();

    public void submitList(List<ServiceHealthUiModel> next) {
        List<ServiceHealthUiModel> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
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
                return items.get(oldPosition).nameRes == incoming.get(newPosition).nameRes;
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                ServiceHealthUiModel oldItem = items.get(oldPosition);
                ServiceHealthUiModel newItem = incoming.get(newPosition);
                return oldItem.status == newItem.status
                        && oldItem.errorRatePct == newItem.errorRatePct
                        && oldItem.p95LatencyMs == newItem.p95LatencyMs
                        && oldItem.requestCount == newItem.requestCount;
            }
        });
        items.clear();
        items.addAll(incoming);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_health, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static int colorFor(ServiceHealthUiModel.Status status) {
        switch (status) {
            case HEALTHY:
                return R.color.ds_success;
            case DEGRADED:
                return R.color.ds_warning;
            case DOWN:
                return R.color.ds_danger;
            default:
                return R.color.ds_outline;
        }
    }

    /** Darker tone for the text label so the row does not rely on colour alone. */
    public static int textColorFor(ServiceHealthUiModel.Status status) {
        switch (status) {
            case HEALTHY:
                return R.color.ds_success_dark;
            case DEGRADED:
                return R.color.ds_warning_dark;
            case DOWN:
                return R.color.ds_danger_dark;
            default:
                return R.color.ds_text_muted;
        }
    }

    public static int labelFor(ServiceHealthUiModel.Status status) {
        switch (status) {
            case HEALTHY:
                return R.string.hc_health_status_healthy;
            case DEGRADED:
                return R.string.hc_health_status_degraded;
            case DOWN:
                return R.string.hc_health_status_down;
            default:
                return R.string.hc_health_status_unknown;
        }
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {

        private final View dot;
        private final TextView name;
        private final TextView meta;
        private final TextView status;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            dot = itemView.findViewById(R.id.viewServiceDot);
            name = itemView.findViewById(R.id.tvServiceName);
            meta = itemView.findViewById(R.id.tvServiceMeta);
            status = itemView.findViewById(R.id.tvServiceStatus);
        }

        void bind(ServiceHealthUiModel item) {
            dot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), colorFor(item.status))));
            name.setText(item.nameRes);
            meta.setText(itemView.getContext().getString(R.string.hc_health_service_meta,
                    item.errorRatePct, item.p95LatencyMs, item.requestCount));
            status.setText(labelFor(item.status));
            status.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    textColorFor(item.status)));
        }
    }
}
