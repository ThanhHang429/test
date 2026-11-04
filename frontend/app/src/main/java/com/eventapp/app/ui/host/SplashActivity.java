package com.eventapp.app.ui.host;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.ui.auth.AuthActivity;

/**
 * FR-U01, FR-U02: màn hình khởi động.
 *
 * <p>Phiên đăng nhập chỉ nằm trong bộ nhớ nên mở lại app luôn phải đăng nhập. Nếu vì lý do nào
 * đó vẫn còn phiên (xoay màn hình, quay lại từ host), mở đúng host theo vai trò JWT.</p>
 *
 * <p>Chưa có phiên → mở luồng xác thực (đăng nhập / đăng ký / quên mật khẩu / OTP).</p>
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View root = findViewById(android.R.id.content);
        root.post(this::openNextScreen);
    }

    private void openNextScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        AuthSession session = SessionManager.get().getSession();
        if (session == null) {
            startActivity(new Intent(this, AuthActivity.class));
        } else {
            SessionRouter.openHost(this, session);
        }
        finish();
    }
}
