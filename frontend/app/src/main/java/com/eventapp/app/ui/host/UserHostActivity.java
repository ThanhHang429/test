package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.eventapp.app.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** FR-U03: host chứa khu vực người dùng và bottom navigation. */
public class UserHostActivity extends AppCompatActivity {

    /** Khoảng hở cố định giữa thanh điều hướng và mép dưới vùng an toàn. */
    private static final int NAV_BAR_BOTTOM_GAP_DP = 4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vẽ tràn viền để thanh điều hướng nổi trên nội dung; safe area xử lý thủ công bên dưới.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user_host);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost != null) {
            NavigationUI.setupWithNavController(
                    (BottomNavigationView) findViewById(R.id.bottom_navigation),
                    navHost.getNavController());
        }

        applyWindowInsets();
    }

    /**
     * Đẩy nội dung xuống dưới status bar và nâng thanh điều hướng lên trên thanh gesture.
     * Phần nền phía sau thanh điều hướng không được tô màu để nội dung cuộn lộ ra.
     */
    private void applyWindowInsets() {
        View root = findViewById(R.id.host_root);
        View navBar = findViewById(R.id.nav_bar_container);
        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) navBar.getLayoutParams();
        int baseGap = Math.round(NAV_BAR_BOTTOM_GAP_DP
                * getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(root, (@NonNull View view,
                                                         @NonNull WindowInsetsCompat insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, bars.top, 0, 0);

            int bottomMargin = bars.bottom + baseGap;
            if (params.bottomMargin != bottomMargin) {
                params.bottomMargin = bottomMargin;
                view.post(() -> navBar.setLayoutParams(params));
            }
            // Safe area đã xử lý thủ công ở đây; chặn inset xuống con để BottomNavigationView
            // không cộng thêm padding đáy lần nữa (trước đây làm thanh cao thêm ~24dp).
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                    .build();
        });
    }
}
