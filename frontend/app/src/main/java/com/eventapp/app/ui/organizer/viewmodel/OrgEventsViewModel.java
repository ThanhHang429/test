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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Sự kiện: tải danh sách từ FR-O02, lọc trạng thái và tìm kiếm phía máy khách. */
public class OrgEventsViewModel extends ViewModel {

    private static final int PAGE_SIZE = 50;

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<List<OrgEvent>> events =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    private final List<OrgEvent> items = new ArrayList<>();
    private String filter = OrgEvent.FILTER_ALL;
    private String query = "";
    private int requestId;

    public LiveData<List<OrgEvent>> getEvents() {
        return events;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public String getFilter() {
        return filter;
    }

    public void selectFilter(@NonNull String value) {
        filter = value;
        load();
    }

    public void setQuery(@Nullable String value) {
        query = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        publish();
    }

    public void consumeError() {
        error.setValue(null);
    }

    private void load() {
        final int token = ++requestId;
        loading.setValue(true);
        repository.events(filter, 0, PAGE_SIZE,
                new ApiCallback<List<OrganizerEventDtos.Event>>() {
                    @Override
                    public void onSuccess(@Nullable List<OrganizerEventDtos.Event> data,
                                          @Nullable JsonElement meta) {
                        if (token != requestId) {
                            return;
                        }
                        items.clear();
                        if (data != null) {
                            for (OrganizerEventDtos.Event dto : data) {
                                items.add(OrgEventMapper.from(dto));
                            }
                        }
                        publish();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        if (token != requestId) {
                            return;
                        }
                        items.clear();
                        publish();
                        error.setValue(exception);
                    }

                    @Override
                    public void onFinish() {
                        if (token == requestId) {
                            loading.setValue(false);
                        }
                    }
                });
    }

    private void publish() {
        List<OrgEvent> result = new ArrayList<>();
        for (OrgEvent event : items) {
            if (query.isEmpty() || matches(event)) {
                result.add(event);
            }
        }
        events.setValue(result);
    }

    private boolean matches(OrgEvent event) {
        return event.title.toLowerCase(Locale.ROOT).contains(query)
                || event.code.toLowerCase(Locale.ROOT).contains(query)
                || event.city.toLowerCase(Locale.ROOT).contains(query)
                || event.venueName.toLowerCase(Locale.ROOT).contains(query);
    }
}
