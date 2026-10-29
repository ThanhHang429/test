package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.eventapp.app.R;

import java.util.Locale;

/**
 * Read-only or editable 1-5 star selector. Display mode supports half stars.
 */
public class StarRatingView extends LinearLayout {

    public interface OnRatingChangedListener {
        void onRatingChanged(float rating);
    }

    private static final int STAR_COUNT = 5;

    private final ImageView[] stars = new ImageView[STAR_COUNT];
    private float rating;
    private boolean editable;
    private int starSizePx;
    private int spacingPx;
    private OnRatingChangedListener listener;

    public StarRatingView(Context context) {
        this(context, null);
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StarRatingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        starSizePx = dp(32);
        spacingPx = dp(4);
        for (int i = 0; i < STAR_COUNT; i++) {
            ImageView star = new ImageView(context);
            LayoutParams params = new LayoutParams(starSizePx, starSizePx);
            if (i > 0) {
                params.setMarginStart(spacingPx);
            }
            star.setLayoutParams(params);
            star.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            final int index = i;
            star.setOnClickListener(v -> {
                if (editable) {
                    setRating(index + 1, true);
                }
            });
            stars[i] = star;
            addView(star);
        }
        updateStars();
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        setRating(rating, false);
    }

    private void setRating(float rating, boolean fromUser) {
        float value = Math.max(0f, Math.min(STAR_COUNT, rating));
        if (editable) {
            value = Math.round(value);
        }
        this.rating = value;
        updateStars();
        if (fromUser && listener != null) {
            listener.onRatingChanged(value);
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        for (ImageView star : stars) {
            star.setClickable(editable);
            star.setFocusable(editable);
            star.setImportantForAccessibility(editable
                    ? IMPORTANT_FOR_ACCESSIBILITY_YES
                    : IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        if (editable) {
            rating = Math.round(rating);
        }
        updateStars();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setOnRatingChangedListener(@Nullable OnRatingChangedListener listener) {
        this.listener = listener;
    }

    public void setStarSize(int dp) {
        starSizePx = dp(dp);
        spacingPx = dp(4);
        for (int i = 0; i < stars.length; i++) {
            LayoutParams params = (LayoutParams) stars[i].getLayoutParams();
            params.width = starSizePx;
            params.height = starSizePx;
            params.setMarginStart(i == 0 ? 0 : spacingPx);
            stars[i].setLayoutParams(params);
        }
        requestLayout();
    }

    private void updateStars() {
        for (int i = 0; i < stars.length; i++) {
            boolean filled;
            boolean half = false;
            if (editable) {
                filled = i < Math.round(rating);
            } else {
                float diff = rating - i;
                filled = diff >= 0.75f;
                half = !filled && diff >= 0.25f;
            }
            int drawable;
            if (filled) {
                drawable = R.drawable.ic_ds_star_filled;
            } else if (half) {
                drawable = R.drawable.ic_ds_star_half;
            } else {
                drawable = R.drawable.ic_ds_star;
            }
            stars[i].setImageResource(drawable);
            String description = getContext().getString(R.string.hc_star_content_description, i + 1);
            stars[i].setContentDescription(description);
        }
        setContentDescription(getContext().getString(
                R.string.hc_rating_content_description, formatRating(rating)));
    }

    private String formatRating(float value) {
        if (value == Math.round(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1f : 0.5f);
        for (ImageView star : stars) {
            star.setEnabled(enabled);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            setAlpha(1f);
        }
    }
}
