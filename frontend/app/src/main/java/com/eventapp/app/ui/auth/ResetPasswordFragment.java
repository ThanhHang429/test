package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** FR-U02: đặt mật khẩu mới cho tài khoản demo đã yêu cầu khôi phục. */
public class ResetPasswordFragment extends Fragment {

    public static final String ARG_EMAIL = "arg_email";

    private String email = "";
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        email = args == null ? "" : args.getString(ARG_EMAIL, "");

        TextView subtitle = view.findViewById(R.id.tvResetSubtitle);
        subtitle.setText(email.isEmpty()
                ? getString(R.string.auth_reset_subtitle_fallback)
                : getString(R.string.auth_reset_subtitle, email));

        passwordLayout = view.findViewById(R.id.layoutResetPassword);
        confirmLayout = view.findViewById(R.id.layoutResetConfirm);
        passwordInput = view.findViewById(R.id.inputResetPassword);
        confirmInput = view.findViewById(R.id.inputResetConfirm);

        view.findViewById(R.id.btnResetPassword).setOnClickListener(v -> submitReset());
        view.findViewById(R.id.btnResetBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.btnResetBackToLogin).setOnClickListener(v -> backToLogin());

        AuthFormUtils.clearErrorOnType(passwordInput, passwordLayout);
        AuthFormUtils.clearErrorOnType(confirmInput, confirmLayout);
        confirmInput.setOnEditorActionListener((v, actionId, event) -> {
            submitReset();
            return true;
        });
    }

    private void submitReset() {
        String password = AuthFormUtils.textOf(passwordInput);
        String confirm = AuthFormUtils.textOf(confirmInput);
        AuthFormUtils.clearErrors(passwordLayout, confirmLayout);
        boolean valid = true;

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.auth_error_password_empty));
            valid = false;
        } else if (!AuthFormUtils.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.auth_error_password));
            valid = false;
        }
        if (!confirm.equals(password)) {
            confirmLayout.setError(getString(R.string.auth_error_password_mismatch));
            valid = false;
        }
        if (!valid) {
            return;
        }

        AuthFormUtils.hideKeyboard(getView());
        boolean updated = DemoAuthStore.get(requireContext()).resetPassword(email, password);
        if (!updated) {
            Toast.makeText(requireContext(), R.string.auth_error_email_not_found,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), R.string.auth_reset_success, Toast.LENGTH_LONG).show();
        backToLogin();
    }

    private void backToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        if (!navController.popBackStack(R.id.loginFragment, false)) {
            navController.navigate(R.id.loginFragment);
        }
    }
}
