package com.eventapp.app.ui.common.adapter;

import android.content.Context;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.organizer.dashboard.DashboardUiModel;

/** Maps the two O03 status dimensions to Vietnamese labels and chip tones. */
public final class DashboardStatusLabels {

    private DashboardStatusLabels() {
    }

    public static String reviewLabel(Context context, DashboardUiModel.ReviewStage stage) {
        if (stage == null) {
            return "";
        }
        switch (stage) {
            case DRAFT:
                return context.getString(R.string.hc_odash_status_draft);
            case PENDING_REVIEW:
                return context.getString(R.string.hc_odash_status_pending);
            case REVISION_REQUIRED:
                return context.getString(R.string.hc_odash_status_revision);
            case REJECTED:
                return context.getString(R.string.hc_odash_status_rejected);
            case APPROVED:
            default:
                return context.getString(R.string.hc_odash_status_approved);
        }
    }

    public static StatusChip.Tone reviewTone(DashboardUiModel.ReviewStage stage) {
        if (stage == null) {
            return StatusChip.Tone.MUTED;
        }
        switch (stage) {
            case PENDING_REVIEW:
            case REVISION_REQUIRED:
                return StatusChip.Tone.WARNING;
            case REJECTED:
                return StatusChip.Tone.DANGER;
            case APPROVED:
                return StatusChip.Tone.SUCCESS;
            case DRAFT:
            default:
                return StatusChip.Tone.MUTED;
        }
    }

    public static String lifecycleLabel(Context context, DashboardUiModel.LifecycleStage stage) {
        if (stage == null) {
            return "";
        }
        switch (stage) {
            case ON_SALE:
                return context.getString(R.string.hc_odash_life_on_sale);
            case UPCOMING:
                return context.getString(R.string.hc_odash_life_upcoming);
            case COMPLETED:
                return context.getString(R.string.hc_odash_life_completed);
            case SUSPENDED:
                return context.getString(R.string.hc_odash_life_suspended);
            case CANCELLED:
                return context.getString(R.string.hc_odash_life_cancelled);
            case UNPUBLISHED:
            default:
                return context.getString(R.string.hc_odash_life_unpublished);
        }
    }

    public static StatusChip.Tone lifecycleTone(DashboardUiModel.LifecycleStage stage) {
        if (stage == null) {
            return StatusChip.Tone.MUTED;
        }
        switch (stage) {
            case ON_SALE:
                return StatusChip.Tone.SUCCESS;
            case UPCOMING:
                return StatusChip.Tone.INFO;
            case SUSPENDED:
            case CANCELLED:
                return StatusChip.Tone.DANGER;
            case COMPLETED:
            case UNPUBLISHED:
            default:
                return StatusChip.Tone.MUTED;
        }
    }
}
