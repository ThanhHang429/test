package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAuthRegisterBinding;

/** Đăng ký 2 bước: thông tin cá nhân → bảo mật. Không show một form dài. */
public class RegisterFragment extends Fragment {

    private FragmentAuthRegisterBinding binding;
    private int step = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.topBar.tvTopChip.setVisibility(View.VISIBLE);
        binding.topBar.btnBack.setOnClickListener(v -> handleBack());

        AuthUi.bindPasswordToggle(binding.etPassword, binding.btnTogglePassword);
        AuthUi.bindPasswordToggle(binding.etConfirm, binding.btnToggleConfirm);
        AuthUi.clearErrorOnType(binding.etName, binding.tvNameError);
        AuthUi.clearErrorOnType(binding.etEmail, binding.tvEmailError);
        AuthUi.clearErrorOnType(binding.etPhone, binding.tvPhoneError);
        AuthUi.clearErrorOnType(binding.etPassword, binding.tvPasswordError);
        AuthUi.clearErrorOnType(binding.etConfirm, binding.tvConfirmError);

        binding.cbTerms.setOnCheckedChangeListener((button, checked) ->
                binding.tvTermsError.setVisibility(View.GONE));

        CharSequence terms = getString(R.string.auth_terms);
        SpannableString styledTerms = new SpannableString(terms);
        int highlightStart = terms.toString().indexOf("Điều khoản");
        if (highlightStart >= 0) {
            styledTerms.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.auth_mint)),
                    highlightStart,
                    highlightStart + "Điều khoản".length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.cbTerms.setText(styledTerms);

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderStrength(s == null ? "" : s.toString());
            }
        });

        binding.btnNext.setOnClickListener(v -> goToStep2());
        binding.btnCreate.setOnClickListener(v -> submit());
        binding.tvLogin.setOnClickListener(v -> auth().backToLogin());

        binding.etPhone.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                goToStep2();
                return true;
            }
            return false;
        });
        binding.etConfirm.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });

        updateChrome();
        renderStrength("");
    }

    private void goToStep2() {
        String name = AuthUi.text(binding.etName);
        String email = AuthUi.text(binding.etEmail);
        String phone = AuthUi.text(binding.etPhone);

        boolean valid = true;
        if (name.isEmpty()) {
            AuthUi.markError(binding.etName, binding.tvNameError, getString(R.string.auth_err_name));
            valid = false;
        }
        if (email.isEmpty()) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email_empty));
            valid = false;
        } else if (!AuthUi.isValidEmail(email)) {
            AuthUi.markError(binding.etEmail, binding.tvEmailError, getString(R.string.auth_err_email));
            valid = false;
        }
        if (!AuthUi.isValidPhone(phone)) {
            AuthUi.markError(binding.etPhone, binding.tvPhoneError, getString(R.string.auth_err_phone));
            valid = false;
        }
        if (!valid) {
            return;
        }

        setStep(2, true);
        binding.etPassword.postDelayed(() -> {
            if (binding != null) {
                binding.etPassword.requestFocus();
            }
        }, 220);
    }

    private void submit() {
        String password = binding.etPassword.getText() == null
                ? "" : binding.etPassword.getText().toString();
        String confirm = binding.etConfirm.getText() == null
                ? "" : binding.etConfirm.getText().toString();

        boolean valid = true;
        if (password.length() < 6) {
            AuthUi.markError(binding.etPassword, binding.tvPasswordError, getString(R.string.auth_err_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            AuthUi.markError(binding.etConfirm, binding.tvConfirmError, getString(R.string.auth_err_password_mismatch));
            valid = false;
        }
        if (!binding.cbTerms.isChecked()) {
            binding.tvTermsError.setText(R.string.auth_err_terms);
            binding.tvTermsError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) {
            return;
        }

        AuthUi.hideKeyboard(binding.getRoot());
        auth().openOtp(AuthUi.text(binding.etEmail), OtpFragment.MODE_REGISTER);
    }

    private void setStep(int target, boolean animate) {
        if (step == target) {
            return;
        }
        View hide = step == 1 ? binding.groupStep1 : binding.groupStep2;
        View show = target == 1 ? binding.groupStep1 : binding.groupStep2;
        step = target;
        updateChrome();

        if (!animate) {
            hide.setVisibility(View.GONE);
            show.setVisibility(View.VISIBLE);
            return;
        }

        int direction = target > 1 ? 1 : -1;
        hide.animate()
                .alpha(0f)
                .translationX(-direction * 32f)
                .setDuration(140)
                .withEndAction(() -> {
                    hide.setVisibility(View.GONE);
                    hide.setAlpha(1f);
                    hide.setTranslationX(0f);

                    show.setVisibility(View.VISIBLE);
                    show.setAlpha(0f);
                    show.setTranslationX(direction * 32f);
                    show.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .start();
                })
                .start();
    }

    private void updateChrome() {
        binding.stepBar1.setBackgroundResource(step == 1
                ? R.drawable.bg_auth_step_active : R.drawable.bg_auth_step_inactive);
        binding.stepBar2.setBackgroundResource(step == 2
                ? R.drawable.bg_auth_step_active : R.drawable.bg_auth_step_inactive);
        binding.tvStepSubtitle.setText(step == 1
                ? R.string.auth_step_personal : R.string.auth_step_security);
        binding.topBar.tvTopChip.setText(getString(R.string.auth_step_counter, step));
    }

    private void renderStrength(String password) {
        AuthUi.renderStrength(
                new View[]{binding.strength1, binding.strength2, binding.strength3},
                AuthUi.passwordScore(password),
                requireContext());
    }

    private void handleBack() {
        if (step == 2) {
            setStep(1, true);
        } else {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
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
