package com.eventapp.app.ui.organizer.event;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.MaterialAlertDialogBuilder;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.ClassificationSectionView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Event form host for the O09 classification slice and the O06 description slice.
 *
 * <p>The AI screen is a producer only: it hands text back through the nav back stack and this
 * fragment decides whether to replace or append, showing an undo snackbar (O06-R01).</p>
 */
public class EventFormFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";

    private EventFormViewModel viewModel;
    private ClassificationSectionView classificationSection;
    private MaterialButton saveButton;
    private LinearProgressIndicator progress;

    private EditText titleInput;
    private TextInputLayout titleLayout;
    private ChipGroup formatGroup;
    private ChipGroup keyPointGroup;
    private ChipGroup suggestionGroup;
    private EditText keyPointInput;
    private TextInputLayout keyPointLayout;
    private TextView keyPointCount;
    private EditText descriptionInput;
    private View aiLabelRow;
    private MaterialButton aiButton;
    private TextView aiReason;

    private NavController navController;
    private boolean prefillApplied;
    private boolean loadingCategories = true;
    private boolean saving;
    private boolean bindingDescription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_event_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(EventFormViewModel.class);
        long eventId = getArguments() == null ? 0L
                : getArguments().getLong(ARG_EVENT_ID, 0L);
        viewModel.prepare(eventId);

        classificationSection = view.findViewById(R.id.classificationSection);
        saveButton = view.findViewById(R.id.btnEventFormSave);
        progress = view.findViewById(R.id.progressEventFormSave);
        bindDescriptionBlock(view);

        navController = Navigation.findNavController(requireView());
        view.findViewById(R.id.btnEventFormBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        saveButton.setOnClickListener(v -> {
            if (!classificationSection.validate()) {
                Snackbar.make(requireView(), R.string.hc_o09_validation_failed,
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            viewModel.saveClassification(
                    classificationSection.getPrimaryId(),
                    classificationSection.getSecondaryIds(),
                    classificationSection.getTags());
        });

        classificationSection.setOnClassificationChangedListener(() ->
                viewModel.setClassificationPrimary(classificationSection.getPrimaryId()));

        observeData();
        observeAiResult();
    }

    private void bindDescriptionBlock(View view) {
        titleLayout = view.findViewById(R.id.tilEventFormTitle);
        titleInput = view.findViewById(R.id.etEventFormTitle);
        formatGroup = view.findViewById(R.id.chipGroupEventFormat);
        keyPointCount = view.findViewById(R.id.tvEventFormKeyPointCount);
        keyPointLayout = view.findViewById(R.id.tilEventFormKeyPoint);
        keyPointInput = view.findViewById(R.id.etEventFormKeyPoint);
        keyPointGroup = view.findViewById(R.id.chipGroupEventKeyPoints);
        suggestionGroup = view.findViewById(R.id.chipGroupEventKeyPointSuggest);
        descriptionInput = view.findViewById(R.id.etEventFormDescription);
        aiLabelRow = view.findViewById(R.id.rowEventFormAiLabel);
        aiButton = view.findViewById(R.id.btnEventFormAi);
        aiReason = view.findViewById(R.id.tvEventFormAiReason);

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String value = s == null ? "" : s.toString();
                viewModel.setDescriptionTitle(value);
                String trimmed = value.trim();
                titleLayout.setError(trimmed.length() > 0 && trimmed.length() < 5
                        ? getString(R.string.hc_aidesc_title_error)
                        : null);
            }
        });

        formatGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            viewModel.setEventFormat(checkedIds.get(0) == R.id.chipEventFormatOnline
                    ? "ONLINE"
                    : "ONSITE");
        });

        keyPointInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND) {
                addKeyPointFromInput();
                return true;
            }
            return false;
        });
        view.findViewById(R.id.btnEventFormAddKeyPoint)
                .setOnClickListener(v -> addKeyPointFromInput());

        renderSuggestions();
        renderKeyPoints();

        descriptionInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (bindingDescription) {
                    return;
                }
                boolean wasAiLabel = viewModel.isDescriptionFromAi();
                viewModel.setDescription(s == null ? "" : s.toString());
                if (wasAiLabel && !viewModel.isDescriptionFromAi()) {
                    // O06-R05: the source label disappears once the organizer edits the text.
                    updateAiLabel();
                    Snackbar.make(requireView(), R.string.hc_aidesc_ai_label_dismissed,
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        aiButton.setOnClickListener(v -> openAiDescription());
    }

    private void renderSuggestions() {
        String[] ideas = getResources().getStringArray(R.array.hc_aidesc_keypoint_ideas);
        for (String idea : ideas) {
            Chip chip = new Chip(requireContext());
            chip.setStyle(R.style.Widget_EventApp_Ds_Chip_Preset);
            chip.setText(idea);
            chip.setContentDescription(getString(R.string.hc_aidesc_keypoints_add) + ": " + idea);
            chip.setOnClickListener(v -> addKeyPoint(idea));
            suggestionGroup.addView(chip);
        }
    }

    private void renderKeyPoints() {
        keyPointGroup.removeAllViews();
        List<String> points = viewModel.getKeyPoints();
        for (String point : points) {
            Chip chip = new Chip(requireContext());
            chip.setStyle(R.style.Widget_EventApp_Ds_Chip_Tag);
            chip.setText(point);
            chip.setContentDescription(
                    getString(R.string.hc_aidesc_keypoint_remove, point));
            chip.setOnCloseIconClickListener(v -> {
                viewModel.removeKeyPoint(point);
                renderKeyPoints();
            });
            keyPointGroup.addView(chip);
        }
        keyPointCount.setText(
                getString(R.string.hc_aidesc_keypoints_count_format, points.size()));
    }

    private void addKeyPointFromInput() {
        String raw = keyPointInput.getText() == null
                ? ""
                : keyPointInput.getText().toString();
        addKeyPoint(raw);
    }

    private void addKeyPoint(String raw) {
        int result = viewModel.addKeyPoint(raw == null ? "" : raw.trim());
        if (result == EventFormViewModel.KEYPOINT_OK) {
            if (keyPointInput.getText() != null) {
                keyPointInput.setText("");
            }
            keyPointLayout.setError(null);
            renderKeyPoints();
            return;
        }
        keyPointLayout.setError(result == EventFormViewModel.KEYPOINT_DUPLICATE
                ? getString(R.string.hc_aidesc_keypoints_duplicate)
                : getString(R.string.hc_aidesc_keypoints_length));
        Snackbar.make(requireView(),
                result == EventFormViewModel.KEYPOINT_DUPLICATE
                        ? R.string.hc_aidesc_keypoints_duplicate
                        : result == EventFormViewModel.KEYPOINT_LIMIT
                        ? R.string.hc_aidesc_keypoints_limit
                        : R.string.hc_aidesc_keypoints_length,
                Snackbar.LENGTH_SHORT).show();
    }

    private void openAiDescription() {
        Bundle args = new Bundle();
        args.putString("arg_event_title", viewModel.getEventTitle());
        args.putLong("arg_category_id", viewModel.getClassificationPrimaryId());
        // The assistant writes human text, so the label is sent rather than the enum value.
        args.putString("arg_format", viewModel.getEventFormat().equals("ONLINE")
                ? getString(R.string.hc_aidesc_format_online)
                : getString(R.string.hc_aidesc_format_onsite));
        args.putStringArrayList("arg_key_points", new ArrayList<>(viewModel.getKeyPoints()));
        args.putString("arg_audience", "");
        Navigation.findNavController(requireView())
                .navigate(R.id.ai_descriptionFragment, args);
    }

    private void observeAiResult() {
        NavBackStackEntry entry = navController == null ? null : navController.getCurrentBackStackEntry();
        if (entry == null) {
            return;
        }
        entry.getSavedStateHandle()
                .<String>getLiveData(AiDescriptionFragment.RESULT_KEY, "")
                .observe(getViewLifecycleOwner(), this::handleAiResult);
    }

    private void handleAiResult(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        NavBackStackEntry entry = navController == null ? null : navController.getCurrentBackStackEntry();
        if (entry != null) {
            entry.getSavedStateHandle().remove(AiDescriptionFragment.RESULT_KEY);
        }
        if (!viewModel.hasDescription()) {
            applyAiResult(text, true);
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hc_aidesc_replace_title)
                .setMessage(R.string.hc_aidesc_replace_message)
                .setPositiveButton(R.string.hc_aidesc_replace,
                        (dialog, which) -> applyAiResult(text, true))
                .setNeutralButton(R.string.hc_aidesc_append,
                        (dialog, which) -> applyAiResult(text, false))
                .setNegativeButton(R.string.hc_close, null)
                .show();
    }

    private void applyAiResult(String text, boolean replace) {
        String applied = viewModel.applyAiDescription(text, replace);
        setDescriptionText(applied);
        updateAiLabel();
        Snackbar.make(requireView(), R.string.hc_aidesc_used, Snackbar.LENGTH_LONG)
                .setAction(R.string.hc_aidesc_undo, v -> {
                    setDescriptionText(viewModel.undoDescription());
                    updateAiLabel();
                    Snackbar.make(requireView(), R.string.hc_aidesc_undo_done,
                            Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setDescriptionText(String value) {
        bindingDescription = true;
        descriptionInput.setText(value);
        bindingDescription = false;
    }

    private void updateAiLabel() {
        aiLabelRow.setVisibility(viewModel.isDescriptionFromAi() ? View.VISIBLE : View.GONE);
    }

    private void observeAiGate() {
        viewModel.getAiGate().observe(getViewLifecycleOwner(), gate -> {
            if (gate == null) {
                return;
            }
            aiButton.setEnabled(gate.ready);
            aiButton.setAlpha(gate.ready ? 1f : 0.55f);
            aiReason.setText(gate.ready
                    ? getString(R.string.hc_aidesc_gate_ready)
                    : getString(R.string.hc_aidesc_gate_reason,
                    TextUtils.join(", ", gate.missing)));
            aiReason.setTextColor(ContextCompat.getColor(requireContext(), gate.ready
                    ? R.color.ds_text_muted
                    : R.color.ds_warning_dark));
        });
    }

    private void observeData() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), options -> {
            classificationSection.setCategories(options);
            if (!prefillApplied) {
                prefillApplied = true;
                if (!classificationSection.hasValue()) {
                    classificationSection.setValue(
                            viewModel.getInitialPrimaryId(),
                            viewModel.getInitialSecondaryIds(),
                            viewModel.getInitialTags());
                }
                viewModel.setClassificationPrimary(classificationSection.getPrimaryId());
            }
        });

        viewModel.getSuggestions().observe(getViewLifecycleOwner(),
                suggestions -> classificationSection.setSuggestions(suggestions));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            loadingCategories = Boolean.TRUE.equals(loading);
            classificationSection.setEditable(!loadingCategories && !saving);
            updateSaveState();
        });

        viewModel.getSaving().observe(getViewLifecycleOwner(), isSaving -> {
            saving = Boolean.TRUE.equals(isSaving);
            progress.setVisibility(saving ? View.VISIBLE : View.GONE);
            classificationSection.setEditable(!loadingCategories && !saving);
            updateSaveState();
        });

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null || result == EventFormViewModel.SAVE_NONE) {
                return;
            }
            Snackbar.make(requireView(),
                    result == EventFormViewModel.SAVE_SUCCESS
                            ? R.string.hc_o09_saved
                            : R.string.hc_o09_save_error,
                    Snackbar.LENGTH_LONG).show();
            viewModel.consumeSaveResult();
        });

        observeAiGate();
    }

    private void updateSaveState() {
        boolean enabled = !loadingCategories && !saving;
        saveButton.setEnabled(enabled);
        saveButton.setAlpha(enabled ? 1f : 0.55f);
        saveButton.setText(saving ? R.string.hc_o09_saving : R.string.hc_o09_save);
    }
}
