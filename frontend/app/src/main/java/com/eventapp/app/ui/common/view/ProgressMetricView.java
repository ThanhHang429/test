package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/**
 * Label + value + progress line used by dashboard and integration health blocks.
 */
public class ProgressMetricView extends LinearLayout {

    private TextView labelView;
    private TextView valueView;
    private TextView captionView;
    private LinearProgressIndicator progressView;

    public ProgressMetricView(Context context) {
        this(context, null);
    }

    public ProgressMetricView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressMetricView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_ds_progress_metric, this, true);
        labelView = findViewById(R.id.tvDsMetricLabel);
        valueView = findViewById(R.id.tvDsMetricValue);
        captionView = findViewById(R.id.tvDsMetricCaption);
        progressView = findViewById(R.id.progressDsMetric);
        progressView.setMax(100);
        progressView.setProgressCompat(0, false);
    }

    public void setLabel(CharSequence label) {
        labelView.setText(label);
        updateContentDescription();
    }

    public void setValue(CharSequence value) {
        valueView.setText(value);
        updateContentDescription();
    }

    public void setValueColor(@ColorInt int color) {
        valueView.setTextColor(color);
    }

    public void setProgress(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        progressView.setProgressCompat(clamped, false);
    }

    public void setProgress(int value, int max) {
        if (max <= 0) {
            setProgress(0);
            return;
        }
        setProgress(Math.round(value * 100f / max));
    }

    public void setProgressColor(@ColorInt int color) {
        progressView.setIndicatorColor(color);
    }

    public void setCaption(CharSequence caption) {
        boolean visible = caption != null && caption.length() > 0;
        captionView.setText(caption);
        captionView.setVisibility(visible ? VISIBLE : GONE);
    }

    public void resetColors() {
        valueView.setTextColor(ContextCompat.getColor(getContext(), R.color.ds_primary));
        progressView.setIndicatorColor(ContextCompat.getColor(getContext(), R.color.ds_primary));
    }

    private void updateContentDescription() {
        String label = labelView.getText() == null ? "" : labelView.getText().toString();
        String value = valueView.getText() == null ? "" : valueView.getText().toString();
        setContentDescription(label + ": " + value);
    }
}
