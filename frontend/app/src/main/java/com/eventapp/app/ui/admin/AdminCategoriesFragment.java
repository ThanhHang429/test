package com.eventapp.app.ui.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.databinding.DialogAdminCategoryBinding;
import com.eventapp.app.databinding.FragmentAdminCategoriesBinding;
import com.eventapp.app.databinding.ItemAdminCategoryBinding;
import com.eventapp.app.ui.common.AppDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Quản lý danh mục sự kiện: tạo, sửa, ẩn/hiện và sắp xếp thứ tự (FR-A09). */
public class AdminCategoriesFragment extends Fragment {

    private FragmentAdminCategoriesBinding binding;
    private final List<AdminCategoryDtos.Category> roots = new ArrayList<>();
    private final Set<Long> expanded = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.btnAddCategory.setOnClickListener(v -> openEditor(null, null));
        load();
    }

    // ------------------------------------------------------------------- tải dữ liệu

    private void load() {
        if (binding == null) {
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        ServiceLocator.get(CategoryRepository.class).adminTree(true,
                new ApiCallback<List<AdminCategoryDtos.Category>>() {
                    @Override
                    public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (binding == null) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        roots.clear();
                        if (data != null) {
                            roots.addAll(data);
                        }
                        render();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        if (binding == null) {
                            return;
                        }
                        binding.progress.setVisibility(View.GONE);
                        render();
                        snack(error.getUserMessage());
                    }
                });
    }

    // ---------------------------------------------------------------------- hiển thị

    private void render() {
        if (binding == null) {
            return;
        }
        binding.categoryList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int total = 0;
        int active = 0;

        for (int i = 0; i < roots.size(); i++) {
            AdminCategoryDtos.Category root = roots.get(i);
            total++;
            if (root.active) {
                active++;
            }
            addRow(inflater, root, 0, roots, i);
            if (expanded.contains(root.id)) {
                List<AdminCategoryDtos.Category> children = root.children == null
                        ? new ArrayList<>() : root.children;
                for (int j = 0; j < children.size(); j++) {
                    AdminCategoryDtos.Category child = children.get(j);
                    total++;
                    if (child.active) {
                        active++;
                    }
                    addRow(inflater, child, 1, children, j);
                }
            }
        }

        boolean empty = total == 0;
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.tvCategoryCount.setText(getString(R.string.admin_cat_count, active, total));
    }

    private void addRow(LayoutInflater inflater, AdminCategoryDtos.Category category, int depth,
                        List<AdminCategoryDtos.Category> siblings, int index) {
        ItemAdminCategoryBinding row = ItemAdminCategoryBinding.inflate(
                inflater, binding.categoryList, false);
        row.getRoot().setPaddingRelative(dp(14 + depth * 18), dp(12), dp(10), dp(10));

        boolean hasChildren = category.children != null && !category.children.isEmpty();
        row.ivExpand.setVisibility(hasChildren ? View.VISIBLE : View.GONE);
        if (hasChildren) {
            row.ivExpand.setImageResource(expanded.contains(category.id)
                    ? R.drawable.ic_common_chevron_up : R.drawable.ic_common_chevron_down);
        }
        row.tvName.setText(category.name);
        row.tvMeta.setText(getString(R.string.admin_cat_meta,
                category.code == null ? "" : category.code, category.eventCount));

        if (category.active) {
            row.tvStatus.setText(R.string.admin_cat_state_active);
            row.tvStatus.setBackgroundResource(R.drawable.bg_admin_pill_success);
            row.tvStatus.setTextColor(requireContext().getColor(R.color.admin_success_text));
        } else {
            row.tvStatus.setText(R.string.admin_cat_state_hidden);
            row.tvStatus.setBackgroundResource(R.drawable.bg_admin_pill_neutral);
            row.tvStatus.setTextColor(requireContext().getColor(R.color.admin_neutral_text));
            row.getRoot().setAlpha(0.7f);
        }

        row.btnUp.setEnabled(index > 0);
        row.btnUp.setAlpha(index > 0 ? 1f : 0.3f);
        row.btnDown.setEnabled(index < siblings.size() - 1);
        row.btnDown.setAlpha(index < siblings.size() - 1 ? 1f : 0.3f);
        row.btnUp.setOnClickListener(v -> move(siblings, index, -1));
        row.btnDown.setOnClickListener(v -> move(siblings, index, 1));

        row.btnAddChild.setVisibility(depth == 0 ? View.VISIBLE : View.GONE);
        row.btnAddChild.setOnClickListener(v -> openEditor(null, category));

        row.btnEdit.setOnClickListener(v -> openEditor(category, null));
        row.btnDelete.setOnClickListener(v -> confirmDelete(category));

        row.btnToggle.setText(category.active
                ? R.string.admin_cat_hide : R.string.admin_cat_unhide);
        row.btnToggle.setTextColor(requireContext().getColor(category.active
                ? R.color.admin_danger_text : R.color.admin_success_text));
        row.btnToggle.setOnClickListener(v -> {
            if (category.active) {
                askHideReason(category);
            } else {
                unhide(category);
            }
        });

        row.getRoot().setOnClickListener(v -> {
            if (!hasChildren) {
                return;
            }
            if (!expanded.remove(category.id)) {
                expanded.add(category.id);
            }
            render();
        });

        binding.categoryList.addView(row.getRoot());
    }

    // ------------------------------------------------------------------------ ghi dữ liệu

    private void move(List<AdminCategoryDtos.Category> siblings, int index, int delta) {
        int target = index + delta;
        if (target < 0 || target >= siblings.size()) {
            return;
        }
        AdminCategoryDtos.Category moving = siblings.remove(index);
        siblings.add(target, moving);

        List<AdminCategoryDtos.OrderItem> items = new ArrayList<>();
        for (int i = 0; i < siblings.size(); i++) {
            items.add(new AdminCategoryDtos.OrderItem(siblings.get(i).id, i));
        }
        ServiceLocator.get(CategoryRepository.class).reorder(items,
                new ApiCallback<List<AdminCategoryDtos.Category>>() {
                    @Override
                    public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        if (data != null) {
                            roots.clear();
                            roots.addAll(data);
                        }
                        render();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                        load();
                    }
                });
    }

    private void askHideReason(AdminCategoryDtos.Category category) {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.admin_cat_hide_reason_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setBackgroundResource(R.drawable.bg_admin_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        FrameLayout container = new FrameLayout(requireContext());
        int padding = dp(20);
        container.setPadding(padding, dp(8), padding, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.admin_cat_hide)
                .setMessage(getString(R.string.admin_cat_hide_message, category.name))
                .setView(container)
                .setNegativeButton(R.string.admin_cat_cancel, null)
                .setPositiveButton(R.string.admin_cat_hide, (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        snack(getString(R.string.admin_cat_reason_required));
                        return;
                    }
                    hide(category.id, reason);
                })
                .show();
    }

    private void confirmDelete(AdminCategoryDtos.Category category) {
        AppDialog.confirm(requireContext(),
                R.drawable.ic_common_delete,
                getString(R.string.admin_cat_delete_title, category.name),
                getString(R.string.admin_cat_delete_body),
                getString(R.string.admin_cat_delete),
                getString(R.string.admin_cat_cancel),
                AppDialog.Tone.DANGER,
                () -> delete(category.id));
    }

    private void delete(long categoryId) {
        ServiceLocator.get(CategoryRepository.class).delete(categoryId,
                new ApiCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        snack(getString(R.string.admin_cat_deleted));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                    }
                });
    }

    private void hide(long categoryId, String reason) {
        ServiceLocator.get(CategoryRepository.class).hide(categoryId, reason,
                new ApiCallback<AdminCategoryDtos.Category>() {
                    @Override
                    public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        snack(getString(R.string.admin_cat_hidden));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                    }
                });
    }

    private void unhide(AdminCategoryDtos.Category category) {
        ServiceLocator.get(CategoryRepository.class).unhide(category.id,
                new ApiCallback<AdminCategoryDtos.Category>() {
                    @Override
                    public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        snack(getString(R.string.admin_cat_unhidden));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                    }
                });
    }

    // -------------------------------------------------------------------- hộp thoại

    private void openEditor(@Nullable AdminCategoryDtos.Category editing,
                            @Nullable AdminCategoryDtos.Category parent) {
        DialogAdminCategoryBinding dialogBinding = DialogAdminCategoryBinding.inflate(
                getLayoutInflater());
        boolean isEdit = editing != null;
        dialogBinding.tvTitle.setText(isEdit
                ? R.string.admin_cat_edit : R.string.admin_cat_add);

        dialogBinding.labelCode.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        dialogBinding.etCode.setVisibility(isEdit ? View.GONE : View.VISIBLE);

        List<AdminCategoryDtos.Category> parents = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.admin_cat_parent_none));
        for (AdminCategoryDtos.Category root : roots) {
            if (isEdit && root.id == editing.id) {
                continue;
            }
            parents.add(root);
            labels.add(root.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBinding.spParent.setAdapter(adapter);

        int selectedParent = 0;
        if (isEdit) {
            dialogBinding.etName.setText(editing.name);
            dialogBinding.etSlug.setText(editing.slug);
            dialogBinding.etDescription.setText(editing.description);
            dialogBinding.etSortOrder.setText(String.valueOf(editing.sortOrder));
            if (editing.parentCategoryId != null) {
                for (int i = 0; i < parents.size(); i++) {
                    if (parents.get(i).id == editing.parentCategoryId) {
                        selectedParent = i + 1;
                        break;
                    }
                }
            }
        } else if (parent != null) {
            for (int i = 0; i < parents.size(); i++) {
                if (parents.get(i).id == parent.id) {
                    selectedParent = i + 1;
                    break;
                }
            }
            dialogBinding.etSortOrder.setText(String.valueOf(parent.children == null
                    ? 0 : parent.children.size()));
        } else {
            dialogBinding.etSortOrder.setText(String.valueOf(roots.size()));
        }
        dialogBinding.spParent.setSelection(selectedParent);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.admin_cat_cancel, null)
                .setPositiveButton(R.string.admin_cat_save, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = text(dialogBinding.etName);
            String slug = text(dialogBinding.etSlug);
            String description = text(dialogBinding.etDescription);
            String code = text(dialogBinding.etCode).toUpperCase(Locale.ROOT);
            int order = parseOrder(text(dialogBinding.etSortOrder),
                    isEdit ? editing.sortOrder : roots.size());

            if (name.isEmpty()) {
                showError(dialogBinding, getString(R.string.admin_cat_name_required));
                return;
            }
            if (!isEdit && code.isEmpty()) {
                showError(dialogBinding, getString(R.string.admin_cat_code_required));
                return;
            }
            int parentIndex = dialogBinding.spParent.getSelectedItemPosition();
            Long parentId = parentIndex <= 0 ? null : parents.get(parentIndex - 1).id;

            dialog.dismiss();
            if (isEdit) {
                AdminCategoryDtos.UpdateRequest request = new AdminCategoryDtos.UpdateRequest();
                request.categoryName = name;
                request.slug = slug.isEmpty() ? null : slug;
                request.description = description.isEmpty() ? null : description;
                request.sortOrder = order;
                request.version = DateTimeUtils.toMillis(editing.updatedAt);
                update(editing.id, request);
            } else {
                AdminCategoryDtos.CreateRequest request = new AdminCategoryDtos.CreateRequest();
                request.categoryCode = code;
                request.categoryName = name;
                request.slug = slug.isEmpty() ? null : slug;
                request.description = description.isEmpty() ? null : description;
                request.parentCategoryId = parentId;
                request.sortOrder = order;
                create(request);
            }
        });
    }

    private void create(AdminCategoryDtos.CreateRequest request) {
        ServiceLocator.get(CategoryRepository.class).create(request,
                new ApiCallback<AdminCategoryDtos.Category>() {
                    @Override
                    public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        snack(getString(R.string.admin_cat_created));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                    }
                });
    }

    private void update(long id, AdminCategoryDtos.UpdateRequest request) {
        ServiceLocator.get(CategoryRepository.class).update(id, request,
                new ApiCallback<AdminCategoryDtos.Category>() {
                    @Override
                    public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        snack(getString(R.string.admin_cat_updated));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException error) {
                        snack(error.getUserMessage());
                    }
                });
    }

    // ----------------------------------------------------------------------- tiện ích

    private void showError(DialogAdminCategoryBinding dialogBinding, String message) {
        dialogBinding.tvError.setText(message);
        dialogBinding.tvError.setVisibility(View.VISIBLE);
    }

    private String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private int parseOrder(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void snack(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
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
