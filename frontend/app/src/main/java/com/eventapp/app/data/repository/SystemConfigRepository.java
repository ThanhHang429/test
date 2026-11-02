package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.IntegrationApi;
import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;

import java.util.List;

/** Giám sát tích hợp AI / thanh toán / FCM (FR-A20). */
public class SystemConfigRepository {

    private final IntegrationApi api;

    public SystemConfigRepository(@NonNull IntegrationApi api) {
        this.api = api;
    }

    public void health(@NonNull ApiCallback<List<IntegrationDtos.Health>> callback) {
        ApiCallback.enqueue(api.health(), callback);
    }

    public void aiUsage(@NonNull String range,
                        @NonNull ApiCallback<IntegrationDtos.Usage> callback) {
        ApiCallback.enqueue(api.usage(range), callback);
    }

    public void aiErrors(int limit,
                         @NonNull ApiCallback<List<IntegrationDtos.ErrorItem>> callback) {
        ApiCallback.enqueue(api.errors(limit), callback);
    }

    public void aiRequest(@NonNull String correlationId,
                          @NonNull ApiCallback<List<IntegrationDtos.RequestDetail>> callback) {
        ApiCallback.enqueue(api.request(correlationId), callback);
    }

    public void toggleFeature(@NonNull String taskType, boolean enabled, @NonNull String reason,
                              @NonNull ApiCallback<IntegrationDtos.ToggleResponse> callback) {
        ApiCallback.enqueue(api.toggle(taskType, new IntegrationDtos.ToggleRequest(enabled, reason)),
                callback);
    }
}
