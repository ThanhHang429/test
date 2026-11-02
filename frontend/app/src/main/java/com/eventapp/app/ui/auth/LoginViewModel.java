package com.eventapp.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.data.repository.AuthRepository;
import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.List;

/**
 * Trạng thái đăng nhập dev.
 *
 * <p>Không có email/mật khẩu: danh sách tài khoản lấy từ backend dev, sau đó đổi userId thành JWT
 * thật. Nhờ vậy toàn bộ API còn lại chạy đúng logic phân quyền.</p>
 */
public class LoginViewModel extends ViewModel {

    private final AuthRepository repository = ServiceLocator.get(AuthRepository.class);

    private final MutableLiveData<List<DevAccountDto>> accounts =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<AuthSession> loggedIn = new MutableLiveData<>();

    private boolean loaded;

    @NonNull
    public LiveData<List<DevAccountDto>> getAccounts() {
        return accounts;
    }

    @NonNull
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    @NonNull
    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    @NonNull
    public LiveData<ApiException> getError() {
        return error;
    }

    @NonNull
    public LiveData<AuthSession> getLoggedIn() {
        return loggedIn;
    }

    public void loadAccounts(boolean force) {
        if (loaded && !force) {
            return;
        }
        loading.setValue(true);
        error.setValue(null);
        repository.loadAccounts(new ApiCallback<List<DevAccountDto>>() {
            @Override
            public void onSuccess(@Nullable List<DevAccountDto> data, @Nullable JsonElement meta) {
                loaded = true;
                accounts.setValue(data == null ? Collections.emptyList() : data);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                loaded = false;
                error.setValue(exception);
            }

            @Override
            public void onFinish() {
                loading.setValue(false);
            }
        });
    }

    public void login(@NonNull DevAccountDto account) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        submitting.setValue(true);
        error.setValue(null);
        repository.login(account, sessionCallback());
    }

    public void loginAsRole(@NonNull String role) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        submitting.setValue(true);
        error.setValue(null);
        repository.loginAsRole(role, sessionCallback());
    }

    public void consumeError() {
        error.setValue(null);
    }

    @NonNull
    private ApiCallback<AuthSession> sessionCallback() {
        return new ApiCallback<AuthSession>() {
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
        };
    }
}
