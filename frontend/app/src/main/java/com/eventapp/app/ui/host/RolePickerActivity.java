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
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.eventapp.app.ui.demo.DemoRouter;

/**
 * Lối tắt debug để vào nhanh một khu vực kiểm thử.
 *
 * <p>Màn này ghi session demo tương ứng trước khi mở host, nhờ vậy trạng thái đăng nhập luôn nhất
 * quán với luồng Login. Đây không phải luồng đăng nhập chính.</p>
 */
public class RolePickerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_role_picker);

        findViewById(R.id.cardModeUser).setOnClickListener(v -> open(Role.USER));
        findViewById(R.id.cardModeOrganizer).setOnClickListener(v -> open(Role.ORGANIZER));
        findViewById(R.id.cardModeAdmin).setOnClickListener(v -> open(Role.ADMIN));

        applyWindowInsets();
    }

    private void open(Role role) {
        DemoAccount account = DemoAuthStore.get(this).loginAs(role);
        if (account == null) {
            return;
        }
        DemoRouter.openHost(this, account.getRole());
        finish();
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.rolePickerRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (@NonNull View view,
                                                         @NonNull WindowInsetsCompat insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
    }
}
