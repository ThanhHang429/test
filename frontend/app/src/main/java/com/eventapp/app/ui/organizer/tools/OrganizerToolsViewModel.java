package com.eventapp.app.ui.organizer.tools;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.ui.model.DashboardUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-O03 / O06 / O09 / O27 — màn "Công cụ" của organizer.
 *
 * <p>Không có API danh sách sự kiện của chính organizer, nên danh sách chọn sự kiện lấy từ khối
 * {@code topEvents} của dashboard (API-17) — cùng nguồn với dashboard để số liệu khớp nhau.</p>
 */
public class OrganizerToolsViewModel extends ViewModel {

    private final OrganizerRepository organizer = ServiceLocator.get(OrganizerRepository.class);
    private final CategoryRepository categories = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<List<DashboardUiModel.TopEvent>> events =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CatalogDtos.CategoryNode>> categoryTree =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);

    private int pendingCalls;

    public OrganizerToolsViewModel() {
        load();
    }

    public LiveData<List<DashboardUiModel.TopEvent>> getEvents() {
        return events;
    }

    public LiveData<List<CatalogDtos.CategoryNode>> getCategories() {
        return categoryTree;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        pendingCalls = 2;
        loadEvents();
        loadCategories();
    }

    public void refresh() {
        load();
    }

    private void loadEvents() {
        organizer.dashboard("30d", null, null, new ApiCallback<DashboardDtos.Dashboard>() {
            @Override
            public void onSuccess(@Nullable DashboardDtos.Dashboard data,
                                  @Nullable JsonElement meta) {
                List<DashboardUiModel.TopEvent> values = new ArrayList<>();
                if (data != null && data.topEvents != null) {
                    for (DashboardDtos.TopEvent dto : data.topEvents) {
                        if (dto == null || dto.eventId <= 0L) {
                            continue;
                        }
                        values.add(new DashboardUiModel.TopEvent(
                                dto.eventId,
                                dto.title,
                                Math.round(dto.verifiedAmount),
                                (int) dto.registrations,
                                null,
                                null));
                    }
                }
                events.setValue(values);
                finishOne(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                finishOne(true);
            }
        });
    }

    private void loadCategories() {
        categories.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                  @Nullable JsonElement meta) {
                categoryTree.setValue(data == null ? new ArrayList<>() : data);
                finishOne(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                finishOne(true);
            }
        });
    }

    private void finishOne(boolean failed) {
        if (failed) {
            error.setValue(true);
        }
        pendingCalls--;
        if (pendingCalls <= 0) {
            loading.setValue(false);
        }
    }
}
