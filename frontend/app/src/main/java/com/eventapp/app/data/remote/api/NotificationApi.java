package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/** Thông báo người tham dự nhận được từ organizer (FR-O28 / FR-U19). */
public interface NotificationApi {

    @GET("me/notifications")
    Call<ResponseWrapper.Envelope<List<NotificationResponse>>> mine();
}
