package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.OrganizerApi;
import com.eventapp.app.data.remote.dto.classification.ClassificationDtos;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;

import java.util.List;

/** Dashboard, AI và phân loại sự kiện cho organizer. */
public class OrganizerRepository {

    private final OrganizerApi api;

    public OrganizerRepository(@NonNull OrganizerApi api) {
        this.api = api;
    }

    public void dashboard(@NonNull String range, String from, String to,
                          @NonNull ApiCallback<DashboardDtos.Dashboard> callback) {
        ApiCallback.enqueue(api.dashboard(range, from, to), callback);
    }

    public void describe(@NonNull OrganizerAiDtos.DescriptionRequest request,
                         @NonNull ApiCallback<OrganizerAiDtos.DescriptionResponse> callback) {
        ApiCallback.enqueue(api.describe(request), callback);
    }

    public void announce(@NonNull OrganizerAiDtos.AnnouncementRequest request,
                         @NonNull ApiCallback<OrganizerAiDtos.AnnouncementResponse> callback) {
        ApiCallback.enqueue(api.announce(request), callback);
    }

    public void analyze(long eventId,
                        @NonNull ApiCallback<OrganizerAiDtos.AnalysisResponse> callback) {
        ApiCallback.enqueue(api.analyze(eventId), callback);
    }

    public void classification(long eventId,
                               @NonNull ApiCallback<ClassificationDtos.Classification> callback) {
        ApiCallback.enqueue(api.classification(eventId), callback);
    }

    public void updateClassification(long eventId,
                                     @NonNull ClassificationDtos.UpdateRequest request,
                                     @NonNull ApiCallback<ClassificationDtos.Classification> callback) {
        ApiCallback.enqueue(api.updateClassification(eventId, request), callback);
    }

    public void suggestTags(String query, int limit,
                            @NonNull ApiCallback<List<ClassificationDtos.TagSuggestion>> callback) {
        ApiCallback.enqueue(api.suggestTags(query, limit), callback);
    }
}
