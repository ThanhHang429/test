package com.eventapp.app.ui.user.order;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Validate và xóa lỗi cho form người nhận vé ở luồng mua vé / đăng ký miễn phí. */
public final class OrderFormUtils {

    private OrderFormUtils() {
    }

    static String textOf(EditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
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

    static void showError(TextInputLayout layout) {
        layout.setErrorEnabled(true);
    }

    static void clearError(TextInputLayout layout) {
        if (layout != null) {
            layout.setError(null);
            layout.setErrorEnabled(false);
        }
    }

    static void clearErrorOnType(TextInputEditText input, TextInputLayout layout) {
        if (input == null || layout == null) {
            return;
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần xử lý.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Không cần xử lý.
            }

            @Override
            public void afterTextChanged(Editable s) {
                clearError(layout);
            }
        });
    }
}
