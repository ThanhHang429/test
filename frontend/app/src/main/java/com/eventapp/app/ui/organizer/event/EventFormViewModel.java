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
        }
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
}
