package com.eventapp.app.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.databinding.FragmentOrgDashboardBinding;
import com.eventapp.app.ui.common.view.NavContentInset;

/** Tab Tổng quan của nhà tổ chức. */
public class OrgDashboardFragment extends Fragment {

    private FragmentOrgDashboardBinding binding;

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
        binding.cardFeaturedEvent.setOnClickListener(v -> openEvent("VTE-2025"));
        binding.tvSeeAllEvents.setOnClickListener(v -> {
            if (requireActivity() instanceof com.eventapp.app.ui.host.OrganizerHostActivity) {
                ((com.eventapp.app.ui.host.OrganizerHostActivity) requireActivity())
                        .openTab(OrgBottomNavView.TAB_EVENTS);
            }
        });
        binding.rowTodoReview.setOnClickListener(v ->
                snack("2 sự kiện đang chờ duyệt nội dung"));
        binding.rowTodoRefund.setOnClickListener(v ->
                snack("3 yêu cầu hoàn tiền mới"));
    }

    private void snack(String message) {
        com.google.android.material.snackbar.Snackbar
                .make(binding.getRoot(), message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                .show();
    }

    private void openEvent(String code) {
        Intent intent = new Intent(requireContext(), OrgEventDetailActivity.class);
        intent.putExtra(OrgEventDetailActivity.EXTRA_EVENT_CODE, code);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
