package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.databinding.FragmentUserAccountBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.common.AppDialog;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.viewmodel.AccountViewModel;
import com.eventapp.app.data.di.ServiceLocator;

import java.util.List;

/** Tab Tài khoản: hồ sơ, thống kê, sở thích, tuỳ chọn thông báo và đăng xuất. */
public class AccountFragment extends Fragment {

    private static final String PREFS = "user_prefs";
    private static final String KEY_PUSH = "push";
    private static final String KEY_REMINDER = "reminder";
    private static final String KEY_EMAIL = "email";

    private FragmentUserAccountBinding binding;
    private SharedPreferences prefs;
    private AccountViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        bindProfile();
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

        binding.btnEditProfile.setOnClickListener(v -> comingSoon());
        binding.rowSecurity.setOnClickListener(v -> comingSoon());
        binding.rowPrivacy.setOnClickListener(v -> comingSoon());
        binding.rowSupport.setOnClickListener(v -> comingSoon());
        binding.rowTerms.setOnClickListener(v -> comingSoon());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());

        binding.colReviews.setOnClickListener(v -> MyReviewsActivity.start(requireContext()));
        binding.tvStatTickets.setText(String.valueOf(viewModel.purchasedCount()));

        observe();
        viewModel.load();
    }

    private void observe() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), data -> buildChips());
        viewModel.getSelectedCategories().observe(getViewLifecycleOwner(), data -> buildChips());
        viewModel.getSavedCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                binding.tvStatSaved.setText(String.valueOf(count == null ? 0L : count));
            }
        });
        viewModel.getReviewCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                binding.tvStatReviews.setText(String.valueOf(count == null ? 0L : count));
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
        });
    }

    private void bindProfile() {
        AuthSession session = SessionManager.get().getSession();
        String name = session == null || session.fullName == null || session.fullName.isEmpty()
                ? DemoContent.USER_NAME : session.fullName;
        String email = session == null || session.email == null ? DemoContent.USER_EMAIL
                : session.email;
        binding.tvName.setText(name);
        binding.tvEmail.setText(email);
        binding.tvInitials.setText(initials(name));
    }

    // ------------------------------------------------------------ preferences

    private void buildChips() {
        if (binding == null || viewModel == null) {
            return;
        }
        List<CategoryNodeUiModel> categories = viewModel.getCategories().getValue();
        List<Long> selected = viewModel.getSelectedCategories().getValue();

        binding.prefChipRow.removeAllViews();
        if (categories == null) {
            return;
        }
        for (CategoryNodeUiModel node : categories) {
            TextView chip = new TextView(new ContextThemeWrapper(requireContext(),
                    R.style.Widget_EventApp_User_Chip));
            chip.setText(node.name);
            chip.setSelected(selected != null && selected.contains(node.id));
            chip.setOnClickListener(v -> toggleCategory(node.id));
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            params.setMarginEnd(dp(8));
            binding.prefChipRow.addView(chip, params);
        }
    }

    private void toggleCategory(long categoryId) {
        if (!viewModel.toggleCategory(categoryId)) {
            SnackPill.show(binding.getRoot(),
                    getString(R.string.user_pref_limit), dp(112));
        }
    }

    private void comingSoon() {
        SnackPill.show(binding.getRoot(), getString(R.string.user_coming_soon_toast), dp(112));
    }

    private void confirmLogout() {
        AppDialog.confirm(requireContext(),
                R.drawable.ic_user_logout,
                getString(R.string.user_logout_confirm_title),
                getString(R.string.user_logout_confirm_body),
                getString(R.string.user_logout_action),
                getString(R.string.user_cancel),
                AppDialog.Tone.DANGER,
                this::logout);
    }

    private void logout() {
        SessionManager.get().clear();
        ServiceLocator.reset();
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
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
        if (viewModel != null) {
            viewModel.refreshStats();
        }
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
