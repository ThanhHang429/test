package com.eventapp.app.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentOrgEventsBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.organizer.adapter.OrgEventAdapter;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.viewmodel.OrgEventsViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Sự kiện: tải danh sách từ FR-O02, lọc trạng thái và mở chi tiết. */
public class OrgEventsFragment extends Fragment implements OrgEventAdapter.Listener {

    private static final long SEARCH_DEBOUNCE_MS = 280L;

    private FragmentOrgEventsBinding binding;
    private OrgEventAdapter adapter;
    private OrgEventsViewModel viewModel;
    private final List<TextView> chips = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String pendingQuery = "";
    private Runnable pendingSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.rvEvents);
        adapter = new OrgEventAdapter(this);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvEvents.setAdapter(adapter);

        chips.add(binding.chipAll);
        chips.add(binding.chipOnSale);
        chips.add(binding.chipPending);
        chips.add(binding.chipDraft);
        chips.add(binding.chipEnded);

        binding.chipAll.setOnClickListener(v -> selectFilter(OrgEvent.FILTER_ALL));
        binding.chipOnSale.setOnClickListener(v -> selectFilter(OrgEvent.FILTER_ON_SALE));
        binding.chipPending.setOnClickListener(v -> selectFilter(OrgEvent.FILTER_PENDING));
        binding.chipDraft.setOnClickListener(v -> selectFilter(OrgEvent.FILTER_DRAFT));
        binding.chipEnded.setOnClickListener(v -> selectFilter(OrgEvent.FILTER_ENDED));

        binding.btnCreateEvent.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), OrgCreateEventActivity.class)));
        binding.btnFilterEvent.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.org_filter_advanced_soon,
                        Snackbar.LENGTH_SHORT).show());

        binding.etSearchEvent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                pendingQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                scheduleRender();
            }
        });

        viewModel = new ViewModelProvider(this).get(OrgEventsViewModel.class);
        observe();
        selectFilter(OrgEvent.FILTER_ALL);
    }

    private void observe() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), this::render);
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            if (busy) {
                binding.tvEventsEmpty.setVisibility(View.GONE);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            Snackbar.make(binding.getRoot(), error.getUserMessage(),
                    Snackbar.LENGTH_LONG).show();
        });
    }

    private void selectFilter(String value) {
        chips.get(0).setSelected(OrgEvent.FILTER_ALL.equals(value));
        chips.get(1).setSelected(OrgEvent.FILTER_ON_SALE.equals(value));
        chips.get(2).setSelected(OrgEvent.FILTER_PENDING.equals(value));
        chips.get(3).setSelected(OrgEvent.FILTER_DRAFT.equals(value));
        chips.get(4).setSelected(OrgEvent.FILTER_ENDED.equals(value));
        viewModel.selectFilter(value);
    }

    private void scheduleRender() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> viewModel.setQuery(pendingQuery);
        handler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    private void render(List<OrgEvent> events) {
        if (binding == null) {
            return;
        }
        List<OrgEvent> result = events == null ? new ArrayList<>() : events;
        adapter.submit(result);
        binding.tvEventsEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvEventsSub.setText(getString(R.string.org_events_count, result.size()));
    }

    @Override
    public void onOpen(OrgEvent event) {
        Intent intent = new Intent(requireContext(), OrgEventDetailActivity.class);
        intent.putExtra(OrgEventDetailActivity.EXTRA_EVENT_ID, event.id);
        startActivity(intent);
    }

    @Override
    public void onManageTickets(OrgEvent event) {
        Snackbar.make(binding.getRoot(), getString(R.string.org_manage_tickets_soon, event.title),
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        if (pendingSearch != null) {
            handler.removeCallbacks(pendingSearch);
        }
        super.onDestroyView();
        binding = null;
    }
}
