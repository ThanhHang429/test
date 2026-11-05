package com.eventapp.app.ui.model;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;

/** Display metadata for the platform categories managed by FR-A09. */
public final class EventCategoryUi {

    private EventCategoryUi() {
    }

    public static long id(EventCategory category) {
        if (category == null) {
            return 9L;
        }
        switch (category) {
            case MUSIC:
                return 1L;
            case SPORTS:
                return 2L;
            case ART:
                return 3L;
            case FOOD:
                return 4L;
            case TECH:
                return 5L;
            case BUSINESS:
                return 6L;
            case EDUCATION:
                return 7L;
            case HEALTH:
                return 8L;
            case OTHER:
            default:
                return 9L;
        }
    }

    public static EventCategory fromId(long id) {
        for (EventCategory category : EventCategory.values()) {
            if (id(category) == id) {
                return category;
            }
        }
        return EventCategory.OTHER;
    }

    @StringRes
    public static int labelRes(EventCategory category) {
        if (category == null) {
            return R.string.hc_category_other;
        }
        switch (category) {
            case MUSIC:
                return R.string.hc_category_music;
            case SPORTS:
                return R.string.hc_category_sports;
            case ART:
                return R.string.hc_category_art;
            case FOOD:
                return R.string.hc_category_food;
            case TECH:
                return R.string.hc_category_tech;
            case BUSINESS:
                return R.string.hc_category_business;
            case EDUCATION:
                return R.string.hc_category_education;
            case HEALTH:
                return R.string.hc_category_health;
            case OTHER:
            default:
                return R.string.hc_category_other;
        }
    }

    public static String label(Context context, EventCategory category) {
        return context.getString(labelRes(category));
    }

    @DrawableRes
    public static int iconRes(EventCategory category) {
        if (category == null) {
            return R.drawable.ic_ds_event;
        }
        switch (category) {
            case MUSIC:
                return R.drawable.ic_ds_mic;
            case SPORTS:
                return R.drawable.ic_ds_users;
            case ART:
                return R.drawable.ic_ds_image;
            case FOOD:
                return R.drawable.ic_ds_ticket;
            case TECH:
                return R.drawable.ic_ds_tech;
            case BUSINESS:
                return R.drawable.ic_ds_chart;
            case EDUCATION:
                return R.drawable.ic_ds_edit;
            case HEALTH:
                return R.drawable.ic_ds_heart_filled;
            case OTHER:
            default:
                return R.drawable.ic_ds_event;
        }
    }

    /** Ảnh bìa placeholder khi backend chưa có coverUrl. */
    @DrawableRes
    public static int coverRes(EventCategory category) {
        if (category == null) {
            return R.drawable.bg_user_cover_indigo;
        }
        switch (category) {
            case MUSIC:
                return R.drawable.bg_user_cover_indigo;
            case SPORTS:
                return R.drawable.bg_user_cover_teal;
            case ART:
                return R.drawable.bg_user_cover_coral;
            case FOOD:
                return R.drawable.bg_user_cover_amber;
            case TECH:
                return R.drawable.bg_user_cover_violet;
            case BUSINESS:
                return R.drawable.bg_user_cover_navy;
            case EDUCATION:
                return R.drawable.bg_user_cover_sky;
            case HEALTH:
                return R.drawable.bg_user_cover_teal;
            case OTHER:
            default:
                return R.drawable.bg_user_cover_indigo;
        }
    }

    @ColorInt
    public static int gradientStart(Context context, EventCategory category) {
        switch (category == null ? EventCategory.OTHER : category) {
            case MUSIC:
                return ContextCompat.getColor(context, R.color.ds_primary);
            case SPORTS:
                return ContextCompat.getColor(context, R.color.ds_success_dark);
            case ART:
                return ContextCompat.getColor(context, R.color.ds_secondary);
            case FOOD:
                return ContextCompat.getColor(context, R.color.ds_warning_dark);
            case TECH:
                return ContextCompat.getColor(context, R.color.ds_info_dark);
            case BUSINESS:
                return ContextCompat.getColor(context, R.color.ds_organizer);
            case EDUCATION:
                return ContextCompat.getColor(context, R.color.ds_primary_dark);
            case HEALTH:
                return ContextCompat.getColor(context, R.color.ds_success);
            case OTHER:
            default:
                return ContextCompat.getColor(context, R.color.ds_text_muted);
        }
    }

    @ColorInt
    public static int gradientEnd(Context context, EventCategory category) {
        switch (category == null ? EventCategory.OTHER : category) {
            case MUSIC:
                return ContextCompat.getColor(context, R.color.ds_secondary);
            case SPORTS:
                return ContextCompat.getColor(context, R.color.ds_info);
            case ART:
                return ContextCompat.getColor(context, R.color.ds_danger);
            case FOOD:
                return ContextCompat.getColor(context, R.color.ds_cta);
            case TECH:
                return ContextCompat.getColor(context, R.color.ds_primary);
            case BUSINESS:
                return ContextCompat.getColor(context, R.color.ds_organizer_bright);
            case EDUCATION:
                return ContextCompat.getColor(context, R.color.ds_primary);
            case HEALTH:
                return ContextCompat.getColor(context, R.color.ds_organizer_bright);
            case OTHER:
            default:
                return ContextCompat.getColor(context, R.color.ds_outline_strong);
        }
    }
}
