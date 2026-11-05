package com.eventapp.app.ui.organizer;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.databinding.ActivityOrgCreateEventBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/** Form tạo sự kiện bước 1 (demo giao diện). Danh mục lấy từ API để luôn khớp dữ liệu Admin. */
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

        loadCategories();
    }

    private void loadCategories() {
        ServiceLocator.get(CategoryRepository.class).tree(
                new ApiCallback<List<CatalogDtos.CategoryNode>>() {
                    @Override
                    public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        renderCategories(data);
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        renderCategories(null);
                    }
                });
    }

    private void renderCategories(@Nullable List<CatalogDtos.CategoryNode> nodes) {
        binding.categoryChips.removeAllViews();
        boolean empty = nodes == null || nodes.isEmpty();
        binding.tvCategoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            return;
        }
        for (CatalogDtos.CategoryNode node : nodes) {
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
