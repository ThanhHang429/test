package com.eventapp.app.ui.auth;

import android.graphics.Color;
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

/** FR-U01, FR-U02: host chứa luồng đăng nhập, đăng ký và quên mật khẩu. */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Trước Android 15 statusBarColor vẫn được vẽ đè lên nội dung edge-to-edge.
        // Đặt trong suốt để nền sáng của màn Auth lộ ra dưới icon status bar tối.
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_auth);
        applyWindowInsets();
    }

    /**
     * Giữ nội dung dưới status bar và tránh bàn phím che form.
     * Chừa thêm đáy cho thanh gesture khi bàn phím đóng.
     */
    private void applyWindowInsets() {
        View root = findViewById(R.id.authRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (@NonNull View view,
                                                         @NonNull WindowInsetsCompat insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(0, bars.top, 0, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }
}
