package com.eventapp.app.ui.organizer;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityOrgOrderDetailBinding;
import com.eventapp.app.ui.organizer.viewmodel.OrgOrderDetailViewModel;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.eventapp.app.ui.common.AppDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/** Chi tiết một đơn hàng: thông tin, người mua và tiến trình. */
public class OrgOrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_CODE = "order_code";

    private ActivityOrgOrderDetailBinding binding;
    private OrgOrderDetailViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(OrgOrderDetailViewModel.class);
        OrgOrder order = viewModel.findOrder(getIntent().getStringExtra(EXTRA_ORDER_CODE));
        bind(order);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnResendTicket.setOnClickListener(v ->
                snack("Đã gửi lại vé tới " + order.buyer));
        binding.btnRefund.setOnClickListener(v -> confirmRefund(order));
    }

    private void bind(OrgOrder order) {
        binding.tvOrderAmount.setText(order.amount > 0
                ? OrgFormat.money(order.amount) : "Miễn phí");
        binding.tvOrderStatus.setText(order.statusLabel);
        OrgUi.applyPill(binding.tvOrderStatus, order.tone);
        binding.tvOrderCodeTop.setText("#" + order.code + " · " + order.time);

        binding.tvInfoCode.setText("#" + order.code);
        binding.tvInfoTime.setText(order.time);
        binding.tvInfoEvent.setText(order.event);
        binding.tvInfoTicket.setText(order.ticketType);
        binding.tvInfoPayment.setText(order.payment);

        binding.tvBuyerName.setText(order.buyer);
        binding.tvBuyerContact.setText(order.contact);
        binding.tvBuyerBadge.setText(order.ticketType);
        binding.tvBuyerInitials.setText(initials(order.buyer));

        binding.tvTimeline1Time.setText(order.time);
        binding.tvTimeline2Time.setText("Xác nhận bởi cổng thanh toán");
        binding.tvTimeline3Time.setText("Vé điện tử đã gửi qua email");
    }

    private void confirmRefund(OrgOrder order) {
        AppDialog.confirm(this,
                R.drawable.ic_user_wallet,
                getString(R.string.org_refund_action),
                "Hoàn " + OrgFormat.money(order.amount) + " cho " + order.buyer + "?",
                getString(R.string.common_confirm),
                getString(R.string.common_close),
                AppDialog.Tone.NEUTRAL,
                () -> snack("Đã tạo yêu cầu hoàn tiền"));
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) {
            return "?";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(parts[0].substring(0, 1).toUpperCase(Locale.ROOT));
        if (parts.length > 1) {
            builder.append(parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
