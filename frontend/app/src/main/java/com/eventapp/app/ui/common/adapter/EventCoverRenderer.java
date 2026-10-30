package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.model.EventCategoryUi;

/**
 * Renders event cover art. Backend photos win; when {@code coverUrl} is empty the card falls back
 * to a category gradient with a subtle sheen and a small watermark icon so the placeholder reads
 * as artwork instead of a giant glyph.
 */
public final class EventCoverRenderer {

    private EventCoverRenderer() {
    }

    public static GradientDrawable gradient(Context context, EventCategory category, int radiusDp) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{EventCategoryUi.gradientStart(context, category),
                        EventCategoryUi.gradientEnd(context, category)});
        gradient.setCornerRadius(dp(context, radiusDp));
        return gradient;
    }

    public static LayerDrawable placeholder(Context context, EventCategory category, int radiusDp) {
        Drawable sheen = ContextCompat.getDrawable(context, R.drawable.bg_event_sheen);
        Drawable icon = ContextCompat.getDrawable(context, EventCategoryUi.iconRes(category));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(Color.argb(46, 255, 255, 255));
        }
        LayerDrawable layers = new LayerDrawable(
                new Drawable[]{gradient(context, category, radiusDp), sheen, icon});
        int iconSize = dp(context, 56);
        layers.setLayerSize(2, iconSize, iconSize);
        layers.setLayerGravity(2, Gravity.BOTTOM | Gravity.END);
        layers.setLayerInset(2, 0, 0, dp(context, 16), dp(context, 16));
        return layers;
    }

    /** Clears any in-flight image request and paints the placeholder. */
    public static void showPlaceholder(ImageView cover, EventCategory category, int radiusDp) {
        cover.setImageTintList(null);
        cover.setImageDrawable(null);
        cover.setScaleType(ImageView.ScaleType.FIT_XY);
        cover.setAlpha(1f);
        cover.setBackground(placeholder(cover.getContext(), category, radiusDp));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
