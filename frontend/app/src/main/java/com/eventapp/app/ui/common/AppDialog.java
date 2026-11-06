package com.eventapp.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.eventapp.app.R;
import com.eventapp.app.databinding.DialogAppConfirmBinding;

/**
 * Hộp thoại xác nhận dùng chung cho cả ba khu (Người dùng / Nhà tổ chức / Quản trị).
 *
 * <p>Màu nhấn lấy từ theme của Activity đang mở nên cùng một layout vẫn ra đúng sắc thái xanh
 * dương, teal hay navy. Dùng cho các câu hỏi ngắn kiểu «Đăng xuất?», «Huỷ sự kiện?».</p>
 */
public final class AppDialog {

    public enum Tone {
        NEUTRAL,
        DANGER
    }

    private AppDialog() {
    }

    @NonNull
    public static Dialog confirm(@NonNull Context context,
                                 @DrawableRes int iconRes,
                                 @NonNull CharSequence title,
                                 @NonNull CharSequence message,
                                 @NonNull CharSequence positive,
                                 @Nullable CharSequence negative,
                                 @NonNull Tone tone,
                                 @NonNull Runnable onConfirm) {
        DialogAppConfirmBinding binding = DialogAppConfirmBinding.inflate(
                LayoutInflater.from(context));

        int primary = themeColor(context, androidx.appcompat.R.attr.colorPrimary,
                R.color.brand_primary);
        int onSurface = themeColor(context, com.google.android.material.R.attr.colorOnSurface,
                R.color.dialog_text);
        int outline = themeColor(context, com.google.android.material.R.attr.colorOutline,
                R.color.dialog_border);

        int accent = tone == Tone.DANGER
                ? ContextCompat.getColor(context, R.color.dialog_danger)
                : primary;
        int tileColor = tone == Tone.DANGER
                ? ContextCompat.getColor(context, R.color.dialog_danger_soft)
                : ColorUtils.setAlphaComponent(primary, 0x1F);

        GradientDrawable tile = new GradientDrawable();
        tile.setShape(GradientDrawable.OVAL);
        tile.setColor(tileColor);
        binding.iconTile.setBackground(tile);

        if (iconRes == 0) {
            binding.iconTile.setVisibility(View.GONE);
        } else {
            binding.ivIcon.setImageResource(iconRes);
            binding.ivIcon.setImageTintList(ColorStateList.valueOf(accent));
        }

        binding.tvTitle.setText(title);
        binding.tvMessage.setText(message);

        binding.btnPositive.setText(positive);
        binding.btnPositive.setBackgroundTintList(ColorStateList.valueOf(accent));
        binding.btnPositive.setTextColor(Color.WHITE);

        if (negative == null) {
            binding.btnNegative.setVisibility(View.GONE);
            binding.buttonSpacer.setVisibility(View.GONE);
        } else {
            binding.btnNegative.setText(negative);
            binding.btnNegative.setTextColor(onSurface);
            binding.btnNegative.setStrokeColor(ColorStateList.valueOf(outline));
            binding.btnNegative.setBackgroundTintList(
                    ColorStateList.valueOf(Color.TRANSPARENT));
        }

        Dialog dialog = new Dialog(context, R.style.Theme_EventApp_Dialog);
        dialog.setContentView(binding.getRoot());
        dialog.setCanceledOnTouchOutside(true);

        binding.btnNegative.setOnClickListener(v -> dialog.dismiss());
        binding.btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            onConfirm.run();
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.45f;
            window.setAttributes(params);
            window.setGravity(Gravity.CENTER);
            int maxWidth = Math.round(context.getResources().getDisplayMetrics().widthPixels
                    - dp(context, 48));
            window.setLayout(Math.min(maxWidth, dp(context, 420)),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    /** Bản rút gọn cho câu hỏi chỉ có một lựa chọn «Đóng». */
    @NonNull
    public static Dialog message(@NonNull Context context,
                                 @DrawableRes int iconRes,
                                 @StringRes int titleRes,
                                 @NonNull CharSequence message) {
        return confirm(context, iconRes, context.getString(titleRes), message,
                context.getString(R.string.dialog_close), null, Tone.NEUTRAL, () -> {
                });
    }

    private static int themeColor(Context context, int attr, int fallbackRes) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(context, value.resourceId);
            }
            return value.data;
        }
        return ContextCompat.getColor(context, fallbackRes);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
