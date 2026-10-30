package com.eventapp.app.ui.common.view;

import com.eventapp.app.R;

import java.util.Locale;

/**
 * Maps a backend severity string (LOW/MEDIUM/HIGH) to the shared {@link StatusChip} tone and a
 * Vietnamese label. Used by both FR-A07 flags and FR-A14 review rows.
 */
public final class SeverityChip {

    private SeverityChip() {
    }

    public static void bind(StatusChip chip, String severity) {
        if (chip == null) {
            return;
        }
        String value = severity == null ? "" : severity.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "HIGH":
                chip.setStatus(chip.getContext().getString(R.string.hc_mod_severity_high),
                        StatusChip.Tone.DANGER);
                break;
            case "MEDIUM":
                chip.setStatus(chip.getContext().getString(R.string.hc_mod_severity_medium),
                        StatusChip.Tone.WARNING);
                break;
            case "LOW":
                chip.setStatus(chip.getContext().getString(R.string.hc_mod_severity_low),
                        StatusChip.Tone.SUCCESS);
                break;
            default:
                chip.setStatus(severity, StatusChip.Tone.MUTED);
                break;
        }
    }
}
