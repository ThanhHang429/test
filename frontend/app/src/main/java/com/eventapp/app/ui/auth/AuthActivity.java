package com.eventapp.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityAuthBinding;
import com.eventapp.app.ui.host.AdminHostActivity;
import com.eventapp.app.ui.host.OrganizerHostActivity;
import com.eventapp.app.ui.host.UserHostActivity;

/**
 * Vỏ chứa 4 màn xác thực: đăng nhập, đăng ký 2 bước, quên mật khẩu, OTP.
 *
 * <p>Chỉ điều hướng và giữ trạng thái UI. Việc gọi API đăng nhập thật sẽ được nối sau khi backend
 * có endpoint auth; hiện tại xác thực OTP thành công sẽ mở khu người dùng để chạy tiếp luồng UI.</p>
 */
public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.authContainer, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });

        if (savedInstanceState == null) {
            showLogin();
        }
    }

    public void showLogin() {
        replace(new LoginFragment(), false);
    }

    public void openRegister() {
        replace(new RegisterFragment(), true);
    }

    public void openForgot() {
        replace(new ForgotPasswordFragment(), true);
    }

    public void openOtp(String email, String mode) {
        openOtp(email, mode, "USER");
    }

    public void openOtp(String email, String mode, String role) {
        replace(OtpFragment.newInstance(email, mode, role), true);
    }

    /** Xoá toàn bộ stack phụ và quay về màn đăng nhập gốc. */
    public void backToLogin() {
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        Fragment current = manager.findFragmentById(R.id.authContainer);
        if (!(current instanceof LoginFragment)) {
            showLogin();
        }
    }

    /** Mở đúng khu chức năng theo vai trò suy ra từ tài khoản đăng nhập. */
    public void openHost(String role) {
        Class<?> target;
        if ("ORGANIZER".equals(role)) {
            target = OrganizerHostActivity.class;
        } else if ("ADMIN".equals(role)) {
            target = AdminHostActivity.class;
        } else {
            target = UserHostActivity.class;
        }
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Snackbar nền kính, dùng cho phản hồi ngắn sau khi đổi màn. */
    public void showMessage(int messageRes) {
        if (binding != null) {
            AuthUi.snack(binding.getRoot(), messageRes);
        }
    }

    private void replace(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.authContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
