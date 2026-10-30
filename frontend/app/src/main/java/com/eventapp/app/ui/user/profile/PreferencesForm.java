package com.eventapp.app.ui.user.profile;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * FR-U03 preference draft. Mirrors the API-01 payload so the screen never sends extra fields;
 * {@code version} is the optimistic key used by the server.
 */
public class PreferencesForm {

    public enum Format {
        ANY,
        ONSITE,
        ONLINE
    }

    public enum PriceMode {
        ANY,
        FREE_ONLY,
        PAID_ONLY
    }

    public enum DayPart {
        WEEKDAY,
        WEEKEND
    }

    public static final int MAX_CATEGORIES = 10;
    public static final int MAX_CITIES = 3;
    public static final long MAX_BUDGET = 100_000_000L;

    public final Set<Long> categoryIds = new LinkedHashSet<>();
    public Format format = Format.ANY;
    public PriceMode priceMode = PriceMode.ANY;
    public long maxBudget;
    public final Set<String> cities = new LinkedHashSet<>();
    public final Set<DayPart> days = new LinkedHashSet<>();
    public int version = 1;

    public PreferencesForm() {
        days.addAll(Arrays.asList(DayPart.WEEKDAY, DayPart.WEEKEND));
    }

    public PreferencesForm copy() {
        PreferencesForm copy = new PreferencesForm();
        copy.categoryIds.clear();
        copy.categoryIds.addAll(categoryIds);
        copy.format = format;
        copy.priceMode = priceMode;
        copy.maxBudget = maxBudget;
        copy.cities.addAll(cities);
        copy.days.clear();
        copy.days.addAll(days);
        copy.version = version;
        return copy;
    }

    /** Free-only preferences ignore the budget field (U03 data rules). */
    public long effectiveBudget() {
        return priceMode == PriceMode.FREE_ONLY ? 0L : Math.max(0L, Math.min(MAX_BUDGET, maxBudget));
    }
}
