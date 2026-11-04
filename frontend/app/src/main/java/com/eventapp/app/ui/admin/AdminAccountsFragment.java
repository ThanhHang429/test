package com.eventapp.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentAdminAccountsBinding;
import com.eventapp.app.databinding.ItemAdminAccountBinding;
import com.eventapp.app.ui.admin.data.AdminDemo;
import com.eventapp.app.ui.admin.model.AdminAccount;

import java.util.List;
import java.util.Locale;

/**
 * Tab Tài khoản: chuyển giữa người dùng và nhà tổ chức, tìm kiếm và lọc theo trạng thái.
 *
 * <p>Quản trị chỉ quản lý danh tính và trạng thái hoạt động — không can thiệp
 * vào tiền vé hay công nợ của nhà tổ chức.</p>
 */
public class AdminAccountsFragment extends Fragment {

    private FragmentAdminAccountsBinding binding;
    private List<AdminAccount> users;
    private List<AdminAccount> organizers;
    private boolean showOrganizers;
    private int filterStatus = -1;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminAccountsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        users = AdminDemo.users();
        organizers = AdminDemo.organizers();

        binding.tabUsers.setOnClickListener(v -> selectType(false));
        binding.tabOrgs.setOnClickListener(v -> selectType(true));

        binding.chipAccAll.setOnClickListener(v -> selectFilter(-1));
        binding.chipAccActive.setOnClickListener(v -> selectFilter(AdminAccount.STATUS_ACTIVE));
        binding.chipAccFlagged.setOnClickListener(v -> selectFilter(AdminAccount.STATUS_FLAGGED));
        binding.chipAccLocked.setOnClickListener(v -> selectFilter(AdminAccount.STATUS_LOCKED));

        binding.etAccountSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                render();
            }
        });

        selectType(false);
    }

    private void selectType(boolean organizer) {
        showOrganizers = organizer;
        binding.tabUsers.setBackgroundResource(organizer ? 0 : R.drawable.bg_admin_segment_active);
        binding.tabOrgs.setBackgroundResource(organizer ? R.drawable.bg_admin_segment_active : 0);
        binding.tabUsers.setTextColor(requireContext().getColor(
                organizer ? R.color.admin_text_muted : R.color.admin_text));
        binding.tabOrgs.setTextColor(requireContext().getColor(
                organizer ? R.color.admin_text : R.color.admin_text_muted));
        selectFilter(-1);
    }

    private void selectFilter(int status) {
        filterStatus = status;
        setChipSelected(binding.chipAccAll, status == -1);
        setChipSelected(binding.chipAccActive, status == AdminAccount.STATUS_ACTIVE);
        setChipSelected(binding.chipAccFlagged, status == AdminAccount.STATUS_FLAGGED);
        setChipSelected(binding.chipAccLocked, status == AdminAccount.STATUS_LOCKED);
        render();
    }

    private void setChipSelected(TextView chip, boolean selected) {
        chip.setSelected(selected);
    }

    private void render() {
        binding.listAccounts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<AdminAccount> source = showOrganizers ? organizers : users;
        int shown = 0;

        for (AdminAccount account : source) {
            if (filterStatus != -1 && account.status != filterStatus) {
                continue;
            }
            if (!query.isEmpty()
                    && !account.name.toLowerCase(Locale.ROOT).contains(query)
                    && !account.email.toLowerCase(Locale.ROOT).contains(query)
                    && !account.id.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            ItemAdminAccountBinding item =
                    ItemAdminAccountBinding.inflate(inflater, binding.listAccounts, false);
            bindItem(item, account);
            binding.listAccounts.addView(item.getRoot());
            shown++;
        }

        binding.tvAccountCount.setText(getString(R.string.admin_acc_count, shown, source.size()));
    }

    private void bindItem(ItemAdminAccountBinding item, AdminAccount account) {
        item.tvAccountInitials.setText(account.initials);
        item.tvAccountName.setText(account.name);
        item.tvAccountEmail.setText(account.email);
        item.tvAccountMeta.setText(account.id + " · " + account.meta);

        switch (account.status) {
            case AdminAccount.STATUS_FLAGGED:
                item.tvAccountStatus.setText(R.string.admin_acc_status_flagged);
                item.tvAccountStatus.setBackgroundResource(R.drawable.bg_admin_pill_warning);
                item.tvAccountStatus.setTextColor(
                        requireContext().getColor(R.color.admin_warning_text));
                item.avatarAccount.setBackgroundResource(R.drawable.bg_admin_icon_amber);
                break;
            case AdminAccount.STATUS_LOCKED:
                item.tvAccountStatus.setText(R.string.admin_acc_status_locked);
                item.tvAccountStatus.setBackgroundResource(R.drawable.bg_admin_pill_neutral);
                item.tvAccountStatus.setTextColor(
                        requireContext().getColor(R.color.admin_neutral_text));
                item.avatarAccount.setBackgroundResource(R.drawable.bg_admin_icon_red);
                break;
            case AdminAccount.STATUS_ACTIVE:
            default:
                item.tvAccountStatus.setText(R.string.admin_acc_status_active);
                item.tvAccountStatus.setBackgroundResource(R.drawable.bg_admin_pill_success);
                item.tvAccountStatus.setTextColor(
                        requireContext().getColor(R.color.admin_success_text));
                item.avatarAccount.setBackgroundResource(R.drawable.bg_admin_avatar_soft);
                break;
        }

        item.getRoot().setOnClickListener(v -> openDetail(account));
    }

    private void openDetail(AdminAccount account) {
        Intent intent = new Intent(requireContext(), AdminAccountDetailActivity.class);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_ID, account.id);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_NAME, account.name);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_EMAIL, account.email);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_INITIALS, account.initials);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_META, account.meta);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_TYPE, account.type);
        intent.putExtra(AdminAccountDetailActivity.EXTRA_STATUS, account.status);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
