package com.eventapp.app.ui.admin;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.data.remote.dto.refund.RefundDtos;
import com.eventapp.app.data.remote.dto.report.ReportDtos;
import com.eventapp.app.data.repository.FinanceRepository;
import com.eventapp.app.data.repository.RefundRepository;
import com.eventapp.app.databinding.FragmentAdminFinanceBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonElement;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Tab Tài chính Admin (FR-A10, FR-A11, FR-A12, FR-A16, FR-A17):
 * - Báo cáo tài chính toàn nền tảng, tổng GMV, phí thu được & đối soát nhà tổ chức từ Backend.
 * - Cấu hình tỷ lệ phí nền tảng (Platform fee rate).
 * - Tra cứu chi tiết đơn hàng & giao dịch.
 * - Xem nhật ký Webhook cổng thanh toán.
 * - Quản lý và xử lý duyệt hoàn tiền tự động vào Ví ETicket.
 */
public class AdminFinanceFragment extends Fragment {

    private FragmentAdminFinanceBinding binding;
    private FinanceRepository financeRepo;
    private RefundRepository refundRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminFinanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        financeRepo = ServiceLocator.get(FinanceRepository.class);
        refundRepo = ServiceLocator.get(RefundRepository.class);

        loadSummary();

        // FR-A10: Tra cứu đơn hàng / giao dịch
        binding.etFinanceLookup.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = binding.etFinanceLookup.getText() != null
                        ? binding.etFinanceLookup.getText().toString().trim()
                        : "";
                if (!query.isEmpty()) {
                    performOrderLookup(query);
                }
                return true;
            }
            return false;
        });

        // FR-A16: Cấu hình tỷ lệ phí nền tảng
        binding.btnEditFee.setOnClickListener(v -> showEditFeeDialog());

        // FR-A12: Xử lý duyệt / từ chối hoàn tiền
        binding.btnOpenRefunds.setOnClickListener(v -> showRefundManagementDialog());

        // FR-A11: Xem nhật ký Webhook thanh toán
        binding.btnViewWebhooks.setOnClickListener(v -> showWebhookLogsDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (financeRepo != null) {
            loadSummary();
        }
    }

    private void loadSummary() {
        financeRepo.getAdminFinanceSummary(new ApiCallback<ReportDtos.AdminFinanceSummary>() {
            @Override
            public void onSuccess(ReportDtos.AdminFinanceSummary summary, JsonElement meta) {
                if (!isAdded() || binding == null || summary == null) {
                    return;
                }
                bindSummary(summary);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                if (!isAdded() || binding == null) return;
                binding.tvFeeMeta.setText("Không thể tải báo cáo tài chính: " + exception.getUserMessage());
                binding.tvWebhookStatusValue.setText("Chưa kết nối");
                binding.tvWebhookStatusValue.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.org_danger_text));
            }
        });
    }

    private void bindSummary(@NonNull ReportDtos.AdminFinanceSummary summary) {
        BigDecimal collected = summary.platformFeeCollected != null ? summary.platformFeeCollected : BigDecimal.ZERO;
        BigDecimal outstanding = summary.platformFeeOutstanding != null ? summary.platformFeeOutstanding : BigDecimal.ZERO;
        BigDecimal gmv = summary.totalGmv != null ? summary.totalGmv : BigDecimal.ZERO;

        binding.tvFeeValue.setText(formatMoney(collected.add(outstanding)));
        binding.tvCollectedValue.setText(formatMoney(collected));
        binding.tvOutstandingValue.setText(formatMoney(outstanding));
        binding.tvFeeMeta.setText(String.format(Locale.US,
                "%d/%d đơn đã thanh toán · %d nhà tổ chức",
                summary.paidOrders, summary.totalOrders, summary.totalOrganizers));
        binding.tvPlatformFeeRate.setText(String.format(Locale.US, "%.1f%%", summary.platformFeeRate));
        binding.tvFeeCycleValue.setText(
                summary.settlementCycle != null && !summary.settlementCycle.trim().isEmpty()
                        ? summary.settlementCycle.trim() : "Hàng tháng ngày 05");

        binding.tvGmvValue.setText(formatMoney(gmv));
        binding.tvGmvEventsValue.setText(String.valueOf(summary.totalEvents));
        binding.tvGmvOrgsValue.setText(String.valueOf(summary.totalOrganizers));
        binding.tvWebhookStatusValue.setText("Đang hoạt động");
        binding.tvWebhookStatusValue.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.admin_success_text));

        renderReconciliations(summary.reconciliations);
    }

    private void renderReconciliations(@Nullable List<ReportDtos.OrganizerReconItem> list) {
        if (binding == null || !isAdded()) return;
        binding.reconContainer.removeAllViews();
        if (list == null || list.isEmpty()) {
            binding.tvReconEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvReconEmpty.setVisibility(View.GONE);
        Context ctx = requireContext();
        float d = getResources().getDisplayMetrics().density;

        for (int i = 0; i < list.size(); i++) {
            ReportDtos.OrganizerReconItem item = list.get(i);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (10 * d), 0, (int) (10 * d));

            LinearLayout infoCol = new LinearLayout(ctx);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvOrg = new TextView(ctx);
            tvOrg.setText(item.organizerName != null && !item.organizerName.isEmpty()
                    ? item.organizerName : ("Nhà tổ chức #" + item.organizerId));
            tvOrg.setTextColor(ContextCompat.getColor(ctx, R.color.admin_text));
            tvOrg.setTextSize(14f);
            tvOrg.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvMeta = new TextView(ctx);
            tvMeta.setText("GMV " + formatMoney(item.gmv) + " · phí " + formatMoney(item.fee));
            tvMeta.setTextColor(ContextCompat.getColor(ctx, R.color.admin_text_dim));
            tvMeta.setTextSize(12f);

            infoCol.addView(tvOrg);
            infoCol.addView(tvMeta);

            boolean isPaid = "PAID".equalsIgnoreCase(item.status);
            TextView tvStatus = new TextView(ctx);
            tvStatus.setText(isPaid ? "ĐÃ THU" : "CHỜ THU");
            tvStatus.setTextSize(10f);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding((int) (8 * d), (int) (4 * d), (int) (8 * d), (int) (4 * d));
            tvStatus.setBackgroundResource(isPaid ? R.drawable.bg_admin_pill_success : R.drawable.bg_admin_pill_warning);
            tvStatus.setTextColor(ContextCompat.getColor(ctx,
                    isPaid ? R.color.admin_success_text : R.color.admin_warning_text));

            row.addView(infoCol);
            row.addView(tvStatus);
            binding.reconContainer.addView(row);

            if (i < list.size() - 1) {
                View divider = new View(ctx);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) Math.max(1, d)));
                divider.setBackgroundColor(ContextCompat.getColor(ctx, R.color.admin_divider));
                binding.reconContainer.addView(divider);
            }
        }
    }

    /** FR-A16: Hộp thoại chỉnh sửa tỷ lệ phí nền tảng có phiên bản & thời điểm áp dụng */
    private void showEditFeeDialog() {
        Context context = getContext();
        if (context == null) return;

        float d = getResources().getDisplayMetrics().density;
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding((int) (20 * d), (int) (10 * d), (int) (20 * d), 0);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Tỷ lệ % mới (ví dụ 5.0)");
        container.addView(input);

        final EditText etNote = new EditText(context);
        etNote.setHint("Ghi chú chính sách (tuỳ chọn)");
        container.addView(etNote);

        final EditText etEffective = new EditText(context);
        etEffective.setHint("Áp dụng từ (bỏ trống = áp dụng ngay)");
        etEffective.setFocusable(false);
        etEffective.setClickable(true);
        final String[] selectedEffectiveIso = {null};
        etEffective.setOnClickListener(v -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            new android.app.DatePickerDialog(context, (dp, y, m, day) -> {
                cal.set(java.util.Calendar.YEAR, y);
                cal.set(java.util.Calendar.MONTH, m);
                cal.set(java.util.Calendar.DAY_OF_MONTH, day);
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                selectedEffectiveIso[0] = java.time.Instant.ofEpochMilli(cal.getTimeInMillis()).toString();
                etEffective.setText(String.format(Locale.US, "%02d/%02d/%04d 00:00", day, m + 1, y));
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });
        container.addView(etEffective);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Cấu hình phí nền tảng (FR-A16)")
                .setMessage("Tạo phiên bản chính sách phí mới (có thời điểm hiệu lực) cho đơn hàng mới:")
                .setView(container)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    String note = etNote.getText().toString().trim();
                    if (!text.isEmpty()) {
                        try {
                            double newRate = Double.parseDouble(text);
                            if (newRate >= 0 && newRate <= 30) {
                                financeRepo.updateFeeRate(
                                        newRate,
                                        selectedEffectiveIso[0],
                                        note.isEmpty() ? null : note,
                                        new ApiCallback<ReportDtos.AdminFinanceSummary>() {
                                            @Override
                                            public void onSuccess(ReportDtos.AdminFinanceSummary data, JsonElement meta) {
                                                if (binding != null) {
                                                    if (data != null) {
                                                        bindSummary(data);
                                                    } else {
                                                        binding.tvPlatformFeeRate.setText(String.format(Locale.US, "%.1f%%", newRate));
                                                    }
                                                    Snackbar.make(binding.getRoot(), "Đã lưu phiên bản tỷ lệ phí: " + newRate + "%", Snackbar.LENGTH_SHORT).show();
                                                }
                                            }

                                            @Override
                                            public void onError(@NonNull ApiException exception) {
                                                Toast.makeText(context, "Không thể cập nhật phí: " + exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(context, "Tỷ lệ phí phải từ 0% đến 30%", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /** FR-A10: Tra cứu đơn hàng và giao dịch */
    private void performOrderLookup(String query) {
        Context context = getContext();
        if (context == null) return;

        financeRepo.lookupOrder(query, new ApiCallback<OrderDtos.Order>() {
            @Override
            public void onSuccess(OrderDtos.Order order, JsonElement meta) {
                if (!isAdded() || getContext() == null) return;

                if (order != null) {
                    showOrderDetailDialog(order);
                } else {
                    Snackbar.make(binding.getRoot(), "Không tìm thấy đơn hàng: " + query, Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                if (isAdded() && binding != null) {
                    Snackbar.make(binding.getRoot(), exception.getUserMessage(), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showOrderDetailDialog(OrderDtos.Order order) {
        Context context = getContext();
        if (context == null) return;

        StringBuilder details = new StringBuilder();
        details.append("• Mã đơn: ").append(order.orderCode != null ? order.orderCode : "ORD-" + order.orderId).append("\n")
                .append("• Sự kiện: ").append(order.eventTitle != null ? order.eventTitle : "Vé sự kiện").append("\n")
                .append("• Người mua: ").append(order.buyerName != null ? order.buyerName : "—")
                .append(order.buyerEmail != null ? " (" + order.buyerEmail + ")" : "").append("\n")
                .append("• Tạm tính: ").append(formatMoney(order.subtotalAmount)).append("\n")
                .append("• Giảm giá: ").append(formatMoney(order.discountAmount)).append("\n")
                .append("• Phí nền tảng: ").append(formatMoney(order.platformFeeAmount)).append("\n")
                .append("• Tổng tiền: ").append(formatMoney(order.totalAmount != null ? order.totalAmount : BigDecimal.ZERO)).append("\n")
                .append("• Trạng thái đơn: ").append(order.status != null ? order.status : "—").append("\n")
                .append("• Trạng thái thanh toán: ").append(order.paymentStatus != null ? order.paymentStatus : "—").append("\n")
                .append("• Phương thức: ").append(order.paymentMethod != null ? order.paymentMethod : "Chưa thanh toán").append("\n")
                .append("• Thời gian tạo: ").append(order.createdAt != null ? order.createdAt : "—");

        if (order.payments != null && !order.payments.isEmpty()) {
            details.append("\n\nLịch sử giao dịch (" + order.payments.size() + "):");
            for (OrderDtos.PaymentAttempt p : order.payments) {
                details.append("\n- ").append(p.paymentReference != null ? p.paymentReference : ("#" + p.paymentId))
                        .append(" · ").append(p.method != null ? p.method : "—")
                        .append(" · [").append(p.status != null ? p.status : "—").append("]")
                        .append(" · ").append(formatMoney(p.amount));
                if (p.verificationSource != null && !p.verificationSource.isEmpty()) {
                    details.append(" (").append(p.verificationSource).append(")");
                }
                if (p.failureMessage != null && !p.failureMessage.isEmpty()) {
                    details.append("\n  Lỗi: ").append(p.failureMessage);
                }
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Chi tiết đơn hàng & giao dịch (FR-A10)")
                .setMessage(details.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    /** FR-A11: Nhật ký Webhook thanh toán */
    private void showWebhookLogsDialog() {
        Context context = getContext();
        if (context == null) return;

        financeRepo.getWebhookLogs(20, new ApiCallback<List<ReportDtos.WebhookLogItem>>() {
            @Override
            public void onSuccess(List<ReportDtos.WebhookLogItem> list, JsonElement meta) {
                if (!isAdded() || getContext() == null) return;
                if (list == null || list.isEmpty()) {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Nhật ký Webhook (FR-A11)")
                            .setMessage("Chưa ghi nhận webhook thanh toán nào.")
                            .setPositiveButton("Đóng", null)
                            .show();
                    return;
                }
                String[] items = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    ReportDtos.WebhookLogItem log = list.get(i);
                    items[i] = "[" + (log.providerCode != null ? log.providerCode : "BANK") + " · " + log.status + "] "
                            + (log.transferContent != null ? log.transferContent : log.externalTransactionId)
                            + " · " + formatMoney(log.amount) + "\n"
                            + (log.errorMessage != null && !log.errorMessage.isEmpty() ? log.errorMessage + " · " : "")
                            + (log.receivedAt != null ? log.receivedAt : "");
                }
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Nhật ký Webhook gần nhất (" + list.size() + ")")
                        .setItems(items, null)
                        .setPositiveButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (isAdded()) {
                    Toast.makeText(context, "Không thể tải nhật ký Webhook: " + error.getUserMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /** FR-A12: Quản lý & duyệt hoàn tiền */
    private void showRefundManagementDialog() {
        Context context = getContext();
        if (context == null) return;

        refundRepo.getRefunds(new ApiCallback<List<RefundDtos.RefundResponse>>() {
            @Override
            public void onSuccess(List<RefundDtos.RefundResponse> list, JsonElement meta) {
                if (!isAdded() || getContext() == null) return;

                if (list == null || list.isEmpty()) {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Yêu cầu hoàn tiền (FR-A12)")
                            .setMessage("Hiện không có yêu cầu hoàn tiền nào.")
                            .setPositiveButton("Đóng", null)
                            .show();
                    return;
                }

                String[] items = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    RefundDtos.RefundResponse r = list.get(i);
                    items[i] = (r.orderCode != null ? r.orderCode : "Đơn #" + r.orderId)
                            + " - " + formatMoney(r.amount != null ? r.amount : BigDecimal.ZERO)
                            + " [" + (r.status != null ? r.status : "REQUESTED") + "]\n"
                            + (r.buyerName != null ? "Khách: " + r.buyerName + " · " : "")
                            + "Lý do: " + (r.reason != null ? r.reason : "Khách yêu cầu");
                }

                new MaterialAlertDialogBuilder(context)
                        .setTitle("Danh sách yêu cầu hoàn tiền (" + list.size() + ")")
                        .setItems(items, (dialog, which) -> {
                            RefundDtos.RefundResponse selected = list.get(which);
                            showProcessRefundDialog(selected);
                        })
                        .setNegativeButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                if (isAdded()) {
                    Toast.makeText(context, "Không thể tải danh sách hoàn tiền: " + exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProcessRefundDialog(RefundDtos.RefundResponse refund) {
        Context context = getContext();
        if (context == null) return;

        long refId = refund.refundId != null ? refund.refundId : 0L;
        String status = refund.status != null ? refund.status.toUpperCase(Locale.ROOT) : "REQUESTED";
        String msg = "Mã đơn hàng: " + (refund.orderCode != null ? refund.orderCode : "Đơn #" + refund.orderId) + "\n"
                + (refund.eventTitle != null ? "Sự kiện: " + refund.eventTitle + "\n" : "")
                + (refund.buyerName != null ? "Người nhận hoàn: " + refund.buyerName + "\n" : "")
                + "Số tiền hoàn: " + formatMoney(refund.amount != null ? refund.amount : BigDecimal.ZERO) + "\n"
                + "Lý do yêu cầu: " + (refund.reason != null ? refund.reason : "Không rõ") + "\n"
                + "Trạng thái: " + status
                + (refund.rejectReason != null && !refund.rejectReason.isEmpty()
                ? "\nGhi chú xử lý: " + refund.rejectReason : "");

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle("Xử lý hoàn tiền (FR-A12)")
                .setMessage(msg)
                .setNeutralButton("Đóng", null);

        if (("REQUESTED".equals(status) || "PROCESSING".equals(status) || "PENDING".equals(status)) && refId > 0) {
            builder.setPositiveButton("Duyệt hoàn vào ví", (dialog, which) ->
                    refundRepo.processRefund(refId, true, null, new ApiCallback<RefundDtos.RefundResponse>() {
                        @Override
                        public void onSuccess(RefundDtos.RefundResponse data, JsonElement meta) {
                            if (isAdded() && binding != null) {
                                String balanceInfo = data != null && data.walletBalanceAfter != null
                                        ? " (Số dư ví khách sau hoàn: " + formatMoney(data.walletBalanceAfter) + ")"
                                        : "";
                                Snackbar.make(binding.getRoot(),
                                        "Đã duyệt hoàn tiền thành công vào Ví ETicket!" + balanceInfo,
                                        Snackbar.LENGTH_LONG).show();
                                loadSummary();
                            }
                        }

                        @Override
                        public void onError(@NonNull ApiException exception) {
                            if (isAdded()) {
                                Toast.makeText(context, "Lỗi: " + exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }));
            builder.setNegativeButton("Từ chối", (dialog, which) ->
                    refundRepo.processRefund(refId, false, "Chính sách sự kiện không hỗ trợ hoàn hủy",
                            new ApiCallback<RefundDtos.RefundResponse>() {
                                @Override
                                public void onSuccess(RefundDtos.RefundResponse data, JsonElement meta) {
                                    if (isAdded() && binding != null) {
                                        Snackbar.make(binding.getRoot(), "Đã từ chối yêu cầu hoàn tiền.", Snackbar.LENGTH_SHORT).show();
                                        loadSummary();
                                    }
                                }

                                @Override
                                public void onError(@NonNull ApiException exception) {
                                    if (isAdded()) {
                                        Toast.makeText(context, "Lỗi: " + exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }));
        }
        builder.show();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount) + " đ";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
