package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserAccountBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.data.DemoContent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Tab Tài khoản: hồ sơ, thống kê, sở thích, tuỳ chọn thông báo và đăng xuất. */
public class AccountFragment extends Fragment {

    private static final String PREFS = "user_prefs";
    private static final String KEY_PUSH = "push";
    private static final String KEY_REMINDER = "reminder";
    private static final String KEY_EMAIL = "email";

    private FragmentUserAccountBinding binding;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        binding.tvName.setText(DemoContent.USER_NAME);
        binding.tvEmail.setText(DemoContent.USER_EMAIL);
        binding.tvInitials.setText(initials(DemoContent.USER_NAME));
        binding.tvVersion.setText(getString(R.string.user_version, BuildConfig.VERSION_NAME));

        binding.switchPush.setChecked(prefs.getBoolean(KEY_PUSH, true));
        binding.switchReminder.setChecked(prefs.getBoolean(KEY_REMINDER, true));
        binding.switchEmail.setChecked(prefs.getBoolean(KEY_EMAIL, false));

        binding.switchPush.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_PUSH, checked).apply());
        binding.switchReminder.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_REMINDER, checked).apply());
        binding.switchEmail.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(KEY_EMAIL, checked).apply());

        binding.chipTech.setSelected(true);
        binding.chipMusic.setSelected(true);
        binding.chipTech.setOnClickListener(this::toggleChip);
        binding.chipMusic.setOnClickListener(this::toggleChip);
        binding.chipArt.setOnClickListener(this::toggleChip);
        binding.chipBusiness.setOnClickListener(this::toggleChip);

        binding.btnEditProfile.setOnClickListener(v -> comingSoon());
        binding.rowSecurity.setOnClickListener(v -> comingSoon());
        binding.rowPrivacy.setOnClickListener(v -> comingSoon());
        binding.rowSupport.setOnClickListener(v -> comingSoon());
        binding.rowTerms.setOnClickListener(v -> comingSoon());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        refreshStats();
    }

    private void toggleChip(View chip) {
        chip.setSelected(!chip.isSelected());
    }

    private void comingSoon() {
        SnackPill.show(binding.getRoot(), getString(R.string.user_coming_soon_toast), dp(112));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.user_logout_confirm_title)
                .setMessage(R.string.user_logout_confirm_body)
                .setNegativeButton(R.string.user_cancel, null)
                .setPositiveButton(R.string.user_logout_action, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void refreshStats() {
        if (binding == null) {
            return;
        }
        DemoContent content = DemoContent.get();
        binding.tvStatTickets.setText(String.valueOf(content.purchasedCount()));
        binding.tvStatSaved.setText(String.valueOf(content.savedCount()));
        binding.tvStatReviews.setText(String.valueOf(4));
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) {
            return "?";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            builder.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return builder.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
