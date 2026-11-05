package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.notification.NotificationResponse;
import com.eventapp.app.data.repository.NotificationRepository;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Trung tâm thông báo của người dùng (FR-O28). */
public class NotificationsViewModel extends ViewModel {

    private final NotificationRepository repository =
            ServiceLocator.get(NotificationRepository.class);

    private final MutableLiveData<List<NotificationResponse>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    public LiveData<List<NotificationResponse>> getItems() {
        return items;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public void reload() {
        error.setValue(null);
        repository.mine(new ApiCallback<List<NotificationResponse>>() {
            @Override
            public void onSuccess(@Nullable List<NotificationResponse> data,
                                  @Nullable JsonElement meta) {
                items.setValue(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                items.setValue(new ArrayList<>());
                error.setValue(exception);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }
}
