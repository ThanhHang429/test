package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityOrgEventDetailBinding;
import com.eventapp.app.databinding.ItemOrgTicketTypeBinding;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.eventapp.app.ui.organizer.viewmodel.OrgEventDetailViewModel;
import com.eventapp.app.ui.common.AppDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

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
        binding.btnBroadcast.setOnClickListener(v -> {
            if (event != null) {
                OrgCrowdActivity.start(this, event.id, event.title);
            }
        });
        binding.btnManageTickets.setOnClickListener(v -> {
            if (event != null) {
                snack(getString(R.string.org_manage_tickets_soon, event.title));
            }
        });
        binding.btnCancelEvent.setOnClickListener(v -> confirmCancel());

        viewModel = new ViewModelProvider(this).get(OrgEventDetailViewModel.class);
        observe();

        long eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, 0L);
        if (eventId <= 0) {
            snack(getString(R.string.org_event_not_found));
            return;
        }
        viewModel.load(eventId);
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
                types.size(), OrgFormat.count(event.totalTickets())));
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

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
