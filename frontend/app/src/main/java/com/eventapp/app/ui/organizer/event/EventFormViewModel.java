package com.eventapp.app.ui.organizer.event;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.eventapp.app.R;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.common.view.ClassificationSectionView;
import com.eventapp.app.ui.user.home.EventCategoryUi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * State for the event form slice that owns O09 classification.
 *
 * <p>The real screen will load categories from GET /categories (API-02, ETag) and save with
 * PUT /organizer/events/{id}/classification. Until those endpoints exist, this view model
 * exposes the same states with deterministic sample data.</p>
 */
public class EventFormViewModel extends AndroidViewModel {

    public static final int SAVE_NONE = 0;
    public static final int SAVE_SUCCESS = 1;
    public static final int SAVE_ERROR = 2;

    private static final long CATEGORY_LOAD_DELAY_MS = 260L;
    private static final long SAVE_DELAY_MS = 480L;

    private final MutableLiveData<List<ClassificationSectionView.CategoryOption>> categories =
            new MutableLiveData<>();
    private final MutableLiveData<List<String>> suggestions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> saveResult = new MutableLiveData<>(SAVE_NONE);

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long initialPrimaryId;
    private final List<Long> initialSecondaryIds = new ArrayList<>();
    private final List<String> initialTags = new ArrayList<>();

    public EventFormViewModel(@NonNull Application application) {
        super(application);
        publishAiGate();
        loadCategories();
    }

    /** Must be called once from the fragment before collecting the classification state. */
    public void prepare(long eventId) {
        if (initialPrimaryId > 0L || !initialSecondaryIds.isEmpty() || !initialTags.isEmpty()) {
            return;
        }
        if (eventId > 0L) {
            // Edit mode: an existing event keeps its classification, including hidden categories.
            initialPrimaryId = EventCategoryUi.id(EventCategory.MUSIC);
            initialSecondaryIds.add(EventCategoryUi.id(EventCategory.TECH));
            initialTags.add("indie");
            initialTags.add("live music");
            classificationPrimaryId = initialPrimaryId;
        }
        publishAiGate();
    }

    public LiveData<List<ClassificationSectionView.CategoryOption>> getCategories() {
        return categories;
    }

    public LiveData<List<String>> getSuggestions() {
        return suggestions;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<Integer> getSaveResult() {
        return saveResult;
    }

    public long getInitialPrimaryId() {
        return initialPrimaryId;
    }

    public List<Long> getInitialSecondaryIds() {
        return new ArrayList<>(initialSecondaryIds);
    }

    public List<String> getInitialTags() {
        return new ArrayList<>(initialTags);
    }

    public void retryCategories() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        loadCategories();
    }

    /**
     * Mirrors PUT /organizer/events/{id}/classification. Validation is repeated here because
     * the category could have been hidden by an admin while the form was open.
     */
    public void saveClassification(long primaryId, List<Long> secondaryIds, List<String> tags) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        if (primaryId <= 0L) {
            saveResult.setValue(SAVE_ERROR);
            return;
        }
        if ((secondaryIds != null && secondaryIds.size() > 2)
                || (tags != null && tags.size() > 5)) {
            saveResult.setValue(SAVE_ERROR);
            return;
        }
        saving.setValue(true);
        handler.postDelayed(() -> {
            List<ClassificationSectionView.CategoryOption> options = categories.getValue();
            boolean primaryActive = false;
            if (options != null) {
                for (ClassificationSectionView.CategoryOption option : options) {
                    if (option.id == primaryId && option.active) {
                        primaryActive = true;
                        break;
                    }
                }
            }
            if (!primaryActive) {
                saving.setValue(false);
                saveResult.setValue(SAVE_ERROR);
                return;
            }
            initialPrimaryId = primaryId;
            initialSecondaryIds.clear();
            if (secondaryIds != null) {
                initialSecondaryIds.addAll(secondaryIds);
            }
            initialTags.clear();
            if (tags != null) {
                initialTags.addAll(tags);
            }
            classificationPrimaryId = primaryId;
            publishAiGate();
            saving.setValue(false);
            saveResult.setValue(SAVE_SUCCESS);
        }, SAVE_DELAY_MS);
    }

    public void consumeSaveResult() {
        saveResult.setValue(SAVE_NONE);
    }

    private void loadCategories() {
        loading.setValue(true);
        handler.postDelayed(() -> {
            List<ClassificationSectionView.CategoryOption> options = new ArrayList<>();
            for (EventCategory category : EventCategory.values()) {
                options.add(new ClassificationSectionView.CategoryOption(
                        EventCategoryUi.id(category),
                        getApplication().getString(EventCategoryUi.labelRes(category)),
                        true,
                        EventCategoryUi.iconRes(category)));
            }
            // Hidden category sample: still rendered for an old event, never selectable.
            options.add(new ClassificationSectionView.CategoryOption(
                    90L, "Sự kiện cộng đồng", false, R.drawable.ic_ds_users));
            categories.setValue(options);
            suggestions.setValue(Arrays.asList(
                    "indie", "live music", "workshop", "công nghệ", "âm nhạc", "miễn phí"));
            loading.setValue(false);
        }, CATEGORY_LOAD_DELAY_MS);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }

    // ===== FR-O06: description slice =====

    public static final int KEYPOINT_OK = 0;
    public static final int KEYPOINT_DUPLICATE = 1;
    public static final int KEYPOINT_LENGTH = 2;
    public static final int KEYPOINT_LIMIT = 3;

    private static final int MIN_KEYPOINT_LENGTH = 3;
    private static final int MAX_KEYPOINT_LENGTH = 200;
    private static final int MIN_KEYPOINTS = 3;
    private static final int MAX_KEYPOINTS = 10;
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MAX_TITLE_LENGTH = 120;

    /** Result of the O06 precondition check: the assistant is gated on real form data. */
    public static class AiGate {
        public final boolean ready;
        public final List<String> missing;

        AiGate(boolean ready, List<String> missing) {
            this.ready = ready;
            this.missing = missing == null ? new ArrayList<>() : missing;
        }
    }

    private final MutableLiveData<AiGate> aiGate =
            new MutableLiveData<>(new AiGate(false, new ArrayList<>()));
    private final List<String> keyPoints = new ArrayList<>();

    private String eventTitle = "";
    private String eventFormat = "ONSITE";
    private long classificationPrimaryId;
    private String description = "";
    private String aiDraft = "";
    private String previousDescription = "";
    private boolean descriptionFromAi;

    public void setDescriptionTitle(String value) {
        eventTitle = value == null ? "" : value;
        publishAiGate();
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventFormat(String value) {
        eventFormat = value == null || value.isEmpty() ? "ONSITE" : value;
        publishAiGate();
    }

    public String getEventFormat() {
        return eventFormat;
    }

    public void setClassificationPrimary(long id) {
        classificationPrimaryId = id;
        publishAiGate();
    }

    public long getClassificationPrimaryId() {
        return classificationPrimaryId;
    }

    public LiveData<AiGate> getAiGate() {
        return aiGate;
    }

    public List<String> getKeyPoints() {
        return new ArrayList<>(keyPoints);
    }

    /** @return one of the KEYPOINT_* codes so the screen can show a precise message. */
    public int addKeyPoint(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() < MIN_KEYPOINT_LENGTH || value.length() > MAX_KEYPOINT_LENGTH) {
            return KEYPOINT_LENGTH;
        }
        if (keyPoints.contains(value)) {
            return KEYPOINT_DUPLICATE;
        }
        if (keyPoints.size() >= MAX_KEYPOINTS) {
            return KEYPOINT_LIMIT;
        }
        keyPoints.add(value);
        publishAiGate();
        return KEYPOINT_OK;
    }

    public void removeKeyPoint(String value) {
        if (keyPoints.remove(value)) {
            publishAiGate();
        }
    }

    public boolean hasDescription() {
        return !description.trim().isEmpty();
    }

    public boolean isDescriptionFromAi() {
        return descriptionFromAi;
    }

    /** Keeps the "Do AI soạn" flag until the organizer actually edits the text (O06-R05). */
    public void setDescription(String value) {
        String next = value == null ? "" : value;
        if (descriptionFromAi && !next.equals(aiDraft)) {
            descriptionFromAi = false;
        }
        description = next;
    }

    /** Puts the generated text into the form and remembers the previous one for undo. */
    public String applyAiDescription(String text, boolean replace) {
        previousDescription = description;
        if (replace || description.trim().isEmpty()) {
            description = text;
        } else {
            description = description + "\n\n" + text;
        }
        aiDraft = description;
        descriptionFromAi = true;
        return description;
    }

    public String undoDescription() {
        description = previousDescription;
        descriptionFromAi = false;
        return description;
    }

    private void publishAiGate() {
        List<String> missing = new ArrayList<>();
        int titleLength = eventTitle.trim().length();
        if (titleLength < MIN_TITLE_LENGTH || titleLength > MAX_TITLE_LENGTH) {
            missing.add(getApplication().getString(R.string.hc_aidesc_missing_title));
        }
        if (classificationPrimaryId <= 0L) {
            missing.add(getApplication().getString(R.string.hc_aidesc_missing_category));
        }
        if (keyPoints.size() < MIN_KEYPOINTS) {
            missing.add(getApplication().getString(R.string.hc_aidesc_missing_keypoints));
        }
        aiGate.setValue(new AiGate(missing.isEmpty(), missing));
    }
}
