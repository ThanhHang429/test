package com.eventapp.app.ui.admin.category;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.eventapp.app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/** Create/edit form for FR-A09 categories; returns the values through a fragment result. */
public class CategoryFormDialog extends DialogFragment {

    public static final String REQUEST_KEY = "category_form_request";
    public static final String RESULT_ID = "id";
    public static final String RESULT_NAME = "name";
    public static final String RESULT_PARENT_ID = "parent_id";
    public static final String RESULT_ICON = "icon";
    public static final String RESULT_COLOR = "color";
    public static final String RESULT_ACTIVE = "active";

    private static final String ARG_ID = "arg_id";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_PARENT_ID = "arg_parent_id";
    private static final String ARG_ICON = "arg_icon";
    private static final String ARG_COLOR = "arg_color";
    private static final String ARG_ACTIVE = "arg_active";
    private static final String ARG_PARENT_IDS = "arg_parent_ids";
    private static final String ARG_PARENT_NAMES = "arg_parent_names";

    private static final int[] ICONS = {
            R.drawable.ic_ds_event, R.drawable.ic_ds_filter, R.drawable.ic_ds_chart,
            R.drawable.ic_ds_flag, R.drawable.ic_ds_users, R.drawable.ic_ds_ticket,
            R.drawable.ic_ds_image, R.drawable.ic_ds_payment
    };

    private static final int[] COLORS = {
            R.color.ds_primary, R.color.ds_secondary, R.color.ds_organizer,
            R.color.ds_success, R.color.ds_warning, R.color.ds_danger,
            R.color.ds_info, R.color.ds_cta
    };

    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private ChipGroup parentGroup;
    private MaterialSwitch activeSwitch;
    private TextView errorView;

    private final List<ImageView> iconViews = new ArrayList<>();
    private final List<View> colorViews = new ArrayList<>();
    private int selectedIcon;
    private int selectedColor;

    public static CategoryFormDialog newInstance(long id, String name, long parentId,
                                                 int iconRes, int colorRes, boolean active,
                                                 long[] parentIds, String[] parentNames) {
        CategoryFormDialog dialog = new CategoryFormDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        args.putString(ARG_NAME, name);
        args.putLong(ARG_PARENT_ID, parentId);
        args.putInt(ARG_ICON, iconRes);
        args.putInt(ARG_COLOR, colorRes);
        args.putBoolean(ARG_ACTIVE, active);
        args.putLongArray(ARG_PARENT_IDS, parentIds);
        args.putStringArray(ARG_PARENT_NAMES, parentNames);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        View view = getLayoutInflater()
                .inflate(R.layout.dialog_category_form, null);

        nameLayout = view.findViewById(R.id.layoutCategoryName);
        nameInput = view.findViewById(R.id.etCategoryName);
        parentGroup = view.findViewById(R.id.chipGroupCategoryParent);
        activeSwitch = view.findViewById(R.id.switchCategoryActive);
        errorView = view.findViewById(R.id.tvCategoryFormError);

        selectedIcon = args.getInt(ARG_ICON, ICONS[0]);
        selectedColor = args.getInt(ARG_COLOR, COLORS[0]);

        buildParentChips(args);
        buildIconPicker(view.findViewById(R.id.layoutCategoryIcons));
        buildColorPicker(view.findViewById(R.id.layoutCategoryColors));

        nameInput.setText(args.getString(ARG_NAME, ""));
        activeSwitch.setChecked(args.getBoolean(ARG_ACTIVE, true));

        boolean editing = args.getLong(ARG_ID, 0L) != 0L;
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(editing ? R.string.hc_cat_edit : R.string.hc_cat_add)
                .setView(view)
                .setNegativeButton(R.string.hc_cat_cancel, null)
                .setPositiveButton(R.string.hc_cat_save, null)
                .create();
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> submit()));
        return dialog;
    }

    private void buildParentChips(Bundle args) {
        parentGroup.removeAllViews();
        long currentParent = args.getLong(ARG_PARENT_ID, 0L);
        long editingId = args.getLong(ARG_ID, 0L);

        addParentChip(0L, getString(R.string.hc_cat_parent_none), currentParent == 0L);

        long[] ids = args.getLongArray(ARG_PARENT_IDS);
        String[] names = args.getStringArray(ARG_PARENT_NAMES);
        if (ids == null || names == null) {
            return;
        }
        for (int i = 0; i < ids.length && i < names.length; i++) {
            if (ids[i] == editingId) {
                continue;
            }
            addParentChip(ids[i], names[i], currentParent == ids[i]);
        }
    }

    private void addParentChip(long id, String label, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setTag(id);
        chip.setChipBackgroundColorResource(R.color.ds_chip_bg);
        parentGroup.addView(chip);
    }

    private void buildIconPicker(LinearLayout container) {
        iconViews.clear();
        container.removeAllViews();
        for (int iconRes : ICONS) {
            ImageView icon = new ImageView(requireContext());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(44), dp(44));
            params.setMarginEnd(dp(8));
            icon.setLayoutParams(params);
            icon.setPadding(dp(10), dp(10), dp(10), dp(10));
            icon.setBackgroundResource(R.drawable.bg_ds_avatar);
            icon.setImageResource(iconRes);
            icon.setOnClickListener(v -> {
                selectedIcon = iconRes;
                renderIcons();
            });
            iconViews.add(icon);
            container.addView(icon);
        }
        renderIcons();
    }

    private void renderIcons() {
        for (int i = 0; i < iconViews.size(); i++) {
            ImageView icon = iconViews.get(i);
            boolean selected = ICONS[i] == selectedIcon;
            icon.setAlpha(selected ? 1f : 0.45f);
            icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    requireContext(), selected ? R.color.ds_primary : R.color.ds_text_muted)));
        }
    }

    private void buildColorPicker(LinearLayout container) {
        colorViews.clear();
        container.removeAllViews();
        for (int colorRes : COLORS) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(dp(36), dp(36));
            params.setMarginEnd(dp(10));
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_ds_avatar);
            dot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), colorRes)));
            dot.setOnClickListener(v -> {
                selectedColor = colorRes;
                renderColors();
            });
            colorViews.add(dot);
            container.addView(dot);
        }
        renderColors();
    }

    private void renderColors() {
        for (int i = 0; i < colorViews.size(); i++) {
            colorViews.get(i).setAlpha(COLORS[i] == selectedColor ? 1f : 0.35f);
        }
    }

    private void submit() {
        nameLayout.setError(null);
        errorView.setVisibility(View.GONE);

        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (name.length() < 2 || name.length() > 50) {
            nameLayout.setError(getString(R.string.hc_cat_name_error));
            nameInput.requestFocus();
            return;
        }

        long parentId = 0L;
        int checkedId = parentGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = parentGroup.findViewById(checkedId);
            if (chip != null && chip.getTag() instanceof Long) {
                parentId = (Long) chip.getTag();
            }
        }

        Bundle args = requireArguments();
        Bundle result = new Bundle();
        result.putLong(RESULT_ID, args.getLong(ARG_ID, 0L));
        result.putString(RESULT_NAME, name);
        result.putLong(RESULT_PARENT_ID, parentId);
        result.putInt(RESULT_ICON, selectedIcon);
        result.putInt(RESULT_COLOR, selectedColor);
        result.putBoolean(RESULT_ACTIVE, activeSwitch.isChecked());
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
