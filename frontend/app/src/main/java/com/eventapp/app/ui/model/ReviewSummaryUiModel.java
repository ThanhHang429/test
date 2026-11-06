package com.eventapp.app.ui.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Điểm trung bình và phân bố số sao của một sự kiện (FR-U20). */
public final class ReviewSummaryUiModel {

    public final double averageRating;
    public final long totalReviews;
    public final Map<String, Long> distribution;

    public ReviewSummaryUiModel(double averageRating, long totalReviews,
                                @NonNull Map<String, Long> distribution) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.distribution = Collections.unmodifiableMap(new LinkedHashMap<>(distribution));
    }
}
