package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;
import com.google.android.material.button.MaterialButton;

/**
 * Empty state with an optional action. The view is data-agnostic; callers set copy and listeners.
 */
public class EmptyStateView extends LinearLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView messageView;
    private MaterialButton actionButton;

    public EmptyStateView(Context context) {
        this(context, null);
    }

    public EmptyStateView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(32);
        setPadding(padding, padding, padding, padding);
        LayoutInflater.from(context).inflate(R.layout.view_ds_empty_state, this, true);
        iconView = findViewById(R.id.ivDsEmptyIcon);
        titleView = findViewById(R.id.tvDsEmptyTitle);
        messageView = findViewById(R.id.tvDsEmptyMessage);
        actionButton = findViewById(R.id.btnDsEmptyAction);
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

    public void setIcon(@DrawableRes int drawableRes) {
        iconView.setImageResource(drawableRes);
    }

    public void setActionText(@StringRes int textRes) {
        setActionText(getContext().getString(textRes));
    }

    public void setActionText(CharSequence text) {
        actionButton.setText(text);
        actionButton.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }

    public void showAction(boolean show) {
        actionButton.setVisibility(show ? VISIBLE : GONE);
    }

    public void setOnActionClickListener(@Nullable OnClickListener listener) {
        actionButton.setOnClickListener(listener);
    }

    public MaterialButton getActionButton() {
        return actionButton;
    }

    public ImageView getIconView() {
        return iconView;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
