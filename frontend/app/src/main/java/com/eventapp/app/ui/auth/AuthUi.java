package com.eventapp.app.ui.auth;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.SnackPill;

/** Tiện ích dùng chung cho 4 màn xác thực. */
public final class AuthUi {

    private AuthUi() {
    }

    static boolean isValidEmail(String value) {
        return value != null && Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches();
    }

    static boolean isValidPhone(String value) {
        if (value == null) {
            return false;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.length() >= 9 && digits.length() <= 11;
    }

    static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    static void markError(EditText field, TextView error, CharSequence message) {
        field.setBackgroundResource(R.drawable.bg_auth_input_error);
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    static void clearErrorOnType(EditText field, TextView... errors) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                field.setBackgroundResource(R.drawable.bg_auth_input);
                for (TextView error : errors) {
                    error.setVisibility(View.GONE);
                }
            }
        });
    }

    static void bindPasswordToggle(EditText field, ImageButton toggle) {
        final boolean[] visible = {false};
        toggle.setOnClickListener(v -> {
            visible[0] = !visible[0];
            int selection = field.getSelectionEnd();
            field.setInputType(visible[0]
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            field.setTypeface(ResourcesCompat.getFont(field.getContext(), R.font.plus_jakarta_sans));
            if (selection >= 0) {
                field.setSelection(Math.min(selection, field.getText() == null ? 0 : field.getText().length()));
            }
            toggle.setImageResource(visible[0] ? R.drawable.ic_auth_eye_off : R.drawable.ic_auth_eye);
        });
    }

    /** 0 = rỗng, 1 = yếu, 2 = vừa, 3 = mạnh. */
    static int passwordScore(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSymbol = password.matches(".*[^A-Za-z0-9].*");
        int score = 1;
        if (password.length() >= 8 && hasLetter && hasDigit) {
            score = 2;
        }
        if (password.length() >= 10 && hasLetter && hasDigit && hasSymbol) {
            score = 3;
        }
        return score;
    }

    static void renderStrength(View[] bars, int score, Context context) {
        int activeColor = ContextCompat.getColor(context,
                score >= 3 ? R.color.auth_success
                        : score == 2 ? R.color.auth_warning
                        : R.color.auth_error);
        int idleColor = ContextCompat.getColor(context, R.color.auth_track);
        for (int i = 0; i < bars.length; i++) {
            bars[i].setBackgroundTintList(ColorStateList.valueOf(i < score ? activeColor : idleColor));
        }
    }

    static void shake(View view) {
        view.animate().cancel();
        view.setTranslationX(0f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX",
                0f, -12f, 12f, -8f, 8f, 0f);
        animator.setDuration(300);
        animator.start();
    }

    static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Pill thông báo nền tối, đồng bộ theme dark thay cho Toast trắng mặc định. */
    static void snack(View anchor, int messageRes) {
        SnackPill.show(anchor, anchor.getContext().getString(messageRes), dp(anchor, 40));
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }
}
