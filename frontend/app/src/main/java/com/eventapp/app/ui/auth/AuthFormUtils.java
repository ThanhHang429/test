package com.eventapp.app.ui.auth;

import android.content.Context;
import android.text.Editable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.util.Patterns;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Tiện ích validate, xóa lỗi và bàn phím dùng chung cho các form Auth UI demo. */
public final class AuthFormUtils {

    private AuthFormUtils() {
    }

    static boolean isValidEmail(String value) {
        return value != null && Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches();
    }

    static boolean isValidPassword(String value) {
        return value != null && value.length() >= 6;
    }

    static boolean isValidPhone(String value) {
        if (value == null) {
            return false;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.length() >= 9 && digits.length() <= 11;
    }

    static String textOf(android.widget.EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    static void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        }
    }

    static void clearErrorOnType(TextInputEditText input, TextInputLayout layout) {
        if (input == null || layout == null) {
            return;
        }
        input.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                layout.setError(null);
                layout.setErrorEnabled(false);
            }
        });
    }

    static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
