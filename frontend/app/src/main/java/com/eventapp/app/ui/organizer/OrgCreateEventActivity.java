package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.databinding.ActivityOrgCreateEventBinding;
import com.google.android.material.snackbar.Snackbar;

/** Form tạo sự kiện bước 1 (demo giao diện). */
public class OrgCreateEventActivity extends AppCompatActivity {

    private ActivityOrgCreateEventBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrgCreateEventBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSaveDraftTop.setOnClickListener(v -> {
            snack("Đã lưu bản nháp sự kiện");
            finish();
        });
        binding.btnContinue.setOnClickListener(v -> {
            String name = binding.etEventName.getText() == null
                    ? "" : binding.etEventName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                binding.etEventName.setError("Nhập tên sự kiện");
                binding.etEventName.requestFocus();
                return;
            }
            snack("Đã lưu · chuyển sang bước 2 (demo)");
        });
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
