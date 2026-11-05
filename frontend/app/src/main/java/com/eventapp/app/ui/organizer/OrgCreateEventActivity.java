package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.databinding.ActivityOrgCreateEventBinding;
import com.eventapp.app.ui.organizer.viewmodel.OrgCreateEventViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Form tạo sự kiện bước 1 (demo giao diện). Danh mục lấy từ API để luôn khớp dữ liệu Admin. */
public class OrgCreateEventActivity extends AppCompatActivity {

    private ActivityOrgCreateEventBinding binding;
    private OrgCreateEventViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(OrgCreateEventViewModel.class);
        viewModel.getCategories().observe(this, this::renderCategories);
        viewModel.loadCategories();
    }

    private void renderCategories(@Nullable List<CategoryNodeUiModel> nodes) {
        binding.categoryChips.removeAllViews();
        boolean empty = nodes == null || nodes.isEmpty();
        binding.tvCategoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        for (CategoryNodeUiModel node : nodes) {
            Chip chip = (Chip) LayoutInflater.from(this)
                    .inflate(R.layout.item_org_select_chip, binding.categoryChips, false);
            chip.setText(node.name);
            binding.categoryChips.addView(chip);
        }
    }

    private void snack(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}
