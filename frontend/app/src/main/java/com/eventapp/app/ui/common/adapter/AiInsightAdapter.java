package com.eventapp.app.ui.common.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.organizer.report.AiInsightUiModel;
import com.eventapp.app.ui.organizer.report.InsightLabels;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/** Observation/suggestion rows for FR-O27. Rows link to another screen, never apply changes. */
public class AiInsightAdapter
        extends RecyclerView.Adapter<AiInsightAdapter.InsightViewHolder> {

    public interface OnInsightActionListener {
        void onInsightAction(AiInsightUiModel.Insight insight);
    }

    private final List<AiInsightUiModel.Insight> items = new ArrayList<>();
    private OnInsightActionListener listener;

    public void setOnInsightActionListener(OnInsightActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<AiInsightUiModel.Insight> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_insight, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        AiInsightUiModel.Insight insight = items.get(position);

        holder.type.setStatus(InsightLabels.typeLabel(holder.itemView.getContext(), insight.type),
                InsightLabels.typeTone(insight.type));
        holder.confidence.setStatus(
                InsightLabels.confidenceLabel(holder.itemView.getContext(), insight.confidence),
                InsightLabels.confidenceTone(insight.confidence));
        holder.text.setText(insight.text);

        if (insight.relatedMetric == null) {
            holder.related.setVisibility(View.GONE);
        } else {
            holder.related.setText(holder.itemView.getContext().getString(
                    R.string.hc_insight_related,
                    InsightLabels.metricLabel(holder.itemView.getContext(),
                            insight.relatedMetric)));
            holder.related.setVisibility(View.VISIBLE);
        }

        holder.estimate.setVisibility(
                insight.type == AiInsightUiModel.Type.SUGGESTION ? View.VISIBLE : View.GONE);

        String actionLabel = InsightLabels.actionLabel(holder.itemView.getContext(),
                insight.action);
        if (actionLabel == null) {
            holder.action.setVisibility(View.GONE);
            holder.action.setOnClickListener(null);
        } else {
            holder.action.setText(actionLabel);
            holder.action.setVisibility(View.VISIBLE);
            holder.action.setContentDescription(actionLabel);
            holder.action.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInsightAction(insight);
                }
            });
        }

        holder.itemView.setContentDescription(holder.type.getText() + ". " + insight.text);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class InsightViewHolder extends RecyclerView.ViewHolder {
        final StatusChip type;
        final StatusChip confidence;
        final TextView text;
        final TextView related;
        final TextView estimate;
        final MaterialButton action;

        InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.chipInsightType);
            confidence = itemView.findViewById(R.id.chipInsightConfidence);
            text = itemView.findViewById(R.id.tvInsightText);
            related = itemView.findViewById(R.id.tvInsightRelated);
            estimate = itemView.findViewById(R.id.tvInsightEstimate);
            action = itemView.findViewById(R.id.btnInsightAction);
        }
    }
}
