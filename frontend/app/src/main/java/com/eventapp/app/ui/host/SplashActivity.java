package com.eventapp.app.ui.host;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;

/**
 * FR-U01, FR-U02: màn hình khởi động.
 *
 * <p>Phiên đăng nhập chỉ nằm trong bộ nhớ nên mở lại app luôn phải đăng nhập. Nếu vì lý do nào
 * đó vẫn còn phiên (xoay màn hình, quay lại từ host), mở đúng host theo vai trò JWT.</p>
 *
 * <p>Giao diện đang được dựng lại: khi chưa có phiên, app vào thẳng khu người dùng để giữ luồng
 * chạy được. Màn đăng nhập mới sẽ được thêm lại sau.</p>
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
            startActivity(new Intent(this, UserHostActivity.class));
        } else {
            SessionRouter.openHost(this, session);
        }
        finish();
    }
}
