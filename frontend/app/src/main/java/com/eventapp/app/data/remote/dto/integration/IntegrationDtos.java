package com.eventapp.app.data.remote.dto.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Giám sát tích hợp (FR-A20). */
public final class IntegrationDtos {

    private IntegrationDtos() {
    }

    public static class Health {
        public String name;
        public String status;
        public double errorRate;
        public long requestCount;
        public String lastSuccessAt;
        public String lastErrorAt;
        public String lastErrorCode;
        public Long p95LatencyMs;
        public String notes;
    }

    public static class Usage {
        public String range;
        public String from;
        public String to;
        public long totalRequests;
        public long successCount;
        public long failureCount;
        public double successRate;
        public Map<String, Long> errorBreakdown = new LinkedHashMap<>();
        public Map<String, Long> byTaskType = new LinkedHashMap<>();
        public long avgLatencyMs;
        public long p95LatencyMs;
        public long inputTokens;
        public long outputTokens;
        public double estimatedCostUsd;
        public double dailyBudgetUsd;
        public double budgetUsedRatio;
        public Map<String, Boolean> features = new LinkedHashMap<>();
        public String circuitState;
    }

    public static class ErrorItem {
        public String at;
        public String taskType;
        public String errorCode;
        public String correlationId;
        public Integer latencyMs;
        public String userRef;
    }

    public static class RequestDetail {
        public Long aiRequestId;
        public String taskType;
        public String status;
        public String providerCode;
        public String modelCode;
        public String inputSummary;
        public String outputSummary;
        public Integer inputTokens;
        public Integer outputTokens;
        public Integer latencyMs;
        public String errorCode;
        public String correlationId;
        public String createdAt;
        public String completedAt;
        public String userRef;
    }

    public static class ToggleRequest {
        public boolean enabled;
        public String reason;

        public ToggleRequest(boolean enabled, String reason) {
            this.enabled = enabled;
            this.reason = reason;
        }
    }

    public static class ToggleResponse {
        public String taskType;
        public boolean enabled;
        public Map<String, Boolean> features = new LinkedHashMap<>();
        public boolean effectiveImmediately;
    }
}
