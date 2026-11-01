package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.Role;
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.eventapp.app.ui.demo.DemoRouter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** FR-U02: đăng nhập bằng email/mật khẩu hoặc thẻ tài khoản demo theo vai trò. */
public class LoginFragment extends Fragment {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private DemoAuthStore authStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authStore = DemoAuthStore.get(requireContext());

        emailLayout = view.findViewById(R.id.layoutLoginEmail);
        passwordLayout = view.findViewById(R.id.layoutLoginPassword);
        emailInput = view.findViewById(R.id.inputLoginEmail);
        passwordInput = view.findViewById(R.id.inputLoginPassword);

        view.findViewById(R.id.btnLogin).setOnClickListener(v -> submitLogin());
        view.findViewById(R.id.btnForgotPassword).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.forgot_passwordFragment));
        view.findViewById(R.id.btnRegister).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.registerFragment));

        view.findViewById(R.id.rowDemoUser).setOnClickListener(v -> loginAs(Role.USER));
        view.findViewById(R.id.rowDemoOrganizer).setOnClickListener(v -> loginAs(Role.ORGANIZER));
        view.findViewById(R.id.rowDemoAdmin).setOnClickListener(v -> loginAs(Role.ADMIN));

        passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            submitLogin();
            return true;
        });

        AuthFormUtils.clearErrorOnType(emailInput, emailLayout);
        AuthFormUtils.clearErrorOnType(passwordInput, passwordLayout);
    }

    private void submitLogin() {
        String email = AuthFormUtils.textOf(emailInput);
        String password = AuthFormUtils.textOf(passwordInput);
        AuthFormUtils.clearErrors(emailLayout, passwordLayout);
        boolean valid = true;

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.auth_error_email_empty));
            valid = false;
        } else if (!AuthFormUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.auth_error_email));
            valid = false;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.auth_error_password_empty));
            valid = false;
        } else if (!AuthFormUtils.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.auth_error_password));
            valid = false;
        }
        if (!valid) {
            return;
        }

        AuthFormUtils.hideKeyboard(getView());
        DemoAuthStore.LoginResult result = authStore.login(email, password);
        switch (result.status) {
            case SUCCESS:
                openHost(result.account);
                break;
            case NOT_FOUND:
                emailLayout.setError(getString(R.string.auth_error_email_not_found));
                break;
            case WRONG_PASSWORD:
                passwordLayout.setError(getString(R.string.auth_error_login));
                break;
            case UNVERIFIED:
                Toast.makeText(requireContext(), R.string.auth_error_unverified,
                        Toast.LENGTH_SHORT).show();
                navigateToVerify(email);
                break;
        }
    }

    private void loginAs(Role role) {
        DemoAccount account = authStore.loginAs(role);
        if (account == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        openHost(account);
    }

    private void openHost(DemoAccount account) {
        if (account == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        AuthFormUtils.hideKeyboard(getView());
        DemoRouter.openHost(requireContext(), account.getRole());
        requireActivity().finish();
    }

    private void navigateToVerify(String email) {
        Bundle args = new Bundle();
        args.putString(VerifyEmailFragment.ARG_EMAIL, email);
        Navigation.findNavController(requireView())
                .navigate(R.id.verify_emailFragment, args);
    }

}
