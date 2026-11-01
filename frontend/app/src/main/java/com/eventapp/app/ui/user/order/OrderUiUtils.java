package com.eventapp.app.ui.user.order;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.eventapp.app.R;
import com.eventapp.app.ui.demo.DemoAccount;
import com.eventapp.app.ui.demo.DemoAuthStore;
import com.eventapp.app.ui.model.EventUiModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Tiện ích hiển thị dùng chung cho các màn hình đơn hàng của User. */
public final class OrderUiUtils {

    private OrderUiUtils() {
    }

    /** Tô đậm bước hiện tại, đánh dấu các bước đã qua và làm mờ các bước còn lại. */
    static void bindSteps(View root, int activeStep) {
        int[] ids = {R.id.tvOrderStep1, R.id.tvOrderStep2, R.id.tvOrderStep3};
        for (int i = 0; i < ids.length; i++) {
            TextView step = root.findViewById(ids[i]);
            if (step == null) {
                continue;
            }
            if (i < activeStep) {
                step.setBackgroundResource(R.drawable.bg_ds_step_pill_done);
                step.setTextColor(ContextCompat.getColor(root.getContext(), R.color.ds_primary));
            } else if (i == activeStep) {
                step.setBackgroundResource(R.drawable.bg_ds_step_pill_active);
                step.setTextColor(ContextCompat.getColor(root.getContext(), R.color.ds_white));
            } else {
                step.setBackgroundResource(R.drawable.bg_ds_step_pill_inactive);
                step.setTextColor(ContextCompat.getColor(root.getContext(),
                        R.color.ds_text_muted));
            }
        }
    }

    /** Điền sẵn thông tin tài khoản demo nếu người dùng chưa nhập gì. */
    static void prefillAccount(Context context, EditText name, EditText email, EditText phone) {
        DemoAccount account = DemoAuthStore.get(context).currentAccount();
        if (account == null) {
            return;
        }
        setIfEmpty(name, account.getDisplayName());
        setIfEmpty(email, account.getEmail());
        setIfEmpty(phone, account.getPhone());
    }

    private static void setIfEmpty(EditText input, String value) {
        if (input == null || value == null || value.trim().isEmpty()) {
            return;
        }
        if (input.getText() == null || input.getText().toString().trim().isEmpty()) {
            input.setText(value);
        }
    }

    static String eventDate(long millis) {
        return new SimpleDateFormat("HH:mm • dd/MM/yyyy", new Locale("vi", "VN"))
                .format(new Date(millis));
    }

    static String dayOfMonth(long millis) {
        return new SimpleDateFormat("dd", new Locale("vi", "VN")).format(new Date(millis));
    }

    static String monthLabel(long millis) {
        String value = new SimpleDateFormat("MMM", new Locale("vi", "VN"))
                .format(new Date(millis));
        return value.toUpperCase(new Locale("vi", "VN"));
    }

    /** Ghép địa điểm và thành phố, bỏ phần tử rỗng để không hiện dấu phân cách thừa. */
    static String venueLine(EventUiModel event) {
        if (event == null) {
            return "";
        }
        String venue = event.venue == null ? "" : event.venue.trim();
        String city = event.city == null ? "" : event.city.trim();
        if (venue.isEmpty()) {
            return city;
        }
        if (city.isEmpty()) {
            return venue;
        }
        return venue + " • " + city;
    }

    static String createdDate(long millis) {
        return new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"))
                .format(new Date(millis));
    }

    static void copyText(Context context, String value) {
        ClipboardManager manager =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("ETicket", value));
        }
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
