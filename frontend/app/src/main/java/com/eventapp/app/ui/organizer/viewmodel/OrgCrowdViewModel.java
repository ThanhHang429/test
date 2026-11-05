package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;
import com.eventapp.app.data.remote.dto.organizer.CrowdDtos;
import com.eventapp.app.data.remote.dto.organizerai.OrganizerAiDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.mapper.CrowdMapper;
import com.eventapp.app.ui.mapper.NotificationMapper;
import com.eventapp.app.ui.model.CrowdStatusUiModel;
import com.eventapp.app.ui.model.NotificationUiModel;
import com.eventapp.app.ui.model.OverflowRoomUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Điều phối phòng (FR-O28): mức lấp đầy, phòng dự phòng, thông báo và lịch sử gửi. */
public class OrgCrowdViewModel extends ViewModel {

    private static final String AUDIENCE_NOT_CHECKED_IN = "REGISTERED_NOT_CHECKED_IN";
    private static final String SOURCE_AI = "AI";
    private static final String SOURCE_MANUAL = "MANUAL";

    /** Bản nháp thông báo do AI soạn, kèm tên phòng đích nếu có. */
    public static class AiDraft {
        public final String text;
        @Nullable
        public final String roomName;

        public AiDraft(@NonNull String text, @Nullable String roomName) {
            this.text = text;
            this.roomName = roomName;
        }
    }

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<String> eventTitle = new MutableLiveData<>("");
    private final MutableLiveData<CrowdStatusUiModel> status = new MutableLiveData<>();
    private final MutableLiveData<List<OverflowRoomUiModel>> rooms =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<NotificationUiModel>> history =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> sending = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> roomAdded = new MutableLiveData<>();
    private final MutableLiveData<Boolean> roomClosed = new MutableLiveData<>();
    private final MutableLiveData<AiDraft> aiDraft = new MutableLiveData<>();
    private final MutableLiveData<Boolean> aiFailed = new MutableLiveData<>();
    private final MutableLiveData<Integer> sentCount = new MutableLiveData<>();

    private long eventId;
    private CrowdDtos.CrowdStatus statusValue;

    public LiveData<String> getEventTitle() {
        return eventTitle;
    }

    public LiveData<CrowdStatusUiModel> getStatus() {
        return status;
    }

    public LiveData<List<OverflowRoomUiModel>> getRooms() {
        return rooms;
    }

    public LiveData<List<NotificationUiModel>> getHistory() {
        return history;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSending() {
        return sending;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<Boolean> getRoomAdded() {
        return roomAdded;
    }

    public LiveData<Boolean> getRoomClosed() {
        return roomClosed;
    }

    public LiveData<AiDraft> getAiDraft() {
        return aiDraft;
    }

    public LiveData<Boolean> getAiFailed() {
        return aiFailed;
    }

    public LiveData<Integer> getSentCount() {
        return sentCount;
    }

    public void init(long eventId, @Nullable String title) {
        this.eventId = eventId;
        eventTitle.setValue(title == null ? "" : title);
    }

    public void load() {
        loadStatus();
        loadRooms();
        loadHistory();
    }

    public void addRoom(@NonNull String name, int capacity, @NonNull String note) {
        loading.setValue(true);
        CrowdDtos.CreateRoomRequest request = new CrowdDtos.CreateRoomRequest(
                name, capacity, note.isEmpty() ? null : note);
        repository.addOverflowRoom(eventId, request,
                new ApiCallback<CrowdDtos.OverflowRoom>() {
                    @Override
                    public void onSuccess(@Nullable CrowdDtos.OverflowRoom data,
                                          @Nullable JsonElement meta) {
                        loading.setValue(false);
                        roomAdded.setValue(Boolean.TRUE);
                        loadRooms();
                        loadStatus();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(exception);
                    }
                });
    }

    public void closeRoom(long roomId) {
        repository.closeOverflowRoom(eventId, roomId,
                new ApiCallback<CrowdDtos.OverflowRoom>() {
                    @Override
                    public void onSuccess(@Nullable CrowdDtos.OverflowRoom data,
                                          @Nullable JsonElement meta) {
                        roomClosed.setValue(Boolean.TRUE);
                        loadRooms();
                        loadStatus();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                    }
                });
    }

    public void composeWithAi(@NonNull String topic, @NonNull String audience,
                              @NonNull String occupancyLabel, @Nullable String capacityLabel) {
        loading.setValue(true);
        CrowdDtos.OverflowRoom room = statusValue == null ? null : statusValue.suggestedRoom;

        OrganizerAiDtos.AnnouncementRequest request = new OrganizerAiDtos.AnnouncementRequest();
        request.eventId = eventId;
        request.eventTitle = eventTitle.getValue();
        request.topic = topic;
        request.audience = audience;
        request.tone = "friendly";
        request.length = "short";
        request.variants = 1;
        request.keyPoints.add(statusValue == null || statusValue.warning == null
                ? occupancyLabel : statusValue.warning);
        if (room != null && capacityLabel != null) {
            request.keyPoints.add(room.roomName + " · " + capacityLabel);
        }

        repository.announce(request, new ApiCallback<OrganizerAiDtos.AnnouncementResponse>() {
            @Override
            public void onSuccess(@Nullable OrganizerAiDtos.AnnouncementResponse data,
                                  @Nullable JsonElement meta) {
                loading.setValue(false);
                if (data == null || data.variants == null || data.variants.isEmpty()) {
                    aiFailed.setValue(Boolean.TRUE);
                    return;
                }
                String text = data.variants.get(0).text;
                if (text == null || text.trim().isEmpty()) {
                    aiFailed.setValue(Boolean.TRUE);
                    return;
                }
                aiDraft.setValue(new AiDraft(text.trim(), room == null ? null : room.roomName));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loading.setValue(false);
                aiFailed.setValue(Boolean.TRUE);
            }
        });
    }

    public void send(@NonNull String title, @NonNull String body, boolean fromAi) {
        sending.setValue(true);
        CrowdDtos.SendNotificationRequest request = new CrowdDtos.SendNotificationRequest(
                title, body, AUDIENCE_NOT_CHECKED_IN, fromAi ? SOURCE_AI : SOURCE_MANUAL);
        repository.sendNotification(eventId, request,
                new ApiCallback<CrowdDtos.SendNotificationResponse>() {
                    @Override
                    public void onSuccess(@Nullable CrowdDtos.SendNotificationResponse data,
                                          @Nullable JsonElement meta) {
                        sending.setValue(false);
                        sentCount.setValue(data == null ? 0 : data.recipientCount);
                        loadHistory();
                        loadStatus();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        sending.setValue(false);
                        error.setValue(exception);
                    }
                });
    }

    public void consumeError() {
        error.setValue(null);
    }

    public void consumeRoomAdded() {
        roomAdded.setValue(null);
    }

    public void consumeRoomClosed() {
        roomClosed.setValue(null);
    }

    public void consumeAiDraft() {
        aiDraft.setValue(null);
    }

    public void consumeAiFailed() {
        aiFailed.setValue(null);
    }

    public void consumeSentCount() {
        sentCount.setValue(null);
    }

    private void loadStatus() {
        loading.setValue(true);
        repository.crowdStatus(eventId, new ApiCallback<CrowdDtos.CrowdStatus>() {
            @Override
            public void onSuccess(@Nullable CrowdDtos.CrowdStatus data,
                                  @Nullable JsonElement meta) {
                if (data == null) {
                    return;
                }
                statusValue = data;
                if (data.eventTitle != null) {
                    String current = eventTitle.getValue();
                    if (current == null || current.isEmpty()) {
                        eventTitle.setValue(data.eventTitle);
                    }
                }
                status.setValue(CrowdMapper.fromStatus(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }

            @Override
            public void onFinish() {
                loading.setValue(false);
            }
        });
    }

    private void loadRooms() {
        repository.overflowRooms(eventId,
                new ApiCallback<List<CrowdDtos.OverflowRoom>>() {
                    @Override
                    public void onSuccess(@Nullable List<CrowdDtos.OverflowRoom> data,
                                          @Nullable JsonElement meta) {
                        rooms.setValue(CrowdMapper.fromRooms(data));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                    }
                });
    }

    private void loadHistory() {
        repository.notifications(eventId,
                new ApiCallback<List<NotificationResponse>>() {
                    @Override
                    public void onSuccess(@Nullable List<NotificationResponse> data,
                                          @Nullable JsonElement meta) {
                        history.setValue(NotificationMapper.fromList(data));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                    }
                });
    }
}
