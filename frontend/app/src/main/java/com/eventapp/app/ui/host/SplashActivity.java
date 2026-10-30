package com.eventapp.app.ui.host;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;

/**
 * FR-U01, FR-U02: màn hình khởi động.
 * Tầng session (core/session) còn là stub nên tạm thời vào thẳng khu vực người dùng.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 600L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::openUserArea, SPLASH_DELAY_MS);
    }

    private void openUserArea() {
        startActivity(new Intent(this, UserHostActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
