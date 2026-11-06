package com.eventapp.app.ui.model;

/** One failed AI request row for FR-A20. Never carries prompt content or user identifiers. */
public class IntegrationErrorUiModel {

    public final String timeLabel;
    public final String taskType;
    public final String errorCode;
    public final int latencyMs;
    public final String correlationId;

    public IntegrationErrorUiModel(String timeLabel, String taskType, String errorCode,
                                   int latencyMs, String correlationId) {
        this.timeLabel = timeLabel;
        this.taskType = taskType;
        this.errorCode = errorCode;
        this.latencyMs = latencyMs;
        this.correlationId = correlationId;
    }
}
