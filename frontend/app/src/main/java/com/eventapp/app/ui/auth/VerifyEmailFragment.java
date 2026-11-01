package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
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
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.eventapp.app.ui.demo.DemoRouter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * FR-U01: xác thực email bằng OTP demo.
 *
 * <p>OTP không được gửi thật; mã cố định {@link DemoAuthStore#DEMO_OTP} được hiển thị ngay trên màn
 * hình để phục vụ demo.</p>
 */
public class VerifyEmailFragment extends Fragment {

    public static final String ARG_EMAIL = "arg_email";

    private static final long RESEND_SECONDS = 30L;

    private String email = "";
    private TextInputLayout codeLayout;
    private TextInputEditText codeInput;
    private MaterialButton resendButton;
    private CountDownTimer resendTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auth_verify_email, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        email = args == null ? "" : args.getString(ARG_EMAIL, "");

        TextView subtitle = view.findViewById(R.id.tvVerifySubtitle);
        subtitle.setText(email.isEmpty()
                ? getString(R.string.auth_verify_subtitle_fallback)
                : getString(R.string.auth_verify_subtitle, email));

        codeLayout = view.findViewById(R.id.layoutVerifyCode);
        codeInput = view.findViewById(R.id.inputVerifyCode);
        resendButton = view.findViewById(R.id.btnVerifyResend);

        view.findViewById(R.id.btnVerify).setOnClickListener(v -> submitCode());
        resendButton.setOnClickListener(v -> resendCode());
        view.findViewById(R.id.btnVerifyBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());
        view.findViewById(R.id.btnVerifyBackToLogin).setOnClickListener(v -> backToLogin());

        AuthFormUtils.clearErrorOnType(codeInput, codeLayout);
        // Giãn chữ khi đã có mã để dễ đọc 6 số, nhưng giữ hint ở dạng bình thường.
        codeInput.setLetterSpacing(0f);
        codeInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                codeInput.setLetterSpacing(editable.length() == 0 ? 0f : 0.35f);
            }
        });
        codeInput.setOnEditorActionListener((v, actionId, event) -> {
            submitCode();
            return true;
        });

        startResendCountdown();
    }

    private void submitCode() {
        String code = AuthFormUtils.textOf(codeInput);
        codeLayout.setError(null);
        codeLayout.setErrorEnabled(false);
        if (!code.matches("\\d{6}")) {
            codeLayout.setError(getString(R.string.auth_error_otp));
            return;
        }

        AuthFormUtils.hideKeyboard(getView());
        DemoAccount account = DemoAuthStore.get(requireContext()).verifyEmail(email, code);
        if (account == null) {
            codeLayout.setError(getString(R.string.auth_error_otp));
            return;
        }

        Toast.makeText(requireContext(), R.string.auth_verify_success, Toast.LENGTH_LONG).show();
        DemoRouter.openHost(requireContext(), account.getRole());
        requireActivity().finish();
    }

    private void resendCode() {
        Toast.makeText(requireContext(), R.string.auth_otp_resent, Toast.LENGTH_SHORT).show();
        startResendCountdown();
    }

    private void startResendCountdown() {
        if (resendButton == null) {
            return;
        }
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        resendButton.setEnabled(false);
        resendButton.setAlpha(0.5f);
        resendTimer = new CountDownTimer(RESEND_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (resendButton == null) {
                    return;
                }
                int seconds = (int) Math.ceil(millisUntilFinished / 1000.0);
                resendButton.setText(getResources().getQuantityString(
                        R.plurals.auth_resend_countdown, seconds, seconds));
            }

            @Override
            public void onFinish() {
                if (resendButton == null) {
                    return;
                }
                resendButton.setEnabled(true);
                resendButton.setAlpha(1f);
                resendButton.setText(R.string.auth_resend);
            }
        };
        resendTimer.start();
    }

    private void backToLogin() {
        NavController navController = Navigation.findNavController(requireView());
        if (!navController.popBackStack(R.id.loginFragment, false)) {
            navController.navigate(R.id.loginFragment);
        }
    }

    @Override
    public void onDestroyView() {
        if (resendTimer != null) {
            resendTimer.cancel();
            resendTimer = null;
        }
        resendButton = null;
        codeLayout = null;
        codeInput = null;
        super.onDestroyView();
    }
}
