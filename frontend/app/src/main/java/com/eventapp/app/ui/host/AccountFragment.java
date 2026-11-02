package com.eventapp.app.ui.host;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.eventapp.app.R;
import com.eventapp.app.core.session.AuthSession;
import com.eventapp.app.core.session.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

/** Màn tài khoản dùng chung cho cả 3 host: hiển thị phiên và đăng xuất. */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AuthSession session = SessionManager.get().getSession();
        if (session == null) {
            return;
        }

        TextView name = view.findViewById(R.id.tvAccountName);
        TextView email = view.findViewById(R.id.tvAccountEmail);
        TextView version = view.findViewById(R.id.tvAccountVersion);
        ChipGroup roles = view.findViewById(R.id.chipGroupAccountRoles);
        View myReviews = view.findViewById(R.id.rowAccountMyReviews);
        View dividerReviews = view.findViewById(R.id.dividerAccountReviews);
        View preferences = view.findViewById(R.id.rowAccountPreferences);
        View logout = view.findViewById(R.id.rowAccountLogout);

        String displayName = session.fullName == null || session.fullName.trim().isEmpty()
                ? "Người dùng ETicket" : session.fullName.trim();
        name.setText(displayName);
        email.setText(session.email == null ? "Không có email" : session.email);
        version.setText(R.string.account_version);

        roles.removeAllViews();
        for (String role : session.roles) {
            Chip chip = new Chip(requireContext());
            chip.setText(roleLabel(role));
            chip.setChipBackgroundColorResource(R.color.ds_primary_soft);
            chip.setTextColor(requireContext().getColor(R.color.ds_primary_dark));
            chip.setChipStrokeWidth(0f);
            chip.setClickable(false);
            chip.setCheckable(false);
            roles.addView(chip);
        }

        // Chỉ khu người dùng có các đích này trong nav graph; organizer/admin vẫn có thể mang
        // role USER nhưng điều hướng sẽ thất bại nên ẩn để tránh bấm vào màn trống.
        boolean user = "USER".equals(session.primaryRole());
        int reviewsVisibility = user ? View.VISIBLE : View.GONE;
        myReviews.setVisibility(reviewsVisibility);
        dividerReviews.setVisibility(reviewsVisibility);
        preferences.setVisibility(reviewsVisibility);

        myReviews.setOnClickListener(v -> navigate(R.id.my_reviewsFragment));
        preferences.setOnClickListener(v -> navigate(R.id.preferencesFragment));
        logout.setOnClickListener(v -> {
            Snackbar.make(v, R.string.account_logout, Snackbar.LENGTH_SHORT).show();
            AccountSheet.logout(requireActivity());
        });
    }

    private void navigate(int destination) {
        NavController controller = NavHostFragment.findNavController(this);
        try {
            if (controller.getCurrentDestination() != null
                    && controller.getCurrentDestination().getId() != destination) {
                controller.navigate(destination);
            }
        } catch (RuntimeException ignored) {
            Snackbar.make(requireView(), "Màn hình này chưa có trong khu vực hiện tại",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private String roleLabel(@Nullable String role) {
        if (role == null) {
            return "";
        }
        switch (role.toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN":
                return getString(R.string.account_role_admin);
            case "ORGANIZER":
                return getString(R.string.account_role_organizer);
            default:
                return getString(R.string.account_role_user);
        }
    }
}
