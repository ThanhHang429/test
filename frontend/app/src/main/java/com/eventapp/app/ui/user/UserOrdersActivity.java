package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.repository.OrderRepository;
import com.eventapp.app.databinding.ActivityUserOrdersBinding;
import com.eventapp.app.ui.user.adapter.UserOrdersAdapter;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Lịch sử đơn hàng của người dùng (FR-U16).
 * Hỗ trợ: xem vé đã mua, thanh toán tiếp đơn chờ, huỷ đơn chờ, và gửi yêu cầu hoàn tiền (FR-O25).
 */
public class UserOrdersActivity extends AppCompatActivity implements UserOrdersAdapter.Listener {

    private static final String EXTRA_FILTER = "initial_filter";

    public static void start(Context context) {
        context.startActivity(new Intent(context, UserOrdersActivity.class));
    }

    public static void start(Context context, String initialFilter) {
        Intent intent = new Intent(context, UserOrdersActivity.class);
        intent.putExtra(EXTRA_FILTER, initialFilter);
        context.startActivity(intent);
    }

    private ActivityUserOrdersBinding binding;
    private UserOrdersAdapter adapter;
    private final List<OrderDtos.Order> allOrders = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new UserOrdersAdapter(this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        initFilterChips();
        String initFilter = getIntent() != null ? getIntent().getStringExtra(EXTRA_FILTER) : null;
        setFilter(initFilter != null ? initFilter : "ALL");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void initFilterChips() {
        binding.chipAll.setOnClickListener(v -> setFilter("ALL"));
        binding.chipPaid.setOnClickListener(v -> setFilter("PAID"));
        binding.chipPending.setOnClickListener(v -> setFilter("PENDING_PAYMENT"));
        binding.chipCancelled.setOnClickListener(v -> setFilter("CANCELLED"));
    }

    private void setFilter(String filter) {
        this.currentFilter = filter;
        binding.chipAll.setSelected("ALL".equals(filter));
        binding.chipPaid.setSelected("PAID".equals(filter));
        binding.chipPending.setSelected("PENDING_PAYMENT".equals(filter));
        binding.chipCancelled.setSelected("CANCELLED".equals(filter));
        applyFilter();
    }

    private void loadOrders() {
        OrderRepository repo = ServiceLocator.get(OrderRepository.class);
        repo.getMyOrders(new ApiCallback<List<OrderDtos.Order>>() {
            @Override
            public void onSuccess(@Nullable List<OrderDtos.Order> data, @Nullable JsonElement meta) {
                allOrders.clear();
                if (data != null && !data.isEmpty()) {
                    allOrders.addAll(data);
                }
                applyFilter();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                allOrders.clear();
                applyFilter();
                Toast.makeText(UserOrdersActivity.this,
                        "Không thể tải lịch sử đơn hàng: " + exception.getUserMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<OrderDtos.Order> filtered = new ArrayList<>();
        for (OrderDtos.Order order : allOrders) {
            String s = order.status != null ? order.status.toUpperCase() : "PAID";
            if ("ALL".equals(currentFilter)) {
                filtered.add(order);
            } else if ("PAID".equals(currentFilter) && ("PAID".equals(s) || "COMPLETED".equals(s))) {
                filtered.add(order);
            } else if ("PENDING_PAYMENT".equals(currentFilter) && ("PENDING_PAYMENT".equals(s) || "PENDING".equals(s))) {
                filtered.add(order);
            } else if ("CANCELLED".equals(currentFilter)
                    && ("CANCELLED".equals(s) || "EXPIRED".equals(s) || "REFUNDED".equals(s) || "REFUND_PENDING".equals(s))) {
                filtered.add(order);
            }
        }

        adapter.submitList(filtered);
        binding.emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvOrders.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onOrderClick(OrderDtos.Order order) {
        showOrderActions(order);
    }

    @Override
    public void onViewTicketsClick(OrderDtos.Order order) {
        openTicketForOrder(order);
    }

    @Override
    public void onContinuePaymentClick(OrderDtos.Order order) {
        CheckoutActivity.startForPendingOrder(this, order);
    }

    @Override
    public void onCancelOrderClick(OrderDtos.Order order) {
        confirmCancelOrder(order);
    }

    private void showOrderActions(OrderDtos.Order order) {
        String status = order.status != null ? order.status.toUpperCase() : "PAID";
        String codeLabel = order.orderCode != null ? order.orderCode : ("#" + order.orderId);
        String summaryMsg = buildOrderTransactionSummary(order);
        if ("PAID".equals(status) || "COMPLETED".equals(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Đơn hàng " + codeLabel)
                    .setMessage(summaryMsg)
                    .setPositiveButton("🎟️ Xem vé điện tử", (dialog, which) -> openTicketForOrder(order))
                    .setNegativeButton("💸 Yêu cầu hoàn tiền (FR-O25)", (dialog, which) -> showRefundRequestDialog(order))
                    .setNeutralButton("Đóng", null)
                    .show();
        } else if ("PENDING_PAYMENT".equals(status) || "PENDING".equals(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Đơn chờ thanh toán " + codeLabel)
                    .setMessage(summaryMsg + "\n\nBạn muốn tiếp tục thanh toán hay huỷ đơn để trả lại vé?")
                    .setPositiveButton("💳 Thanh toán ngay", (dialog, which) -> CheckoutActivity.startForPendingOrder(this, order))
                    .setNegativeButton("❌ Huỷ đơn hàng", (dialog, which) -> confirmCancelOrder(order))
                    .setNeutralButton("Đóng", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Chi tiết đơn & giao dịch " + codeLabel)
                    .setMessage(summaryMsg)
                    .setPositiveButton("Đóng", null)
                    .show();
        }
    }

    private String buildOrderTransactionSummary(OrderDtos.Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.eventTitle != null && !order.eventTitle.isEmpty()) {
            sb.append(order.eventTitle).append("\n");
        }
        sb.append("• Trạng thái đơn: ").append(order.status != null ? order.status : "—").append("\n");
        sb.append("• Trạng thái thanh toán: ").append(order.paymentStatus != null ? order.paymentStatus : "CHƯA CÓ GIAO DỊCH").append("\n");
        sb.append("• Tổng thanh toán: ")
                .append(order.totalAmount != null ? com.eventapp.app.ui.user.util.UserFmt.amount(order.totalAmount.longValue()) : "0 đ");
        if (order.paymentMethod != null && !order.paymentMethod.isEmpty()) {
            sb.append("\n• Phương thức: ").append(order.paymentMethod);
        }
        if (order.payments != null && !order.payments.isEmpty()) {
            sb.append("\n\nLịch sử giao dịch (" + order.payments.size() + "):");
            for (OrderDtos.PaymentAttempt p : order.payments) {
                String amt = p.amount != null ? com.eventapp.app.ui.user.util.UserFmt.amount(p.amount.longValue()) : "0 đ";
                sb.append("\n- ").append(p.paymentReference != null ? p.paymentReference : ("#" + p.paymentId))
                        .append(" · ").append(p.method != null ? p.method : "—")
                        .append(" [").append(p.status != null ? p.status : "—").append("] · ").append(amt);
                if (p.failureMessage != null && !p.failureMessage.isEmpty()) {
                    sb.append("\n  Lý do lỗi: ").append(p.failureMessage);
                }
            }
        }
        return sb.toString();
    }

    private void confirmCancelOrder(OrderDtos.Order order) {
        String codeLabel = order.orderCode != null ? order.orderCode : ("#" + order.orderId);
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận huỷ đơn " + codeLabel)
                .setMessage("Huỷ đơn hàng sẽ giải phóng số lượng vé đang giữ chỗ. Bạn chắc chắn muốn huỷ?")
                .setPositiveButton("Huỷ đơn", (dialog, which) -> {
                    OrderRepository repo = ServiceLocator.get(OrderRepository.class);
                    repo.cancelOrder(order.orderId, new ApiCallback<OrderDtos.Order>() {
                        @Override
                        public void onSuccess(OrderDtos.Order data, JsonElement meta) {
                            order.status = "CANCELLED";
                            applyFilter();
                            Toast.makeText(UserOrdersActivity.this, "✅ Đã huỷ đơn hàng " + codeLabel, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ApiException error) {
                            Toast.makeText(UserOrdersActivity.this, "❌ " + error.getUserMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void showRefundRequestDialog(OrderDtos.Order order) {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Nhập lý do yêu cầu hoàn tiền (ví dụ: bận việc đột xuất)...");
        etReason.setMinLines(3);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(etReason);

        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu hoàn tiền (FR-O25)")
                .setMessage("Đơn hàng: " + order.orderCode + "\nSố tiền: " +
                        (order.totalAmount != null ? com.eventapp.app.ui.user.util.UserFmt.amount(order.totalAmount.longValue()) : ""))
                .setView(container)
                .setPositiveButton("Gửi yêu cầu", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        reason = "Không thể tham dự sự kiện";
                    }
                    com.eventapp.app.data.repository.RefundRepository repo =
                            ServiceLocator.get(com.eventapp.app.data.repository.RefundRepository.class);
                    repo.requestRefund(order.orderId, order.orderCode, order.eventTitle,
                            order.totalAmount, reason,
                            new ApiCallback<com.eventapp.app.data.remote.dto.refund.RefundDtos.RefundResponse>() {
                                @Override
                                public void onSuccess(com.eventapp.app.data.remote.dto.refund.RefundDtos.RefundResponse data, JsonElement meta) {
                                    Toast.makeText(UserOrdersActivity.this,
                                            "✅ Đã gửi yêu cầu hoàn tiền thành công! Admin sẽ duyệt yêu cầu.",
                                            Toast.LENGTH_LONG).show();
                                    order.status = "REFUND_PENDING";
                                    applyFilter();
                                }

                                @Override
                                public void onError(@NonNull ApiException error) {
                                    Toast.makeText(UserOrdersActivity.this,
                                            "❌ " + error.getUserMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openTicketForOrder(OrderDtos.Order order) {
        String code = order.orderCode != null ? order.orderCode : ("ORD-" + order.orderId);
        TicketDetailActivity.start(this, code);
    }
}
