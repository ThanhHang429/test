package com.eventapp.app.data.remote.dto.classification;

import java.util.ArrayList;
import java.util.List;

/** Phân loại & nhãn (FR-O09). */
public final class ClassificationDtos {

    private ClassificationDtos() {
    }

    public static class CategoryBrief {
        public long id;
        public String code;
        public String name;
        public String slug;
        public boolean active;
    }

    public static class Classification {
        public long eventId;
        public CategoryBrief primaryCategory;
        public List<CategoryBrief> secondaryCategories = new ArrayList<>();
        public List<String> tags = new ArrayList<>();
        public boolean requiresReReview;
        public String updatedAt;
    }

    public static class UpdateRequest {
        public Long primaryCategoryId;
        public List<Long> secondaryCategoryIds = new ArrayList<>();
        public List<String> tags = new ArrayList<>();
    }

    public static class TagSuggestion {
        public long id;
        public String name;
        public String slug;
        public long usageCount;
    }
}
