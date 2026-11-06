package com.eventapp.app.ui.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Số liệu tổng quan của nhà tổ chức (FR-O03). */
public final class OrgDashboardUiModel {

    public final long onSaleCount;
    public final long pendingReviewCount;
    public final long ticketsIssued;
    public final long confirmedRegistrations;
    public final long checkedIn;
    public final int checkinRatePercent;
    public final long verifiedRevenue;
    public final List<Task> tasks;

    public OrgDashboardUiModel(long onSaleCount, long pendingReviewCount, long ticketsIssued,
                               long confirmedRegistrations, long checkedIn,
                               int checkinRatePercent, long verifiedRevenue,
                               @NonNull List<Task> tasks) {
        this.onSaleCount = onSaleCount;
        this.pendingReviewCount = pendingReviewCount;
        this.ticketsIssued = ticketsIssued;
        this.confirmedRegistrations = confirmedRegistrations;
        this.checkedIn = checkedIn;
        this.checkinRatePercent = checkinRatePercent;
        this.verifiedRevenue = verifiedRevenue;
        this.tasks = Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    /** Việc cần làm hiển thị ở khối TODO. */
    public static final class Task {
        public final String title;
        public final String detail;

        public Task(String title, String detail) {
            this.title = title == null ? "" : title;
            this.detail = detail == null ? "" : detail;
        }
    }
}
