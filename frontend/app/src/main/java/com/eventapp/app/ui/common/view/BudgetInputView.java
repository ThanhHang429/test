package com.eventapp.app.ui.common.view;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.eventapp.app.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/**
 * Budget selector for U03: preset chips, a slider and a numeric field kept in sync.
 * The value range is 0..100.000.000 VND with 500.000 VND slider steps.
 */
public class BudgetInputView extends LinearLayout {

    public interface OnAmountChangedListener {
        void onAmountChanged(long amount);
    }

    public static final long MAX_AMOUNT = 100_000_000L;
    private static final long SLIDER_STEP = 500_000L;

    private ChipGroup presetGroup;
    private Slider slider;
    private TextInputLayout inputLayout;
    private TextInputEditText input;
    private TextView valueView;

    private final Chip[] presetChips = new Chip[5];
    private final long[] presetValues = {200_000L, 500_000L, 1_000_000L, 2_000_000L, 5_000_000L};

    private long amount;
    private boolean updating;
    private boolean valid = true;
    private OnAmountChangedListener listener;

    public BudgetInputView(Context context) {
        this(context, null);
    }

    public BudgetInputView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BudgetInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_ds_budget_input, this, true);
        presetGroup = findViewById(R.id.chipGroupDsBudgetPreset);
        slider = findViewById(R.id.sliderDsBudget);
        inputLayout = findViewById(R.id.tilDsBudget);
        input = findViewById(R.id.etDsBudget);
        valueView = findViewById(R.id.tvDsBudgetValue);

        presetChips[0] = findViewById(R.id.chipDsBudget200);
        presetChips[1] = findViewById(R.id.chipDsBudget500);
        presetChips[2] = findViewById(R.id.chipDsBudget1m);
        presetChips[3] = findViewById(R.id.chipDsBudget2m);
        presetChips[4] = findViewById(R.id.chipDsBudget5m);

        slider.setValueFrom(0f);
        slider.setValueTo(MAX_AMOUNT);
        slider.setStepSize(SLIDER_STEP);

        presetGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (updating || checkedIds.isEmpty()) {
                return;
            }
            int checkedId = checkedIds.get(0);
            for (int i = 0; i < presetChips.length; i++) {
                if (presetChips[i].getId() == checkedId) {
                    setAmount(presetValues[i], true);
                    return;
                }
            }
        });

        slider.addOnChangeListener((s, value, fromUser) -> {
            if (updating || !fromUser) {
                return;
            }
            long snapped = Math.round(value / (float) SLIDER_STEP) * SLIDER_STEP;
            setAmount(snapped, true);
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (updating) {
                    return;
                }
                String raw = s == null ? "" : s.toString().trim();
                long parsed = raw.isEmpty() ? 0L : parseAmount(raw);
                if (parsed < 0 || parsed > MAX_AMOUNT) {
                    valid = false;
                    inputLayout.setError(getContext().getString(R.string.hc_budget_invalid));
                    return;
                }
                valid = true;
                inputLayout.setError(null);
                applyAmount(parsed, true, false, true);
                notifyChanged();
            }
        });

        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                inputLayout.setError(null);
                syncInputText(amount);
            }
        });

        setAmount(0L, false);
    }

    /** Returns the current budget in VND. */
    public long getAmount() {
        return amount;
    }

    /** False while the numeric field holds a value outside 0..100.000.000. */
    public boolean isValid() {
        return valid;
    }

    public void setAmount(long amount) {
        setAmount(amount, false);
    }

    public void setAmount(long amount, boolean notify) {
        long clamped = Math.max(0L, Math.min(MAX_AMOUNT, amount));
        applyAmount(clamped, true, true, true);
        if (notify) {
            notifyChanged();
        }
    }

    public void setOnAmountChangedListener(@Nullable OnAmountChangedListener listener) {
        this.listener = listener;
    }

    public void setError(@Nullable CharSequence error) {
        inputLayout.setError(error);
    }

    public void setEnabledState(boolean enabled) {
        slider.setEnabled(enabled);
        input.setEnabled(enabled);
        presetGroup.setEnabled(enabled);
        for (Chip chip : presetChips) {
            chip.setEnabled(enabled);
        }
        setAlpha(enabled ? 1f : 0.5f);
    }

    /**
     * @param updateControls true when the change comes from the host (public setter), false when
     *                       the text field itself already holds the typed value.
     */
    private void applyAmount(long value, boolean updateControls, boolean updateInput,
                             boolean updatePresets) {
        updating = true;
        try {
            amount = value;
            valid = true;
            if (updateControls) {
                slider.setValue(snapToStep(value));
            }
            if (updateInput) {
                syncInputText(value);
            }
            valueView.setText(getContext().getString(R.string.hc_budget_value,
                    PriceTextView.formatPlain(value)));
            setContentDescription(getContext().getString(R.string.hc_budget_a11y,
                    PriceTextView.formatPlain(value)));
            if (updatePresets) {
                syncPresetSelection(value);
            }
        } finally {
            updating = false;
        }
    }

    private void syncInputText(long value) {
        String formatted = value == 0L ? "" : String.format(Locale.US, "%d", value);
        if (!formatted.equals(textOf(input))) {
            input.setText(formatted);
            input.setSelection(formatted.length());
        }
    }

    private void syncPresetSelection(long value) {
        int match = -1;
        for (int i = 0; i < presetValues.length; i++) {
            if (presetValues[i] == value) {
                match = i;
                break;
            }
        }
        if (match >= 0) {
            if (!presetChips[match].isChecked()) {
                presetChips[match].setChecked(true);
            }
        } else {
            presetGroup.clearCheck();
        }
    }

    private void notifyChanged() {
        if (listener != null) {
            listener.onAmountChanged(amount);
        }
    }

    private static String textOf(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    private static long parseAmount(String raw) {
        String digits = raw.replace(".", "").replace(",", "").replace(" ", "");
        if (digits.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long snapToStep(long value) {
        return Math.round(value / (double) SLIDER_STEP) * SLIDER_STEP;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setEnabledState(enabled);
    }
}
