package com.eventapp.app.data.remote.dto.preference;

import java.util.ArrayList;
import java.util.List;

/** Sở thích người dùng (FR-U03). */
public final class PreferenceDtos {

    private PreferenceDtos() {
    }

    public static class Preference {
        public List<Long> categoryIds = new ArrayList<>();
        public String eventFormat = "ANY";
        public String priceMode = "ANY";
        public Long maxBudgetVnd;
        public List<String> preferredCities = new ArrayList<>();
        public List<String> preferredDays = new ArrayList<>();
        public boolean onboardingCompleted;
        public boolean personalizationEnabled;
        public int version;
        public List<Long> removedCategoryIds = new ArrayList<>();
    }

    public static class UpdateRequest {
        public List<Long> categoryIds = new ArrayList<>();
        public String eventFormat = "ANY";
        public String priceMode = "ANY";
        public Long maxBudgetVnd;
        public List<String> preferredCities = new ArrayList<>();
        public List<String> preferredDays = new ArrayList<>();
        public Boolean onboardingCompleted;
        public Boolean personalizationEnabled;
        public Integer version;
    }
}
