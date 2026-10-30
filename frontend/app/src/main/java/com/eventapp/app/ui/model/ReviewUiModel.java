package com.eventapp.app.ui.model;

/**
 * Normalized review row used by FR-U05, FR-U20 and the admin moderation queue (FR-A14).
 * Backend status values are mapped into {@link Status}; the UI never depends on raw DTO names.
 */
public class ReviewUiModel {

    public enum Status {
        VISIBLE,
        HELD,
        HIDDEN
    }

    public enum Source {
        USER,
        HELD,
        FLAGGED,
        REPORTED
    }

    public final long id;
    public final long eventId;
    public final String eventTitle;
    public final String authorName;
    public final String authorCode;
    public final float rating;
    public final String comment;
    public final long createdAt;
    public final long editedAt;
    public final int editCount;
    public final Status status;
    public final Source source;
    public final boolean mine;
    public final String flagReason;
    public final String evidence;
    public final String severity;
    public final Integer spamScore;
    public final Integer toxicityScore;

    private ReviewUiModel(Builder builder) {
        id = builder.id;
        eventId = builder.eventId;
        eventTitle = builder.eventTitle;
        authorName = builder.authorName;
        authorCode = builder.authorCode;
        rating = builder.rating;
        comment = builder.comment;
        createdAt = builder.createdAt;
        editedAt = builder.editedAt;
        editCount = builder.editCount;
        status = builder.status;
        source = builder.source;
        mine = builder.mine;
        flagReason = builder.flagReason;
        evidence = builder.evidence;
        severity = builder.severity;
        spamScore = builder.spamScore;
        toxicityScore = builder.toxicityScore;
    }

    public static class Builder {
        private long id;
        private long eventId;
        private String eventTitle = "";
        private String authorName = "Người dùng";
        private String authorCode = "";
        private float rating = 5f;
        private String comment = "";
        private long createdAt = System.currentTimeMillis();
        private long editedAt;
        private int editCount;
        private Status status = Status.VISIBLE;
        private Source source = Source.USER;
        private boolean mine;
        private String flagReason;
        private String evidence;
        private String severity = "LOW";
        private Integer spamScore;
        private Integer toxicityScore;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder eventId(long eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventTitle(String eventTitle) {
            this.eventTitle = eventTitle == null ? "" : eventTitle;
            return this;
        }

        public Builder authorName(String authorName) {
            this.authorName = authorName == null ? "Người dùng" : authorName;
            return this;
        }

        public Builder authorCode(String authorCode) {
            this.authorCode = authorCode == null ? "" : authorCode;
            return this;
        }

        public Builder rating(float rating) {
            this.rating = rating;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment == null ? "" : comment;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder editedAt(long editedAt) {
            this.editedAt = editedAt;
            return this;
        }

        public Builder editCount(int editCount) {
            this.editCount = editCount;
            return this;
        }

        public Builder status(Status status) {
            this.status = status == null ? Status.VISIBLE : status;
            return this;
        }

        public Builder source(Source source) {
            this.source = source == null ? Source.USER : source;
            return this;
        }

        public Builder mine(boolean mine) {
            this.mine = mine;
            return this;
        }

        public Builder flagReason(String flagReason) {
            this.flagReason = flagReason;
            return this;
        }

        public Builder evidence(String evidence) {
            this.evidence = evidence;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity == null ? "LOW" : severity;
            return this;
        }

        public Builder spamScore(Integer spamScore) {
            this.spamScore = spamScore;
            return this;
        }

        public Builder toxicityScore(Integer toxicityScore) {
            this.toxicityScore = toxicityScore;
            return this;
        }

        public ReviewUiModel build() {
            return new ReviewUiModel(this);
        }
    }
}
