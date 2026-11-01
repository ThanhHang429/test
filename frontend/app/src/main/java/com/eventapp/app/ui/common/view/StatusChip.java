package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.eventapp.app.R;
import com.google.android.material.chip.Chip;

/**
 * Small status pill used for event/order/review state. Mapping from backend enums to tone
 * stays in the screen layer so the view does not depend on DTOs.
 */
public class StatusChip extends Chip {

    public enum Tone {
        NEUTRAL,
        PRIMARY,
        SUCCESS,
        WARNING,
        DANGER,
        INFO,
        MUTED
    }

    public StatusChip(Context context) {
        this(context, null);
    }

    public StatusChip(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.chipStyle);
    }

    public StatusChip(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setCheckable(false);
        setClickable(false);
        setFocusable(false);
        setChipIconVisible(false);
        setCloseIconVisible(false);
        setEnsureMinTouchTargetSize(false);
        setChipMinHeight(dp(28));
        setChipCornerRadius(dp(999));
        setChipStrokeWidth(0f);
        setChipStartPadding(dp(8));
        setChipEndPadding(dp(8));
        setTextStartPadding(0f);
        setTextEndPadding(0f);
        setGravity(Gravity.CENTER_VERTICAL);
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f);
        setTypeface(Typeface.create(ResourcesCompat.getFont(getContext(),
                R.font.plus_jakarta_sans), Typeface.BOLD));
        setTone(Tone.NEUTRAL);
    }

    public void setStatus(CharSequence text, Tone tone) {
        setText(text);
        setTone(tone);
    }

    public void setTone(Tone tone) {
        int background;
        int foreground;
        switch (tone == null ? Tone.NEUTRAL : tone) {
            case PRIMARY:
                // Theo colorPrimary để chip tự đổi màu ở khu Organizer (theme overlay teal).
                int primary = com.google.android.material.color.MaterialColors.getColor(
                        this, androidx.appcompat.R.attr.colorPrimary);
                background = androidx.core.graphics.ColorUtils.setAlphaComponent(primary, 0x20);
                foreground = primary;
                break;
            case SUCCESS:
                background = ContextCompat.getColor(getContext(), R.color.ds_success_soft);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_success_dark);
                break;
            case WARNING:
                background = ContextCompat.getColor(getContext(), R.color.ds_warning_soft);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_warning_dark);
                break;
            case DANGER:
                background = ContextCompat.getColor(getContext(), R.color.ds_danger_soft);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_danger_dark);
                break;
            case INFO:
                background = ContextCompat.getColor(getContext(), R.color.ds_info_soft);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_info_dark);
                break;
            case MUTED:
                background = ContextCompat.getColor(getContext(), R.color.ds_surface_alt);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_text_muted);
                break;
            case NEUTRAL:
            default:
                background = ContextCompat.getColor(getContext(), R.color.ds_surface_alt);
                foreground = ContextCompat.getColor(getContext(), R.color.ds_text_muted);
                break;
        }
        setChipBackgroundColor(ColorStateList.valueOf(background));
        setTextColor(foreground);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
