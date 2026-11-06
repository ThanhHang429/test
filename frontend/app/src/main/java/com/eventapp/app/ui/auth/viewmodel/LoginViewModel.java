package com.eventapp.app.ui.auth.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.data.repository.AuthRepository;
import com.eventapp.app.ui.mapper.DevAccountMapper;
import com.eventapp.app.ui.model.DevAccountUiModel;
import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.List;

/** Trạng thái màn đăng nhập bằng tài khoản seed của backend dev. */
public class LoginViewModel extends ViewModel {

    private final AuthRepository repository = ServiceLocator.get(AuthRepository.class);

    private final MutableLiveData<List<DevAccountUiModel>> accounts =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<AuthSession> loggedIn = new MutableLiveData<>();

    public LiveData<List<DevAccountUiModel>> getAccounts() {
        return accounts;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<AuthSession> getLoggedIn() {
        return loggedIn;
    }

    public void loadAccounts() {
        loading.setValue(true);
        error.setValue(null);
        repository.loadAccounts(new ApiCallback<List<DevAccountDto>>() {
            @Override
            public void onSuccess(@Nullable List<DevAccountDto> data, @Nullable JsonElement meta) {
                accounts.setValue(DevAccountMapper.fromList(data));
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

    public void login(@NonNull DevAccountUiModel account) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        submitting.setValue(true);
        error.setValue(null);
        repository.login(DevAccountMapper.toDto(account), new ApiCallback<AuthSession>() {
            @Override
            public void onSuccess(@Nullable AuthSession data, @Nullable JsonElement meta) {
                if (data == null) {
                    error.setValue(ApiException.malformed(null));
                    return;
                }
                loggedIn.setValue(data);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }

            @Override
            public void onFinish() {
                submitting.setValue(false);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }

    public void consumeLoggedIn() {
        loggedIn.setValue(null);
    }
}
