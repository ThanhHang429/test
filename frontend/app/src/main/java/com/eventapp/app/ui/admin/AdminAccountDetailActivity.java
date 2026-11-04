package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityAdminAccountDetailBinding;
import com.eventapp.app.ui.admin.model.AdminAccount;
import com.google.android.material.snackbar.Snackbar;

/**
 * Hồ sơ một tài khoản: thông tin định danh, hoạt động gần đây và hành động quản trị.
 *
 * <p>Quản trị chỉ tác động tới trạng thái tài khoản (cảnh báo, tạm khoá) —
 * không chạm vào tiền vé hay công nợ của nhà tổ chức.</p>
 */
public class AdminAccountDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_account_id";
    public static final String EXTRA_NAME = "extra_account_name";
    public static final String EXTRA_EMAIL = "extra_account_email";
    public static final String EXTRA_INITIALS = "extra_account_initials";
    public static final String EXTRA_META = "extra_account_meta";
    public static final String EXTRA_TYPE = "extra_account_type";
    public static final String EXTRA_STATUS = "extra_account_status";

    private ActivityAdminAccountDetailBinding binding;
    private int status;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityAdminAccountDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyInsets();
        bind();
    }

    private void bind() {
        String id = getIntent().getStringExtra(EXTRA_ID);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        String initials = getIntent().getStringExtra(EXTRA_INITIALS);
        int type = getIntent().getIntExtra(EXTRA_TYPE, AdminAccount.TYPE_USER);
        status = getIntent().getIntExtra(EXTRA_STATUS, AdminAccount.STATUS_ACTIVE);

        binding.tvAccountId.setText("#" + id);
        binding.tvAccountHeroName.setText(name);
        binding.tvAccountHeroEmail.setText(email);
        binding.tvAccountHeroInitials.setText(initials);
        binding.tvAccountType.setText(type == AdminAccount.TYPE_ORG
                ? R.string.admin_acc_type_org : R.string.admin_acc_type_user);
        binding.tvAccountVerify.setText(type == AdminAccount.TYPE_ORG
                ? R.string.admin_acc_verify_org : R.string.admin_acc_verify_user);

        if (type == AdminAccount.TYPE_ORG) {
            binding.tvAccountStat1.setText("12");
            binding.tvAccountStat1Label.setText(R.string.admin_acc_stat_1_org);
            binding.tvAccountStat2.setText("4.820");
            binding.tvAccountStat2Label.setText(R.string.admin_acc_stat_2_org);
            binding.tvAccountStat3.setText("1,65 tỷ");
            binding.tvAccountStat3Label.setText(R.string.admin_acc_stat_3_org);
        } else {
            binding.tvAccountStat1.setText("24");
            binding.tvAccountStat1Label.setText(R.string.admin_acc_stat_1_user);
            binding.tvAccountStat2.setText("6");
            binding.tvAccountStat2Label.setText(R.string.admin_acc_stat_2_user);
            binding.tvAccountStat3.setText("0");
            binding.tvAccountStat3Label.setText(R.string.admin_acc_stat_3_user);
        }

        applyStatus();
        binding.btnAccountBack.setOnClickListener(v -> finish());
        binding.btnAccountAlert.setOnClickListener(v ->
                snack(R.string.admin_acc_alerted));
        binding.btnAccountLock.setOnClickListener(v -> toggleLock());
    }

    private void applyStatus() {
        switch (status) {
            case AdminAccount.STATUS_FLAGGED:
                binding.tvAccountHeroStatus.setText(R.string.admin_acc_status_flagged);
                binding.tvAccountHeroStatus.setTextColor(getColor(R.color.admin_warning));
                break;
            case AdminAccount.STATUS_LOCKED:
                binding.tvAccountHeroStatus.setText(R.string.admin_acc_status_locked);
                binding.tvAccountHeroStatus.setTextColor(getColor(R.color.admin_danger));
                break;
            case AdminAccount.STATUS_ACTIVE:
            default:
                binding.tvAccountHeroStatus.setText(R.string.admin_acc_status_active);
                binding.tvAccountHeroStatus.setTextColor(getColor(R.color.admin_accent_bright));
                break;
        }

        boolean locked = status == AdminAccount.STATUS_LOCKED;
        TextView button = binding.btnAccountLock;
        button.setText(locked ? R.string.admin_acc_unlock : R.string.admin_acc_lock);
        button.setBackgroundResource(locked
                ? R.drawable.bg_admin_btn_secondary : R.drawable.bg_admin_btn_danger);
        button.setTextColor(getColor(locked
                ? R.color.admin_primary : R.color.admin_danger_text));
    }

    private void toggleLock() {
        boolean locked = status == AdminAccount.STATUS_LOCKED;
        status = locked ? AdminAccount.STATUS_ACTIVE : AdminAccount.STATUS_LOCKED;
        applyStatus();
        snack(locked ? R.string.admin_acc_unlocked : R.string.admin_acc_locked);
    }

    private void snack(int message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.accountRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            ViewGroup.LayoutParams params = binding.accountTopBar.getLayoutParams();
            int height = dp(64) + bars.top;
            if (params.height != height) {
                params.height = height;
                binding.accountTopBar.setLayoutParams(params);
            }
            binding.accountTopBar.setPaddingRelative(binding.accountTopBar.getPaddingStart(),
                    bars.top, binding.accountTopBar.getPaddingEnd(), 0);
            binding.accountBottomBar.setPaddingRelative(
                    binding.accountBottomBar.getPaddingStart(),
                    binding.accountBottomBar.getPaddingTop(),
                    binding.accountBottomBar.getPaddingEnd(),
                    bars.bottom + dp(12));
            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
