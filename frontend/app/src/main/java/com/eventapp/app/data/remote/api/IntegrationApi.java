package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Giám sát tích hợp (FR-A20). */
public interface IntegrationApi {

    @GET("admin/integrations")
    Call<ResponseWrapper.Envelope<List<IntegrationDtos.Health>>> health();

    @GET("admin/integrations/ai/requests")
    Call<ResponseWrapper.Envelope<IntegrationDtos.Usage>> usage(@Query("range") String range);

    @GET("admin/integrations/ai/requests/errors")
    Call<ResponseWrapper.Envelope<List<IntegrationDtos.ErrorItem>>> errors(@Query("limit") int limit);

    @GET("admin/integrations/ai/requests/{correlationId}")
    Call<ResponseWrapper.Envelope<List<IntegrationDtos.RequestDetail>>> request(
            @Path("correlationId") String correlationId);

    @PUT("admin/integrations/ai/features/{taskType}")
    Call<ResponseWrapper.Envelope<IntegrationDtos.ToggleResponse>> toggle(
            @Path("taskType") String taskType,
            @Body IntegrationDtos.ToggleRequest request);
}
