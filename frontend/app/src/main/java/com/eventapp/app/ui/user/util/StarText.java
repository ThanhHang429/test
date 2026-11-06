package com.eventapp.app.ui.user.util;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.eventapp.app.R;

/** Vẽ 5 ngôi sao bằng ký tự, tô màu phần đã chấm. */
public final class StarText {

    private static final int MAX = 5;

    private StarText() {
    }

    public static void apply(TextView view, float rating) {
        int filled = Math.max(0, Math.min(MAX, Math.round(rating)));
        int colorFilled = ContextCompat.getColor(view.getContext(), R.color.user_warning);
        int colorEmpty = ContextCompat.getColor(view.getContext(), R.color.user_border_strong);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int i = 1; i <= MAX; i++) {
            int start = builder.length();
            builder.append(i <= filled ? "★" : "☆");
            builder.setSpan(
                    new ForegroundColorSpan(i <= filled ? colorFilled : colorEmpty),
                    start,
                    builder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        view.setText(builder);
    }
}
