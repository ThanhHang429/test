package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminDashboardBinding;
import com.eventapp.app.ui.host.AdminHostActivity;

/** Tab Tổng quan: sức khỏe nền tảng, hàng đợi, doanh thu và nhật ký gần đây. */
public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.chartRevenue.setSeries(
                new float[]{5.2f, 6.8f, 5.9f, 8.1f, 7.4f, 9.6f, 8.6f},
                new float[]{4.6f, 5.2f, 5.5f, 6.9f, 6.4f, 7.8f, 7.2f});

        binding.btnQueueReview.setOnClickListener(v -> open(AdminHostActivity.SCREEN_MODERATION));
        binding.btnQueueReport.setOnClickListener(v -> open(AdminHostActivity.SCREEN_EVENTS));
        binding.btnQueueRefund.setOnClickListener(v -> open(AdminHostActivity.SCREEN_ACCOUNTS));
        binding.btnQueuePayout.setOnClickListener(v -> open(AdminHostActivity.SCREEN_ACCOUNTS));
        binding.btnAuditAll.setOnClickListener(v -> open(AdminHostActivity.SCREEN_SYSTEM));
    }

    private void open(int screen) {
        if (requireActivity() instanceof AdminHostActivity) {
            ((AdminHostActivity) requireActivity()).openScreen(screen);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
