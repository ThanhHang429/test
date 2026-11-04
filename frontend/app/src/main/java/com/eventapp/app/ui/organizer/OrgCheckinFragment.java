package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentOrgCheckinBinding;
import com.eventapp.app.ui.organizer.model.OrgTone;
import com.eventapp.app.ui.organizer.util.OrgUi;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/** Tab Check-in: khung quét QR (demo) + nhập mã thủ công + kết quả. */
public class OrgCheckinFragment extends Fragment {

    private FragmentOrgCheckinBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrgCheckinBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.scanPanel.setOnClickListener(v -> {
            binding.etCode.setText("VE-123456");
            verify();
        });
        binding.btnConfirmCode.setOnClickListener(v -> verify());
        binding.btnFlash.setOnClickListener(v ->
                snack("Đèn flash sẽ bật khi nối camera thật"));
        binding.btnFocusManual.setOnClickListener(v -> {
            binding.etCode.requestFocus();
            InputMethodManager ime = ContextCompat.getSystemService(requireContext(),
                    InputMethodManager.class);
            if (ime != null) {
                ime.showSoftInput(binding.etCode, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        binding.etCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verify();
                return true;
            }
            return false;
        });
    }

    private void verify() {
        String code = binding.etCode.getText() == null
                ? "" : binding.etCode.getText().toString().trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            snack("Nhập mã vé để check-in");
            return;
        }

        InputMethodManager ime = ContextCompat.getSystemService(requireContext(),
                InputMethodManager.class);
        if (ime != null) {
            ime.hideSoftInputFromWindow(binding.etCode.getWindowToken(), 0);
        }

        if ("VE-123456".equals(code)) {
            showResult(OrgTone.SUCCESS, R.drawable.ic_org_check_circle,
                    getString(R.string.org_result_success),
                    "Nguyễn Văn A · VIP All-Access · Cổng A · 10:42");
        } else if ("VE-654321".equals(code)) {
            showResult(OrgTone.WARNING, R.drawable.ic_ds_warning,
                    getString(R.string.org_result_duplicate),
                    "Lần quét trước 10:42 · Cổng A · Nguyễn Văn A");
        } else {
            showResult(OrgTone.DANGER, R.drawable.ic_ds_error,
                    getString(R.string.org_result_invalid),
                    getString(R.string.org_result_invalid_sub));
        }
    }

    private void showResult(int tone, int iconRes, String title, String subtitle) {
        binding.cardResult.setVisibility(View.VISIBLE);
        binding.tvResultTitle.setText(title);
        binding.tvResultSub.setText(subtitle);
        binding.ivResultIcon.setImageResource(iconRes);

        int titleColor;
        int iconColor;
        int tile;
        switch (tone) {
            case OrgTone.WARNING:
                titleColor = R.color.org_warning_text;
                iconColor = R.color.org_warning_text;
                tile = R.drawable.bg_org_pill_warning;
                break;
            case OrgTone.DANGER:
                titleColor = R.color.org_danger_text;
                iconColor = R.color.org_danger_text;
                tile = R.drawable.bg_org_pill_danger;
                break;
            case OrgTone.SUCCESS:
            default:
                titleColor = R.color.org_success_text;
                iconColor = R.color.org_success_text;
                tile = R.drawable.bg_org_pill_success;
                break;
        }

        binding.tvResultTitle.setTextColor(requireContext().getColor(titleColor));
        binding.ivResultIcon.setImageTintList(
                android.content.res.ColorStateList.valueOf(requireContext().getColor(iconColor)));
        binding.resultIconTile.setBackgroundResource(tile);

        binding.cardResult.setAlpha(0f);
        binding.cardResult.setTranslationY(12f);
        binding.cardResult.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
