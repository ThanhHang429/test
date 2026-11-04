package com.eventapp.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminEventsBinding;
import com.eventapp.app.databinding.ItemAdminEventBinding;
import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminEventItem;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

/** Tab Sự kiện: tìm kiếm, lọc trạng thái và giám sát sức chứa/doanh thu. */
public class AdminEventsFragment extends Fragment {

    private static final DecimalFormat COUNT = new DecimalFormat("#,###");

    private FragmentAdminEventsBinding binding;
    private List<AdminEventItem> events;
    private int filterTone = -1;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        events = AdminDemo.events();

        binding.chipEventAll.setOnClickListener(v -> selectFilter(-1));
        binding.chipEventLive.setOnClickListener(v -> selectFilter(AdminEventItem.TONE_LIVE));
        binding.chipEventFlagged.setOnClickListener(v -> selectFilter(AdminEventItem.TONE_FLAGGED));
        binding.chipEventPaused.setOnClickListener(v -> selectFilter(AdminEventItem.TONE_PAUSED));

        binding.etEventSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                render();
            }
        });

        selectFilter(-1);
    }

    private void selectFilter(int tone) {
        filterTone = tone;
        setChipSelected(binding.chipEventAll, tone == -1);
        setChipSelected(binding.chipEventLive, tone == AdminEventItem.TONE_LIVE);
        setChipSelected(binding.chipEventFlagged, tone == AdminEventItem.TONE_FLAGGED);
        setChipSelected(binding.chipEventPaused, tone == AdminEventItem.TONE_PAUSED);
        render();
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setSelected(selected);
    }

    private void render() {
        binding.listEvents.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (AdminEventItem event : events) {
            if (filterTone != -1 && event.tone != filterTone) {
                continue;
            }
            if (!query.isEmpty()
                    && !event.title.toLowerCase(Locale.ROOT).contains(query)
                    && !event.organizer.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            ItemAdminEventBinding item =
                    ItemAdminEventBinding.inflate(inflater, binding.listEvents, false);
            bindItem(item, event);
            binding.listEvents.addView(item.getRoot());
        }
    }

    private void bindItem(ItemAdminEventBinding item, AdminEventItem event) {
        item.coverEvent.setBackgroundResource(event.coverRes);
        item.tvEventCategory.setText(event.category);
        item.tvEventTitle.setText(event.title);
        item.tvEventOrg.setText(event.organizer);
        item.tvEventCapacity.setText(formatCount(event.sold) + " / "
                + formatCount(event.total) + " vé");
        item.tvEventCapacityPercent.setText(event.percent() + "%");
        item.pbEventCapacity.setProgress(event.percent());
        item.tvEventSold.setText(formatCount(event.sold));
        item.tvEventRevenue.setText(event.revenue);
        item.tvEventCheckin.setText(event.checkin);

        switch (event.tone) {
            case AdminEventItem.TONE_FLAGGED:
                item.tvEventStatus.setText(R.string.admin_events_flagged_pill);
                item.tvEventStatus.setBackgroundResource(R.drawable.bg_admin_pill_danger);
                item.tvEventStatus.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                break;
            case AdminEventItem.TONE_PAUSED:
                item.tvEventStatus.setText(R.string.admin_events_paused_pill);
                item.tvEventStatus.setBackgroundResource(R.drawable.bg_admin_pill_neutral);
                item.tvEventStatus.setTextColor(
                        requireContext().getColor(R.color.admin_neutral_text));
                break;
            case AdminEventItem.TONE_LIVE:
            default:
                item.tvEventStatus.setText(R.string.admin_events_live);
                item.tvEventStatus.setBackgroundResource(R.drawable.bg_admin_pill_success);
                item.tvEventStatus.setTextColor(
                        requireContext().getColor(R.color.admin_success_text));
                break;
        }

        if (event.risk.isEmpty()) {
            item.tvEventRisk.setText(R.string.admin_events_no_risk);
            item.tvEventRisk.setTextColor(requireContext().getColor(R.color.admin_success_text));
        } else {
            item.tvEventRisk.setText(event.risk);
            item.tvEventRisk.setTextColor(requireContext().getColor(
                    event.tone == AdminEventItem.TONE_PAUSED
                            ? R.color.admin_text_dim : R.color.admin_warning_text));
        }

        item.getRoot().setOnClickListener(v -> openDetail());
        item.btnEventDetail.setOnClickListener(v -> openDetail());
    }

    private String formatCount(int value) {
        return COUNT.format(value).replace(',', '.');
    }

    private void openDetail() {
        startActivity(new Intent(requireContext(), AdminEventDetailActivity.class));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
