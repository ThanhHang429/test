package com.eventapp.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminSystemBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.host.AdminHostActivity;

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
