package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.eventapp.app.ui.demo.DemoRouter;

/**
 * FR-U01, FR-U02: màn hình khởi động.
 *
 * <p>Trong giai đoạn UI-only, phiên đăng nhập được đọc từ {@link DemoAuthStore}. Có phiên thì mở
 * host theo vai trò; chưa có thì mở luồng đăng nhập demo.</p>
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 600L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(this::openNextScreen, SPLASH_DELAY_MS);
    }

    private void openNextScreen() {
        DemoAccount account = DemoAuthStore.get(this).currentAccount();
        if (account == null) {
            DemoRouter.openAuth(this);
        } else {
            DemoRouter.openHost(this, account.getRole());
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
