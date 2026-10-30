package com.eventapp.app.ui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI model for FR-O27. Metrics come from API-18 {@code metrics}; text only explains them
 * (O27-R01). Insights never carry an "apply" action (O27-R03) — they link to the owning screen.
 */
public class AiInsightUiModel {

    public enum Type {
        OBSERVATION,
        SUGGESTION
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    /** AI output, or the rule-based fallback used when the assistant is unavailable. */
    public enum Source {
        AI,
        RULE
    }

    public enum MetricKey {
        REGISTRATIONS,
        FILL_RATE,
        DAYS_LEFT,
        CHECKIN_RATE,
        RATING
    }

    /** Screen the organizer decides to open; never an automatic change. */
    public enum ActionTarget {
        NONE,
        EDIT_EVENT,
        EDIT_TICKETS,
        VIEW_ORDERS
    }

    public static class Metric {
        public final MetricKey key;
        public final double value;
        public final String caption;

        public Metric(MetricKey key, double value, String caption) {
            this.key = key;
            this.value = value;
            this.caption = caption;
        }
    }

    public static class Insight {
        public final Type type;
        public final String text;
        public final MetricKey relatedMetric;
        public final Confidence confidence;
        public final ActionTarget action;

        public Insight(Type type, String text, MetricKey relatedMetric, Confidence confidence,
                       ActionTarget action) {
            this.type = type == null ? Type.OBSERVATION : type;
            this.text = text == null ? "" : text;
            this.relatedMetric = relatedMetric;
            this.confidence = confidence == null ? Confidence.LOW : confidence;
            this.action = action == null ? ActionTarget.NONE : action;
        }
    }

    public static class EventOption {
        public final long id;
        public final String title;

        public EventOption(long id, String title) {
            this.id = id;
            this.title = title == null ? "" : title;
        }
    }

    public static class Analysis {
        public final long eventId;
        public final String eventTitle;
        public final long generatedAt;
        public final Source source;
        public final boolean insufficientData;
        public final String insufficientMessage;
        public final List<Metric> metrics;
        public final List<String> summary;
        public final List<Insight> insights;
        public final List<String> limitations;
        public final List<Float> dailyRegistrations;
        public final List<String> dailyLabels;
        public final boolean fromCache;

        public Analysis(long eventId, String eventTitle, long generatedAt, Source source,
                        boolean insufficientData, String insufficientMessage, List<Metric> metrics,
                        List<String> summary, List<Insight> insights, List<String> limitations,
                        List<Float> dailyRegistrations, List<String> dailyLabels,
                        boolean fromCache) {
            this.eventId = eventId;
            this.eventTitle = eventTitle == null ? "" : eventTitle;
            this.generatedAt = generatedAt;
            this.source = source == null ? Source.AI : source;
            this.insufficientData = insufficientData;
            this.insufficientMessage = insufficientMessage == null ? "" : insufficientMessage;
            this.metrics = copy(metrics);
            this.summary = copy(summary);
            this.insights = copy(insights);
            this.limitations = copy(limitations);
            this.dailyRegistrations = copy(dailyRegistrations);
            this.dailyLabels = copy(dailyLabels);
            this.fromCache = fromCache;
        }

        public Metric metric(MetricKey key) {
            for (Metric metric : metrics) {
                if (metric.key == key) {
                    return metric;
                }
            }
            return null;
        }

        private static <T> List<T> copy(List<T> values) {
            return values == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}
