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
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.DialogAdminCategoryBinding;
import com.eventapp.app.databinding.FragmentAdminCategoriesBinding;
import com.eventapp.app.databinding.ItemAdminCategoryBinding;
import com.eventapp.app.ui.admin.viewmodel.AdminCategoriesViewModel;
import com.eventapp.app.ui.common.AppDialog;
import com.eventapp.app.ui.model.AdminCategoryUiModel;
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
    private AdminCategoriesViewModel viewModel;
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

        viewModel = new ViewModelProvider(this).get(AdminCategoriesViewModel.class);
        observe();
        viewModel.load();
    }

    private void observe() {
        viewModel.getRoots().observe(getViewLifecycleOwner(), data -> render());
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            if (busy) {
                binding.tvEmpty.setVisibility(View.GONE);
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null || binding == null) {
                return;
            }
            viewModel.consumeError();
            render();
            snack(error.getUserMessage());
        });
        viewModel.getNotice().observe(getViewLifecycleOwner(), notice -> {
            if (notice == null) {
                return;
            }
            viewModel.consumeNotice();
            snack(noticeText(notice));
        });
    }

    private String noticeText(AdminCategoriesViewModel.Notice notice) {
        switch (notice) {
            case CREATED:
                return getString(R.string.admin_cat_created);
            case UPDATED:
                return getString(R.string.admin_cat_updated);
            case DELETED:
                return getString(R.string.admin_cat_deleted);
            case HIDDEN:
                return getString(R.string.admin_cat_hidden);
            case UNHIDDEN:
            default:
                return getString(R.string.admin_cat_unhidden);
        }
    }

    // ---------------------------------------------------------------------- hiển thị

    private void render() {
        if (binding == null || viewModel == null) {
            return;
        }
        List<AdminCategoryUiModel> roots = viewModel.getRoots().getValue();
        if (roots == null) {
            roots = new ArrayList<>();
        }
        binding.categoryList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        int total = 0;
        int active = 0;

        for (int i = 0; i < roots.size(); i++) {
            AdminCategoryUiModel root = roots.get(i);
            total++;
            if (root.active) {
                active++;
            }
            addRow(inflater, root, 0, roots, i);
            if (expanded.contains(root.id)) {
                List<AdminCategoryUiModel> children = root.children;
                for (int j = 0; j < children.size(); j++) {
                    AdminCategoryUiModel child = children.get(j);
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

    private void addRow(LayoutInflater inflater, AdminCategoryUiModel category, int depth,
                        List<AdminCategoryUiModel> siblings, int index) {
        ItemAdminCategoryBinding row = ItemAdminCategoryBinding.inflate(
                inflater, binding.categoryList, false);
        row.getRoot().setPaddingRelative(dp(14 + depth * 18), dp(12), dp(10), dp(10));

        boolean hasChildren = category.hasChildren();
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

    private void move(List<AdminCategoryUiModel> siblings, int index, int delta) {
        viewModel.move(siblings, index, delta);
    }

    private void askHideReason(AdminCategoryUiModel category) {
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

    private void confirmDelete(AdminCategoryUiModel category) {
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
        viewModel.delete(categoryId);
    }

    private void hide(long categoryId, String reason) {
        viewModel.hide(categoryId, reason);
    }

    private void unhide(AdminCategoryUiModel category) {
        viewModel.unhide(category.id);
    }

    // -------------------------------------------------------------------- hộp thoại

    private void openEditor(@Nullable AdminCategoryUiModel editing,
                            @Nullable AdminCategoryUiModel parent) {
        DialogAdminCategoryBinding dialogBinding = DialogAdminCategoryBinding.inflate(
                getLayoutInflater());
        boolean isEdit = editing != null;
        dialogBinding.tvTitle.setText(isEdit
                ? R.string.admin_cat_edit : R.string.admin_cat_add);

        dialogBinding.labelCode.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        dialogBinding.etCode.setVisibility(isEdit ? View.GONE : View.VISIBLE);

        List<AdminCategoryUiModel> currentRoots = viewModel.getRoots().getValue();
        final List<AdminCategoryUiModel> roots = currentRoots == null
                ? new ArrayList<>() : currentRoots;
        List<AdminCategoryUiModel> parents = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.admin_cat_parent_none));
        for (AdminCategoryUiModel root : roots) {
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
            dialogBinding.etSortOrder.setText(String.valueOf(parent.children.size()));
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
                update(editing.id, name, slug.isEmpty() ? null : slug,
                        description.isEmpty() ? null : description, order,
                        editing.updatedAtMillis);
            } else {
                create(code, name, slug.isEmpty() ? null : slug,
                        description.isEmpty() ? null : description, parentId, order);
            }
        });
    }

    private void create(String code, String name, String slug, String description,
                        Long parentId, int order) {
        viewModel.create(code, name, slug, description, parentId, order);
    }

    private void update(long id, String name, String slug, String description, int order,
                        long version) {
        viewModel.update(id, name, slug, description, order, version);
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
