package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminNotificationsBinding;
import com.eventapp.app.databinding.ItemAdminNotificationBinding;
import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminNotification;
import com.eventapp.app.ui.host.AdminHostActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Trung tâm thông báo vận hành: lọc theo nhóm và đánh dấu đã đọc. */
public class AdminNotificationsFragment extends Fragment {

    private FragmentAdminNotificationsBinding binding;
    private List<AdminNotification> notifications;
    private int filterType = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        notifications = AdminDemo.notifications();

        binding.chipNotiAll.setOnClickListener(v -> selectFilter(-1));
        binding.chipNotiMod.setOnClickListener(v ->
                selectFilter(AdminNotification.TYPE_MODERATION));
        binding.chipNotiFin.setOnClickListener(v -> selectFilter(AdminNotification.TYPE_FINANCE));
        binding.chipNotiSys.setOnClickListener(v -> selectFilter(AdminNotification.TYPE_SYSTEM));

        binding.btnNotiReadAll.setOnClickListener(v -> {
            for (AdminNotification notification : notifications) {
                notification.unread = false;
            }
            if (requireActivity() instanceof AdminHostActivity) {
                ((AdminHostActivity) requireActivity()).markNotificationsRead();
            }
            render();
            Snackbar.make(binding.getRoot(), R.string.admin_noti_read_done,
                    Snackbar.LENGTH_SHORT).show();
        });

        selectFilter(-1);
    }

    private void selectFilter(int type) {
        filterType = type;
        setChipSelected(binding.chipNotiAll, type == -1);
        setChipSelected(binding.chipNotiMod, type == AdminNotification.TYPE_MODERATION);
        setChipSelected(binding.chipNotiFin, type == AdminNotification.TYPE_FINANCE);
        setChipSelected(binding.chipNotiSys, type == AdminNotification.TYPE_SYSTEM);
        render();
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setSelected(selected);
    }

    private void render() {
        binding.listNotifications.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        int shown = 0;
        int unread = 0;

        for (AdminNotification notification : notifications) {
            if (notification.unread) {
                unread++;
            }
            if (filterType != -1 && notification.type != filterType) {
                continue;
            }
            ItemAdminNotificationBinding item =
                    ItemAdminNotificationBinding.inflate(inflater, binding.listNotifications, false);
            bindItem(item, notification);
            binding.listNotifications.addView(item.getRoot());
            shown++;
        }

        binding.tvNotiEmpty.setVisibility(shown == 0 ? View.VISIBLE : View.GONE);
        binding.tvNotiUnread.setText(getString(R.string.admin_noti_unread_count, unread));
        binding.tvNotiUnread.setVisibility(unread == 0 ? View.GONE : View.VISIBLE);
    }

    private void bindItem(ItemAdminNotificationBinding item, AdminNotification notification) {
        item.tvNotificationTitle.setText(notification.title);
        item.tvNotificationBody.setText(notification.body);
        item.tvNotificationTime.setText(notification.time);
        item.viewNotificationDot.setVisibility(notification.unread ? View.VISIBLE : View.GONE);
        item.tvNotificationTitle.setTextColor(requireContext().getColor(
                notification.unread ? R.color.admin_text : R.color.admin_text_muted));

        switch (notification.type) {
            case AdminNotification.TYPE_FINANCE:
                item.tvNotificationType.setText(R.string.admin_noti_type_fin);
                item.tvNotificationType.setBackgroundResource(R.drawable.bg_admin_pill_warning);
                item.tvNotificationType.setTextColor(
                        requireContext().getColor(R.color.admin_warning_text));
                item.ivNotificationIcon.setImageResource(R.drawable.ic_org_receipt);
                item.ivNotificationIcon.setImageTintList(
                        requireContext().getColorStateList(R.color.admin_warning_text));
                item.tileNotification.setBackgroundResource(R.drawable.bg_admin_icon_amber);
                break;
            case AdminNotification.TYPE_SYSTEM:
                item.tvNotificationType.setText(R.string.admin_noti_type_sys);
                item.tvNotificationType.setBackgroundResource(R.drawable.bg_admin_pill_neutral);
                item.tvNotificationType.setTextColor(
                        requireContext().getColor(R.color.admin_neutral_text));
                item.ivNotificationIcon.setImageResource(R.drawable.ic_org_settings);
                item.ivNotificationIcon.setImageTintList(
                        requireContext().getColorStateList(R.color.admin_text_muted));
                item.tileNotification.setBackgroundResource(R.drawable.bg_admin_icon_navy);
                break;
            case AdminNotification.TYPE_ACCOUNT:
                item.tvNotificationType.setText(R.string.admin_noti_type_acc);
                item.tvNotificationType.setBackgroundResource(R.drawable.bg_admin_pill_danger);
                item.tvNotificationType.setTextColor(
                        requireContext().getColor(R.color.admin_danger_text));
                item.ivNotificationIcon.setImageResource(R.drawable.ic_ds_users);
                item.ivNotificationIcon.setImageTintList(
                        requireContext().getColorStateList(R.color.admin_danger_text));
                item.tileNotification.setBackgroundResource(R.drawable.bg_admin_icon_red);
                break;
            case AdminNotification.TYPE_MODERATION:
            default:
                item.tvNotificationType.setText(R.string.admin_noti_type_mod);
                item.tvNotificationType.setBackgroundResource(R.drawable.bg_admin_pill_teal_outline);
                item.tvNotificationType.setTextColor(
                        requireContext().getColor(R.color.admin_accent_dark));
                item.ivNotificationIcon.setImageResource(R.drawable.ic_user_shield);
                item.ivNotificationIcon.setImageTintList(
                        requireContext().getColorStateList(R.color.admin_accent_dark));
                item.tileNotification.setBackgroundResource(R.drawable.bg_admin_icon_teal);
                break;
        }

        item.getRoot().setOnClickListener(v -> {
            if (notification.unread) {
                notification.unread = false;
                render();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
