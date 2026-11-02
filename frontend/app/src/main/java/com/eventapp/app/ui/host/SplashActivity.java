package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;

/**
 * FR-U01, FR-U02: màn hình khởi động.
 *
 * <p>Phiên đăng nhập chỉ nằm trong bộ nhớ nên mở lại app luôn phải đăng nhập. Nếu vì lý do nào
 * đó vẫn còn phiên (xoay màn hình, quay lại từ host), mở đúng host theo vai trò JWT.</p>
 *
 * <p>Không còn delay cứng: chuyển màn ngay sau khi splash vẽ xong frame đầu. Trước đây chờ cố
 * định 600 ms khiến mọi lần mở app chậm thêm đúng ngần ấy mà không đổi lại được gì — trên
 * Android 12+ hệ thống đã có splash riêng trong lúc tiến trình khởi động.</p>
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View root = findViewById(android.R.id.content);
        root.post(this::openNextScreen);
    }

    private void openNextScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        AuthSession session = SessionManager.get().getSession();
        if (session == null) {
            SessionRouter.openAuth(this);
        } else {
            SessionRouter.openHost(this, session);
        }
        finish();
    }
}
