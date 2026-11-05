package com.eventapp.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.databinding.FragmentAuthLoginBinding;
import com.eventapp.app.databinding.ItemAuthAccountBinding;
import com.eventapp.app.ui.host.SessionRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * Đăng nhập bằng tài khoản seed.
 *
 * <p>Backend chưa có module xác thực thật, chỉ có {@code GET /dev/accounts} và
 * {@code POST /dev/token}. Màn này liệt kê tài khoản seed rồi đổi sang JWT để đi đúng chuỗi
 * xác thực (Bearer token) như bản thật.</p>
 */
public class LoginFragment extends Fragment {

    private FragmentAuthLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.topBar.btnBack.setVisibility(View.GONE);
        binding.topBar.tvTopChip.setVisibility(View.GONE);
        binding.btnRetry.setOnClickListener(v -> viewModel.loadAccounts());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        observe();
        viewModel.loadAccounts();
    }

    private void observe() {
        viewModel.getAccounts().observe(getViewLifecycleOwner(), this::render);
        viewModel.getLoading().observe(getViewLifecycleOwner(), ignored -> renderBusy());
        viewModel.getSubmitting().observe(getViewLifecycleOwner(), ignored -> renderBusy());
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null) {
                binding.tvError.setVisibility(View.GONE);
                return;
            }
            boolean loadFailed = isEmptyAccounts();
            binding.tvError.setText(loadFailed
                    ? getString(R.string.auth_seed_error) + "\n" + error.getUserMessage()
                    : error.getUserMessage());
            binding.tvError.setVisibility(View.VISIBLE);
            binding.btnRetry.setVisibility(loadFailed ? View.VISIBLE : View.GONE);
        });
        viewModel.getLoggedIn().observe(getViewLifecycleOwner(), this::openHost);
    }

    private void renderBusy() {
        boolean loading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
        boolean submitting = Boolean.TRUE.equals(viewModel.getSubmitting().getValue());
        binding.progress.setVisibility(loading || submitting ? View.VISIBLE : View.GONE);
        setRowsEnabled(!submitting);
    }

    private boolean isEmptyAccounts() {
        List<DevAccountDto> accounts = viewModel.getAccounts().getValue();
        return accounts == null || accounts.isEmpty();
    }

    private void render(List<DevAccountDto> accounts) {
        if (binding == null) {
            return;
        }
        binding.accountList.removeAllViews();
        binding.tvError.setVisibility(View.GONE);
        binding.btnRetry.setVisibility(View.GONE);

        if (accounts.isEmpty()) {
            if (!Boolean.TRUE.equals(viewModel.getLoading().getValue())) {
                binding.tvError.setText(R.string.auth_seed_empty);
                binding.tvError.setVisibility(View.VISIBLE);
            }
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DevAccountDto account : accounts) {
            ItemAuthAccountBinding row = ItemAuthAccountBinding.inflate(
                    inflater, binding.accountList, false);
            row.tvName.setText(account.fullName == null ? account.email : account.fullName);
            row.tvEmail.setText(account.email);
            String role = primaryRole(account.roles);
            row.tvRole.setText(role);
            row.ivRoleIcon.setImageResource(iconFor(role));
            row.getRoot().setOnClickListener(v -> viewModel.login(account));
            binding.accountList.addView(row.getRoot());
        }
        renderBusy();
    }

    private void openHost(@Nullable AuthSession session) {
        if (session == null || binding == null) {
            return;
        }
        viewModel.consumeLoggedIn();
        SessionRouter.openHost(requireContext(), session);
        requireActivity().finish();
    }

    private void setRowsEnabled(boolean enabled) {
        for (int i = 0; i < binding.accountList.getChildCount(); i++) {
            View row = binding.accountList.getChildAt(i);
            row.setEnabled(enabled);
            row.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    private String primaryRole(@Nullable List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "USER";
        }
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("ORGANIZER")) {
            return "ORGANIZER";
        }
        return roles.get(0);
    }

    private int iconFor(String role) {
        if ("ADMIN".equals(role)) {
            return R.drawable.ic_user_shield;
        }
        if ("ORGANIZER".equals(role)) {
            return R.drawable.ic_org_building;
        }
        return R.drawable.ic_auth_person;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
