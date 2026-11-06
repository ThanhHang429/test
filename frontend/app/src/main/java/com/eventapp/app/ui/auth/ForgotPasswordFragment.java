package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAuthForgotBinding;

/** Quên mật khẩu: một ô email + một CTA. */
public class ForgotPasswordFragment extends Fragment {

    private FragmentAuthForgotBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthForgotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.topBar.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.tvBackLogin.setOnClickListener(v -> auth().backToLogin());

        AuthUi.clearErrorOnType(binding.etEmail, binding.tvEmailError);
        binding.btnSend.setOnClickListener(v -> submit());
        binding.etEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    private void submit() {
        String email = AuthUi.text(binding.etEmail);
        if (email.isEmpty()) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email_empty));
            return;
        }
        if (!AuthUi.isValidEmail(email)) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email));
            return;
        }
        AuthUi.hideKeyboard(binding.getRoot());
        auth().openOtp(email, OtpFragment.MODE_RESET);
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
