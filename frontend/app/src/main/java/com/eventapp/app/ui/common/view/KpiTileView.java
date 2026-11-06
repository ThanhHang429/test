package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

/**
 * Compact KPI card used by the organizer dashboard and the AI analysis screen.
 *
 * <p>Each block is independent (FR-O03 O03-R05): the view can show an inline error with a retry
 * action without hiding the rest of the screen.</p>
 */
public class KpiTileView extends LinearLayout {

    public enum Tone {
        DEFAULT,
        SUCCESS,
        WARNING,
        DANGER
    }

    private TextView labelView;
    private TextView valueView;
    private TextView deltaView;
    private TextView captionView;
    private TextView errorView;
    private View footView;
    private View errorBlock;
    private MaterialButton retryButton;

    public KpiTileView(Context context) {
        this(context, null);
    }

    public KpiTileView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KpiTileView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_ds_card);
        int padding = dp(12);
        setPadding(padding, padding, padding, padding);
        setMinimumHeight(dp(96));
        LayoutInflater.from(context).inflate(R.layout.view_ds_kpi_tile, this, true);

        labelView = findViewById(R.id.tvDsKpiLabel);
        valueView = findViewById(R.id.tvDsKpiValue);
        deltaView = findViewById(R.id.tvDsKpiDelta);
        captionView = findViewById(R.id.tvDsKpiCaption);
        footView = findViewById(R.id.layoutDsKpiFoot);
        errorBlock = findViewById(R.id.layoutDsKpiError);
        errorView = findViewById(R.id.tvDsKpiError);
        retryButton = findViewById(R.id.btnDsKpiRetry);
        setTone(Tone.DEFAULT);
    }

    public void setLabel(@StringRes int labelRes) {
        setLabel(getContext().getString(labelRes));
    }

    public void setLabel(@Nullable CharSequence label) {
        labelView.setText(label);
        updateContentDescription();
    }

    public void setValue(@StringRes int valueRes) {
        setValue(getContext().getString(valueRes));
    }

    public void setValue(@Nullable CharSequence value) {
        valueView.setText(value);
        updateContentDescription();
    }

    /** Small text after the value, e.g. "128 đánh giá". Pass {@code null} to hide it. */
    public void setCaption(@Nullable CharSequence caption) {
        boolean visible = caption != null && caption.length() > 0;
        captionView.setText(caption);
        captionView.setVisibility(visible ? VISIBLE : GONE);
        updateFoot();
    }

    /** Delta line such as "+9% kỳ trước"; {@code tone} colours it. */
    public void setDelta(@Nullable CharSequence delta, Tone tone) {
        boolean visible = delta != null && delta.length() > 0;
        deltaView.setText(delta);
        deltaView.setTextColor(colorFor(tone));
        deltaView.setVisibility(visible ? VISIBLE : GONE);
        updateFoot();
    }

    public void setTone(Tone tone) {
        valueView.setTextColor(colorFor(tone));
    }

    /** Replaces the number with an inline error and an optional retry action. */
    public void showError(@StringRes int messageRes, @Nullable OnClickListener retryListener) {
        showError(getContext().getString(messageRes), retryListener);
    }

    public void showError(@Nullable CharSequence message, @Nullable OnClickListener retryListener) {
        errorView.setText(message);
        valueView.setVisibility(GONE);
        footView.setVisibility(GONE);
        errorBlock.setVisibility(VISIBLE);
        retryButton.setVisibility(retryListener == null ? GONE : VISIBLE);
        retryButton.setOnClickListener(retryListener);
        setContentDescription(errorView.getText());
    }

    public void clearError() {
        errorBlock.setVisibility(GONE);
        valueView.setVisibility(VISIBLE);
        updateFoot();
        updateContentDescription();
    }

    private void updateFoot() {
        boolean hasDelta = deltaView.getVisibility() == VISIBLE;
        boolean hasCaption = captionView.getVisibility() == VISIBLE;
        footView.setVisibility(hasDelta || hasCaption ? VISIBLE : GONE);
    }

    private void updateContentDescription() {
        String label = labelView.getText() == null ? "" : labelView.getText().toString();
        String value = valueView.getText() == null ? "" : valueView.getText().toString();
        setContentDescription(value.length() == 0 ? label : label + ": " + value);
    }

    private int colorFor(Tone tone) {
        switch (tone == null ? Tone.DEFAULT : tone) {
            case SUCCESS:
                return ContextCompat.getColor(getContext(), R.color.ds_success_dark);
            case WARNING:
                return ContextCompat.getColor(getContext(), R.color.ds_warning_dark);
            case DANGER:
                return ContextCompat.getColor(getContext(), R.color.ds_danger_dark);
            case DEFAULT:
            default:
                // Theo colorPrimary để khu Nhà tổ chức (theme overlay teal) đổi màu theo.
                return MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
