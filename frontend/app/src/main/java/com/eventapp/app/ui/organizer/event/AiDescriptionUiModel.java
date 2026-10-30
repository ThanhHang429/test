package com.eventapp.app.ui.organizer.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI model for FR-O06. The screen never owns event data: it only carries the snapshot it was
 * opened with plus the generated variants, warnings and the list of missing fields.
 */
public class AiDescriptionUiModel {

    public enum Tone {
        PROFESSIONAL,
        FRIENDLY,
        FORMAL
    }

    public enum Length {
        SHORT,
        MEDIUM,
        LONG
    }

    public enum Status {
        IDLE,
        REQUESTING,
        SUCCESS,
        ERROR,
        BLOCKED,
        QUOTA
    }

    /** Post-check categories (O06-R03): numbers, dates, phone numbers, links. */
    public enum WarningKind {
        NUMBER,
        DATE,
        PHONE,
        LINK
    }

    public static class Warning {
        public final WarningKind kind;
        public final String detail;

        public Warning(WarningKind kind, String detail) {
            this.kind = kind;
            this.detail = detail == null ? "" : detail;
        }
    }

    public static class Variant {
        public final String text;
        public final String shortSummary;

        public Variant(String text, String shortSummary) {
            this.text = text == null ? "" : text;
            this.shortSummary = shortSummary == null ? "" : shortSummary;
        }
    }

    public static class Result {
        public final String aiRequestId;
        public final long createdAtMs;
        public final List<Variant> variants;
        public final List<String> missingInfo;
        public final List<Warning> warnings;

        public Result(String aiRequestId, long createdAtMs, List<Variant> variants,
                      List<String> missingInfo, List<Warning> warnings) {
            this.aiRequestId = aiRequestId == null ? "" : aiRequestId;
            this.createdAtMs = createdAtMs;
            this.variants = unmodifiable(variants);
            this.missingInfo = unmodifiable(missingInfo);
            this.warnings = unmodifiable(warnings);
        }

        public Variant variantAt(int index) {
            if (variants.isEmpty()) {
                return null;
            }
            int safe = Math.max(0, Math.min(index, variants.size() - 1));
            return variants.get(safe);
        }

        private static <T> List<T> unmodifiable(List<T> values) {
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}