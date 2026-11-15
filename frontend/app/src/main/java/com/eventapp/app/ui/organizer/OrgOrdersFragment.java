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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.eventapp.app.databinding.FragmentOrgOrdersBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.organizer.adapter.OrgOrderAdapter;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.viewmodel.OrgOrdersViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab Đơn hàng: lọc trạng thái, tìm kiếm và mở chi tiết đơn thật từ Backend (FR-O17). */
public class OrgOrdersFragment extends Fragment implements OrgOrderAdapter.Listener {

    private FragmentOrgOrdersBinding binding;
    private OrgOrderAdapter adapter;
    private final List<TextView> chips = new ArrayList<>();
    private final List<OrgOrder> loadedOrders = new ArrayList<>();
    private String filter = OrgOrder.FILTER_ALL;
    private OrgOrdersViewModel viewModel;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.rvOrders);
        adapter = new OrgOrderAdapter(this);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvOrders.setAdapter(adapter);

        chips.clear();
        chips.add(binding.chipOrderAll);
        chips.add(binding.chipOrderPaid);
        chips.add(binding.chipOrderPending);
        chips.add(binding.chipOrderRefund);

        binding.chipOrderAll.setOnClickListener(v -> selectFilter(OrgOrder.FILTER_ALL));
        binding.chipOrderPaid.setOnClickListener(v -> selectFilter(OrgOrder.FILTER_PAID));
        binding.chipOrderPending.setOnClickListener(v -> selectFilter(OrgOrder.FILTER_PENDING));
        binding.chipOrderRefund.setOnClickListener(v -> selectFilter(OrgOrder.FILTER_REFUND));
        binding.btnFilterOrder.setOnClickListener(v -> viewModel.loadOrders(null, null));

        binding.etSearchOrder.addTextChangedListener(new TextWatcher() {
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

        viewModel = new ViewModelProvider(this).get(OrgOrdersViewModel.class);
        observeViewModel();
        selectFilter(OrgOrder.FILTER_ALL);
        viewModel.loadOrders(null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadOrders(null, null);
        }
    }

    private void observeViewModel() {
        viewModel.getOrders().observe(getViewLifecycleOwner(), data -> {
            loadedOrders.clear();
            if (data != null) {
                loadedOrders.addAll(data);
            }
            applyFilter();
        });

        viewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (binding == null || stats == null) return;
            binding.tvStatTotal.setText(OrgFormat.count(stats.total));
            binding.tvStatPaid.setText(OrgFormat.count(stats.paid));
            binding.tvStatPending.setText(OrgFormat.count(stats.pending));
            binding.tvStatRefund.setText(OrgFormat.count(stats.refund));
            binding.tvOrdersSubtitle.setText(OrgFormat.count(stats.total) + " đơn hàng từ các sự kiện bạn tổ chức");
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.progressOrders.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), err -> {
            if (err == null || binding == null) return;
            viewModel.consumeError();
            Snackbar.make(binding.getRoot(), err, Snackbar.LENGTH_LONG).show();
        });
    }

    private void selectFilter(String value) {
        filter = value;
        chips.get(0).setSelected(OrgOrder.FILTER_ALL.equals(value));
        chips.get(1).setSelected(OrgOrder.FILTER_PAID.equals(value));
        chips.get(2).setSelected(OrgOrder.FILTER_PENDING.equals(value));
        chips.get(3).setSelected(OrgOrder.FILTER_REFUND.equals(value));
        applyFilter();
    }

    private void applyFilter() {
        if (binding == null) return;
        List<OrgOrder> result = new ArrayList<>();
        for (OrgOrder order : loadedOrders) {
            boolean matchFilter = OrgOrder.FILTER_ALL.equals(filter) || filter.equals(order.filter);
            boolean matchQuery = query.isEmpty()
                    || order.code.toLowerCase(Locale.ROOT).contains(query)
                    || order.buyer.toLowerCase(Locale.ROOT).contains(query)
                    || order.event.toLowerCase(Locale.ROOT).contains(query);
            if (matchFilter && matchQuery) {
                result.add(order);
            }
        }
        adapter.submit(result);
        binding.tvOrdersEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onOpen(OrgOrder order) {
        Intent intent = new Intent(requireContext(), OrgOrderDetailActivity.class);
        intent.putExtra(OrgOrderDetailActivity.EXTRA_ORDER_CODE, order.code);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
