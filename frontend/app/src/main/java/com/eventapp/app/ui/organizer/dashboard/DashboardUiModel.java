package com.eventapp.app.ui.organizer.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI model for FR-O03. Every number is a server aggregate (API-17): the client only formats it
 * (O03-R03) and never re-computes totals.
 */
public class DashboardUiModel {

    /** Per-block status returned by API-17 so one failing source does not break the screen. */
    public enum BlockStatus {
        OK,
        STALE,
        ERROR
    }

    public enum MetricKey {
        REGISTRATIONS,
        TICKETS,
        CHECKIN,
        RATING
    }

    /**
     * Review dimension of an event (O03-R04). Kept separate from the lifecycle dimension so the
     * dashboard never collapses the two backend fields into one label.
     */
    public enum ReviewStage {
        DRAFT,
        PENDING_REVIEW,
        REVISION_REQUIRED,
        REJECTED,
        APPROVED
    }

    /** Lifecycle dimension of an event (O03-R04). */
    public enum LifecycleStage {
        ON_SALE,
        UPCOMING,
        COMPLETED,
        SUSPENDED,
        CANCELLED,
        UNPUBLISHED
    }

    public enum TaskKind {
        EVENT_STARTING_SOON,
        REFUND_PENDING,
        EVENT_REVISION,
        EVENT_REJECTED,
        TICKET_SALE_ENDING
    }

    /** O03-R07 ordering: starting events, refunds, events needing edits. */
    public enum Urgency {
        NOW,
        SOON,
        LATER
    }

    public static class Finance {
        public final long verifiedAmount;
        public final long pendingAmount;
        public final long refundedAmount;
        public final long feeAmount;
        public final long netAmount;
        public final Integer changePercent;
        public final BlockStatus status;

        public Finance(long verifiedAmount, long pendingAmount, long refundedAmount,
                       long feeAmount, long netAmount, Integer changePercent, BlockStatus status) {
            this.verifiedAmount = verifiedAmount;
            this.pendingAmount = pendingAmount;
            this.refundedAmount = refundedAmount;
            this.feeAmount = feeAmount;
            this.netAmount = netAmount;
            this.changePercent = changePercent;
            this.status = status == null ? BlockStatus.OK : status;
        }

        public boolean isAvailable() {
            return status != BlockStatus.ERROR;
        }
    }

    public static class Metric {
        public final MetricKey key;
        public final double value;
        public final String caption;
        public final Integer deltaPercent;
        public final BlockStatus status;

        public Metric(MetricKey key, double value, String caption, Integer deltaPercent,
                      BlockStatus status) {
            this.key = key;
            this.value = value;
            this.caption = caption;
            this.deltaPercent = deltaPercent;
            this.status = status == null ? BlockStatus.OK : status;
        }

        public boolean isAvailable() {
            return status != BlockStatus.ERROR;
        }
    }

    public static class StatusCount {
        public final ReviewStage reviewStage;
        public final LifecycleStage lifecycleStage;
        public final int count;

        private StatusCount(ReviewStage reviewStage, LifecycleStage lifecycleStage, int count) {
            this.reviewStage = reviewStage;
            this.lifecycleStage = lifecycleStage;
            this.count = count;
        }

        public static StatusCount review(ReviewStage stage, int count) {
            return new StatusCount(stage, null, count);
        }

        public static StatusCount lifecycle(LifecycleStage stage, int count) {
            return new StatusCount(null, stage, count);
        }

        public boolean isReview() {
            return reviewStage != null;
        }
    }

    public static class Task {
        public final TaskKind kind;
        public final Urgency urgency;
        public final String title;
        public final String subtitle;
        public final long eventId;
        public final int count;

        public Task(TaskKind kind, Urgency urgency, String title, String subtitle, long eventId,
                    int count) {
            this.kind = kind;
            this.urgency = urgency == null ? Urgency.LATER : urgency;
            this.title = title == null ? "" : title;
            this.subtitle = subtitle == null ? "" : subtitle;
            this.eventId = eventId;
            this.count = count;
        }
    }

    public static class TopEvent {
        public final long id;
        public final String title;
        public final long verifiedRevenue;
        public final int registrations;
        public final ReviewStage reviewStage;
        public final LifecycleStage lifecycleStage;

        public TopEvent(long id, String title, long verifiedRevenue, int registrations,
                        ReviewStage reviewStage, LifecycleStage lifecycleStage) {
            this.id = id;
            this.title = title == null ? "" : title;
            this.verifiedRevenue = verifiedRevenue;
            this.registrations = registrations;
            this.reviewStage = reviewStage;
            this.lifecycleStage = lifecycleStage;
        }
    }

    /** One API-17 response: blocks keep their own status and the timestamp of the payload. */
    public static class Snapshot {
        public final String organizerName;
        public final Finance finance;
        public final List<Metric> metrics;
        public final List<StatusCount> statusCounts;
        public final List<Task> tasks;
        public final List<TopEvent> topEvents;
        public final long asOf;
        public final boolean fromCache;
        public final boolean readOnly;

        public Snapshot(String organizerName, Finance finance, List<Metric> metrics,
                        List<StatusCount> statusCounts, List<Task> tasks,
                        List<TopEvent> topEvents, long asOf, boolean fromCache,
                        boolean readOnly) {
            this.organizerName = organizerName == null ? "" : organizerName;
            this.finance = finance;
            this.metrics = metrics == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(metrics));
            this.statusCounts = statusCounts == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(statusCounts));
            this.tasks = tasks == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(tasks));
            this.topEvents = topEvents == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(topEvents));
            this.asOf = asOf;
            this.fromCache = fromCache;
            this.readOnly = readOnly;
        }

        public boolean hasEvents() {
            return !statusCounts.isEmpty() || !topEvents.isEmpty();
        }

        public Metric metric(MetricKey key) {
            for (Metric metric : metrics) {
                if (metric.key == key) {
                    return metric;
                }
            }
            return null;
        }
    }
}
