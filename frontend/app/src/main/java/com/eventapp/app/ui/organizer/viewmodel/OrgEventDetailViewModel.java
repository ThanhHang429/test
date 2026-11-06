package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.organizer.OrganizerEventDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgEventMapper;
import com.google.gson.JsonElement;

/** Chi tiết sự kiện của nhà tổ chức: tổng quan, hạng vé và hành động vận hành. */
public class OrgEventDetailViewModel extends ViewModel {

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<OrgEvent> event = new MutableLiveData<>();
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    public LiveData<OrgEvent> getEvent() {
        return event;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public void load(long eventId) {
        error.setValue(null);
        repository.event(eventId, new ApiCallback<OrganizerEventDtos.Event>() {
            @Override
            public void onSuccess(@Nullable OrganizerEventDtos.Event data,
                                  @Nullable JsonElement meta) {
                if (data != null) {
                    event.setValue(OrgEventMapper.from(data));
                }
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }
}
