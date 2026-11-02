package com.eventapp.app.ui.user.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.preference.PreferenceDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.UserRepository;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FR-U03 — sở thích lấy từ {@code GET /me/preferences} và lưu qua {@code PUT}.
 *
 * <p>Danh mục là dữ liệu thật từ {@code /categories} nên id khớp với sự kiện. Giới hạn 10 danh mục
 * được chặn ở client để không phải chờ lỗi 400; {@code version} là khoá lạc quan của server
 * (409 nghĩa là dữ liệu đã đổi ở thiết bị khác).</p>
 */
public class PreferencesViewModel extends ViewModel {

    public static final int RESULT_NONE = 0;
    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_CONFLICT = 2;
    public static final int RESULT_ERROR = 3;

    private final UserRepository repository = ServiceLocator.get(UserRepository.class);
    private final CategoryRepository categoryRepository =
            ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<PreferencesForm> form = new MutableLiveData<>();
    private final MutableLiveData<List<CatalogDtos.CategoryNode>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<CatalogDtos.CategoryNode>> hiddenCategories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> saveResult = new MutableLiveData<>(RESULT_NONE);

    private int pendingCalls;

    public PreferencesViewModel() {
        load();
    }

    public LiveData<PreferencesForm> getForm() {
        return form;
    }

    public LiveData<List<CatalogDtos.CategoryNode>> getCategories() {
        return categories;
    }

    public LiveData<List<CatalogDtos.CategoryNode>> getHiddenCategories() {
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
        pendingCalls = 2;
        loadCategories();
        loadPreferences();
    }

    public void retry() {
        load();
    }

    private void loadCategories() {
        categoryRepository.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                  @Nullable JsonElement meta) {
                categories.setValue(data == null ? new ArrayList<>() : data);
                finishOne(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                finishOne(true);
            }
        });
    }

    private void loadPreferences() {
        repository.preferences(new ApiCallback<PreferenceDtos.Preference>() {
            @Override
            public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                  @Nullable JsonElement meta) {
                if (data != null) {
                    form.setValue(map(data));
                }
                finishOne(false);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                finishOne(true);
            }
        });
    }

    private void finishOne(boolean failed) {
        if (failed) {
            error.setValue(true);
        }
        pendingCalls--;
        if (pendingCalls <= 0) {
            loading.setValue(false);
        }
    }

    @NonNull
    private PreferencesForm map(@NonNull PreferenceDtos.Preference dto) {
        PreferencesForm value = new PreferencesForm();
        value.categoryIds.clear();
        if (dto.categoryIds != null) {
            value.categoryIds.addAll(dto.categoryIds);
        }
        value.format = format(dto.eventFormat);
        value.priceMode = priceMode(dto.priceMode);
        value.maxBudget = dto.maxBudgetVnd == null ? 0L : dto.maxBudgetVnd;
        value.cities.clear();
        if (dto.preferredCities != null) {
            value.cities.addAll(dto.preferredCities);
        }
        value.days.clear();
        if (dto.preferredDays != null) {
            for (String day : dto.preferredDays) {
                PreferencesForm.DayPart part = dayPart(day);
                if (part != null) {
                    value.days.add(part);
                }
            }
        }
        value.version = dto.version;
        return value;
    }

    public boolean toggleCategory(long categoryId) {
        PreferencesForm current = form.getValue();
        if (current == null || categoryId <= 0L) {
            return false;
        }
        PreferencesForm next = current.copy();
        if (next.categoryIds.contains(categoryId)) {
            next.categoryIds.remove(categoryId);
        } else {
            if (next.categoryIds.size() >= PreferencesForm.MAX_CATEGORIES) {
                return false;
            }
            next.categoryIds.add(categoryId);
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
        repository.resetPreferences(new ApiCallback<PreferenceDtos.Preference>() {
            @Override
            public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                  @Nullable JsonElement meta) {
                form.setValue(data == null ? new PreferencesForm() : map(data));
                saveResult.setValue(RESULT_SUCCESS);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                saveResult.setValue(RESULT_ERROR);
            }
        });
    }

    public void save() {
        PreferencesForm current = form.getValue();
        if (current == null || Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        saving.setValue(true);
        PreferenceDtos.UpdateRequest request = new PreferenceDtos.UpdateRequest();
        request.categoryIds = new ArrayList<>(current.categoryIds);
        request.eventFormat = current.format.name();
        request.priceMode = current.priceMode.name();
        request.maxBudgetVnd = current.effectiveBudget();
        request.preferredCities = new ArrayList<>(current.cities);
        request.preferredDays = new ArrayList<>();
        for (PreferencesForm.DayPart day : current.days) {
            request.preferredDays.add(day.name());
        }
        request.version = current.version;

        repository.savePreferences(request, new ApiCallback<PreferenceDtos.Preference>() {
            @Override
            public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                  @Nullable JsonElement meta) {
                saving.setValue(false);
                if (data != null) {
                    form.setValue(map(data));
                }
                saveResult.setValue(RESULT_SUCCESS);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                saving.setValue(false);
                saveResult.setValue(exception.getHttpCode() == 409
                        ? RESULT_CONFLICT : RESULT_ERROR);
            }
        });
    }

    /** "Dùng bản mới nhất" for a 409 conflict: reload the server copy. */
    public void reloadLatest() {
        load();
    }

    /** "Giữ bản của bạn" for a 409 conflict: resend after reloading the version. */
    public void forceSave() {
        repository.preferences(new ApiCallback<PreferenceDtos.Preference>() {
            @Override
            public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                  @Nullable JsonElement meta) {
                PreferencesForm current = form.getValue();
                if (data == null || current == null) {
                    return;
                }
                PreferencesForm next = current.copy();
                next.version = data.version;
                form.setValue(next);
                save();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                saveResult.setValue(RESULT_ERROR);
            }
        });
    }

    public void consumeSaveResult() {
        saveResult.setValue(RESULT_NONE);
    }

    @NonNull
    private static PreferencesForm.Format format(@Nullable String raw) {
        if (raw == null) {
            return PreferencesForm.Format.ANY;
        }
        switch (raw.toUpperCase(java.util.Locale.ROOT)) {
            case "ONSITE":
                return PreferencesForm.Format.ONSITE;
            case "ONLINE":
                return PreferencesForm.Format.ONLINE;
            case "ANY":
            default:
                return PreferencesForm.Format.ANY;
        }
    }

    @NonNull
    private static PreferencesForm.PriceMode priceMode(@Nullable String raw) {
        if (raw == null) {
            return PreferencesForm.PriceMode.ANY;
        }
        switch (raw.toUpperCase(java.util.Locale.ROOT)) {
            case "FREE_ONLY":
                return PreferencesForm.PriceMode.FREE_ONLY;
            case "PAID_ONLY":
                return PreferencesForm.PriceMode.PAID_ONLY;
            case "ANY":
            default:
                return PreferencesForm.PriceMode.ANY;
        }
    }

    @Nullable
    private static PreferencesForm.DayPart dayPart(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        switch (raw.toUpperCase(java.util.Locale.ROOT)) {
            case "WEEKDAY":
                return PreferencesForm.DayPart.WEEKDAY;
            case "WEEKEND":
                return PreferencesForm.DayPart.WEEKEND;
            default:
                return null;
        }
    }
}
