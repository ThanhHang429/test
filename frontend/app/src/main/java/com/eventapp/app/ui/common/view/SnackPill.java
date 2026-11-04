package com.eventapp.app.ui.common.view;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.eventapp.app.R;

/**
 * Pill thông báo ngắn tự dựng, thay cho {@code Toast}/{@code Snackbar} mặc định.
 *
 * <p>Luôn nền tối + chữ trắng nên dùng được cho cả khu xác thực (nền tối) và khu người dùng
 * (nền sáng). Tự xoá sau 2 giây, chỉ giữ một pill tại một thời điểm.</p>
 */
public final class SnackPill {

    private static final String TAG = "snack_pill";

    private SnackPill() {
    }

    public static void show(View anchor, CharSequence message, int bottomMarginPx) {
        ViewGroup parent = resolveParent(anchor);
        if (parent == null) {
            return;
        }
        View previous = parent.findViewWithTag(TAG);
        if (previous != null) {
            parent.removeView(previous);
        }

        TextView pill = new TextView(anchor.getContext());
        pill.setTag(TAG);
        pill.setText(message);
        pill.setTextColor(ContextCompat.getColor(anchor.getContext(), R.color.white));
        pill.setTextSize(13f);
        pill.setTypeface(ResourcesCompat.getFont(anchor.getContext(), R.font.plus_jakarta_sans));
        pill.setBackgroundResource(R.drawable.bg_snack_pill);
        pill.setPadding(dp(anchor, 20), dp(anchor, 14), dp(anchor, 20), dp(anchor, 14));
        pill.setElevation(dp(anchor, 8));
        pill.setGravity(Gravity.CENTER);
        pill.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = bottomMarginPx;
        parent.addView(pill, params);

        pill.setAlpha(0f);
        pill.setTranslationY(dp(anchor, 12));
        pill.animate().alpha(1f).translationY(0f).setDuration(180).start();
        pill.postDelayed(() -> pill.animate()
                .alpha(0f)
                .translationY(dp(anchor, 12))
                .setDuration(180)
                .withEndAction(() -> parent.removeView(pill))
                .start(), 2000);
        pill.announceForAccessibility(message);
    }

    private static ViewGroup resolveParent(View anchor) {
        View root = anchor.getRootView();
        View content = root.findViewById(R.id.authContainer);
        if (!(content instanceof ViewGroup)) {
            content = root.findViewById(android.R.id.content);
        }
        return content instanceof ViewGroup ? (ViewGroup) content : null;
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
