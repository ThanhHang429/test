package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAuthOtpBinding;

/**
 * OTP / 2FA. Dùng chung cho 3 luồng:
 * đăng nhập 2 lớp, xác thực email sau đăng ký, đặt lại mật khẩu.
 */
public class OtpFragment extends Fragment {

    public static final String MODE_LOGIN = "login";
    public static final String MODE_REGISTER = "register";
    public static final String MODE_RESET = "reset";

    private static final String ARG_EMAIL = "email";
    private static final String ARG_MODE = "mode";
    private static final String ARG_ROLE = "role";
    private static final String DEMO_CODE = "123456";

    private FragmentAuthOtpBinding binding;
    private EditText[] cells;
    private String email;
    private String mode;
    private String role;
    private CountDownTimer timer;
    private boolean verifying;

    public static OtpFragment newInstance(String email, String mode) {
        return newInstance(email, mode, "USER");
    }

    public static OtpFragment newInstance(String email, String mode, String role) {
        OtpFragment fragment = new OtpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL, email);
        args.putString(ARG_MODE, mode);
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        email = args == null ? "" : args.getString(ARG_EMAIL, "");
        mode = args == null ? MODE_LOGIN : args.getString(ARG_MODE, MODE_LOGIN);
        role = args == null ? "USER" : args.getString(ARG_ROLE, "USER");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.topBar.tvTopChip.setVisibility(View.VISIBLE);
        binding.topBar.btnBack.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.tvEmail.setText(email);
        cells = new EditText[]{
                binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6
        };

        configureMode();
        setupCells();

        binding.tvDemoHint.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        binding.btnVerify.setOnClickListener(v -> verify());
        binding.tvBackLogin.setOnClickListener(v -> auth().backToLogin());
        binding.tvResend.setOnClickListener(v -> resend());

        startCountdown();
        binding.otp1.requestFocus();
    }

    private void configureMode() {
        if (MODE_RESET.equals(mode)) {
            binding.tvTitle.setText(R.string.auth_otp_title_reset);
            binding.btnVerify.setText(R.string.auth_reset_action);
            binding.groupReset.setVisibility(View.VISIBLE);
            binding.topBar.tvTopChip.setText(getString(R.string.auth_step_counter, 2));

            AuthUi.bindPasswordToggle(binding.etNewPassword, binding.btnToggleNew);
            AuthUi.bindPasswordToggle(binding.etConfirmNew, binding.btnToggleConfirmNew);
            AuthUi.clearErrorOnType(binding.etNewPassword, binding.tvNewPasswordError);
            AuthUi.clearErrorOnType(binding.etConfirmNew, binding.tvConfirmError);
            binding.etNewPassword.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    AuthUi.renderStrength(
                            new View[]{binding.strength1, binding.strength2, binding.strength3},
                            AuthUi.passwordScore(s == null ? "" : s.toString()),
                            requireContext());
                }
            });
        } else if (MODE_REGISTER.equals(mode)) {
            binding.tvTitle.setText(R.string.auth_otp_title_verify);
            binding.btnVerify.setText(R.string.auth_verify);
            binding.topBar.tvTopChip.setText(getString(R.string.auth_step_counter, 2));
        } else {
            binding.tvTitle.setText(R.string.auth_otp_title);
            binding.btnVerify.setText(R.string.auth_verify);
            binding.topBar.tvTopChip.setText(R.string.auth_2fa);
        }
    }

    private void setupCells() {
        for (int index = 0; index < cells.length; index++) {
            final int position = index;
            EditText cell = cells[index];
            cell.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    cell.setActivated(s != null && s.length() > 0);
                    clearOtpError();
                    if (s != null && s.length() == 1 && position < cells.length - 1) {
                        cells[position + 1].requestFocus();
                    }
                }
            });

            cell.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (cell.getText() == null || cell.getText().length() == 0) {
                        if (position > 0) {
                            cells[position - 1].setText("");
                            cells[position - 1].requestFocus();
                            return true;
                        }
                    }
                }
                return false;
            });

            cell.setOnFocusChangeListener((v, hasFocus) ->
                    cell.setActivated(cell.getText() != null && cell.getText().length() > 0));
        }

        cells[cells.length - 1].setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verify();
                return true;
            }
            return false;
        });
    }

    private void verify() {
        if (verifying) {
            return;
        }
        String code = readCode();
        if (code.length() < cells.length) {
            showOtpError(getString(R.string.auth_err_otp_incomplete));
            return;
        }
        if (!DEMO_CODE.equals(code)) {
            showOtpError(getString(R.string.auth_err_otp));
            AuthUi.shake(binding.otpRow);
            return;
        }
        if (MODE_RESET.equals(mode) && !validateNewPassword()) {
            return;
        }

        verifying = true;
        binding.btnVerify.setEnabled(false);
        binding.btnVerify.setText(R.string.auth_verifying);
        AuthUi.hideKeyboard(binding.getRoot());

        boolean resetFlow = MODE_RESET.equals(mode);
        binding.btnVerify.postDelayed(() -> {
            if (!isAdded() || binding == null) {
                return;
            }
            if (resetFlow) {
                auth().backToLogin();
                auth().showMessage(R.string.auth_reset_success);
            } else {
                auth().openHost(role);
            }
        }, 550);
    }

    private boolean validateNewPassword() {
        String password = binding.etNewPassword.getText() == null
                ? "" : binding.etNewPassword.getText().toString();
        String confirm = binding.etConfirmNew.getText() == null
                ? "" : binding.etConfirmNew.getText().toString();

        boolean valid = true;
        if (password.length() < 6) {
            AuthUi.markError(binding.etNewPassword, binding.tvNewPasswordError,
                    getString(R.string.auth_err_password));
            valid = false;
        }
        if (!password.equals(confirm)) {
            AuthUi.markError(binding.etConfirmNew, binding.tvConfirmError,
                    getString(R.string.auth_err_password_mismatch));
            valid = false;
        }
        return valid;
    }

    private String readCode() {
        StringBuilder builder = new StringBuilder();
        for (EditText cell : cells) {
            if (cell.getText() != null) {
                builder.append(cell.getText().toString());
            }
        }
        return builder.toString();
    }

    private void clearCells() {
        for (EditText cell : cells) {
            cell.setText("");
            cell.setActivated(false);
        }
        clearOtpError();
    }

    private void resend() {
        if (!binding.tvResend.isEnabled()) {
            return;
        }
        clearCells();
        AuthUi.snack(binding.getRoot(), R.string.auth_resent);
        binding.otp1.requestFocus();
        startCountdown();
    }

    private void startCountdown() {
        if (timer != null) {
            timer.cancel();
        }
        binding.tvResend.setEnabled(false);
        binding.tvResend.setTextColor(ContextCompat.getColor(requireContext(), R.color.auth_text_dim));
        binding.tvCountdown.setText("");

        timer = new CountDownTimer(60_000L, 1_000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) {
                    return;
                }
                binding.tvCountdown.setText(
                        getString(R.string.auth_resend_countdown, millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                if (binding == null) {
                    return;
                }
                binding.tvResend.setEnabled(true);
                binding.tvResend.setTextColor(ContextCompat.getColor(requireContext(), R.color.auth_mint));
                binding.tvCountdown.setText("");
            }
        };
        timer.start();
    }

    private void showOtpError(String message) {
        binding.tvOtpError.setText(message);
        binding.tvOtpError.setVisibility(View.VISIBLE);
        for (EditText cell : cells) {
            cell.setBackgroundResource(R.drawable.bg_auth_otp_cell_error);
        }
    }

    private void clearOtpError() {
        if (binding.tvOtpError.getVisibility() == View.VISIBLE) {
            binding.tvOtpError.setVisibility(View.GONE);
        }
        for (EditText cell : cells) {
            cell.setBackgroundResource(R.drawable.bg_auth_otp_cell);
        }
    }

    private AuthActivity auth() {
        return (AuthActivity) requireActivity();
    }

    @Override
    public void onDestroyView() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
