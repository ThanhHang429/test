package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** FR-U01: đăng ký tài khoản mới, mặc định tạo tài khoản Người dùng chưa xác thực email. */
public class RegisterFragment extends Fragment {

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private CheckBox termsCheck;
    private View termsError;
    private DemoAuthStore authStore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authStore = DemoAuthStore.get(requireContext());

        nameLayout = view.findViewById(R.id.layoutRegisterName);
        emailLayout = view.findViewById(R.id.layoutRegisterEmail);
        phoneLayout = view.findViewById(R.id.layoutRegisterPhone);
        passwordLayout = view.findViewById(R.id.layoutRegisterPassword);
        confirmLayout = view.findViewById(R.id.layoutRegisterConfirm);
        nameInput = view.findViewById(R.id.inputRegisterName);
        emailInput = view.findViewById(R.id.inputRegisterEmail);
        phoneInput = view.findViewById(R.id.inputRegisterPhone);
        passwordInput = view.findViewById(R.id.inputRegisterPassword);
        confirmInput = view.findViewById(R.id.inputRegisterConfirm);
        termsCheck = view.findViewById(R.id.checkRegisterTerms);
        termsError = view.findViewById(R.id.tvRegisterTermsError);

        view.findViewById(R.id.btnRegisterBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.btnLoginLink).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.btnRegisterSubmit).setOnClickListener(v -> submitRegister());

        termsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                termsError.setVisibility(View.GONE);
            }
        });

        AuthFormUtils.clearErrorOnType(nameInput, nameLayout);
        AuthFormUtils.clearErrorOnType(emailInput, emailLayout);
        AuthFormUtils.clearErrorOnType(phoneInput, phoneLayout);
        AuthFormUtils.clearErrorOnType(passwordInput, passwordLayout);
        AuthFormUtils.clearErrorOnType(confirmInput, confirmLayout);

        confirmInput.setOnEditorActionListener((v, actionId, event) -> {
            submitRegister();
            return true;
        });
    }

    private void submitRegister() {
        String name = AuthFormUtils.textOf(nameInput);
        String email = AuthFormUtils.textOf(emailInput);
        String phone = AuthFormUtils.textOf(phoneInput);
        String password = AuthFormUtils.textOf(passwordInput);
        String confirm = AuthFormUtils.textOf(confirmInput);

        AuthFormUtils.clearErrors(nameLayout, emailLayout, phoneLayout, passwordLayout, confirmLayout);
        termsError.setVisibility(View.GONE);
        boolean valid = true;

        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.auth_error_name));
            valid = false;
        }
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.auth_error_email_empty));
            valid = false;
        } else if (!AuthFormUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.auth_error_email));
            valid = false;
        }
        if (!phone.isEmpty() && !AuthFormUtils.isValidPhone(phone)) {
            phoneLayout.setError(getString(R.string.auth_error_phone));
            valid = false;
        }
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
        if (!termsCheck.isChecked()) {
            termsError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) {
            return;
        }

        AuthFormUtils.hideKeyboard(getView());
        DemoAccount account = authStore.register(name, email, phone, password);
        if (account == null) {
            emailLayout.setError(getString(R.string.auth_error_email_exists));
            return;
        }

        Toast.makeText(requireContext(), R.string.auth_register_success, Toast.LENGTH_LONG).show();
        Bundle args = new Bundle();
        args.putString(VerifyEmailFragment.ARG_EMAIL, account.getEmail());
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, false)
                .build();
        Navigation.findNavController(requireView())
                .navigate(R.id.verify_emailFragment, args, options);
    }
}
