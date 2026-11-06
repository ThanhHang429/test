package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.eventapp.app.R;
import com.google.android.material.button.MaterialButton;

/**
 * Inline banner for offline mode or stale cache. Screens decide whether retry is available.
 */
public class OfflineBanner extends LinearLayout {

    private TextView messageView;
    private MaterialButton retryButton;

    public OfflineBanner(Context context) {
        this(context, null);
    }

    public OfflineBanner(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OfflineBanner(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.bg_ds_offline_banner);
        setPadding(dp(12), dp(10), dp(8), dp(10));
        LayoutInflater.from(context).inflate(R.layout.view_ds_offline_banner, this, true);
        messageView = findViewById(R.id.tvDsOfflineMessage);
        retryButton = findViewById(R.id.btnDsOfflineRetry);
        setMessage(context.getString(R.string.hc_offline));
    }

    public void setMessage(CharSequence message) {
        messageView.setText(message);
        setContentDescription(message);
    }

    public void setMessage(@StringRes int messageRes) {
        setMessage(getContext().getString(messageRes));
    }

    public void setCachedAt(CharSequence formattedTime) {
        setMessage(getContext().getString(R.string.hc_offline_cached, formattedTime));
    }

    public void setOnRetryClickListener(@Nullable OnClickListener listener) {
        retryButton.setOnClickListener(listener);
    }

    public void setRetryVisible(boolean visible) {
        retryButton.setVisibility(visible ? VISIBLE : GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
