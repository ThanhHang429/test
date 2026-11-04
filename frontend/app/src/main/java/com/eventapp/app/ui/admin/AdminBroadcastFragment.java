package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminBroadcastBinding;
import com.google.android.material.snackbar.Snackbar;

/** Soạn thông báo toàn hệ thống: đối tượng nhận, kênh gửi, nội dung và xem trước. */
public class AdminBroadcastFragment extends Fragment {

    private FragmentAdminBroadcastBinding binding;
    private boolean emailChannel;
    private boolean pushChannel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminBroadcastBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.chipAudienceAll.setOnClickListener(v -> selectAudience(R.id.chipAudienceAll));
        binding.chipAudienceUser.setOnClickListener(v -> selectAudience(R.id.chipAudienceUser));
        binding.chipAudienceOrg.setOnClickListener(v -> selectAudience(R.id.chipAudienceOrg));

        binding.chipChannelInApp.setOnClickListener(v -> binding.chipChannelInApp.setSelected(
                !binding.chipChannelInApp.isSelected()));
        binding.chipChannelPush.setOnClickListener(v -> {
            pushChannel = !pushChannel;
            binding.chipChannelPush.setSelected(pushChannel);
        });
        binding.chipChannelEmail.setOnClickListener(v -> {
            emailChannel = !emailChannel;
            binding.chipChannelEmail.setSelected(emailChannel);
        });

        binding.chipScheduleNow.setOnClickListener(v -> selectSchedule(false));
        binding.chipScheduleLater.setOnClickListener(v -> selectSchedule(true));

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreview();
            }
        };
        binding.etBroadcastTitle.addTextChangedListener(watcher);
        binding.etBroadcastMessage.addTextChangedListener(watcher);

        binding.btnBroadcastSend.setOnClickListener(v -> send());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.broadcastBottomBar.setPaddingRelative(
                    binding.broadcastBottomBar.getPaddingStart(),
                    binding.broadcastBottomBar.getPaddingTop(),
                    binding.broadcastBottomBar.getPaddingEnd(),
                    dp(12) + bars.bottom);
            return insets;
        });

        selectAudience(R.id.chipAudienceAll);
        binding.chipChannelInApp.setSelected(true);
        selectSchedule(false);
        updatePreview();
    }

    private void selectAudience(int chipId) {
        binding.chipAudienceAll.setSelected(chipId == R.id.chipAudienceAll);
        binding.chipAudienceUser.setSelected(chipId == R.id.chipAudienceUser);
        binding.chipAudienceOrg.setSelected(chipId == R.id.chipAudienceOrg);

        if (chipId == R.id.chipAudienceUser) {
            binding.tvBroadcastReach.setText(R.string.admin_bc_reach_user);
        } else if (chipId == R.id.chipAudienceOrg) {
            binding.tvBroadcastReach.setText(R.string.admin_bc_reach_org);
        } else {
            binding.tvBroadcastReach.setText(R.string.admin_bc_reach_value);
        }
    }

    private void selectSchedule(boolean later) {
        binding.chipScheduleNow.setSelected(!later);
        binding.chipScheduleLater.setSelected(later);
        binding.tvBroadcastSchedule.setVisibility(later ? View.VISIBLE : View.GONE);
    }

    private void updatePreview() {
        String title = binding.etBroadcastTitle.getText().toString().trim();
        String message = binding.etBroadcastMessage.getText().toString().trim();

        binding.tvBroadcastPreviewTitle.setText(title.isEmpty()
                ? getString(R.string.admin_bc_preview_title) : title);
        binding.tvBroadcastPreviewBody.setText(message.isEmpty()
                ? getString(R.string.admin_bc_preview_body) : message);
    }

    private void send() {
        String title = binding.etBroadcastTitle.getText().toString().trim();
        String message = binding.etBroadcastMessage.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
            snack(R.string.admin_bc_empty);
            return;
        }
        if (!binding.chipChannelInApp.isSelected() && !pushChannel && !emailChannel) {
            snack(R.string.admin_bc_channel_empty);
            return;
        }

        snack(R.string.admin_bc_sent);
        binding.etBroadcastTitle.setText("");
        binding.etBroadcastMessage.setText("");
    }

    private void snack(int message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
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
