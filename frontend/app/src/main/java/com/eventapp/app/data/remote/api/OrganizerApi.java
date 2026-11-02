package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.classification.ClassificationDtos;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** AI, phân loại và dashboard cho organizer (FR-O03 / O06 / O09 / O27). */
public interface OrganizerApi {

    @GET("organizer/dashboard")
    Call<ResponseWrapper.Envelope<DashboardDtos.Dashboard>> dashboard(
            @Query("range") String range,
            @Query("from") String from,
            @Query("to") String to);

    @POST("organizer/ai/event-description")
    Call<ResponseWrapper.Envelope<OrganizerAiDtos.DescriptionResponse>> describe(
            @Body OrganizerAiDtos.DescriptionRequest request);

    @POST("organizer/ai/announcement")
    Call<ResponseWrapper.Envelope<OrganizerAiDtos.AnnouncementResponse>> announce(
            @Body OrganizerAiDtos.AnnouncementRequest request);

    @POST("organizer/events/{eventId}/ai-analysis")
    Call<ResponseWrapper.Envelope<OrganizerAiDtos.AnalysisResponse>> analyze(
            @Path("eventId") long eventId);

    @GET("organizer/events/{eventId}/classification")
    Call<ResponseWrapper.Envelope<ClassificationDtos.Classification>> classification(
            @Path("eventId") long eventId);

    @PUT("organizer/events/{eventId}/classification")
    Call<ResponseWrapper.Envelope<ClassificationDtos.Classification>> updateClassification(
            @Path("eventId") long eventId,
            @Body ClassificationDtos.UpdateRequest request);

    @GET("tags/suggest")
    Call<ResponseWrapper.Envelope<List<ClassificationDtos.TagSuggestion>>> suggestTags(
            @Query("q") String q,
            @Query("limit") int limit);
}
