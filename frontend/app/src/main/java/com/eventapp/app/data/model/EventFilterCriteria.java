package com.eventapp.app.data.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Filter state for FR-U04. Kept serializable so the EventList -> BottomSheet round trip and
 * process recreation can restore the exact selection.
 */
public class EventFilterCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long MAX_PRICE = 5_000_000L;

    public enum TimePreset {
        ANY,
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        CUSTOM
    }

    public enum Format {
        ANY,
        ONSITE,
        ONLINE
    }

    public enum PriceType {
        ANY,
        FREE,
        PAID
    }

    public enum Sort {
        START_TIME_ASC,
        NEWEST,
        PRICE_ASC,
        PRICE_DESC,
        POPULAR
    }

    public final Set<Long> categoryIds = new LinkedHashSet<>();
    public TimePreset timePreset = TimePreset.ANY;
    public long dateFrom;
    public long dateTo;
    public String city = "";
    public Format format = Format.ANY;
    public PriceType priceType = PriceType.ANY;
    public boolean priceRangeSet;
    public long priceMin;
    public long priceMax = MAX_PRICE;
    public boolean availableOnly;
    public Sort sort = Sort.START_TIME_ASC;

    public EventFilterCriteria copy() {
        EventFilterCriteria copy = new EventFilterCriteria();
        copy.categoryIds.addAll(categoryIds);
        copy.timePreset = timePreset;
        copy.dateFrom = dateFrom;
        copy.dateTo = dateTo;
        copy.city = city;
        copy.format = format;
        copy.priceType = priceType;
        copy.priceRangeSet = priceRangeSet;
        copy.priceMin = priceMin;
        copy.priceMax = priceMax;
        copy.availableOnly = availableOnly;
        copy.sort = sort;
        return copy;
    }

    public void reset() {
        categoryIds.clear();
        timePreset = TimePreset.ANY;
        dateFrom = 0L;
        dateTo = 0L;
        city = "";
        format = Format.ANY;
        priceType = PriceType.ANY;
        priceRangeSet = false;
        priceMin = 0L;
        priceMax = MAX_PRICE;
        availableOnly = false;
        sort = Sort.START_TIME_ASC;
    }

    public boolean isEmpty() {
        return categoryIds.isEmpty()
                && timePreset == TimePreset.ANY
                && city.trim().isEmpty()
                && format == Format.ANY
                && priceType == PriceType.ANY
                && !priceRangeSet
                && !availableOnly;
    }

    public int activeCount() {
        int count = categoryIds.isEmpty() ? 0 : 1;
        if (timePreset != TimePreset.ANY) {
            count++;
        }
        if (!city.trim().isEmpty()) {
            count++;
        }
        if (format != Format.ANY) {
            count++;
        }
        if (priceType != PriceType.ANY || priceRangeSet) {
            count++;
        }
        if (availableOnly) {
            count++;
        }
        return count;
    }
}
