package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.Role;
import com.eventapp.app.data.repository.AuthRepository;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.AuthSession;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonElement;

/**
 * Lối tắt debug để vào nhanh một khu vực kiểm thử.
 *
 * <p>Không còn ghi session giả: mỗi lựa chọn gọi {@code POST /dev/token} để lấy JWT thật rồi mới
 * mở host. Đây là lối tắt phụ; luồng chính vẫn là {@link com.eventapp.app.ui.auth.LoginFragment}.</p>
 */
public class RolePickerActivity extends AppCompatActivity {

    private AuthRepository repository;
    private View root;
    private boolean busy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_role_picker);
        repository = ServiceLocator.get(AuthRepository.class);
        root = findViewById(R.id.rolePickerRoot);

        findViewById(R.id.cardModeUser).setOnClickListener(v -> open(Role.USER));
        findViewById(R.id.cardModeOrganizer).setOnClickListener(v -> open(Role.ORGANIZER));
        findViewById(R.id.cardModeAdmin).setOnClickListener(v -> open(Role.ADMIN));

        applyWindowInsets();
    }

    private void open(Role role) {
        if (busy) {
            return;
        }
        busy = true;
        repository.loginAsRole(role.name(), new ApiCallback<AuthSession>() {
            @Override
            public void onSuccess(@Nullable AuthSession data, @Nullable JsonElement meta) {
                if (data == null) {
                    showError("Không nhận được phiên đăng nhập.");
                    return;
                }
                SessionRouter.openHost(RolePickerActivity.this, data);
                finish();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                busy = false;
                showError(error.getUserMessage());
            }

            @Override
            public void onFinish() {
                busy = false;
            }
        });
    }

    private void showError(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (@NonNull View view,
                                                         @NonNull WindowInsetsCompat insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
    }
}
