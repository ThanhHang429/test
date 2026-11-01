package com.eventapp.app.ui.auth;

import android.text.Editable;
import android.text.TextWatcher;

/** TextWatcher rút gọn cho các form Auth: chỉ cần override {@link #afterTextChanged(Editable)}. */
public abstract class SimpleTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Không cần xử lý.
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Không cần xử lý.
    }
}
