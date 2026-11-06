package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.eventapp.app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * FR-O09 classification block for the organizer event form.
 *
 * <p>Rules: exactly one primary category, at most two secondary categories and at most five
 * tags. Every tag is 2-30 characters after normalization, unique, and free of links, phone
 * numbers and the local banned-word sample. Hidden categories stay visible on old events but
 * cannot be selected again. The server remains the source of truth for the banned-word list.</p>
 */
public class ClassificationSectionView extends LinearLayout {

    public interface OnClassificationChangedListener {
        void onClassificationChanged();
    }

    /** Minimal display model so the view does not depend on the category DTO. */
    public static class CategoryOption {
        public final long id;
        public final String name;
        public final boolean active;
        @DrawableRes
        public final int iconRes;

        public CategoryOption(long id, String name, boolean active, @DrawableRes int iconRes) {
            this.id = id;
            this.name = name == null ? "" : name;
            this.active = active;
            this.iconRes = iconRes;
        }
    }

    private static final int MAX_SECONDARY = 2;
    private static final int MAX_TAGS = 5;
    private static final int MIN_TAG_LENGTH = 2;
    private static final int MAX_TAG_LENGTH = 30;

    private static final String STATE_PRIMARY = "state_primary";
    private static final String STATE_SECONDARY = "state_secondary";
    private static final String STATE_TAGS = "state_tags";

    private static final Pattern URL_PATTERN = Pattern.compile(
            ".*(https?://|www\\.|\\b[a-z0-9-]+\\.(com|vn|net|org|io)\\b).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            ".*(\\+?\\d[\\d\\s.\\-]{7,}\\d).*");
    private static final String[] BANNED_WORDS = {
            "lừa đảo", "cờ bạc", "cá độ", "ma túy", "khiêu dâm", "hàng giả", "bán hàng giả"
    };

    private final List<CategoryOption> categories = new ArrayList<>();
    private final Set<Long> secondaryIds = new LinkedHashSet<>();
    private final List<String> tags = new ArrayList<>();
    private final List<String> suggestions = new ArrayList<>();

    private ChipGroup primaryGroup;
    private ChipGroup secondaryGroup;
    private ChipGroup tagGroup;
    private ChipGroup suggestionGroup;
    private TextInputLayout tagInputLayout;
    private TextInputEditText tagInput;
    private MaterialButton addTagButton;
    private TextView primaryError;
    private TextView secondaryError;
    private TextView secondaryCount;
    private TextView tagCount;
    private TextView tagsEmpty;
    private TextView suggestionsLabel;

    private Long primaryId;
    private boolean editable = true;
    private boolean binding;
    private OnClassificationChangedListener listener;

    public ClassificationSectionView(Context context) {
        this(context, null);
    }

    public ClassificationSectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClassificationSectionView(Context context, @Nullable AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_ds_classification, this, true);

        primaryGroup = findViewById(R.id.chipGroupO09Primary);
        secondaryGroup = findViewById(R.id.chipGroupO09Secondary);
        tagGroup = findViewById(R.id.chipGroupO09Tags);
        suggestionGroup = findViewById(R.id.chipGroupO09Suggestions);
        tagInputLayout = findViewById(R.id.tilO09Tag);
        tagInput = findViewById(R.id.etO09Tag);
        addTagButton = findViewById(R.id.btnO09AddTag);
        primaryError = findViewById(R.id.tvO09PrimaryError);
        secondaryError = findViewById(R.id.tvO09SecondaryError);
        secondaryCount = findViewById(R.id.tvO09SecondaryCount);
        tagCount = findViewById(R.id.tvO09TagCount);
        tagsEmpty = findViewById(R.id.tvO09TagsEmpty);
        suggestionsLabel = findViewById(R.id.tvO09SuggestionsLabel);

        addTagButton.setOnClickListener(v -> addTagFromInput());
        tagInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTagFromInput();
                return true;
            }
            return false;
        });
        tagInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (tagInputLayout.getError() != null) {
                    tagInputLayout.setError(null);
                }
            }
        });

        updateCounters();
        renderTags();
        renderSuggestions();
    }

    public void setOnClassificationChangedListener(
            @Nullable OnClassificationChangedListener listener) {
        this.listener = listener;
    }

    public void setCategories(@Nullable List<CategoryOption> options) {
        categories.clear();
        if (options != null) {
            for (CategoryOption option : options) {
                if (option != null && option.id > 0L) {
                    categories.add(option);
                }
            }
        }
        rebuildCategoryChips();
        applySelectionState();
    }

    public void setSuggestions(@Nullable List<String> values) {
        suggestions.clear();
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeTag(value);
                if (normalized.length() >= MIN_TAG_LENGTH && !suggestions.contains(normalized)) {
                    suggestions.add(normalized);
                }
            }
        }
        renderSuggestions();
    }

    public void setValue(long primaryId, @Nullable List<Long> secondary,
                         @Nullable List<String> values) {
        this.primaryId = primaryId > 0L ? primaryId : null;
        secondaryIds.clear();
        if (secondary != null) {
            for (Long id : secondary) {
                if (id != null && id > 0L && secondaryIds.size() < MAX_SECONDARY) {
                    secondaryIds.add(id);
                }
            }
        }
        tags.clear();
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeTag(value);
                if (isStorableTag(normalized) && !tags.contains(normalized)
                        && tags.size() < MAX_TAGS) {
                    tags.add(normalized);
                }
            }
        }
        applySelectionState();
        renderTags();
        updateCounters();
        clearErrors();
    }

    /** @return primary category id or 0 when nothing is selected. */
    public long getPrimaryId() {
        return primaryId == null ? 0L : primaryId;
    }

    public List<Long> getSecondaryIds() {
        return new ArrayList<>(secondaryIds);
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    /** True when the user (or a restored instance state) already has a classification. */
    public boolean hasValue() {
        return primaryId != null || !secondaryIds.isEmpty() || !tags.isEmpty();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        rebuildCategoryChips();
        applySelectionState();
        tagInput.setEnabled(editable);
        addTagButton.setEnabled(editable);
        addTagButton.setAlpha(editable ? 1f : 0.45f);
        renderSuggestions();
    }

    /** Validates all O09 rules and paints the inline errors. */
    public boolean validate() {
        clearErrors();
        boolean valid = true;
        if (primaryId == null) {
            primaryError.setText(R.string.hc_o09_primary_required);
            primaryError.setVisibility(VISIBLE);
            valid = false;
        }
        if (secondaryIds.size() > MAX_SECONDARY) {
            secondaryError.setText(R.string.hc_o09_secondary_limit);
            secondaryError.setVisibility(VISIBLE);
            valid = false;
        }
        if (tags.size() > MAX_TAGS) {
            tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_max));
            valid = false;
        }
        for (String tag : tags) {
            if (!isStorableTag(tag)) {
                tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_error_invalid));
                valid = false;
                break;
            }
        }
        return valid;
    }

    public void clearErrors() {
        primaryError.setVisibility(GONE);
        secondaryError.setVisibility(GONE);
        tagInputLayout.setError(null);
    }

    private void rebuildCategoryChips() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        ColorStateList iconTint = AppCompatResources.getColorStateList(getContext(),
                R.color.ds_chip_text);

        binding = true;
        primaryGroup.removeAllViews();
        secondaryGroup.removeAllViews();
        for (CategoryOption option : categories) {
            Chip primaryChip = createCategoryChip(inflater, primaryGroup, option, iconTint);
            primaryChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (binding || !isChecked) {
                    return;
                }
                primaryId = option.id;
                primaryError.setVisibility(GONE);
                notifyChanged();
            });
            primaryGroup.addView(primaryChip);

            Chip secondaryChip = createCategoryChip(inflater, secondaryGroup, option, iconTint);
            secondaryChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (binding) {
                    return;
                }
                if (isChecked) {
                    if (secondaryIds.size() >= MAX_SECONDARY) {
                        binding = true;
                        buttonView.setChecked(false);
                        binding = false;
                        secondaryError.setText(R.string.hc_o09_secondary_limit);
                        secondaryError.setVisibility(VISIBLE);
                        return;
                    }
                    secondaryIds.add(option.id);
                } else {
                    secondaryIds.remove(option.id);
                }
                secondaryError.setVisibility(GONE);
                updateCounters();
                notifyChanged();
            });
            secondaryGroup.addView(secondaryChip);
        }
        binding = false;
    }

    private Chip createCategoryChip(LayoutInflater inflater, ChipGroup parent,
                                    CategoryOption option, ColorStateList iconTint) {
        Chip chip = (Chip) inflater.inflate(R.layout.view_ds_filter_chip, parent, false);
        chip.setId(View.generateViewId());
        chip.setText(option.active ? option.name
                : option.name + " (" + getContext().getString(R.string.hc_o09_hidden_category) + ")");
        chip.setTag(option.id);
        if (option.iconRes != 0) {
            chip.setChipIconResource(option.iconRes);
            chip.setChipIconVisible(true);
            chip.setChipIconSize(dp(16));
            chip.setChipIconTint(iconTint);
        }
        chip.setEnabled(editable && option.active);
        chip.setAlpha(option.active ? 1f : 0.55f);
        return chip;
    }

    private void applySelectionState() {
        binding = true;
        try {
            for (int i = 0; i < primaryGroup.getChildCount(); i++) {
                View child = primaryGroup.getChildAt(i);
                if (child instanceof Chip && child.getTag() instanceof Long) {
                    ((Chip) child).setChecked(primaryId != null
                            && primaryId.equals(child.getTag()));
                }
            }
            for (int i = 0; i < secondaryGroup.getChildCount(); i++) {
                View child = secondaryGroup.getChildAt(i);
                if (child instanceof Chip && child.getTag() instanceof Long) {
                    long id = (Long) child.getTag();
                    boolean selected = secondaryIds.contains(id);
                    ((Chip) child).setChecked(selected);
                    // An old event may still carry a hidden secondary category. Keep it
                    // visible and removable, but never allow adding it again.
                    child.setEnabled(editable && (selected || isActiveCategory(id)));
                    child.setAlpha(isActiveCategory(id) ? 1f : 0.55f);
                }
            }
        } finally {
            binding = false;
        }
        updateCounters();
    }

    private boolean isActiveCategory(long id) {
        for (CategoryOption option : categories) {
            if (option.id == id) {
                return option.active;
            }
        }
        return false;
    }

    private void addTagFromInput() {
        String normalized = normalizeTag(tagInput.getText() == null
                ? "" : tagInput.getText().toString());
        if (normalized.isEmpty()) {
            return;
        }
        if (tags.size() >= MAX_TAGS) {
            tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_max));
            return;
        }
        if (normalized.length() < MIN_TAG_LENGTH || normalized.length() > MAX_TAG_LENGTH) {
            tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_error_length));
            return;
        }
        for (String tag : tags) {
            if (tag.equalsIgnoreCase(normalized)) {
                tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_error_duplicate));
                return;
            }
        }
        if (!isContentAllowed(normalized)) {
            tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_error_invalid));
            return;
        }
        tags.add(normalized);
        tagInput.setText("");
        tagInputLayout.setError(null);
        renderTags();
        updateCounters();
        notifyChanged();
    }

    private void addTag(String value) {
        String normalized = normalizeTag(value);
        if (normalized.isEmpty() || tags.size() >= MAX_TAGS) {
            if (tags.size() >= MAX_TAGS) {
                tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_max));
            }
            return;
        }
        for (String tag : tags) {
            if (tag.equalsIgnoreCase(normalized)) {
                return;
            }
        }
        if (!isStorableTag(normalized)) {
            tagInputLayout.setError(getContext().getString(R.string.hc_o09_tag_error_invalid));
            return;
        }
        tags.add(normalized);
        renderTags();
        updateCounters();
        notifyChanged();
    }

    private void removeTag(String value) {
        tags.remove(value);
        renderTags();
        updateCounters();
        notifyChanged();
    }

    private void renderTags() {
        tagGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String tag : tags) {
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_tag_chip, tagGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(tag);
            chip.setCloseIconVisible(editable);
            chip.setCloseIconContentDescription(getContext().getString(
                    R.string.hc_o09_tag_remove, tag));
            chip.setOnCloseIconClickListener(v -> removeTag(tag));
            tagGroup.addView(chip);
        }
        tagsEmpty.setVisibility(tags.isEmpty() ? VISIBLE : GONE);
        tagGroup.setVisibility(tags.isEmpty() ? GONE : VISIBLE);
    }

    private void renderSuggestions() {
        suggestionGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String suggestion : suggestions) {
            if (tags.contains(suggestion)) {
                continue;
            }
            Chip chip = (Chip) inflater.inflate(R.layout.view_ds_tag_chip, suggestionGroup, false);
            chip.setId(View.generateViewId());
            chip.setText(suggestion);
            chip.setCloseIconVisible(false);
            chip.setEnabled(editable && tags.size() < MAX_TAGS);
            chip.setAlpha(editable ? 1f : 0.5f);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> addTag(suggestion));
            suggestionGroup.addView(chip);
        }
        boolean show = suggestionGroup.getChildCount() > 0;
        suggestionsLabel.setVisibility(show ? VISIBLE : GONE);
        suggestionGroup.setVisibility(show ? VISIBLE : GONE);
    }

    private void updateCounters() {
        secondaryCount.setText(getContext().getString(R.string.hc_o09_secondary_count,
                secondaryIds.size()));
        tagCount.setText(getContext().getString(R.string.hc_o09_tag_count, tags.size()));
    }

    private void notifyChanged() {
        renderSuggestions();
        if (listener != null) {
            listener.onClassificationChanged();
        }
    }

    private static String normalizeTag(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(new Locale("vi", "VN"));
        return normalized;
    }

    private static boolean isContentAllowed(String tag) {
        if (URL_PATTERN.matcher(tag).matches() || PHONE_PATTERN.matcher(tag).matches()) {
            return false;
        }
        for (String banned : BANNED_WORDS) {
            if (tag.contains(banned)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isStorableTag(String tag) {
        return tag != null
                && tag.length() >= MIN_TAG_LENGTH
                && tag.length() <= MAX_TAG_LENGTH
                && isContentAllowed(tag);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("super", super.onSaveInstanceState());
        state.putLong(STATE_PRIMARY, getPrimaryId());
        state.putLongArray(STATE_SECONDARY,
                secondaryIds.stream().mapToLong(Long::longValue).toArray());
        state.putStringArrayList(STATE_TAGS, new ArrayList<>(tags));
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }
        Bundle bundle = (Bundle) state;
        super.onRestoreInstanceState(bundle.getParcelable("super"));
        long restoredPrimary = bundle.getLong(STATE_PRIMARY, 0L);
        primaryId = restoredPrimary > 0L ? restoredPrimary : null;
        secondaryIds.clear();
        long[] restoredSecondary = bundle.getLongArray(STATE_SECONDARY);
        if (restoredSecondary != null) {
            for (long id : restoredSecondary) {
                if (id > 0L && secondaryIds.size() < MAX_SECONDARY) {
                    secondaryIds.add(id);
                }
            }
        }
        tags.clear();
        ArrayList<String> restoredTags = bundle.getStringArrayList(STATE_TAGS);
        if (restoredTags != null) {
            for (String tag : restoredTags) {
                if (isStorableTag(tag) && tags.size() < MAX_TAGS) {
                    tags.add(tag);
                }
            }
        }
        applySelectionState();
        renderTags();
        updateCounters();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
