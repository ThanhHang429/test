package com.eventapp.app.ui.model;

/**
 * One FR-U07 suggestion. The reason is supplied by the backend; the screen must not build
 * reasons from prices or dates itself.
 */
public class RecommendationUiModel {

    public enum Source {
        AI,
        RULE
    }

    public final EventUiModel event;
    public final String reason;
    public final Source source;

    public RecommendationUiModel(EventUiModel event, String reason, Source source) {
        this.event = event;
        this.reason = reason;
        this.source = source == null ? Source.RULE : source;
    }
}
