package com.eventapp.app.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.databinding.FragmentOrgDashboardBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.model.OrgDashboardUiModel;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.viewmodel.OrgDashboardViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Tab Tổng quan của nhà tổ chức — dữ liệu từ FR-O03. */
public class OrgDashboardFragment extends Fragment {

    private FragmentOrgDashboardBinding binding;
    private OrgDashboardViewModel viewModel;
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

        viewModel = new ViewModelProvider(this).get(OrgDashboardViewModel.class);
        observe();
        viewModel.load();
    }

    private void observe() {
        viewModel.getDashboard().observe(getViewLifecycleOwner(), data -> {
            if (data != null && binding != null) {
                bind(data);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            snack(error.getUserMessage());
        });
        viewModel.getFeatured().observe(getViewLifecycleOwner(), data -> {
            if (binding == null) {
                return;
            }
            if (data == null) {
                featured = null;
                binding.cardFeaturedEvent.setVisibility(View.GONE);
                return;
            }
            featured = data;
            binding.cardFeaturedEvent.setVisibility(View.VISIBLE);
            bindFeatured(data);
        });
    }

    private void bind(OrgDashboardUiModel data) {
        long onSale = data.onSaleCount;
        long pending = data.pendingReviewCount;
        long sold = data.ticketsIssued;
        long confirmed = data.confirmedRegistrations;
        long checkedIn = data.checkedIn;
        int rate = data.checkinRatePercent;
        long revenue = data.verifiedRevenue;

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

        List<OrgDashboardUiModel.Task> tasks = data.tasks;
        binding.tvTodoCount.setText(String.valueOf(tasks.size()));
        bindTask(binding.tvTodo1Title, binding.tvTodo1Sub, binding.rowTodoReview, tasks, 0);
        bindTask(binding.tvTodo2Title, binding.tvTodo2Sub, binding.rowTodoRefund, tasks, 1);
    }

    private void bindTask(android.widget.TextView title, android.widget.TextView sub,
                          View row, List<OrgDashboardUiModel.Task> tasks, int index) {
        if (tasks.size() <= index) {
            row.setVisibility(View.GONE);
            return;
        }
        OrgDashboardUiModel.Task task = tasks.get(index);
        row.setVisibility(View.VISIBLE);
        title.setText(task.title);
        sub.setText(task.detail);
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
