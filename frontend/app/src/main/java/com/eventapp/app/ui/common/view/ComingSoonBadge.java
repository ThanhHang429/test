package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;

/**
 * Small pill marking a Stitch extra whose backend contract is not available yet.
 * Pair it with {@link #disableTarget(View)} so the control cannot be tapped.
 */
public class ComingSoonBadge extends AppCompatTextView {

    public ComingSoonBadge(Context context) {
        this(context, null);
    }

    public ComingSoonBadge(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComingSoonBadge(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setText(R.string.hc_coming_soon);
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f);
        setTextColor(ContextCompat.getColor(getContext(), R.color.ds_warning_dark));
        setBackgroundResource(R.drawable.bg_ds_badge_coming_soon);
        setGravity(Gravity.CENTER);
        setIncludeFontPadding(false);
        setPadding(dp(8), dp(3), dp(8), dp(3));
        setClickable(false);
        setFocusable(false);
    }

    /**
     * Disables the interactive target and annotates it for accessibility. The label keeps the
     * original control visible so the design stays intact while the feature is unavailable.
     */
    public static void disableTarget(@Nullable View target) {
        if (target == null) {
            return;
        }
        target.setEnabled(false);
        target.setAlpha(0.45f);
        target.setContentDescription(target.getContext().getString(R.string.hc_coming_soon));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
