package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.remote.dto.organizer.OrganizerEventDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.mapper.OrgDashboardMapper;
import com.eventapp.app.ui.model.OrgDashboardUiModel;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgEventMapper;
import com.google.gson.JsonElement;

import java.util.List;

/** Trạng thái tab Tổng quan của nhà tổ chức (FR-O03). */
public class OrgDashboardViewModel extends ViewModel {

    private final OrganizerRepository repository = ServiceLocator.get(OrganizerRepository.class);

    private final MutableLiveData<OrgDashboardUiModel> dashboard = new MutableLiveData<>();
    private final MutableLiveData<OrgEvent> featured = new MutableLiveData<>();
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    public LiveData<OrgDashboardUiModel> getDashboard() {
        return dashboard;
    }

    public LiveData<OrgEvent> getFeatured() {
        return featured;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public void load() {
        loadDashboard();
        loadFeatured();
    }

    private void loadDashboard() {
        repository.dashboard("30d", null, null,
                new ApiCallback<DashboardDtos.Dashboard>() {
                    @Override
                    public void onSuccess(@Nullable DashboardDtos.Dashboard data,
                                          @Nullable JsonElement meta) {
                        if (data != null) {
                            dashboard.setValue(OrgDashboardMapper.from(data));
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                    }
                });
    }

    private void loadFeatured() {
        repository.events(OrgEvent.FILTER_ON_SALE, 0, 1,
                new ApiCallback<List<OrganizerEventDtos.Event>>() {
                    @Override
                    public void onSuccess(@Nullable List<OrganizerEventDtos.Event> data,
                                          @Nullable JsonElement meta) {
                        if (data == null || data.isEmpty()) {
                            featured.setValue(null);
                            return;
                        }
                        featured.setValue(OrgEventMapper.from(data.get(0)));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        // Khối sự kiện nổi bật không chặn phần còn lại của dashboard.
                        featured.setValue(null);
                    }
                });
    }

    public void consumeError() {
        error.setValue(null);
    }
}
