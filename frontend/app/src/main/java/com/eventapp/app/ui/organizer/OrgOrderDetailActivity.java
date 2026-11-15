package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.refund.RefundDtos;
import com.eventapp.app.data.repository.RefundRepository;
import com.eventapp.app.databinding.ActivityOrgOrderDetailBinding;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.eventapp.app.ui.organizer.viewmodel.OrgOrderDetailViewModel;
import com.eventapp.app.ui.organizer.viewmodel.OrgOrdersViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonElement;

import java.time.Instant;
import java.util.Locale;

/** Chi tiết một đơn hàng thật từ Backend (FR-O17, FR-O25). */
public class OrgOrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_CODE = "order_code";

    private ActivityOrgOrderDetailBinding binding;
    private OrgOrderDetailViewModel viewModel;
    @Nullable private OrderDtos.Order currentDto;
    @Nullable private OrgOrder currentOrder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Chưa có endpoint gửi lại email vé riêng -> ẩn nút để không tạo nút chết
        binding.btnResendTicket.setVisibility(View.GONE);
        binding.btnRefund.setEnabled(false);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRefund.setOnClickListener(v -> openRefundReasonDialog());

        viewModel = new ViewModelProvider(this).get(OrgOrderDetailViewModel.class);
        observe();

        String orderCode = getIntent().getStringExtra(EXTRA_ORDER_CODE);
        if (orderCode == null || orderCode.trim().isEmpty()) {
            snack("Không tìm thấy mã đơn hàng");
            return;
        }
        viewModel.load(orderCode.trim());
    }

    private void observe() {
        viewModel.getOrder().observe(this, dto -> {
            if (dto == null || binding == null) return;
            currentDto = dto;
            currentOrder = OrgOrdersViewModel.fromDto(dto);
            bind(dto, currentOrder);
        });
        viewModel.getError().observe(this, err -> {
            if (err == null || binding == null) return;
            viewModel.consumeError();
            snack(err);
        });
    }

    private void bind(OrderDtos.Order dto, OrgOrder order) {
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

        binding.tvTimeline1Time.setText(formatIso(dto.createdAt));
        binding.tvTimeline2Time.setText(dto.paidAt != null ? formatIso(dto.paidAt) : "Chưa thanh toán");
        binding.tvTimeline3Time.setText(dto.ticketsIssued > 0
                ? ("Đã phát hành " + dto.ticketsIssued + " vé điện tử")
                : "Chưa phát hành vé");

        boolean canRefund = "PAID".equalsIgnoreCase(order.rawStatus) && order.amount > 0;
        binding.btnRefund.setVisibility(canRefund ? View.VISIBLE : View.GONE);
        binding.btnRefund.setEnabled(canRefund);
    }

    private void openRefundReasonDialog() {
        if (currentDto == null || currentOrder == null) return;
        EditText etReason = new EditText(this);
        etReason.setHint("Nhập lý do hoàn tiền (ví dụ: Ban tổ chức hỗ trợ hoàn vé)...");
        etReason.setMinLines(2);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(etReason);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.org_refund_action))
                .setMessage("Tạo yêu cầu hoàn " + OrgFormat.money(currentOrder.amount)
                        + " cho " + currentOrder.buyer + "?\nAdmin sẽ duyệt và hoàn tiền tự động vào Ví ETicket của người mua.")
                .setView(container)
                .setPositiveButton(getString(R.string.common_confirm), (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "Nhà tổ chức hỗ trợ hoàn tiền đơn #" + currentOrder.code;
                    }
                    binding.btnRefund.setEnabled(false);
                    RefundRepository repo = ServiceLocator.get(RefundRepository.class);
                    repo.requestRefund(currentDto.orderId, currentDto.orderCode,
                            currentDto.eventTitle, currentDto.totalAmount, reason,
                            new ApiCallback<RefundDtos.RefundResponse>() {
                                @Override
                                public void onSuccess(RefundDtos.RefundResponse data, JsonElement meta) {
                                    if (isFinishing() || isDestroyed()) return;
                                    snack("✅ Đã gửi yêu cầu hoàn tiền cho Admin duyệt");
                                    viewModel.load(currentOrder.code);
                                }

                                @Override
                                public void onError(@NonNull ApiException error) {
                                    if (isFinishing() || isDestroyed()) return;
                                    binding.btnRefund.setEnabled(true);
                                    snack("❌ " + error.getUserMessage());
                                }
                            });
                })
                .setNegativeButton(getString(R.string.common_close), null)
                .show();
    }

    private static String formatIso(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return "—";
        try {
            return OrgFormat.dateTime(Instant.parse(iso.trim()).toEpochMilli());
        } catch (Exception ignored) {
            return iso;
        }
    }

    private static String initials(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return "?";
        String[] parts = trimmed.split("\\s+");
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
