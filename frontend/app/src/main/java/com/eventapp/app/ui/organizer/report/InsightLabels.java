package com.eventapp.app.ui.organizer.report;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;

/** Vietnamese labels for O27 metrics, confidence levels and link actions. */
public final class InsightLabels {

    private InsightLabels() {
    }

    public static String metricLabel(android.content.Context context,
                                     AiInsightUiModel.MetricKey key) {
        if (key == null) {
            return "";
        }
        switch (key) {
            case FILL_RATE:
                return context.getString(R.string.hc_insight_metric_fill);
            case DAYS_LEFT:
                return context.getString(R.string.hc_insight_metric_days_left);
            case CHECKIN_RATE:
                return context.getString(R.string.hc_insight_metric_checkin);
            case RATING:
                return context.getString(R.string.hc_insight_metric_rating);
            case REGISTRATIONS:
            default:
                return context.getString(R.string.hc_insight_metric_registrations);
        }
    }

    public static String typeLabel(android.content.Context context,
                                   AiInsightUiModel.Type type) {
        return context.getString(type == AiInsightUiModel.Type.SUGGESTION
                ? R.string.hc_insight_type_suggestion
                : R.string.hc_insight_type_observation);
    }

    public static StatusChip.Tone typeTone(AiInsightUiModel.Type type) {
        return type == AiInsightUiModel.Type.SUGGESTION
                ? StatusChip.Tone.PRIMARY
                : StatusChip.Tone.INFO;
    }

    public static String confidenceLabel(android.content.Context context,
                                         AiInsightUiModel.Confidence confidence) {
        if (confidence == null) {
            return context.getString(R.string.hc_insight_conf_low);
        }
        switch (confidence) {
            case HIGH:
                return context.getString(R.string.hc_insight_conf_high);
            case MEDIUM:
                return context.getString(R.string.hc_insight_conf_medium);
            case LOW:
            default:
                return context.getString(R.string.hc_insight_conf_low);
        }
    }

    public static StatusChip.Tone confidenceTone(AiInsightUiModel.Confidence confidence) {
        if (confidence == null) {
            return StatusChip.Tone.MUTED;
        }
        switch (confidence) {
            case HIGH:
                return StatusChip.Tone.SUCCESS;
            case MEDIUM:
                return StatusChip.Tone.INFO;
            case LOW:
            default:
                return StatusChip.Tone.MUTED;
        }
    }

    /** Returns {@code null} when the insight has no link action. */
    public static String actionLabel(android.content.Context context,
                                     AiInsightUiModel.ActionTarget action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case EDIT_EVENT:
                return context.getString(R.string.hc_insight_action_edit_event);
            case EDIT_TICKETS:
                return context.getString(R.string.hc_insight_action_edit_tickets);
            case VIEW_ORDERS:
                return context.getString(R.string.hc_insight_action_view_orders);
            case NONE:
            default:
                return null;
        }
    }
}
