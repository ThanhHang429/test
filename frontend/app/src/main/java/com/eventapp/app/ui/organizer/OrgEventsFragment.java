package com.eventapp.app.ui.organizer;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.databinding.FragmentOrgEventsBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.organizer.adapter.OrgEventAdapter;
import com.eventapp.app.ui.organizer.data.OrgDemo;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Sự kiện: tìm kiếm, lọc trạng thái và mở chi tiết. */
public class OrgEventsFragment extends Fragment implements OrgEventAdapter.Listener {

    private FragmentOrgEventsBinding binding;
    private OrgEventAdapter adapter;
    private final List<TextView> chips = new ArrayList<>();
    private String filter = OrgEvent.FILTER_ALL;
    private String query = "";

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
                Snackbar.make(binding.getRoot(), "Bộ lọc nâng cao đang được hoàn thiện",
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
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                applyFilter();
            }
        });

        selectFilter(OrgEvent.FILTER_ALL);
    }

    private void selectFilter(String value) {
        filter = value;
        chips.get(0).setSelected(OrgEvent.FILTER_ALL.equals(value));
        chips.get(1).setSelected(OrgEvent.FILTER_ON_SALE.equals(value));
        chips.get(2).setSelected(OrgEvent.FILTER_PENDING.equals(value));
        chips.get(3).setSelected(OrgEvent.FILTER_DRAFT.equals(value));
        chips.get(4).setSelected(OrgEvent.FILTER_ENDED.equals(value));
        applyFilter();
    }

    private void applyFilter() {
        List<OrgEvent> result = new ArrayList<>();
        for (OrgEvent event : OrgDemo.events()) {
            boolean matchFilter = OrgEvent.FILTER_ALL.equals(filter) || filter.equals(event.filter);
            boolean matchQuery = query.isEmpty()
                    || event.title.toLowerCase(Locale.ROOT).contains(query)
                    || event.code.toLowerCase(Locale.ROOT).contains(query)
                    || event.category.toLowerCase(Locale.ROOT).contains(query);
            if (matchFilter && matchQuery) {
                result.add(event);
            }
        }
        adapter.submit(result);
        binding.tvEventsEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOpen(OrgEvent event) {
        Intent intent = new Intent(requireContext(), OrgEventDetailActivity.class);
        intent.putExtra(OrgEventDetailActivity.EXTRA_EVENT_CODE, event.code);
        startActivity(intent);
    }

    @Override
    public void onManageTickets(OrgEvent event) {
        Snackbar.make(binding.getRoot(), "Quản lý vé · " + event.title,
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
