package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;

/**
 * Hiển thị mã VietQR thật 100%, quét được bằng tất cả các ứng dụng ngân hàng.
 */
public class DemoQrView extends AppCompatImageView {

    public DemoQrView(@NonNull Context context) {
        super(context);
        init();
    }

    public DemoQrView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DemoQrView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.FIT_CENTER);
    }

    public void setPayload(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            setImageDrawable(null);
            return;
        }

        // Nếu payload chứa ký tự pipe '|' (thường dùng cho thanh toán ngân hàng: ETICKET|Mã|Số tiền)
        // thì mới tạo ảnh VietQR ngân hàng.
        if (value.contains("|")) {
            String amount = "";
            String content = "ETICKET";
            String[] parts = value.split("\\|");
            if (parts.length >= 3) {
                content = parts[1];
                amount = parts[2];
            } else if (parts.length >= 2) {
                content = parts[1];
            }

            String qrUrl = "https://img.vietqr.io/image/ACB-190368688888-compact2.png"
                    + "?amount=" + amount
                    + "&addInfo=" + Uri.encode(content)
                    + "&accountName=" + Uri.encode("CONG TY ETICKET");

            Glide.with(this)
                    .load(qrUrl)
                    .into(this);
        } else {
            // Đây là MÃ VÉ CHECK-IN (ví dụ: ET-5411-7078#0).
            // Dùng dịch vụ tạo mã QR tiêu chuẩn quốc tế, nét căng, đen trắng, không dính logo ngân hàng.
            String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&format=png&data="
                    + Uri.encode(value);

            Glide.with(this)
                    .load(qrUrl)
                    .into(this);
        }
    }
}
