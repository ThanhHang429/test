package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityOrgEventDetailBinding;
import com.eventapp.app.databinding.ItemOrgTicketTypeBinding;
import com.eventapp.app.ui.organizer.data.OrgDemo;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.model.OrgTicketType;
import com.eventapp.app.ui.organizer.model.OrgTone;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Chi tiết sự kiện của nhà tổ chức: tổng quan, hạng vé và hành động vận hành. */
public class OrgEventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_CODE = "event_code";

    private ActivityOrgEventDetailBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        OrgEvent event = OrgDemo.findEvent(getIntent().getStringExtra(EXTRA_EVENT_CODE));
        bind(event);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShareEvent.setOnClickListener(v ->
                snack("Đã sao chép liên kết sự kiện"));
        binding.btnBroadcast.setOnClickListener(v ->
                snack("Soạn thông báo cho " + event.title));
        binding.btnManageTickets.setOnClickListener(v ->
                snack("Quản lý hạng vé · " + event.title));
        binding.btnCancelEvent.setOnClickListener(v -> confirmCancel(event));
    }

    private void bind(OrgEvent event) {
        binding.tvDetailCategory.setText(event.category);
        binding.tvDetailStatus.setText(event.statusLabel);
        OrgUi.applyPill(binding.tvDetailStatus, event.tone);
        binding.tvDetailTitle.setText(event.title);
        binding.tvDetailMeta.setText(event.meta);
        binding.tvDetailLocation.setText(event.location);

        binding.tvStatSold.setText(OrgFormat.count(event.sold));
        binding.tvStatRevenue.setText(event.revenue > 0
                ? OrgFormat.compactMoney(event.revenue) : "—");
        binding.tvStatCheckin.setText(OrgFormat.count(event.checkin));

        boolean draft = OrgEvent.FILTER_DRAFT.equals(event.filter);
        boolean pending = OrgEvent.FILTER_PENDING.equals(event.filter);
        if (draft) {
            binding.tvReviewStatus.setText("Bản nháp");
            OrgUi.applyPill(binding.tvReviewStatus, OrgTone.NEUTRAL);
        } else if (pending) {
            binding.tvReviewStatus.setText("Chờ duyệt");
            OrgUi.applyPill(binding.tvReviewStatus, OrgTone.WARNING);
        } else {
            binding.tvReviewStatus.setText("Đã duyệt");
            OrgUi.applyPill(binding.tvReviewStatus, OrgTone.SUCCESS);
        }

        if (OrgEvent.FILTER_ON_SALE.equals(event.filter)) {
            binding.tvLifecycleStatus.setText("Đang bán");
            OrgUi.applyPill(binding.tvLifecycleStatus, OrgTone.SUCCESS);
        } else if (OrgEvent.FILTER_ENDED.equals(event.filter)) {
            binding.tvLifecycleStatus.setText("Đã kết thúc");
            OrgUi.applyPill(binding.tvLifecycleStatus, OrgTone.NEUTRAL);
        } else {
            binding.tvLifecycleStatus.setText("Chưa mở bán");
            OrgUi.applyPill(binding.tvLifecycleStatus, OrgTone.NEUTRAL);
        }

        binding.tvCapacity.setText(OrgFormat.count(event.sold) + " / "
                + OrgFormat.count(event.total) + " vé");
        binding.progressCapacity.setProgressCompat(event.percent(), false);

        List<OrgTicketType> types = OrgDemo.ticketTypes(event);
        binding.tvTicketTypesSub.setText(types.size() + " hạng vé · "
                + OrgFormat.count(event.total) + " vé phát hành");
        bindTicketTypes(types);
    }

    private void bindTicketTypes(List<OrgTicketType> types) {
        binding.ticketTypeContainer.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < types.size(); i++) {
            OrgTicketType type = types.get(i);
            ItemOrgTicketTypeBinding row = ItemOrgTicketTypeBinding.inflate(
                    inflater, binding.ticketTypeContainer, false);
            row.tvTypeName.setText(type.name);
            row.tvTypePrice.setText(type.price > 0 ? OrgFormat.money(type.price) : "Miễn phí");
            row.tvTypeSold.setText(OrgFormat.count(type.sold) + " / "
                    + OrgFormat.count(type.total));
            row.tvTypeStatus.setText(type.statusLabel);
            OrgUi.applyPill(row.tvTypeStatus, type.tone);
            row.progressType.setProgressCompat(type.percent(), false);
            row.divider.setVisibility(i == types.size() - 1 ? View.GONE : View.VISIBLE);
            binding.ticketTypeContainer.addView(row.getRoot());
        }
    }

    private void confirmCancel(OrgEvent event) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.org_cancel_event)
                .setMessage("Yêu cầu hủy \"" + event.title + "\" sẽ được gửi tới ban quản trị.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Gửi yêu cầu", (dialog, which) ->
                        snack("Đã gửi yêu cầu hủy sự kiện"))
                .show();
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
