package com.eventapp.app.ui.model;

import androidx.annotation.StringRes;

/** Kill switch row for one AI task type (FR-A20 / AI_DISABLED contract). */
public class AiTaskToggleUiModel {

    public final String taskType;
    @StringRes
    public final int labelRes;
    public final boolean enabled;

    public AiTaskToggleUiModel(String taskType, @StringRes int labelRes, boolean enabled) {
        this.taskType = taskType;
        this.labelRes = labelRes;
        this.enabled = enabled;
    }

    public AiTaskToggleUiModel withEnabled(boolean nextEnabled) {
        return new AiTaskToggleUiModel(taskType, labelRes, nextEnabled);
    }
}
