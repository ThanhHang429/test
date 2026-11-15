package com.eventapp.app.ui.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.wallet.WalletResponse;
import com.eventapp.app.databinding.ActivityUserCreateWalletBinding;
import com.eventapp.app.ui.user.viewmodel.WalletViewModel;
import com.google.gson.JsonElement;

/** Màn hình Kích hoạt Ví ETicket mới (Thiết lập mã PIN 6 số trên Backend và khởi tạo số dư 0 đ). */
public class CreateWalletActivity extends AppCompatActivity {

    private ActivityUserCreateWalletBinding binding;
    private WalletViewModel viewModel;

    public static void start(Context context) {
        Intent intent = new Intent(context, CreateWalletActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserCreateWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnActivate.setOnClickListener(v -> handleActivate());

        observeViewModel();
    }

    private void handleActivate() {
        String pin = binding.etPin.getText() != null ? binding.etPin.getText().toString().trim() : "";
        String confirmPin = binding.etConfirmPin.getText() != null ? binding.etConfirmPin.getText().toString().trim() : "";

        if (!pin.matches("\\d{6}")) {
            Toast.makeText(this, "Mã PIN ví phải gồm đúng 6 chữ số!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin.equals(confirmPin)) {
            Toast.makeText(this, "Mã PIN xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnActivate.setEnabled(false);

        // Bước 1: Tạo ví mới trên Backend (idempotent nếu ví đã tồn tại)
        viewModel.createWallet(() -> {
            // Bước 2: Lưu mã PIN băm BCrypt trên Backend (FR-U13)
            viewModel.setPin(null, pin, new ApiCallback<WalletResponse>() {
                @Override
                public void onSuccess(WalletResponse data, JsonElement meta) {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(CreateWalletActivity.this,
                            "🎉 Kích hoạt Ví ETicket & thiết lập mã PIN thành công!",
                            Toast.LENGTH_LONG).show();
                    WalletActivity.start(CreateWalletActivity.this);
                    finish();
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnActivate.setEnabled(true);
                    Toast.makeText(CreateWalletActivity.this,
                            "❌ " + error.getUserMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void observeViewModel() {
        viewModel.getError().observe(this, err -> {
            if (err == null) return;
            binding.progressBar.setVisibility(View.GONE);
            binding.btnActivate.setEnabled(true);
            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
        });
    }
}
