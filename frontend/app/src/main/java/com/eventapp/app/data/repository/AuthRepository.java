package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.data.remote.api.AuthApi;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.data.remote.dto.auth.DevTokenDto;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đăng nhập / đăng xuất.
 *
 * <p>Không có mật khẩu: backend chỉ có {@code /dev/accounts} + {@code /dev/token}. Đổi lại, app
 * đăng nhập được bằng mọi tài khoản seed (kể cả organizer bị treo, admin, user thường) để kiểm
 * thử đủ nhánh quyền.</p>
 */
public class AuthRepository {

    private final AuthApi api;

    public AuthRepository(@NonNull AuthApi api) {
        this.api = api;
    }

    /** Tài khoản để hiển thị trong màn đăng nhập. */
    public void loadAccounts(@NonNull ApiCallback<List<DevAccountDto>> callback) {
        ApiCallback.enqueue(api.accounts(), callback);
    }

    /** Đổi userId thành phiên đăng nhập thật (có JWT). */
    public void login(long userId, @NonNull ApiCallback<AuthSession> callback) {
        login(userId, null, callback);
    }

    /**
     * Đổi một tài khoản seed thành phiên thật.
     *
     * <p>Token endpoint không trả {@code fullName}, nên lấy tên từ danh sách tài khoản đã chọn để
     * account sheet và header hiển thị đúng.</p>
     */
    public void login(@NonNull DevAccountDto account, @NonNull ApiCallback<AuthSession> callback) {
        login(account.userId, account.fullName, callback);
    }

    private void login(long userId, @Nullable final String fullName,
                       @NonNull ApiCallback<AuthSession> callback) {
        ApiCallback.enqueue(api.token(userId), new ApiCallback<DevTokenDto>() {
            @Override
            public void onSuccess(@Nullable DevTokenDto data, @Nullable JsonElement meta) {
                if (data == null || data.token == null || data.token.isEmpty()) {
                    callback.onError(ApiException.malformed(null));
                    return;
                }
                AuthSession session = new AuthSession(
                        data.userId,
                        data.email,
                        fullName,
                        data.roles == null ? Collections.emptyList() : new ArrayList<>(data.roles),
                        data.token);
                SessionManager.get().start(session);
                callback.onSuccess(session, meta);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    /** Đăng nhập nhanh theo vai trò: chọn tài khoản seed đầu tiên có role tương ứng. */
    public void loginAsRole(@NonNull String role, @NonNull ApiCallback<AuthSession> callback) {
        loadAccounts(new ApiCallback<List<DevAccountDto>>() {
            @Override
            public void onSuccess(@Nullable List<DevAccountDto> data, @Nullable JsonElement meta) {
                DevAccountDto picked = pickByRole(data, role);
                if (picked == null) {
                    callback.onError(ApiException.of(404, "ACCOUNT_NOT_FOUND",
                            "Không có tài khoản " + role + " trong dữ liệu seed."));
                    return;
                }
                login(picked, callback);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        });
    }

    public void logout() {
        SessionManager.get().clear();
    }

    @Nullable
    private static DevAccountDto pickByRole(@Nullable List<DevAccountDto> accounts, @NonNull String role) {
        if (accounts == null) {
            return null;
        }
        // Ưu tiên tài khoản chỉ có đúng vai trò đó (ví dụ user1 chỉ có USER) để nút đăng nhập
        // nhanh không mở nhầm organizer kiêm user.
        for (DevAccountDto account : accounts) {
            if (account.roles != null && account.roles.size() == 1
                    && role.equalsIgnoreCase(account.roles.get(0))) {
                return account;
            }
        }
        for (DevAccountDto account : accounts) {
            if (account.roles != null) {
                for (String value : account.roles) {
                    if (role.equalsIgnoreCase(value)) {
                        return account;
                    }
                }
            }
        }
        return null;
    }
}
