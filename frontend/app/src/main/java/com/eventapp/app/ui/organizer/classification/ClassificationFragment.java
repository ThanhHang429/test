package com.eventapp.app.ui.organizer.classification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.classification.ClassificationDtos;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/** FR-O09 — chọn danh mục chính/phụ và nhãn phụ rồi lưu qua API-12. */
public class ClassificationFragment extends Fragment {

    private ClassificationViewModel viewModel;

    private LinearProgressIndicator progress;
    private TextView errorView;
    private View retryButton;
    private View form;
    private View lockedNote;
    private View reReviewNote;
    private ChipGroup primaryGroup;
    private ChipGroup secondaryGroup;
    private ChipGroup tagsGroup;
    private ChipGroup suggestionsGroup;
    private TextInputLayout tagLayout;
    private TextInputEditText tagInput;
    private MaterialButton saveButton;

    private List<CatalogDtos.CategoryNode> categories = new ArrayList<>();
    private List<ClassificationDtos.TagSuggestion> suggestions = new ArrayList<>();
    private ClassificationViewModel.State lastState;
    private boolean rendering;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_classification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ClassificationViewModel.class);
        bindViews(view);

        view.findViewById(R.id.btnClassificationBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        retryButton.setOnClickListener(v -> viewModel.retry());
        saveButton.setOnClickListener(v -> viewModel.save());
        view.findViewById(R.id.btnClassificationAddTag).setOnClickListener(v ->
                addTypedTag());

        observe();

        Bundle args = getArguments();
        long eventId = args == null ? -1L : args.getLong("arg_event_id", -1L);
        viewModel.load(eventId);
    }

    private void bindViews(View view) {
        progress = view.findViewById(R.id.progressClassification);
        errorView = view.findViewById(R.id.tvClassificationError);
        retryButton = view.findViewById(R.id.btnClassificationRetry);
        form = view.findViewById(R.id.layoutClassificationForm);
        lockedNote = view.findViewById(R.id.tvClassificationLocked);
        reReviewNote = view.findViewById(R.id.tvClassificationReReview);
        primaryGroup = view.findViewById(R.id.chipGroupClassificationPrimary);
        secondaryGroup = view.findViewById(R.id.chipGroupClassificationSecondary);
        tagsGroup = view.findViewById(R.id.chipGroupClassificationTags);
        suggestionsGroup = view.findViewById(R.id.chipGroupClassificationSuggestions);
        tagLayout = view.findViewById(R.id.tilClassificationTag);
        tagInput = view.findViewById(R.id.etClassificationTag);
        saveButton = view.findViewById(R.id.btnClassificationSave);
    }

    private void observe() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), nodes -> {
            categories = nodes == null ? new ArrayList<>() : nodes;
            renderCategories();
        });

        viewModel.getSuggestions().observe(getViewLifecycleOwner(), nodes -> {
            suggestions = nodes == null ? new ArrayList<>() : nodes;
            renderSuggestions();
        });

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            lastState = state;
            renderState();
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), failed -> {
            boolean hasError = Boolean.TRUE.equals(failed);
            errorView.setVisibility(hasError ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(hasError ? View.VISIBLE : View.GONE);
            form.setVisibility(hasError ? View.GONE : View.VISIBLE);
        });

        viewModel.getSaving().observe(getViewLifecycleOwner(), saving -> {
            boolean busy = Boolean.TRUE.equals(saving);
            saveButton.setEnabled(!busy && (lastState == null || !lastState.locked));
            saveButton.setText(busy
                    ? R.string.org_classification_saving
                    : R.string.org_classification_save);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message == null) {
                return;
            }
            viewModel.consumeMessage();
            if ("SAVED".equals(message)) {
                Snackbar.make(requireView(), R.string.org_classification_saved,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    // ------------------------------------------------------------------ render

    private void renderCategories() {
        if (categories.isEmpty()) {
            return;
        }
        rendering = true;
        primaryGroup.removeAllViews();
        secondaryGroup.removeAllViews();
        for (CatalogDtos.CategoryNode node : flatten(categories)) {
            primaryGroup.addView(categoryChip(node, true));
            secondaryGroup.addView(categoryChip(node, false));
        }
        rendering = false;
        renderState();
    }

    @NonNull
    private Chip categoryChip(@NonNull CatalogDtos.CategoryNode node, boolean primary) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setTag(node.id);
        chip.setText(node.name);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.ds_chip_bg);
        chip.setChipStrokeColorResource(R.color.ds_chip_stroke);
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        if (primary) {
            chip.setOnClickListener(v -> viewModel.selectPrimary(node.id));
        } else {
            chip.setOnClickListener(v -> {
                String problem = viewModel.toggleSecondary(node.id);
                if ("MAX_SECONDARY".equals(problem)) {
                    Snackbar.make(requireView(), R.string.org_classification_secondary_limit,
                            Snackbar.LENGTH_SHORT).show();
                    renderState();
                }
            });
        }
        return chip;
    }

    private void renderState() {
        ClassificationViewModel.State state = lastState;
        if (state == null || rendering) {
            return;
        }
        rendering = true;
        checkOnly(primaryGroup, state.primaryId);
        for (int i = 0; i < secondaryGroup.getChildCount(); i++) {
            View child = secondaryGroup.getChildAt(i);
            if (child instanceof Chip && child.getTag() instanceof Long) {
                long id = (Long) child.getTag();
                ((Chip) child).setChecked(state.secondaryIds.contains(id));
                child.setEnabled(!state.locked);
            }
        }
        for (int i = 0; i < primaryGroup.getChildCount(); i++) {
            View child = primaryGroup.getChildAt(i);
            if (child instanceof Chip && child.getTag() instanceof Long) {
                child.setEnabled(!state.locked);
            }
        }
        renderTags(state);
        rendering = false;

        reReviewNote.setVisibility(state.requiresReReview ? View.VISIBLE : View.GONE);
        lockedNote.setVisibility(state.locked ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!state.locked);
        saveButton.setAlpha(state.locked ? 0.5f : 1f);
        tagInput.setEnabled(!state.locked);
        tagLayout.setEnabled(!state.locked);
    }

    private void renderTags(@NonNull ClassificationViewModel.State state) {
        tagsGroup.removeAllViews();
        for (String tag : state.tags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCloseIconVisible(!state.locked);
            chip.setChipBackgroundColorResource(R.color.ds_primary_soft);
            chip.setTextColor(requireContext().getColor(R.color.ds_primary_dark));
            chip.setChipStrokeWidth(0f);
            chip.setOnCloseIconClickListener(v -> viewModel.removeTag(tag));
            tagsGroup.addView(chip);
        }
    }

    private void renderSuggestions() {
        suggestionsGroup.removeAllViews();
        if (suggestions.isEmpty()) {
            return;
        }
        for (ClassificationDtos.TagSuggestion suggestion : suggestions) {
            if (suggestion == null || suggestion.name == null) {
                continue;
            }
            Chip chip = new Chip(requireContext());
            chip.setText(suggestion.name);
            chip.setCheckable(false);
            chip.setChipBackgroundColorResource(R.color.ds_surface_alt);
            chip.setChipStrokeColorResource(R.color.ds_outline_variant);
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
            chip.setOnClickListener(v -> {
                String problem = viewModel.addTag(suggestion.name);
                if (problem != null) {
                    showTagProblem(problem);
                }
            });
            suggestionsGroup.addView(chip);
        }
    }

    private void addTypedTag() {
        String problem = viewModel.addTag(tagInput.getText() == null
                ? null : tagInput.getText().toString());
        if (problem != null) {
            showTagProblem(problem);
            return;
        }
        tagInput.setText("");
    }

    private void showTagProblem(@Nullable String problem) {
        if ("MAX_TAGS".equals(problem)) {
            Snackbar.make(requireView(),
                    getString(R.string.org_classification_tag_limit, ClassificationViewModel.MAX_TAGS),
                    Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(requireView(), R.string.org_classification_tag_invalid,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void checkOnly(@NonNull ChipGroup group, long id) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && child.getTag() instanceof Long) {
                ((Chip) child).setChecked(((Long) child.getTag()) == id);
            }
        }
    }

    @NonNull
    private static List<CatalogDtos.CategoryNode> flatten(
            @NonNull List<CatalogDtos.CategoryNode> nodes) {
        List<CatalogDtos.CategoryNode> values = new ArrayList<>();
        for (CatalogDtos.CategoryNode node : nodes) {
            if (node == null || node.name == null) {
                continue;
            }
            values.add(node);
            if (node.children != null) {
                for (CatalogDtos.CategoryNode child : node.children) {
                    if (child != null && child.name != null) {
                        values.add(child);
                    }
                }
            }
        }
        return values;
    }
}
