package com.eventapp.app.ui.auth;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAuthLoginBinding;

/** Đăng nhập: social row + email/mật khẩu + CTA gradient. */
public class LoginFragment extends Fragment {

    private static final String PREFS = "auth_ui";
    private static final String KEY_EMAIL = "remembered_email";

    /**
     * Tài khoản dùng thử cho giai đoạn test giao diện. Hai vai trò dùng chung màn đăng nhập;
     * vai trò được suy ra từ email để mở đúng khu sau khi xác thực OTP.
     */
    private static final String USER_EMAIL = "demo@nexus.vn";
    private static final String ORGANIZER_EMAIL = "organizer@nexus.vn";
    private static final String ADMIN_EMAIL = "admin@nexus.vn";
    private static final String DEMO_PASSWORD = "123456";

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ORGANIZER = "ORGANIZER";
    private static final String ROLE_ADMIN = "ADMIN";

    private FragmentAuthLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.topBar.btnBack.setVisibility(View.GONE);
        binding.topBar.tvTopChip.setVisibility(View.GONE);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, 0);
        String remembered = prefs.getString(KEY_EMAIL, "");
        if (remembered != null && !remembered.isEmpty()) {
            binding.etEmail.setText(remembered);
            binding.cbRemember.setChecked(true);
        }

        AuthUi.bindPasswordToggle(binding.etPassword, binding.btnTogglePassword);
        AuthUi.clearErrorOnType(binding.etEmail, binding.tvEmailError);
        AuthUi.clearErrorOnType(binding.etPassword, binding.tvPasswordError);

        binding.btnLogin.setOnClickListener(v -> submit());
        binding.tvForgot.setOnClickListener(v -> auth().openForgot());
        binding.tvRegister.setOnClickListener(v -> auth().openRegister());
        binding.btnGoogle.setOnClickListener(v -> comingSoon());
        binding.btnFacebook.setOnClickListener(v -> comingSoon());

        if (BuildConfig.DEBUG) {
            binding.demoCard.setVisibility(View.VISIBLE);
            binding.btnDemoUser.setOnClickListener(v -> fillDemo(USER_EMAIL));
            binding.btnDemoOrganizer.setOnClickListener(v -> fillDemo(ORGANIZER_EMAIL));
            binding.btnDemoAdmin.setOnClickListener(v -> fillDemo(ADMIN_EMAIL));
        }

        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    private void submit() {
        String email = AuthUi.text(binding.etEmail);
        String password = binding.etPassword.getText() == null
                ? "" : binding.etPassword.getText().toString();

        boolean valid = true;
        if (email.isEmpty()) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email_empty));
            valid = false;
        } else if (!AuthUi.isValidEmail(email)) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email));
            valid = false;
        }
        if (password.isEmpty()) {
            AuthUi.markError(binding.etPassword, binding.tvPasswordError, getString(R.string.auth_err_password_empty));
            valid = false;
        } else if (password.length() < 6) {
            AuthUi.markError(binding.etPassword, binding.tvPasswordError, getString(R.string.auth_err_password));
            valid = false;
        }
        if (!valid) {
            return;
        }

        String role;
        if (USER_EMAIL.equalsIgnoreCase(email) && DEMO_PASSWORD.equals(password)) {
            role = ROLE_USER;
        } else if (ORGANIZER_EMAIL.equalsIgnoreCase(email) && DEMO_PASSWORD.equals(password)) {
            role = ROLE_ORGANIZER;
        } else if (ADMIN_EMAIL.equalsIgnoreCase(email) && DEMO_PASSWORD.equals(password)) {
            role = ROLE_ADMIN;
        } else {
            AuthUi.markError(binding.etPassword, binding.tvPasswordError,
                    getString(R.string.auth_err_credentials));
            AuthUi.shake(binding.btnLogin);
            return;
        }

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, 0);
        if (binding.cbRemember.isChecked()) {
            prefs.edit().putString(KEY_EMAIL, email).apply();
        } else {
            prefs.edit().remove(KEY_EMAIL).apply();
        }

        AuthUi.hideKeyboard(binding.getRoot());
        auth().openOtp(email, OtpFragment.MODE_LOGIN, role);
    }

    private void fillDemo(String email) {
        binding.etEmail.setText(email);
        binding.etPassword.setText(DEMO_PASSWORD);
        binding.cbRemember.setChecked(true);
        binding.tvEmailError.setVisibility(View.GONE);
        binding.tvPasswordError.setVisibility(View.GONE);
    }

    private void comingSoon() {
        AuthUi.snack(binding.getRoot(), R.string.auth_soon);
    }

    private AuthActivity auth() {
        return (AuthActivity) requireActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
