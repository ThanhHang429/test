package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserCheckoutBinding;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.eventapp.app.ui.user.model.DemoTicket;
import com.eventapp.app.ui.user.util.UserFmt;

/** Xác nhận đơn hàng, chọn phương thức thanh toán (mô phỏng) và hiển thị kết quả. */
public class CheckoutActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_QUANTITIES = "quantities";

    private static final int METHOD_WALLET = 0;
    private static final int METHOD_VIETQR = 1;
    private static final int METHOD_CARD = 2;

    private ActivityUserCheckoutBinding binding;
    private DemoEvent event;
    private int[] quantities;
    private long total;
    private int quantity;
    private int method = METHOD_VIETQR;
    private String pendingCode;

    public static void start(Context context, long eventId, int[] quantities) {
        Intent intent = new Intent(context, CheckoutActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        intent.putExtra(EXTRA_QUANTITIES, quantities);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        event = DemoContent.get().event(getIntent().getLongExtra(EXTRA_EVENT_ID, -1L));
        quantities = getIntent().getIntArrayExtra(EXTRA_QUANTITIES);
        if (event == null || quantities == null || quantities.length != event.ticketTypes.size()) {
            finish();
            return;
        }

        pendingCode = "NX" + (100000 + (int) (Math.random() * 899999));
        applyInsets();
        bindSummary();
        buildOrderLines();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.optWallet.setOnClickListener(v -> selectMethod(METHOD_WALLET));
        binding.optVietqr.setOnClickListener(v -> selectMethod(METHOD_VIETQR));
        binding.optCard.setOnClickListener(v -> selectMethod(METHOD_CARD));
        binding.btnPay.setOnClickListener(v -> pay());
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        selectMethod(total > 0 ? METHOD_VIETQR : METHOD_WALLET);
        updateCta();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.checkoutRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            LinearLayout.LayoutParams topParams =
                    (LinearLayout.LayoutParams) binding.topBar.getLayoutParams();
            topParams.height = dp(60) + bars.top;
            binding.topBar.setLayoutParams(topParams);
            binding.topBar.setPadding(dp(16), bars.top + dp(6), dp(16), dp(6));

            binding.bottomBar.setPadding(binding.bottomBar.getPaddingStart(),
                    binding.bottomBar.getPaddingTop(), binding.bottomBar.getPaddingEnd(),
                    dp(12) + bars.bottom);
            binding.successContent.setPadding(dp(28), bars.top + dp(24), dp(28),
                    bars.bottom + dp(24));
            return insets;
        });
    }

    private void bindSummary() {
        binding.cover.setBackgroundResource(event.coverRes);
        binding.ivIcon.setImageResource(event.iconRes);
        binding.tvEventTitle.setText(event.title);
        binding.tvEventDate.setText(UserFmt.dateTime(event.startAt));
        binding.tvEventVenue.setText(event.venue);
    }

    private void buildOrderLines() {
        binding.orderLines.removeAllViews();
        total = 0L;
        quantity = 0;
        for (int i = 0; i < event.ticketTypes.size(); i++) {
            int qty = quantities[i];
            if (qty <= 0) {
                continue;
            }
            DemoEvent.TicketType type = event.ticketTypes.get(i);
            long lineTotal = type.price * qty;
            total += lineTotal;
            quantity += qty;
            binding.orderLines.addView(orderLine(type.name + " × " + qty,
                    UserFmt.amount(lineTotal)));
        }

        binding.tvSubtotal.setText(UserFmt.amount(total));
        binding.tvFee.setText(UserFmt.amount(0));
        binding.tvTotal.setText(total == 0 ? getString(R.string.user_free)
                : UserFmt.amount(total));
        binding.tvBottomTotal.setText(total == 0 ? getString(R.string.user_free)
                : UserFmt.amount(total));
        binding.paymentSection.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
    }

    private View orderLine(String label, String amount) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextSize(13f);
        name.setTextColor(ContextCompat.getColor(this, R.color.user_text_muted));
        name.setTypeface(ResourcesCompat.getFont(this, R.font.plus_jakarta_sans));
        row.addView(name, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView value = new TextView(this);
        value.setText(amount);
        value.setTextSize(13f);
        value.setTextColor(ContextCompat.getColor(this, R.color.user_text));
        value.setTypeface(ResourcesCompat.getFont(this, R.font.plus_jakarta_sans),
                Typeface.BOLD);
        row.addView(value);
        return row;
    }

    private void selectMethod(int selected) {
        method = selected;
        binding.optWallet.setSelected(selected == METHOD_WALLET);
        binding.optVietqr.setSelected(selected == METHOD_VIETQR);
        binding.optCard.setSelected(selected == METHOD_CARD);
        binding.radioWallet.setChecked(selected == METHOD_WALLET);
        binding.radioVietqr.setChecked(selected == METHOD_VIETQR);
        binding.radioCard.setChecked(selected == METHOD_CARD);

        boolean showQr = selected == METHOD_VIETQR && total > 0;
        binding.qrPanel.setVisibility(showQr ? View.VISIBLE : View.GONE);
        if (showQr) {
            binding.qrCode.setPayload("NEXUS|" + pendingCode + "|" + total);
            binding.tvQrAmount.setText(UserFmt.amount(total));
            binding.tvQrInfo.setText(getString(R.string.user_vietqr_bank) + "\n"
                    + getString(R.string.user_vietqr_account) + " · "
                    + getString(R.string.user_vietqr_account_no) + "\n"
                    + getString(R.string.user_vietqr_content, pendingCode));
        }
    }

    private void updateCta() {
        if (total <= 0) {
            binding.btnPay.setText(R.string.user_confirm_register);
        } else {
            binding.btnPay.setText(getString(R.string.user_pay_now, UserFmt.amount(total)));
        }
    }

    private void pay() {
        binding.btnPay.setEnabled(false);
        binding.btnPay.setText(R.string.user_payment_processing);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            DemoTicket ticket = DemoContent.get().purchase(event, typeName(), quantity, total);
            showSuccess(ticket);
        }, 900);
    }

    private String typeName() {
        int selectedTypes = 0;
        String name = "";
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] > 0) {
                selectedTypes++;
                if (name.isEmpty()) {
                    name = event.ticketTypes.get(i).name;
                }
            }
        }
        return selectedTypes > 1 ? "Nhiều loại vé" : name;
    }

    private void showSuccess(DemoTicket ticket) {
        binding.contentRoot.setVisibility(View.GONE);
        binding.successPanel.setVisibility(View.VISIBLE);
        binding.tvOrderCode.setText(ticket.code);
        binding.btnViewTicket.setOnClickListener(v -> {
            TicketDetailActivity.start(this, ticket.code);
            finish();
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
