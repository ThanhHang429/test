package com.eventapp.app.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminSystemBinding;
import com.eventapp.app.ui.admin.viewmodel.AdminSystemViewModel;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.model.AdminSystemUiModel;
import com.eventapp.app.ui.host.AdminHostActivity;

/** Tab Hệ thống: tài khoản quản trị, tích hợp, thông báo và nhật ký thao tác. */
public class AdminSystemFragment extends Fragment {

    private FragmentAdminSystemBinding binding;
    private AdminSystemViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminSystemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnBroadcast.setOnClickListener(v -> {
            if (requireActivity() instanceof AdminHostActivity) {
                ((AdminHostActivity) requireActivity()).openScreen(AdminHostActivity.SCREEN_BROADCAST);
            }
        });

        binding.btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        viewModel = new ViewModelProvider(this).get(AdminSystemViewModel.class);
        observe();
        viewModel.loadHealth();
    }

    private void observe() {
        viewModel.getHealth().observe(getViewLifecycleOwner(), data -> {
            if (binding == null) {
                return;
            }
            if (data == null) {
                bindUnknown(binding.tvVietqrState, binding.dotVietqr);
                bindUnknown(binding.tvFcmState, binding.dotFcm);
                bindUnknown(binding.tvAiState, binding.dotAi);
                return;
            }
            bindHealth(data);
        });
    }

    private void bindHealth(AdminSystemUiModel item) {
        bindRow(binding.tvVietqrState, binding.dotVietqr, item.paymentStatus);
        bindRow(binding.tvFcmState, binding.dotFcm, item.fcmStatus);
        bindRow(binding.tvAiState, binding.dotAi, item.aiStatus);
    }

    private void bindRow(TextView label, View dot, AdminSystemUiModel.Status status) {
        label.setText(statusLabel(status));
        int color;
        switch (status) {
            case HEALTHY:
                color = R.color.admin_success_text;
                break;
            case DEGRADED:
                color = R.color.admin_warning_text;
                break;
            case DOWN:
                color = R.color.admin_danger_text;
                break;
            default:
                color = R.color.admin_text_dim;
                break;
        }
        label.setTextColor(ContextCompat.getColor(requireContext(), color));
        dot.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), color)));
    }

    private void bindUnknown(TextView label, View dot) {
        bindRow(label, dot, AdminSystemUiModel.Status.UNKNOWN);
    }

    private String statusLabel(AdminSystemUiModel.Status status) {
        switch (status) {
            case HEALTHY:
                return getString(R.string.admin_sys_state_healthy);
            case DEGRADED:
                return getString(R.string.admin_sys_state_degraded);
            case DOWN:
                return getString(R.string.admin_sys_state_down);
            default:
                return getString(R.string.admin_sys_state_unknown);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
