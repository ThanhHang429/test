package com.eventapp.app.ui.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.databinding.FragmentOrgSettingsBinding;
import com.eventapp.app.databinding.ViewOrgSettingRowBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/** Tab Cài đặt: hồ sơ tổ chức, các nhóm thiết lập và đăng xuất. */
public class OrgSettingsFragment extends Fragment {

    private FragmentOrgSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        bindRow(binding.rowProfile, R.drawable.ic_org_building,
                getString(R.string.org_set_profile), getString(R.string.org_set_profile_sub),
                null);
        bindRow(binding.rowLegal, R.drawable.ic_user_shield,
                getString(R.string.org_set_legal), getString(R.string.org_set_legal_sub), "100%");
        bindRow(binding.rowBank, R.drawable.ic_user_wallet,
                getString(R.string.org_set_bank), getString(R.string.org_set_bank_sub), "VietQR");
        bindRow(binding.rowNotify, R.drawable.ic_user_bell,
                getString(R.string.org_set_notify), getString(R.string.org_set_notify_sub), null);
        bindRow(binding.rowTeam, R.drawable.ic_ds_users,
                getString(R.string.org_set_team), getString(R.string.org_set_team_sub), "4");
        bindRow(binding.rowDevices, R.drawable.ic_user_qr,
                getString(R.string.org_set_devices), getString(R.string.org_set_devices_sub), "2");
        bindRow(binding.row2fa, R.drawable.ic_user_shield,
                getString(R.string.org_set_2fa), getString(R.string.org_set_2fa_sub), "Bật");
        bindRow(binding.rowLanguage, R.drawable.ic_org_globe,
                getString(R.string.org_set_language), getString(R.string.org_set_language_sub), "VI");
        bindRow(binding.rowHelp, R.drawable.ic_user_help,
                getString(R.string.org_set_help), getString(R.string.org_set_help_sub), null);

        binding.btnEditProfile.setOnClickListener(v -> snack("Chỉnh sửa hồ sơ tổ chức"));
        binding.btnOrgLogout.setOnClickListener(v -> confirmLogout());
    }

    private void bindRow(ViewOrgSettingRowBinding row, int icon, String title, String subtitle,
                         @Nullable String badge) {
        row.ivRowIcon.setImageResource(icon);
        row.tvRowTitle.setText(title);
        row.tvRowSub.setText(subtitle);
        if (badge == null || badge.isEmpty()) {
            row.tvRowBadge.setVisibility(View.GONE);
        } else {
            row.tvRowBadge.setVisibility(View.VISIBLE);
            row.tvRowBadge.setText(badge);
        }
        row.getRoot().setOnClickListener(v -> snack(title));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.org_logout)
                .setMessage("Bạn sẽ quay lại màn hình đăng nhập.")
                .setNegativeButton("Ở lại", null)
                .setPositiveButton(R.string.org_logout, (dialog, which) -> {
                    SessionManager.get().clear();
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message + " · đang hoàn thiện",
                Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
