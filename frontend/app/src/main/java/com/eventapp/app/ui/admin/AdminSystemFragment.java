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

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.integration.IntegrationDtos;
import com.eventapp.app.data.repository.SystemConfigRepository;
import com.eventapp.app.databinding.FragmentAdminSystemBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.host.AdminHostActivity;

import java.util.List;

/** Tab Hệ thống: tài khoản quản trị, tích hợp, thông báo và nhật ký thao tác. */
public class AdminSystemFragment extends Fragment {

    private FragmentAdminSystemBinding binding;

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

        loadHealth();
    }

    private void loadHealth() {
        ServiceLocator.get(SystemConfigRepository.class).health(
                new ApiCallback<List<IntegrationDtos.Health>>() {
                    @Override
                    public void onSuccess(@Nullable List<IntegrationDtos.Health> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null || data == null) {
                            return;
                        }
                        for (IntegrationDtos.Health item : data) {
                            bindHealth(item);
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null) {
                            return;
                        }
                        bindUnknown(binding.tvVietqrState, binding.dotVietqr);
                        bindUnknown(binding.tvFcmState, binding.dotFcm);
                        bindUnknown(binding.tvAiState, binding.dotAi);
                    }
                });
    }

    private void bindHealth(IntegrationDtos.Health item) {
        if (item == null || item.name == null) {
            return;
        }
        switch (item.name) {
            case "PAYMENT":
                bindRow(binding.tvVietqrState, binding.dotVietqr, item.status);
                break;
            case "FCM":
                bindRow(binding.tvFcmState, binding.dotFcm, item.status);
                break;
            case "AI":
                bindRow(binding.tvAiState, binding.dotAi, item.status);
                break;
            default:
                break;
        }
    }

    private void bindRow(TextView label, View dot, String status) {
        label.setText(statusLabel(status));
        int color;
        switch (status == null ? "" : status) {
            case "HEALTHY":
                color = R.color.admin_success_text;
                break;
            case "DEGRADED":
                color = R.color.admin_warning_text;
                break;
            case "DOWN":
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
        bindRow(label, dot, "UNKNOWN");
    }

    private String statusLabel(String status) {
        switch (status == null ? "" : status) {
            case "HEALTHY":
                return getString(R.string.admin_sys_state_healthy);
            case "DEGRADED":
                return getString(R.string.admin_sys_state_degraded);
            case "DOWN":
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
