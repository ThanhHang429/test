package com.eventapp.app.ui.admin.event;

/**
 * One moderation flag from FR-A07. The backend DTO will be mapped into this class so the screen
 * never depends on raw API names.
 */
public class ModerationUiModel {

    public enum TargetType {
        EVENT_TEXT,
        EVENT_IMAGE,
        TAG,
        REVIEW
    }

    public enum Severity {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum Status {
        OPEN,
        CONFIRMED,
        DISMISSED
    }

    public enum Source {
        RULE,
        AI
    }

    public final long id;
    public final TargetType targetType;
    public final long targetId;
    public final String targetName;
    public final Severity severity;
    public final String reason;
    public final String evidence;
    public final long createdAt;

    /** Mutable because a local confirm/dismiss/rescan updates the row without a round trip. */
    public Status status;
    public Source source;
    public int confidence;
    public boolean unchecked;
    public boolean contentChanged;
    public long reviewedAt;
    public String reviewNote;

    private ModerationUiModel(Builder builder) {
        id = builder.id;
        targetType = builder.targetType;
        targetId = builder.targetId;
        targetName = builder.targetName;
        severity = builder.severity;
        status = builder.status;
        source = builder.source;
        confidence = builder.confidence;
        reason = builder.reason;
        evidence = builder.evidence;
        createdAt = builder.createdAt;
        unchecked = builder.unchecked;
        contentChanged = builder.contentChanged;
        reviewedAt = builder.reviewedAt;
        reviewNote = builder.reviewNote;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public static class Builder {
        private long id;
        private TargetType targetType = TargetType.EVENT_TEXT;
        private long targetId;
        private String targetName = "";
        private Severity severity = Severity.LOW;
        private Status status = Status.OPEN;
        private Source source = Source.RULE;
        private int confidence;
        private String reason = "";
        private String evidence = "";
        private long createdAt = System.currentTimeMillis();
        private boolean unchecked;
        private boolean contentChanged;
        private long reviewedAt;
        private String reviewNote = "";

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder targetType(TargetType targetType) {
            this.targetType = targetType == null ? TargetType.EVENT_TEXT : targetType;
            return this;
        }

        public Builder targetId(long targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder targetName(String targetName) {
            this.targetName = targetName == null ? "" : targetName;
            return this;
        }

        public Builder severity(Severity severity) {
            this.severity = severity == null ? Severity.LOW : severity;
            return this;
        }

        public Builder status(Status status) {
            this.status = status == null ? Status.OPEN : status;
            return this;
        }

        public Builder source(Source source) {
            this.source = source == null ? Source.RULE : source;
            return this;
        }

        public Builder confidence(int confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason == null ? "" : reason;
            return this;
        }

        public Builder evidence(String evidence) {
            this.evidence = evidence == null ? "" : evidence;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder unchecked(boolean unchecked) {
            this.unchecked = unchecked;
            return this;
        }

        public Builder contentChanged(boolean contentChanged) {
            this.contentChanged = contentChanged;
            return this;
        }

        public Builder reviewedAt(long reviewedAt) {
            this.reviewedAt = reviewedAt;
            return this;
        }

        public Builder reviewNote(String reviewNote) {
            this.reviewNote = reviewNote == null ? "" : reviewNote;
            return this;
        }

        public ModerationUiModel build() {
            return new ModerationUiModel(this);
        }
    }
}
