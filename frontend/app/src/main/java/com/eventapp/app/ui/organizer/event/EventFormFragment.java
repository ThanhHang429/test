package com.eventapp.app.ui.organizer.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.ClassificationSectionView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Event form host for the O09 slice. The event-information fields are owned by the event
 * module; this fragment wires the classification block and its save state.
 */
public class EventFormFragment extends Fragment {

    public static final String ARG_EVENT_ID = "arg_event_id";

    private EventFormViewModel viewModel;
    private ClassificationSectionView classificationSection;
    private MaterialButton saveButton;
    private LinearProgressIndicator progress;

    private boolean prefillApplied;
    private boolean loadingCategories = true;
    private boolean saving;

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

        observeData();
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
    }

    private void updateSaveState() {
        boolean enabled = !loadingCategories && !saving;
        saveButton.setEnabled(enabled);
        saveButton.setAlpha(enabled ? 1f : 0.55f);
        saveButton.setText(saving ? R.string.hc_o09_saving : R.string.hc_o09_save);
    }
}
