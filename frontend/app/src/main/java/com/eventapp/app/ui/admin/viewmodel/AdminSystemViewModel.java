package com.eventapp.app.ui.admin.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;
import com.eventapp.app.data.repository.SystemConfigRepository;
import com.eventapp.app.ui.mapper.AdminSystemMapper;
import com.eventapp.app.ui.model.AdminSystemUiModel;
import com.google.gson.JsonElement;

import java.util.List;

/** Trạng thái tab Hệ thống: tình trạng các tích hợp thanh toán, FCM và AI. */
public class AdminSystemViewModel extends ViewModel {

    private final SystemConfigRepository repository =
            ServiceLocator.get(SystemConfigRepository.class);

    private final MutableLiveData<AdminSystemUiModel> health = new MutableLiveData<>();

    public LiveData<AdminSystemUiModel> getHealth() {
        return health;
    }

    public void loadHealth() {
        repository.health(new ApiCallback<List<IntegrationDtos.Health>>() {
            @Override
            public void onSuccess(@Nullable List<IntegrationDtos.Health> data,
                                  @Nullable JsonElement meta) {
                health.setValue(data == null ? null : AdminSystemMapper.fromHealth(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                health.setValue(null);
            }
        });
    }
}
