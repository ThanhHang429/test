package com.eventapp.app.ui.model;

import androidx.annotation.StringRes;

/** One external integration row for FR-A20. */
public class ServiceHealthUiModel {

    public enum Status {
        HEALTHY,
        DEGRADED,
        DOWN,
        UNKNOWN
    }

    @StringRes
    public final int nameRes;
    public final Status status;
    public final int errorRatePct;
    public final int p95LatencyMs;
    public final int requestCount;

    public ServiceHealthUiModel(@StringRes int nameRes, Status status, int errorRatePct,
                                int p95LatencyMs, int requestCount) {
        this.nameRes = nameRes;
        this.status = status;
        this.errorRatePct = errorRatePct;
        this.p95LatencyMs = p95LatencyMs;
        this.requestCount = requestCount;
    }
}
