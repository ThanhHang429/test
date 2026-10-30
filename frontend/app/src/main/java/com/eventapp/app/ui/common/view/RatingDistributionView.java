package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.eventapp.app.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

/**
 * 5-to-1 star distribution with count and percentage per row.
 */
public class RatingDistributionView extends LinearLayout {

    private static final int STAR_COUNT = 5;

    private final LinearProgressIndicator[] bars = new LinearProgressIndicator[STAR_COUNT];
    private final TextView[] counts = new TextView[STAR_COUNT];
    private int[] distribution = new int[STAR_COUNT];

    public RatingDistributionView(Context context) {
        this(context, null);
    }

    public RatingDistributionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RatingDistributionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        for (int i = 0; i < STAR_COUNT; i++) {
            int starValue = STAR_COUNT - i;

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(24));
            LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = dp(6);
            row.setLayoutParams(rowParams);

            TextView label = new TextView(context);
            label.setText(starValue + "★");
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            label.setTextColor(ContextCompat.getColor(context, R.color.ds_text_muted));
            label.setTypeface(Typeface.create(
                    ResourcesCompat.getFont(getContext(), R.font.be_vietnam_pro), Typeface.NORMAL));
            label.setWidth(dp(32));
            row.addView(label);

            LinearProgressIndicator bar = new LinearProgressIndicator(context);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0,
                    dp(8), 1f);
            barParams.setMarginStart(dp(8));
            barParams.setMarginEnd(dp(8));
            bar.setLayoutParams(barParams);
            bar.setMax(100);
            bar.setProgressCompat(0, false);
            bar.setTrackThickness(dp(8));
            bar.setTrackCornerRadius(dp(4));
            bar.setIndicatorColor(ContextCompat.getColor(context, R.color.ds_star));
            bar.setTrackColor(ContextCompat.getColor(context, R.color.ds_outline));
            row.addView(bar);

            TextView count = new TextView(context);
            count.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f);
            count.setTextColor(ContextCompat.getColor(context, R.color.ds_text_muted));
            count.setTypeface(Typeface.create(
                    ResourcesCompat.getFont(getContext(), R.font.be_vietnam_pro), Typeface.NORMAL));
            count.setGravity(Gravity.END);
            count.setWidth(dp(76));
            row.addView(count);

            bars[i] = bar;
            counts[i] = count;
            addView(row);
        }
    }

    public void setDistribution(int[] values) {
        if (values == null || values.length != STAR_COUNT) {
            distribution = new int[STAR_COUNT];
        } else {
            distribution = values.clone();
        }
        int total = 0;
        for (int value : distribution) {
            total += Math.max(0, value);
        }
        for (int i = 0; i < STAR_COUNT; i++) {
            int count = Math.max(0, distribution[STAR_COUNT - 1 - i]);
            int percent = total == 0 ? 0 : Math.round(count * 100f / total);
            bars[i].setProgressCompat(percent, false);
            counts[i].setText(total == 0
                    ? "0"
                    : String.format(Locale.US, "%d (%d%%)", count, percent));
        }
        setContentDescription(total == 0
                ? getContext().getString(R.string.hc_rating_empty)
                : getContext().getString(R.string.hc_rating_distribution, total));
    }

    public void setStarColor(@ColorInt int color) {
        for (LinearProgressIndicator bar : bars) {
            bar.setIndicatorColor(color);
        }
    }

    public void setTrackColor(@ColorInt int color) {
        for (LinearProgressIndicator bar : bars) {
            bar.setTrackColor(color);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
