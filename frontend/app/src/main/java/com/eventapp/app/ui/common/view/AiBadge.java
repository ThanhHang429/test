package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;

/**
 * "Gợi ý bởi AI" pill with an optional reason line.
 *
 * <p>The match percentage is only rendered when a backend score is supplied. Screens must pass
 * {@code null} when the API omits the score instead of inventing a number.</p>
 */
public class AiBadge extends LinearLayout {

    private TextView badgeView;
    private TextView reasonView;
    private Integer score;

    public AiBadge(Context context) {
        this(context, null);
    }

    public AiBadge(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AiBadge(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_ds_ai_badge, this, true);
        badgeView = findViewById(R.id.tvDsAiBadgeText);
        reasonView = findViewById(R.id.tvDsAiBadgeReason);
        updateBadgeText();
    }

    /**
     * @param score backend match score in percent, or {@code null} when the API did not return one.
     */
    public void setScore(@Nullable Integer score) {
        this.score = score == null ? null : Math.max(0, Math.min(100, score));
        updateBadgeText();
    }

    @Nullable
    public Integer getScore() {
        return score;
    }

    public void setReason(@Nullable CharSequence reason) {
        boolean visible = reason != null && reason.length() > 0;
        reasonView.setText(reason);
        reasonView.setVisibility(visible ? VISIBLE : GONE);
        updateContentDescription();
    }

    public void setReason(@StringRes int reasonRes) {
        setReason(getContext().getString(reasonRes));
    }

    public void showReason(boolean show) {
        reasonView.setVisibility(show ? VISIBLE : GONE);
    }

    /** Hide the pill while keeping the reason line, e.g. for a plain list row. */
    public void showPill(boolean show) {
        View pill = findViewById(R.id.layoutDsAiPill);
        pill.setVisibility(show ? VISIBLE : GONE);
    }

    private void updateBadgeText() {
        badgeView.setText(tierLabel());
        updateContentDescription();
    }

    /** Nhãn mức độ phù hợp thay cho con số phần trăm, dễ hiểu hơn với người dùng. */
    private int tierLabel() {
        if (score == null) {
            return R.string.hc_ai_badge;
        }
        if (score >= 85) {
            return R.string.hc_ai_tier_high;
        }
        if (score >= 70) {
            return R.string.hc_ai_tier_medium;
        }
        return R.string.hc_ai_tier_low;
    }

    private void updateContentDescription() {
        String badge = badgeView.getText() == null ? "" : badgeView.getText().toString();
        String reason = reasonView.getVisibility() == VISIBLE && reasonView.getText() != null
                ? reasonView.getText().toString()
                : "";
        setContentDescription(reason.length() == 0 ? badge : badge + ". " + reason);
    }
}
