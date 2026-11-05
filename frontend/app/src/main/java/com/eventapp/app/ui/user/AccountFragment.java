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

import com.eventapp.app.BuildConfig;
import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.remote.dto.preference.PreferenceDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.data.repository.UserRepository;
import com.eventapp.app.databinding.FragmentUserAccountBinding;
import com.eventapp.app.ui.auth.AuthActivity;
import com.eventapp.app.ui.common.AppDialog;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.data.DemoContent;

import java.util.ArrayList;
import java.util.List;

/** Tab Tài khoản: hồ sơ, thống kê, sở thích, tuỳ chọn thông báo và đăng xuất. */
public class AccountFragment extends Fragment {

    private static final String PREFS = "user_prefs";
    private static final String KEY_PUSH = "push";
    private static final String KEY_REMINDER = "reminder";
    private static final String KEY_EMAIL = "email";

    private FragmentUserAccountBinding binding;
    private SharedPreferences prefs;
    private PreferenceDtos.Preference preference;
    private final List<CatalogDtos.CategoryNode> categories = new ArrayList<>();
    private final List<Long> selectedCategories = new ArrayList<>();

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

        loadCategories();
        loadPreference();
        refreshStats();
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

    private void loadCategories() {
        ServiceLocator.get(CategoryRepository.class).tree(
                new ApiCallback<List<CatalogDtos.CategoryNode>>() {
                    @Override
                    public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null || data == null) {
                            return;
                        }
                        categories.clear();
                        categories.addAll(data);
                        buildChips();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        // Không có danh mục thì giữ khung sở thích trống.
                    }
                });
    }

    private void loadPreference() {
        ServiceLocator.get(UserRepository.class).preferences(
                new ApiCallback<PreferenceDtos.Preference>() {
                    @Override
                    public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null || data == null) {
                            return;
                        }
                        preference = data;
                        selectedCategories.clear();
                        if (data.categoryIds != null) {
                            selectedCategories.addAll(data.categoryIds);
                        }
                        buildChips();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding != null) {
                            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
                        }
                    }
                });
    }

    private void buildChips() {
        if (binding == null) {
            return;
        }
        binding.prefChipRow.removeAllViews();
        for (CatalogDtos.CategoryNode node : categories) {
            TextView chip = new TextView(new ContextThemeWrapper(requireContext(),
                    R.style.Widget_EventApp_User_Chip));
            chip.setText(node.name);
            chip.setSelected(selectedCategories.contains(node.id));
            chip.setOnClickListener(v -> toggleCategory(node.id, chip));
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            params.setMarginEnd(dp(8));
            binding.prefChipRow.addView(chip, params);
        }
    }

    private void toggleCategory(long categoryId, TextView chip) {
        boolean nowSelected = !selectedCategories.contains(categoryId);
        if (nowSelected) {
            if (selectedCategories.size() >= 10) {
                SnackPill.show(binding.getRoot(),
                        getString(R.string.user_pref_limit), dp(112));
                return;
            }
            selectedCategories.add(categoryId);
        } else {
            selectedCategories.remove(categoryId);
        }
        chip.setSelected(nowSelected);
        savePreferences();
    }

    private void savePreferences() {
        PreferenceDtos.UpdateRequest request = new PreferenceDtos.UpdateRequest();
        request.categoryIds = new ArrayList<>(selectedCategories);
        request.version = preference == null ? null : preference.version;
        if (preference != null) {
            request.eventFormat = preference.eventFormat;
            request.priceMode = preference.priceMode;
            request.maxBudgetVnd = preference.maxBudgetVnd;
            request.preferredCities = preference.preferredCities;
            request.preferredDays = preference.preferredDays;
            request.personalizationEnabled = preference.personalizationEnabled;
            request.onboardingCompleted = true;
        } else {
            request.onboardingCompleted = true;
        }

        ServiceLocator.get(UserRepository.class).savePreferences(request,
                new ApiCallback<PreferenceDtos.Preference>() {
                    @Override
                    public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (data != null) {
                            preference = data;
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null) {
                            return;
                        }
                        SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(112));
                        loadPreference();
                    }
                });
    }

    // ------------------------------------------------------------------ stats

    private void refreshStats() {
        if (binding == null) {
            return;
        }
        binding.colReviews.setOnClickListener(v -> MyReviewsActivity.start(requireContext()));
        binding.tvStatTickets.setText(String.valueOf(DemoContent.get().purchasedCount()));
        binding.tvStatSaved.setText("0");
        binding.tvStatReviews.setText("0");

        ServiceLocator.get(FavoriteRepository.class).list("ALL", 0, 1,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding != null && data != null) {
                            binding.tvStatSaved.setText(String.valueOf(data.totalItems()));
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                    }
                });

        ServiceLocator.get(ReviewRepository.class).mine(0, 1,
                new ApiCallback<Paged<ReviewDtos.MyReview>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding != null && data != null) {
                            binding.tvStatReviews.setText(String.valueOf(data.totalItems()));
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                    }
                });
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
