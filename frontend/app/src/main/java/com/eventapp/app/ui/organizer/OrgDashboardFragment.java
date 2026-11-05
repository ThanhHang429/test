package com.eventapp.app.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.data.remote.dto.dashboard.DashboardDtos;
import com.eventapp.app.data.remote.dto.organizer.OrganizerEventDtos;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.eventapp.app.databinding.FragmentOrgDashboardBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgEventMapper;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Tab Tổng quan của nhà tổ chức — dữ liệu từ FR-O03. */
public class OrgDashboardFragment extends Fragment {

    private FragmentOrgDashboardBinding binding;
    private OrgEvent featured;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        binding.tvGreetingName.setText(greetingName());
        binding.cardFeaturedEvent.setOnClickListener(v -> {
            if (featured != null) {
                openEvent(featured.id);
            }
        });
        binding.tvSeeAllEvents.setOnClickListener(v -> {
            if (requireActivity() instanceof com.eventapp.app.ui.host.OrganizerHostActivity) {
                ((com.eventapp.app.ui.host.OrganizerHostActivity) requireActivity())
                        .openTab(OrgBottomNavView.TAB_EVENTS);
            }
        });
        binding.rowTodoReview.setOnClickListener(v -> snack(getString(R.string.org_soon)));
        binding.rowTodoRefund.setOnClickListener(v -> snack(getString(R.string.org_soon)));

        loadDashboard();
        loadFeatured();
    }

    private void loadDashboard() {
        ServiceLocator.get(OrganizerRepository.class).dashboard("30d", null, null,
                new ApiCallback<DashboardDtos.Dashboard>() {
                    @Override
                    public void onSuccess(@Nullable DashboardDtos.Dashboard data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null || data == null) {
                            return;
                        }
                        bind(data);
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding != null) {
                            snack(error.getUserMessage());
                        }
                    }
                });
    }

    private void bind(DashboardDtos.Dashboard data) {
        long onSale = countOf(data.events, "ON_SALE");
        long pending = countOf(data.events, "PENDING_REVIEW");
        long sold = data.registrations == null ? 0 : data.registrations.ticketsIssued;
        long confirmed = data.registrations == null ? 0 : data.registrations.confirmed;
        long checkedIn = data.checkin == null ? 0 : data.checkin.checkedIn;
        int rate = data.checkin == null ? 0 : Math.round((float) data.checkin.checkinRate * 100f);
        long revenue = data.finance == null ? 0L : Math.round(data.finance.verifiedAmount);

        binding.tvRevenueTrend.setVisibility(View.GONE);
        binding.tvRevenueValue.setText(OrgFormat.compactMoney(revenue));
        binding.tvTicketsSold.setText(OrgFormat.count(sold));
        binding.tvCheckinCount.setText(OrgFormat.count(checkedIn));
        binding.tvKpiEvents.setText(OrgFormat.count(onSale));
        binding.tvKpiTickets.setText(OrgFormat.count(sold));
        binding.tvKpiCheckin.setText(OrgFormat.count(checkedIn));
        binding.tvKpiPending.setText(OrgFormat.count(pending));
        binding.tvKpiEventsNote.setText(getString(R.string.org_kpi_events_note_dynamic, pending));
        binding.tvKpiTicketsNote.setText(getString(R.string.org_kpi_tickets_note_dynamic, confirmed));
        binding.tvKpiCheckinNote.setText(getString(R.string.org_kpi_checkin_note_dynamic, rate));
        binding.tvKpiPendingNote.setText(R.string.org_kpi_pending_note_dynamic);
        binding.tvGreetingSub.setText(getString(R.string.org_greeting_sub_dynamic,
                onSale, pending));

        List<DashboardDtos.Task> tasks = data.tasks;
        binding.tvTodoCount.setText(String.valueOf(tasks == null ? 0 : tasks.size()));
        bindTask(binding.tvTodo1Title, binding.tvTodo1Sub, binding.rowTodoReview, tasks, 0);
        bindTask(binding.tvTodo2Title, binding.tvTodo2Sub, binding.rowTodoRefund, tasks, 1);
    }

    private void bindTask(android.widget.TextView title, android.widget.TextView sub,
                          View row, List<DashboardDtos.Task> tasks, int index) {
        if (tasks == null || tasks.size() <= index) {
            row.setVisibility(View.GONE);
            return;
        }
        DashboardDtos.Task task = tasks.get(index);
        row.setVisibility(View.VISIBLE);
        title.setText(task.title);
        sub.setText(task.detail);
    }

    private void loadFeatured() {
        ServiceLocator.get(OrganizerRepository.class).events(OrgEvent.FILTER_ON_SALE, 0, 1,
                new ApiCallback<List<OrganizerEventDtos.Event>>() {
                    @Override
                    public void onSuccess(@Nullable List<OrganizerEventDtos.Event> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null) {
                            return;
                        }
                        if (data == null || data.isEmpty()) {
                            binding.cardFeaturedEvent.setVisibility(View.GONE);
                            return;
                        }
                        binding.cardFeaturedEvent.setVisibility(View.VISIBLE);
                        featured = OrgEventMapper.from(data.get(0));
                        bindFeatured(featured);
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        // Khối sự kiện nổi bật không chặn phần còn lại của dashboard.
                        if (binding != null) {
                            binding.cardFeaturedEvent.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void bindFeatured(OrgEvent event) {
        binding.tvFeaturedCategory.setText(event.code);
        binding.tvFeaturedStatus.setText(event.statusLabel());
        binding.tvFeaturedTitle.setText(event.title);
        binding.tvFeaturedMeta.setText(OrgFormat.dateRange(event.startAt, event.endAt));
        binding.tvFeaturedSold.setText(OrgFormat.count(event.ticketsSold) + " / "
                + OrgFormat.count(event.totalTickets()) + " vé");
        binding.tvFeaturedPercent.setText(event.percent() + "%");
        binding.tvFeaturedRevenue.setText(event.revenue > 0
                ? OrgFormat.compactMoney(event.revenue) : "—");
        binding.tvFeaturedCheckin.setText("—");
    }

    private long countOf(DashboardDtos.EventStatusBlock block, String key) {
        if (block == null || block.counts == null) {
            return 0;
        }
        Long value = block.counts.get(key);
        return value == null ? 0 : value;
    }

    private String greetingName() {
        String name = SessionManager.get().getSession() == null
                ? null
                : SessionManager.get().getSession().fullName;
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.org_greeting_fallback);
        }
        String[] parts = name.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void openEvent(long eventId) {
        Intent intent = new Intent(requireContext(), OrgEventDetailActivity.class);
        intent.putExtra(OrgEventDetailActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
