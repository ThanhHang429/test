package com.eventapp.app.ui.admin.system;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.system.adapter.IntegrationErrorAdapter;
import com.eventapp.app.ui.admin.system.adapter.ServiceHealthAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.KpiTileView;
import com.eventapp.app.ui.common.view.SectionHeaderView;
import com.eventapp.app.ui.common.view.SimpleBarChartView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.AiTaskToggleUiModel;
import com.eventapp.app.ui.model.IntegrationErrorUiModel;
import com.eventapp.app.ui.model.ServiceHealthUiModel;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FR-A20: integration health for the AI gateway, the payment gateway and FCM, the per-task kill
 * switches and a correlation-id lookup over recent errors. Each block keeps its own state so one
 * failing block does not blank the screen.
 */
public class IntegrationHealthFragment extends Fragment {

    private static final int REASON_MIN_LENGTH = 10;

    private IntegrationHealthViewModel viewModel;
    private ServiceHealthAdapter serviceAdapter;
    private IntegrationErrorAdapter errorAdapter;

    private StatusChip overallChip;
    private TextView updatedView;
    private TextView chartCaption;
    private ChipGroup rangeGroup;
    private KpiTileView kpiErrorRate;
    private KpiTileView kpiLatency;
    private KpiTileView kpiRequests;
    private KpiTileView kpiFallback;
    private SimpleBarChartView chart;
    private RecyclerView servicesList;
    private ViewGroup taskContainer;
    private RecyclerView errorsList;
    private View errorsCard;
    private EmptyStateView emptyErrors;
    private TextInputEditText correlationInput;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;

    private final List<IntegrationErrorUiModel> allErrors = new ArrayList<>();
    private String errorQuery = "";
    private boolean firstLoad = true;
    private boolean bindingRange;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_system_integration_health, container,
                false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(IntegrationHealthViewModel.class);
        bindViews(view);
        setupToolbar();
        setupRangeChips();
        setupKpiTiles();
        setupServices();
        setupErrors();
        observe();
    }

    private void bindViews(View view) {
        overallChip = view.findViewById(R.id.chipOverall);
        updatedView = view.findViewById(R.id.tvHealthUpdated);
        chartCaption = view.findViewById(R.id.tvChartCaption);
        rangeGroup = view.findViewById(R.id.chipGroupRange);
        kpiErrorRate = view.findViewById(R.id.kpiErrorRate);
        kpiLatency = view.findViewById(R.id.kpiLatency);
        kpiRequests = view.findViewById(R.id.kpiRequests);
        kpiFallback = view.findViewById(R.id.kpiFallback);
        chart = view.findViewById(R.id.chartErrors);
        servicesList = view.findViewById(R.id.rvServices);
        taskContainer = view.findViewById(R.id.layoutTaskToggles);
        errorsList = view.findViewById(R.id.rvErrors);
        errorsCard = view.findViewById(R.id.cardErrors);
        emptyErrors = view.findViewById(R.id.emptyErrors);
        correlationInput = view.findViewById(R.id.etCorrelation);
        skeleton = view.findViewById(R.id.skeletonHealth);
        errorState = view.findViewById(R.id.errorHealth);
    }

    private void setupToolbar() {
        requireView().findViewById(R.id.btnHealthBack).setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());
        requireView().findViewById(R.id.btnHealthRefresh).setOnClickListener(v ->
                viewModel.retry());

        SectionHeaderView services = requireView().findViewById(R.id.headerHealthServices);
        services.setTitle(R.string.hc_health_section_services);
        services.setSubtitle(R.string.hc_health_section_services_sub);

        SectionHeaderView tasks = requireView().findViewById(R.id.headerHealthTasks);
        tasks.setTitle(R.string.hc_health_section_tasks);
        tasks.setSubtitle(R.string.hc_health_section_tasks_sub);

        SectionHeaderView errors = requireView().findViewById(R.id.headerHealthErrors);
        errors.setTitle(R.string.hc_health_section_errors);
        errors.setSubtitle(R.string.hc_health_section_errors_sub);
    }

    private void setupRangeChips() {
        rangeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (bindingRange) {
                return;
            }
            int hours = 24;
            if (checkedIds.contains(R.id.chipRange1h)) {
                hours = 1;
            } else if (checkedIds.contains(R.id.chipRange7d)) {
                hours = 168;
            }
            viewModel.setRange(hours);
        });
    }

    private void setupKpiTiles() {
        kpiErrorRate.setLabel(R.string.hc_health_kpi_error_rate);
        kpiErrorRate.setCaption(getString(R.string.hc_health_kpi_error_rate_caption));
        kpiLatency.setLabel(R.string.hc_health_kpi_latency);
        kpiLatency.setCaption(getString(R.string.hc_health_kpi_latency_caption));
        kpiRequests.setLabel(R.string.hc_health_kpi_requests);
        kpiRequests.setCaption(getString(R.string.hc_health_kpi_requests_caption));
        kpiFallback.setLabel(R.string.hc_health_kpi_fallback);
        kpiFallback.setCaption(getString(R.string.hc_health_kpi_fallback_caption));
    }

    private void setupServices() {
        serviceAdapter = new ServiceHealthAdapter();
        servicesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        servicesList.setAdapter(serviceAdapter);
    }

    private void setupErrors() {
        errorAdapter = new IntegrationErrorAdapter();
        errorsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        errorsList.setAdapter(errorAdapter);

        emptyErrors.setIcon(R.drawable.ic_ds_error);
        emptyErrors.showAction(false);

        correlationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                errorQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                renderErrors();
            }
        });
    }

    private void observe() {
        viewModel.getServices().observe(getViewLifecycleOwner(), items -> {
            serviceAdapter.submitList(items);
            renderKpi(items, viewModel.getTasks().getValue());
        });
        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            renderTasks(tasks);
            renderKpi(viewModel.getServices().getValue(), tasks);
        });
        viewModel.getErrors().observe(getViewLifecycleOwner(), items -> {
            allErrors.clear();
            if (items != null) {
                allErrors.addAll(items);
            }
            renderErrors();
        });
        viewModel.getOverall().observe(getViewLifecycleOwner(), this::renderOverall);
        viewModel.getUpdatedAt().observe(getViewLifecycleOwner(), time -> {
            boolean hasTime = time != null && time.length() > 0;
            updatedView.setVisibility(hasTime ? View.VISIBLE : View.GONE);
            if (hasTime) {
                updatedView.setText(getString(R.string.hc_health_updated, time));
            }
        });
        viewModel.getHourlyErrors().observe(getViewLifecycleOwner(), this::renderChart);
        viewModel.getRangeHours().observe(getViewLifecycleOwner(), hours -> {
            bindingRange = true;
            if (hours != null && hours <= 1) {
                rangeGroup.check(R.id.chipRange1h);
            } else if (hours != null && hours >= 168) {
                rangeGroup.check(R.id.chipRange7d);
            } else {
                rangeGroup.check(R.id.chipRange24h);
            }
            bindingRange = false;
            renderChart(viewModel.getHourlyErrors().getValue());
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = loading != null && loading;
            skeleton.setVisibility(busy && firstLoad ? View.VISIBLE : View.GONE);
            if (!busy) {
                firstLoad = false;
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean failed = error != null && error;
            errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        });
        errorState.setOnRetryClickListener(v -> viewModel.retry());
        viewModel.getActionEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null && !event.enabled) {
                Snackbar.make(requireView(), R.string.hc_health_task_off, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    private void renderOverall(ServiceHealthUiModel.Status status) {
        ServiceHealthUiModel.Status value =
                status == null ? ServiceHealthUiModel.Status.UNKNOWN : status;
        overallChip.setStatus(getString(ServiceHealthAdapter.labelFor(value)),
                toneFor(value));
    }

    private StatusChip.Tone toneFor(ServiceHealthUiModel.Status status) {
        switch (status) {
            case HEALTHY:
                return StatusChip.Tone.SUCCESS;
            case DEGRADED:
                return StatusChip.Tone.WARNING;
            case DOWN:
                return StatusChip.Tone.DANGER;
            default:
                return StatusChip.Tone.MUTED;
        }
    }

    private void renderKpi(List<ServiceHealthUiModel> services,
                           List<AiTaskToggleUiModel> tasks) {
        List<ServiceHealthUiModel> serviceItems =
                services == null ? new ArrayList<>() : services;
        List<AiTaskToggleUiModel> taskItems = tasks == null ? new ArrayList<>() : tasks;

        int requests = 0;
        int latency = 0;
        double weightedErrors = 0d;
        for (ServiceHealthUiModel service : serviceItems) {
            requests += service.requestCount;
            weightedErrors += (double) service.errorRatePct * service.requestCount;
            latency = Math.max(latency, service.p95LatencyMs);
        }
        double errorRate = requests == 0 ? 0d : weightedErrors / requests;

        kpiErrorRate.clearError();
        kpiErrorRate.setValue(getString(R.string.hc_health_kpi_value_pct,
                String.format(Locale.getDefault(), "%.1f", errorRate)));
        kpiErrorRate.setTone(errorRate >= 10d
                ? KpiTileView.Tone.DANGER
                : errorRate >= 5d ? KpiTileView.Tone.WARNING : KpiTileView.Tone.SUCCESS);

        kpiLatency.clearError();
        kpiLatency.setValue(getString(R.string.hc_health_kpi_value_ms, latency));
        kpiLatency.setTone(latency >= 2000 ? KpiTileView.Tone.WARNING : KpiTileView.Tone.DEFAULT);

        kpiRequests.clearError();
        kpiRequests.setValue(getString(R.string.hc_health_kpi_value_count, requests));

        int disabled = 0;
        for (AiTaskToggleUiModel task : taskItems) {
            if (!task.enabled) {
                disabled++;
            }
        }
        kpiFallback.clearError();
        kpiFallback.setValue(getString(R.string.hc_health_kpi_value_ratio, disabled,
                taskItems.size()));
        kpiFallback.setTone(disabled == 0 ? KpiTileView.Tone.SUCCESS : KpiTileView.Tone.WARNING);
    }

    private void renderChart(float[] values) {
        float[] data = values == null ? new float[0] : values;
        Integer hours = viewModel.getRangeHours().getValue();
        int range = hours == null ? 24 : hours;

        chart.setData(data, chartLabels(range, data.length));
        int peak = peakIndex(data);
        chart.setHighlightIndex(peak);

        if (peak < 0 || data[peak] <= 0f) {
            chartCaption.setText(getString(R.string.hc_health_chart_caption_empty,
                    rangeLabel(range)));
            return;
        }
        String[] labels = chartLabels(range, data.length);
        chartCaption.setText(getString(R.string.hc_health_chart_caption,
                rangeLabel(range), Math.round(data[peak]), labels[peak]));
    }

    private int peakIndex(float[] data) {
        int index = -1;
        float best = 0f;
        for (int i = 0; i < data.length; i++) {
            if (data[i] > best) {
                best = data[i];
                index = i;
            }
        }
        return index;
    }

    private String rangeLabel(int hours) {
        if (hours <= 1) {
            return getString(R.string.hc_health_range_1h);
        }
        if (hours >= 168) {
            return getString(R.string.hc_health_range_7d);
        }
        return getString(R.string.hc_health_range_24h);
    }

    private String[] chartLabels(int hours, int count) {
        String[] labels = new String[count];
        if (hours <= 1) {
            for (int i = 0; i < count; i++) {
                labels[i] = String.format(Locale.getDefault(), "%02d′", i * 5);
            }
        } else if (hours >= 168) {
            String[] weekdays = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
            for (int i = 0; i < count; i++) {
                labels[i] = weekdays[i % weekdays.length];
            }
        } else {
            for (int i = 0; i < count; i++) {
                labels[i] = String.format(Locale.getDefault(), "%02d", i);
            }
        }
        return labels;
    }

    private void renderTasks(List<AiTaskToggleUiModel> tasks) {
        taskContainer.removeAllViews();
        if (tasks == null) {
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        for (AiTaskToggleUiModel task : tasks) {
            View row = inflater.inflate(R.layout.item_ai_task_toggle, taskContainer, false);
            TextView label = row.findViewById(R.id.tvTaskLabel);
            TextView code = row.findViewById(R.id.tvTaskCode);
            MaterialSwitch toggle = row.findViewById(R.id.switchTask);

            label.setText(task.labelRes);
            code.setText(task.taskType);
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(task.enabled);
            toggle.setOnCheckedChangeListener(new TaskSwitchListener(task.taskType, toggle));
            taskContainer.addView(row);
        }
    }

    private void renderErrors() {
        List<IntegrationErrorUiModel> filtered = new ArrayList<>();
        for (IntegrationErrorUiModel item : allErrors) {
            if (errorQuery.isEmpty()
                    || item.correlationId.toLowerCase(Locale.ROOT).startsWith(errorQuery)) {
                filtered.add(item);
            }
        }
        errorAdapter.submitList(filtered);

        boolean empty = filtered.isEmpty();
        emptyErrors.setVisibility(empty ? View.VISIBLE : View.GONE);
        errorsCard.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            boolean searching = !errorQuery.isEmpty();
            emptyErrors.setTitle(searching
                    ? R.string.hc_health_error_no_match_title
                    : R.string.hc_health_error_empty_title);
            emptyErrors.setMessage(searching
                    ? R.string.hc_health_error_no_match_message
                    : R.string.hc_health_error_empty_message);
        }
    }

    /** Tắt một tác vụ là thao tác rủi ro: phải có lý do trước khi gọi ViewModel. */
    private void askTaskReason(String taskType) {
        View content = getLayoutInflater()
                .inflate(R.layout.dialog_ai_task_reason, null, false);
        TextInputEditText input = content.findViewById(R.id.etTaskReason);
        TextView error = content.findViewById(R.id.tvTaskReasonError);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hc_health_reason_title)
                .setMessage(R.string.hc_health_reason_message)
                .setView(content)
                .setNegativeButton(R.string.hc_health_cancel, null)
                .setPositiveButton(R.string.hc_health_reason_confirm, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = input.getText() == null
                    ? "" : input.getText().toString().trim();
            if (reason.length() < REASON_MIN_LENGTH) {
                error.setVisibility(View.VISIBLE);
                error.setText(R.string.hc_health_reason_error);
                return;
            }
            viewModel.setTaskEnabled(taskType, false);
            dialog.dismiss();
        });
    }

    private final class TaskSwitchListener implements CompoundButton.OnCheckedChangeListener {

        private final String taskType;
        private final MaterialSwitch switchView;

        TaskSwitchListener(String taskType, MaterialSwitch switchView) {
            this.taskType = taskType;
            this.switchView = switchView;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                return;
            }
            // Hoàn nguyên ngay; chỉ đổi trạng thái sau khi người dùng nhập lý do.
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(true);
            switchView.setOnCheckedChangeListener(this);
            askTaskReason(taskType);
        }
    }
}
