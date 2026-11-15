package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.promotion.PromotionDtos;
import com.eventapp.app.data.remote.dto.refund.RefundDtos;
import com.eventapp.app.data.remote.dto.report.ReportDtos;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeRequest;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeResponse;
import com.eventapp.app.data.repository.FinanceRepository;
import com.eventapp.app.data.repository.PromotionRepository;
import com.eventapp.app.data.repository.RefundRepository;
import com.eventapp.app.data.repository.TicketTypeRepository;
import com.eventapp.app.databinding.ActivityOrgEventDetailBinding;
import com.eventapp.app.databinding.ItemOrgTicketTypeBinding;
import com.eventapp.app.ui.common.AppDialog;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.eventapp.app.ui.organizer.viewmodel.OrgEventDetailViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonElement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Chi tiết sự kiện của nhà tổ chức: tổng quan, hạng vé và hành động vận hành. */
public class OrgEventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "event_id";

    private ActivityOrgEventDetailBinding binding;
    private OrgEventDetailViewModel viewModel;
    private OrgEvent event;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShareEvent.setOnClickListener(v -> snack(getString(R.string.org_copied)));
        binding.btnManageCoupons.setOnClickListener(v -> showCouponsDialog());
        binding.btnEventRefunds.setOnClickListener(v -> showEventRefundsDialog());
        binding.btnBroadcast.setOnClickListener(v -> {
            if (event != null) {
                OrgCrowdActivity.start(this, event.id, event.title);
            }
        });
        binding.btnManageTickets.setOnClickListener(v -> showAddTicketTypeDialog());
        binding.btnAddTicketType.setOnClickListener(v -> showAddTicketTypeDialog());
        binding.btnCancelEvent.setOnClickListener(v -> confirmCancel());

        viewModel = new ViewModelProvider(this).get(OrgEventDetailViewModel.class);
        observe();

        long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        if (eventId <= 0) {
            snack(getString(R.string.org_event_not_found));
            return;
        }
        viewModel.load(eventId);
        loadEventFinanceReport(eventId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (event != null && event.id > 0) {
            loadEventFinanceReport(event.id);
        }
    }

    private void observe() {
        viewModel.getEvent().observe(this, data -> {
            if (data == null || binding == null) {
                return;
            }
            event = data;
            bind(data);
        });
        viewModel.getError().observe(this, error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            snack(error.getUserMessage());
        });
    }

    private void bind(OrgEvent event) {
        binding.tvDetailCategory.setText(event.code);
        binding.tvDetailStatus.setText(event.statusLabel());
        OrgUi.applyPill(binding.tvDetailStatus, event.tone());
        binding.tvDetailTitle.setText(event.title);
        binding.tvDetailMeta.setText(OrgFormat.dateRange(event.startAt, event.endAt));
        binding.tvDetailLocation.setText(OrgFormat.location(event.venueName, event.city));

        binding.tvStatSold.setText(OrgFormat.count(event.ticketsSold));
        binding.tvStatRevenue.setText(event.revenue > 0
                ? OrgFormat.compactMoney(event.revenue) : "—");
        binding.tvStatCheckin.setText("—");

        binding.tvReviewStatus.setText(event.reviewLabel());
        OrgUi.applyPill(binding.tvReviewStatus, event.reviewTone());
        binding.tvLifecycleStatus.setText(event.lifecycleLabel());
        OrgUi.applyPill(binding.tvLifecycleStatus, event.lifecycleTone());

        binding.tvCapacity.setText(OrgFormat.count(event.ticketsSold) + " / "
                + OrgFormat.count(event.totalTickets()) + " vé");
        binding.progressCapacity.setProgressCompat(event.percent(), false);

        List<OrgEvent.TicketType> types = event.ticketTypes;
        binding.tvTicketTypesSub.setText(getString(R.string.org_ticket_types_count,
                types.size(), OrgFormat.count(calculateTotalIssuedTickets(event))));
        bindTicketTypes(types);
    }

    private void bindTicketTypes(List<OrgEvent.TicketType> types) {
        binding.ticketTypeContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < types.size(); i++) {
            OrgEvent.TicketType type = types.get(i);
            ItemOrgTicketTypeBinding row = ItemOrgTicketTypeBinding.inflate(
                    inflater, binding.ticketTypeContainer, false);
            row.tvTypeName.setText(type.name);
            row.tvTypePrice.setText(type.price > 0 ? OrgFormat.money(type.price)
                    : getString(R.string.org_ticket_free));
            row.tvTypeSold.setText(OrgFormat.count(type.sold) + " / "
                    + OrgFormat.count(type.total));
            row.tvTypeStatus.setText(type.statusLabel());
            OrgUi.applyPill(row.tvTypeStatus, type.tone());
            row.progressType.setProgressCompat(type.percent(), false);
            row.divider.setVisibility(i == types.size() - 1 ? View.GONE : View.VISIBLE);
            row.getRoot().setOnClickListener(v -> showTicketTypeDialog(type));
            binding.ticketTypeContainer.addView(row.getRoot());
        }
    }

    private void confirmCancel() {
        if (event == null) {
            return;
        }
        AppDialog.confirm(this,
                R.drawable.ic_ds_warning,
                getString(R.string.org_cancel_event),
                getString(R.string.org_cancel_confirm, event.title),
                getString(R.string.org_send_request),
                getString(R.string.org_close),
                AppDialog.Tone.DANGER,
                () -> snack(getString(R.string.org_cancel_sent)));
    }

    private void showAddTicketTypeDialog() {
        showTicketTypeDialog(null);
    }

    private void showTicketTypeDialog(@Nullable OrgEvent.TicketType existing) {
        if (event == null) {
            snack("Đang tải dữ liệu sự kiện...");
            return;
        }
        final boolean isEdit = existing != null;

        // Kiểm tra xem sự kiện đã đạt tối đa sức chứa chưa khi tạo mới (FR-O11)
        if (!isEdit && event.capacityLimit != null && event.capacityLimit > 0) {
            int currentIssued = calculateTotalIssuedTickets(event);
            if (currentIssued >= event.capacityLimit) {
                snack("Sự kiện đã phát hành đủ " + currentIssued + " / " + event.capacityLimit + " vé (hết sức chứa). Không thể tạo thêm loại vé!");
                return;
            }
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.dialog_org_add_ticket_type, null);
        dialog.setContentView(sheet);

        android.widget.TextView tvTitle = sheet.findViewById(R.id.tvTicketDialogTitle);
        EditText etName = sheet.findViewById(R.id.etTicketName);
        EditText etPrice = sheet.findViewById(R.id.etTicketPrice);
        EditText etTotalQty = sheet.findViewById(R.id.etTotalQuantity);
        EditText etMaxPerUser = sheet.findViewById(R.id.etMaxPerUser);
        EditText etDesc = sheet.findViewById(R.id.etTicketDesc);
        EditText etSaleStart = sheet.findViewById(R.id.etSaleStartAt);
        EditText etSaleEnd = sheet.findViewById(R.id.etSaleEndAt);
        View btnClose = sheet.findViewById(R.id.btnCloseDialog);
        com.google.android.material.button.MaterialButton btnSubmit = sheet.findViewById(R.id.btnSubmitTicketType);
        com.google.android.material.button.MaterialButton btnDelete = sheet.findViewById(R.id.btnDeleteTicketType);

        final String[] saleStartIso = {existing != null ? existing.saleStartAt : null};
        final String[] saleEndIso = {existing != null ? existing.saleEndAt : null};

        if (isEdit) {
            if (tvTitle != null) tvTitle.setText("Cập nhật loại vé");
            if (etName != null) etName.setText(existing.name);
            if (etPrice != null) etPrice.setText(String.valueOf(existing.price));
            if (etTotalQty != null) etTotalQty.setText(String.valueOf(existing.total));
            if (etSaleStart != null && existing.saleStartAt != null) {
                etSaleStart.setText(formatIsoDisplay(existing.saleStartAt));
            }
            if (etSaleEnd != null && existing.saleEndAt != null) {
                etSaleEnd.setText(formatIsoDisplay(existing.saleEndAt));
            }
            if (btnSubmit != null) btnSubmit.setText("Lưu thay đổi");
            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setTitle("Xoá loại vé \"" + existing.name + "\"?")
                        .setMessage("Chỉ có thể xoá loại vé chưa có vé nào được bán.")
                        .setPositiveButton("Xoá", (d, w) -> {
                            btnDelete.setEnabled(false);
                            ServiceLocator.get(TicketTypeRepository.class)
                                    .deleteTicketType(existing.id, new ApiCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(@Nullable Boolean data, @Nullable JsonElement meta) {
                                            dialog.dismiss();
                                            snack("Đã xoá loại vé '" + existing.name + "'");
                                            viewModel.load(event.id);
                                        }

                                        @Override
                                        public void onError(@NonNull ApiException exception) {
                                            btnDelete.setEnabled(true);
                                            snack(exception.getUserMessage());
                                        }
                                    });
                        })
                        .setNegativeButton("Huỷ", null)
                        .show());
            }
        }

        if (etSaleStart != null) {
            etSaleStart.setOnClickListener(v -> pickDateTime(etSaleStart, saleStartIso, "Thời gian mở bán"));
        }
        if (etSaleEnd != null) {
            etSaleEnd.setOnClickListener(v -> pickDateTime(etSaleEnd, saleEndIso, "Thời gian đóng bán"));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
                String priceStr = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
                String qtyStr = etTotalQty != null && etTotalQty.getText() != null ? etTotalQty.getText().toString().trim() : "";
                String maxStr = etMaxPerUser != null && etMaxPerUser.getText() != null ? etMaxPerUser.getText().toString().trim() : "";
                String desc = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    if (etName != null) {
                        etName.setError("Vui lòng nhập tên loại vé");
                        etName.requestFocus();
                    }
                    return;
                }

                if (priceStr.isEmpty()) {
                    if (etPrice != null) {
                        etPrice.setError("Vui lòng nhập giá vé (nhập 0 nếu vé miễn phí)");
                        etPrice.requestFocus();
                    }
                    return;
                }

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        if (etPrice != null) etPrice.setError("Giá vé không được là số âm");
                        return;
                    }
                } catch (Exception e) {
                    if (etPrice != null) etPrice.setError("Giá vé không hợp lệ");
                    return;
                }

                if (qtyStr.isEmpty()) {
                    if (etTotalQty != null) {
                        etTotalQty.setError("Vui lòng nhập tổng số lượng vé phát hành");
                        etTotalQty.requestFocus();
                    }
                    return;
                }

                int totalQty;
                try {
                    totalQty = Integer.parseInt(qtyStr);
                    if (totalQty <= 0) {
                        if (etTotalQty != null) etTotalQty.setError("Số lượng vé phải lớn hơn 0");
                        return;
                    }
                    if (isEdit && totalQty < existing.sold) {
                        if (etTotalQty != null) {
                            etTotalQty.setError("Không thể giảm dưới số vé đã bán (" + existing.sold + ")");
                            etTotalQty.requestFocus();
                        }
                        return;
                    }
                } catch (Exception e) {
                    if (etTotalQty != null) etTotalQty.setError("Số lượng vé không hợp lệ");
                    return;
                }

                // Kiểm tra không cho tạo/sửa vượt quá sức chứa sự kiện (FR-O11)
                if (event.capacityLimit != null && event.capacityLimit > 0) {
                    int currentIssued = calculateTotalIssuedTickets(event) - (isEdit ? existing.total : 0);
                    if (currentIssued + totalQty > event.capacityLimit) {
                        int remainingSlots = Math.max(0, event.capacityLimit - currentIssued);
                        if (etTotalQty != null) {
                            if (remainingSlots == 0) {
                                etTotalQty.setError("Sự kiện đã phát hành hết sức chứa (" + event.capacityLimit + " vé).");
                            } else {
                                etTotalQty.setError("Vượt quá sức chứa sự kiện (" + event.capacityLimit + " vé). Tối đa còn " + remainingSlots + " vé.");
                            }
                            etTotalQty.requestFocus();
                        }
                        return;
                    }
                }

                int maxPerUser = 5;
                if (!maxStr.isEmpty()) {
                    try {
                        maxPerUser = Integer.parseInt(maxStr);
                        if (maxPerUser <= 0) {
                            if (etMaxPerUser != null) etMaxPerUser.setError("Số vé tối đa mỗi khách phải > 0");
                            return;
                        }
                    } catch (Exception e) {
                        if (etMaxPerUser != null) etMaxPerUser.setError("Số lượng không hợp lệ");
                        return;
                    }
                }

                // Kiểm tra hợp lệ thời gian mở/đóng bán (FR-O12)
                if (saleStartIso[0] != null && saleEndIso[0] != null) {
                    try {
                        java.time.Instant st = java.time.Instant.parse(saleStartIso[0]);
                        java.time.Instant en = java.time.Instant.parse(saleEndIso[0]);
                        if (!en.isAfter(st)) {
                            snack("Thời gian đóng bán phải sau thời gian mở bán");
                            return;
                        }
                    } catch (Exception ignored) {}
                }

                TicketTypeRequest request = new TicketTypeRequest(
                        name,
                        desc.isEmpty() ? null : desc,
                        price,
                        totalQty,
                        1,
                        maxPerUser
                );
                request.salesStartAt = saleStartIso[0];
                request.salesEndAt = saleEndIso[0];

                btnSubmit.setEnabled(false);
                TicketTypeRepository repo = ServiceLocator.get(TicketTypeRepository.class);
                ApiCallback<TicketTypeResponse> cb = new ApiCallback<TicketTypeResponse>() {
                    @Override
                    public void onSuccess(@Nullable TicketTypeResponse data, @Nullable JsonElement meta) {
                        dialog.dismiss();
                        snack(isEdit
                                ? "Đã cập nhật loại vé '" + name + "'!"
                                : "Đã tạo loại vé '" + name + "' thành công!");
                        viewModel.load(event.id);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        btnSubmit.setEnabled(true);
                        snack(exception.getUserMessage());
                    }
                };
                if (isEdit) {
                    repo.updateTicketType(existing.id, request, cb);
                } else {
                    repo.createTicketType(event.id, request, cb);
                }
            });
        }

        dialog.show();
    }

    private void pickDateTime(@NonNull EditText target, @NonNull String[] isoHolder, @NonNull String label) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (isoHolder[0] != null) {
            try {
                cal.setTimeInMillis(java.time.Instant.parse(isoHolder[0]).toEpochMilli());
            } catch (Exception ignored) {}
        }
        android.app.DatePickerDialog dateDlg = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(java.util.Calendar.YEAR, year);
                    cal.set(java.util.Calendar.MONTH, month);
                    cal.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
                    new android.app.TimePickerDialog(
                            this,
                            (tp, hourOfDay, minute) -> {
                                cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                                cal.set(java.util.Calendar.MINUTE, minute);
                                cal.set(java.util.Calendar.SECOND, 0);
                                cal.set(java.util.Calendar.MILLISECOND, 0);
                                String iso = java.time.Instant.ofEpochMilli(cal.getTimeInMillis()).toString();
                                isoHolder[0] = iso;
                                target.setText(formatIsoDisplay(iso));
                            },
                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                            cal.get(java.util.Calendar.MINUTE),
                            true
                    ).show();
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
        );
        dateDlg.setTitle(label);
        dateDlg.setButton(android.content.DialogInterface.BUTTON_NEUTRAL, "Bỏ trống", (d, w) -> {
            isoHolder[0] = null;
            target.setText("");
        });
        dateDlg.show();
    }

    private static String formatIsoDisplay(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return "";
        try {
            long ms = java.time.Instant.parse(iso.trim()).toEpochMilli();
            return OrgFormat.dateTime(ms);
        } catch (Exception e) {
            return iso;
        }
    }

    private void loadEventFinanceReport(long eventId) {
        FinanceRepository repo = ServiceLocator.get(FinanceRepository.class);
        repo.getEventFinanceReport(eventId, new ApiCallback<ReportDtos.EventFinanceReport>() {
            @Override
            public void onSuccess(ReportDtos.EventFinanceReport report, JsonElement meta) {
                if (isFinishing() || isDestroyed() || binding == null || report == null) return;
                long netRev = report.netRevenue != null ? report.netRevenue.longValue() : 0L;
                long grossRev = report.grossRevenue != null ? report.grossRevenue.longValue() : 0L;
                long feeAmt = report.platformFeeAmount != null ? report.platformFeeAmount.longValue() : 0L;
                long discountAmt = report.discountTotal != null ? report.discountTotal.longValue() : 0L;
                long refundedAmt = report.refundedAmount != null ? report.refundedAmount.longValue() : 0L;

                binding.tvStatSold.setText(OrgFormat.count(report.ticketsSold));
                binding.tvStatRevenue.setText(netRev > 0 ? OrgFormat.compactMoney(netRev) : "0 đ");
                String checkinText = report.ticketsIssued > 0
                        ? (report.checkedInCount + "/" + report.ticketsIssued)
                        : OrgFormat.count(report.checkedInCount);
                binding.tvStatCheckin.setText(checkinText);

                binding.tvFinanceBreakdown.setVisibility(View.VISIBLE);
                binding.tvFinanceBreakdown.setText(String.format(Locale.US,
                        "Báo cáo tài chính (FR-O26): Doanh thu gộp %s · Giảm giá %s · Phí nền tảng (%.1f%%) %s · Đã hoàn %s · Thực nhận %s (%d đơn thành công, %d chờ hoàn)",
                        OrgFormat.money(grossRev),
                        OrgFormat.money(discountAmt),
                        report.platformFeeRate,
                        OrgFormat.money(feeAmt),
                        OrgFormat.money(refundedAmt),
                        OrgFormat.money(netRev),
                        report.paidOrdersCount,
                        report.pendingRefundsCount));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                // Giữ số liệu cơ bản từ OrgEvent nếu chưa tải được báo cáo tài chính
            }
        });
    }

    private void showCouponsDialog() {
        if (event == null) {
            snack("Đang tải dữ liệu sự kiện...");
            return;
        }
        PromotionRepository repo = ServiceLocator.get(PromotionRepository.class);
        repo.getEventCoupons(event.id, new ApiCallback<List<PromotionDtos.OrganizerCoupon>>() {
            @Override
            public void onSuccess(List<PromotionDtos.OrganizerCoupon> list, JsonElement meta) {
                if (isFinishing() || isDestroyed()) return;
                if (list == null || list.isEmpty()) {
                    new AlertDialog.Builder(OrgEventDetailActivity.this)
                            .setTitle("Mã giảm giá sự kiện (FR-O18)")
                            .setMessage("Sự kiện này chưa có mã giảm giá nào. Bạn có muốn tạo mã mới không?")
                            .setPositiveButton("+ Tạo mã mới", (d, w) -> showCreateCouponDialog())
                            .setNegativeButton("Đóng", null)
                            .show();
                    return;
                }

                String[] rows = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    PromotionDtos.OrganizerCoupon c = list.get(i);
                    String valLabel = "PERCENT".equalsIgnoreCase(c.discountType)
                            ? ((c.discountValue != null ? c.discountValue.stripTrailingZeros().toPlainString() : "0") + "%")
                            : OrgFormat.money(c.discountValue != null ? c.discountValue.longValue() : 0L);
                    String usageLabel = c.usageLimit != null
                            ? (c.usedCount + "/" + c.usageLimit + " lượt")
                            : (c.usedCount + " lượt");
                    rows[i] = c.code + " · Giảm " + valLabel + " [" + c.status + "]\n"
                            + (c.description != null && !c.description.isEmpty() ? c.description + " · " : "")
                            + usageLabel + " (Chạm để Bật/Tạm dừng)";
                }

                new AlertDialog.Builder(OrgEventDetailActivity.this)
                        .setTitle("Mã giảm giá (" + list.size() + ")")
                        .setItems(rows, (dialog, which) -> {
                            PromotionDtos.OrganizerCoupon selected = list.get(which);
                            String nextStatus = "ACTIVE".equalsIgnoreCase(selected.status) ? "PAUSED" : "ACTIVE";
                            repo.updateCouponStatus(selected.couponId, nextStatus,
                                    new ApiCallback<PromotionDtos.OrganizerCoupon>() {
                                        @Override
                                        public void onSuccess(PromotionDtos.OrganizerCoupon updated, JsonElement m) {
                                            snack("Đã chuyển mã " + selected.code + " sang " + nextStatus);
                                            showCouponsDialog();
                                        }

                                        @Override
                                        public void onError(@NonNull ApiException error) {
                                            snack("❌ " + error.getUserMessage());
                                        }
                                    });
                        })
                        .setPositiveButton("+ Tạo mã mới", (d, w) -> showCreateCouponDialog())
                        .setNegativeButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (!isFinishing()) snack("❌ " + error.getUserMessage());
            }
        });
    }

    private void showCreateCouponDialog() {
        if (event == null) return;
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        EditText etCode = new EditText(this);
        etCode.setHint("Mã giảm giá (VD: VIP20)");
        container.addView(etCode);

        EditText etDesc = new EditText(this);
        etDesc.setHint("Mô tả ngắn (VD: Giảm 20% vé VIP)");
        container.addView(etDesc);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbPercent = new RadioButton(this);
        rbPercent.setText("Phần trăm (%)");
        rbPercent.setId(View.generateViewId());
        RadioButton rbFixed = new RadioButton(this);
        rbFixed.setText("Số tiền (đ)");
        rbFixed.setId(View.generateViewId());
        rgType.addView(rbPercent);
        rgType.addView(rbFixed);
        rbPercent.setChecked(true);
        container.addView(rgType);

        EditText etValue = new EditText(this);
        etValue.setHint("Giá trị giảm (VD: 15 nếu 15%, hoặc 50000 nếu 50.000đ)");
        etValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(etValue);

        EditText etMinOrder = new EditText(this);
        etMinOrder.setHint("Đơn tối thiểu (đ, bỏ trống = 0)");
        etMinOrder.setInputType(InputType.TYPE_CLASS_NUMBER);
        container.addView(etMinOrder);

        EditText etMaxDiscount = new EditText(this);
        etMaxDiscount.setHint("Giảm tối đa (đ, tuỳ chọn)");
        etMaxDiscount.setInputType(InputType.TYPE_CLASS_NUMBER);
        container.addView(etMaxDiscount);

        EditText etLimit = new EditText(this);
        etLimit.setHint("Giới hạn lượt dùng (bỏ trống = không giới hạn)");
        etLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        container.addView(etLimit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Tạo mã giảm giá (FR-O18)")
                .setView(container)
                .setPositiveButton("Tạo mã", null)
                .setNegativeButton("Huỷ", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = etCode.getText().toString().trim().toUpperCase(Locale.ROOT);
            String valStr = etValue.getText().toString().trim();
            if (code.length() < 3) {
                etCode.setError("Mã cần ít nhất 3 ký tự");
                return;
            }
            if (valStr.isEmpty()) {
                etValue.setError("Vui lòng nhập giá trị giảm");
                return;
            }
            BigDecimal val;
            try {
                val = new BigDecimal(valStr);
                if (val.compareTo(BigDecimal.ZERO) <= 0) {
                    etValue.setError("Giá trị giảm phải > 0");
                    return;
                }
            } catch (Exception e) {
                etValue.setError("Số không hợp lệ");
                return;
            }

            PromotionDtos.CreateCouponRequest req = new PromotionDtos.CreateCouponRequest();
            req.code = code;
            req.description = etDesc.getText().toString().trim();
            req.discountType = rbPercent.isChecked() ? "PERCENT" : "FIXED";
            req.discountValue = val;
            String minStr = etMinOrder.getText().toString().trim();
            if (!minStr.isEmpty()) {
                try { req.minOrderAmount = new BigDecimal(minStr); } catch (Exception ignored) {}
            }
            String maxStr = etMaxDiscount.getText().toString().trim();
            if (!maxStr.isEmpty()) {
                try { req.maxDiscountAmount = new BigDecimal(maxStr); } catch (Exception ignored) {}
            }
            String limitStr = etLimit.getText().toString().trim();
            if (!limitStr.isEmpty()) {
                try { req.usageLimit = Integer.parseInt(limitStr); } catch (Exception ignored) {}
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            PromotionRepository repo = ServiceLocator.get(PromotionRepository.class);
            repo.createCoupon(event.id, req, new ApiCallback<PromotionDtos.OrganizerCoupon>() {
                @Override
                public void onSuccess(PromotionDtos.OrganizerCoupon created, JsonElement meta) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.dismiss();
                    snack("✅ Đã tạo mã giảm giá " + created.code);
                    showCouponsDialog();
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    if (isFinishing() || isDestroyed()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    snack("❌ " + error.getUserMessage());
                }
            });
        }));
        dialog.show();
    }

    private void showEventRefundsDialog() {
        if (event == null) {
            snack("Đang tải dữ liệu sự kiện...");
            return;
        }
        RefundRepository repo = ServiceLocator.get(RefundRepository.class);
        repo.getOrganizerRefunds(event.id, new ApiCallback<List<RefundDtos.RefundResponse>>() {
            @Override
            public void onSuccess(List<RefundDtos.RefundResponse> list, JsonElement meta) {
                if (isFinishing() || isDestroyed()) return;
                if (list == null || list.isEmpty()) {
                    new AlertDialog.Builder(OrgEventDetailActivity.this)
                            .setTitle("Yêu cầu hoàn tiền sự kiện")
                            .setMessage("Chưa có yêu cầu hoàn tiền nào cho sự kiện này.")
                            .setPositiveButton("Đóng", null)
                            .show();
                    return;
                }
                String[] items = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    RefundDtos.RefundResponse r = list.get(i);
                    long amt = r.amount != null ? r.amount.longValue() : 0L;
                    items[i] = (r.orderCode != null ? "#" + r.orderCode : "Đơn #" + r.orderId)
                            + " · " + OrgFormat.money(amt)
                            + " [" + (r.status != null ? r.status : "REQUESTED") + "]\n"
                            + (r.buyerName != null ? "Khách: " + r.buyerName + " · " : "")
                            + "Lý do: " + (r.reason != null ? r.reason : "—");
                }
                new AlertDialog.Builder(OrgEventDetailActivity.this)
                        .setTitle("Yêu cầu hoàn tiền (" + list.size() + ")")
                        .setItems(items, null)
                        .setPositiveButton("Đóng", null)
                        .show();
            }

            @Override
            public void onError(@NonNull ApiException error) {
                if (!isFinishing()) snack("❌ " + error.getUserMessage());
            }
        });
    }

    private int calculateTotalIssuedTickets(OrgEvent event) {
        if (event == null || event.ticketTypes == null) {
            return 0;
        }
        int total = 0;
        for (OrgEvent.TicketType t : event.ticketTypes) {
            total += t.total;
        }
        return total;
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
