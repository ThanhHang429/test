package com.eventapp.app.ui.organizer.util;

import android.widget.TextView;

import com.eventapp.app.R;
import com.eventapp.app.ui.organizer.model.OrgTone;

/** Gán màu nền/chữ cho pill trạng thái theo {@link OrgTone}. */
public final class OrgUi {

    private OrgUi() {
    }

    public static void applyPill(TextView view, int tone) {
        switch (tone) {
            case OrgTone.WARNING:
                view.setBackgroundResource(R.drawable.bg_org_pill_warning);
                view.setTextColor(view.getContext().getColor(R.color.org_warning_text));
                break;
            case OrgTone.DANGER:
                view.setBackgroundResource(R.drawable.bg_org_pill_danger);
                view.setTextColor(view.getContext().getColor(R.color.org_danger_text));
                break;
            case OrgTone.NEUTRAL:
                view.setBackgroundResource(R.drawable.bg_org_pill_neutral);
                view.setTextColor(view.getContext().getColor(R.color.org_neutral_text));
                break;
            case OrgTone.INFO:
                view.setBackgroundResource(R.drawable.bg_org_pill_info);
                view.setTextColor(view.getContext().getColor(R.color.org_info_text));
                break;
            case OrgTone.SUCCESS:
            default:
                view.setBackgroundResource(R.drawable.bg_org_pill_success);
                view.setTextColor(view.getContext().getColor(R.color.org_success_text));
                break;
        }
    }
}
