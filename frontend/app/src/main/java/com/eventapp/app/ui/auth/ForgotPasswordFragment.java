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
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** FR-U02: gửi hướng dẫn đặt lại mật khẩu (demo: chuyển thẳng sang màn tạo mật khẩu mới). */
public class ForgotPasswordFragment extends Fragment {

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emailLayout = view.findViewById(R.id.layoutForgotEmail);
        emailInput = view.findViewById(R.id.inputForgotEmail);

        view.findViewById(R.id.btnSendReset).setOnClickListener(v -> submitEmail());
        view.findViewById(R.id.btnForgotBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.btnForgotBackToLogin).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        AuthFormUtils.clearErrorOnType(emailInput, emailLayout);
        emailInput.setOnEditorActionListener((v, actionId, event) -> {
            submitEmail();
            return true;
        });
    }

    private void submitEmail() {
        String email = AuthFormUtils.textOf(emailInput);
        AuthFormUtils.clearErrors(emailLayout);
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.auth_error_email_empty));
            return;
        }
        if (!AuthFormUtils.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.auth_error_email));
            return;
        }

        DemoAuthStore authStore = DemoAuthStore.get(requireContext());
        if (!authStore.accountExists(email)) {
            emailLayout.setError(getString(R.string.auth_error_email_not_found));
            return;
        }

        AuthFormUtils.hideKeyboard(getView());
        Toast.makeText(requireContext(), R.string.auth_reset_sent, Toast.LENGTH_SHORT).show();
        Bundle args = new Bundle();
        args.putString(ResetPasswordFragment.ARG_EMAIL, email);
        Navigation.findNavController(requireView())
                .navigate(R.id.reset_passwordFragment, args);
    }
}
