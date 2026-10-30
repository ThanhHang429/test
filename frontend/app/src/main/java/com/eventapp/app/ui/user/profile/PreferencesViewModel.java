package com.eventapp.app.ui.user.profile;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.user.home.EventCategoryUi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FR-U03 state holder. Category limits are enforced here as well as on the server, and the
 * screen only reports success after the save callback returns.
 */
public class PreferencesViewModel extends ViewModel {

    public static final int RESULT_NONE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_CONFLICT = 2;
    public static final int RESULT_ERROR = 3;

    private final MutableLiveData<PreferencesForm> form = new MutableLiveData<>();
    private final MutableLiveData<List<EventCategory>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventCategory>> hiddenCategories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> saveResult = new MutableLiveData<>(RESULT_NONE);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int serverVersion = 1;

    public PreferencesViewModel() {
        load();
    }

    public LiveData<PreferencesForm> getForm() {
        return form;
    }

    public LiveData<List<EventCategory>> getCategories() {
        return categories;
    }

    public LiveData<List<EventCategory>> getHiddenCategories() {
        return hiddenCategories;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Integer> getSaveResult() {
        return saveResult;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            List<EventCategory> visible = new ArrayList<>();
            List<EventCategory> hidden = new ArrayList<>();
            for (EventCategory category : EventCategory.values()) {
                if (category == EventCategory.ART) {
                    hidden.add(category);
                } else {
                    visible.add(category);
                }
            }
            categories.setValue(visible);
            hiddenCategories.setValue(hidden);

            PreferencesForm initial = new PreferencesForm();
            initial.categoryIds.add(EventCategoryUi.id(EventCategory.MUSIC));
            initial.categoryIds.add(EventCategoryUi.id(EventCategory.TECH));
            initial.format = PreferencesForm.Format.ONSITE;
            initial.priceMode = PreferencesForm.PriceMode.ANY;
            initial.maxBudget = 500_000L;
            initial.cities.add("TP. Hồ Chí Minh");
            initial.version = serverVersion;
            form.setValue(initial);
            loading.setValue(false);
        }, 320L);
    }

    public void retry() {
        load();
    }

    public boolean toggleCategory(EventCategory category) {
        PreferencesForm current = form.getValue();
        if (current == null || category == null) {
            return false;
        }
        long id = EventCategoryUi.id(category);
        PreferencesForm next = current.copy();
        if (next.categoryIds.contains(id)) {
            next.categoryIds.remove(id);
        } else {
            if (next.categoryIds.size() >= PreferencesForm.MAX_CATEGORIES) {
                return false;
            }
            next.categoryIds.add(id);
        }
        form.setValue(next);
        return true;
    }

    public boolean toggleCity(String city) {
        PreferencesForm current = form.getValue();
        if (current == null || city == null || city.trim().isEmpty()) {
            return false;
        }
        PreferencesForm next = current.copy();
        if (next.cities.contains(city)) {
            next.cities.remove(city);
        } else {
            if (next.cities.size() >= PreferencesForm.MAX_CITIES) {
                return false;
            }
            next.cities.add(city);
        }
        form.setValue(next);
        return true;
    }

    public void setFormat(PreferencesForm.Format format) {
        PreferencesForm current = form.getValue();
        if (current == null || format == null) {
            return;
        }
        PreferencesForm next = current.copy();
        next.format = format;
        form.setValue(next);
    }

    public void setPriceMode(PreferencesForm.PriceMode priceMode) {
        PreferencesForm current = form.getValue();
        if (current == null || priceMode == null) {
            return;
        }
        PreferencesForm next = current.copy();
        next.priceMode = priceMode;
        if (priceMode == PreferencesForm.PriceMode.FREE_ONLY) {
            next.maxBudget = 0L;
        }
        form.setValue(next);
    }

    public void setMaxBudget(long amount) {
        PreferencesForm current = form.getValue();
        if (current == null) {
            return;
        }
        PreferencesForm next = current.copy();
        next.maxBudget = Math.max(0L, Math.min(PreferencesForm.MAX_BUDGET, amount));
        form.setValue(next);
    }

    public void setDays(Set<PreferencesForm.DayPart> days) {
        PreferencesForm current = form.getValue();
        if (current == null) {
            return;
        }
        PreferencesForm next = current.copy();
        next.days.clear();
        if (days != null) {
            next.days.addAll(days);
        }
        form.setValue(next);
    }

    public void reset() {
        PreferencesForm next = new PreferencesForm();
        next.version = serverVersion;
        form.setValue(next);
    }

    public void save() {
        PreferencesForm current = form.getValue();
        if (current == null) {
            return;
        }
        saving.setValue(true);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            saving.setValue(false);
            PreferencesForm saved = current.copy();
            saved.version = current.version + 1;
            serverVersion = saved.version;
            form.setValue(saved);
            saveResult.setValue(RESULT_SUCCESS);
        }, 600L);
    }

    /** "Dùng bản mới nhất" for a 409 conflict: reload the server copy. */
    public void reloadLatest() {
        load();
    }

    /** "Giữ bản của bạn" for a 409 conflict: resend with the newest version. */
    public void forceSave() {
        PreferencesForm current = form.getValue();
        if (current == null) {
            return;
        }
        PreferencesForm next = current.copy();
        next.version = serverVersion;
        form.setValue(next);
        save();
    }

    public void consumeSaveResult() {
        saveResult.setValue(RESULT_NONE);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
