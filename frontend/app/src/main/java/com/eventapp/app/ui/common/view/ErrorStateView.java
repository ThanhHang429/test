package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;
import com.google.android.material.button.MaterialButton;

/**
 * Error state with a retry action. Screens may override title, message and retry label.
 */
public class ErrorStateView extends LinearLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView messageView;
    private MaterialButton retryButton;

    public ErrorStateView(Context context) {
        this(context, null);
    }

    public ErrorStateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ErrorStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(32);
        setPadding(padding, padding, padding, padding);
        LayoutInflater.from(context).inflate(R.layout.view_ds_error_state, this, true);
        iconView = findViewById(R.id.ivDsErrorIcon);
        titleView = findViewById(R.id.tvDsErrorTitle);
        messageView = findViewById(R.id.tvDsErrorMessage);
        retryButton = findViewById(R.id.btnDsErrorRetry);
    }

    public void setTitle(@StringRes int titleRes) {
        setTitle(getContext().getString(titleRes));
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setMessage(@StringRes int messageRes) {
        setMessage(getContext().getString(messageRes));
    }

    public void setMessage(CharSequence message) {
        messageView.setText(message);
    }

    public void setRetryText(@StringRes int textRes) {
        retryButton.setText(textRes);
    }

    public void setIcon(@DrawableRes int drawableRes) {
        iconView.setImageResource(drawableRes);
    }

    public void setOnRetryClickListener(@Nullable OnClickListener listener) {
        retryButton.setOnClickListener(listener);
    }

    public MaterialButton getRetryButton() {
        return retryButton;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
