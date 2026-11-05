package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.NotificationApi;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;

import java.util.List;

/** Thông báo người tham dự nhận được từ organizer (FR-O28). */
public class NotificationRepository {

    private final NotificationApi api;

    public NotificationRepository(@NonNull NotificationApi api) {
        this.api = api;
    }

    public void mine(@NonNull ApiCallback<List<NotificationResponse>> callback) {
        ApiCallback.enqueue(api.mine(), callback);
    }
}
