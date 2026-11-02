package com.eventapp.app.ui.user.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;
import com.eventapp.app.data.repository.AiRepository;
import com.eventapp.app.ui.mapper.PageMappers;
import com.eventapp.app.ui.model.RecommendationUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * FR-U07 — gợi ý lấy từ {@code GET /recommendations}.
 *
 * <p>Lý do gợi ý do backend viết (AI hoặc rule), màn hình không tự suy luận. Nút làm mới bị giới
 * hạn 1 lần/phút ở server; chặn sớm ở client để tránh 429.</p>
 */
public class RecommendViewModel extends ViewModel {

    public static class Meta {
        public final long generatedAt;
        public final boolean personalized;
        public final boolean fallback;

        public Meta(long generatedAt, boolean personalized, boolean fallback) {
            this.generatedAt = generatedAt;
            this.personalized = personalized;
            this.fallback = fallback;
        }
    }

    private static final long REFRESH_INTERVAL_MS = 60_000L;
    private static final int MAX_PREFERENCE_CHIPS = 4;
    private static final int LIMIT = 20;

    private final AiRepository repository = ServiceLocator.get(AiRepository.class);

    private final MutableLiveData<List<RecommendationUiModel>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Meta> meta = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<List<EventCategory>> preferences =
            new MutableLiveData<>(new ArrayList<>());

    private long lastRefreshAt;
    private RecommendationUiModel dismissed;
    private int dismissedIndex = -1;
    private boolean loadedOnce;

    public RecommendViewModel() {
        load(false);
    }

    public LiveData<List<RecommendationUiModel>> getItems() {
        return items;
    }

    public LiveData<Meta> getMeta() {
        return meta;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<List<EventCategory>> getPreferences() {
        return preferences;
    }

    public void load() {
        load(false);
    }

    /**
     * @return false when the one-minute refresh quota has not elapsed; the caller shows the
     *     cooldown message instead of calling the API again (U07-R06).
     */
    public boolean refresh() {
        long now = System.currentTimeMillis();
        if (lastRefreshAt > 0L && now - lastRefreshAt < REFRESH_INTERVAL_MS) {
            return false;
        }
        lastRefreshAt = now;
        load(true);
        return true;
    }

    private void load(boolean refresh) {
        if (!loadedOnce) {
            loading.setValue(true);
        }
        error.setValue(false);
        repository.recommendations(LIMIT, refresh,
                new ApiCallback<RecommendationDtos.ListResult>() {
                    @Override
                    public void onSuccess(@Nullable RecommendationDtos.ListResult data,
                                          @Nullable JsonElement envelopeMeta) {
                        List<RecommendationUiModel> values = new ArrayList<>();
                        if (data != null && data.items != null) {
                            for (RecommendationDtos.Item item : data.items) {
                                if (item == null || item.event == null) {
                                    continue;
                                }
                                values.add(PageMappers.recommendation(item));
                            }
                        }
                        items.setValue(values);
                        preferences.setValue(collectCategories(values));
                        meta.setValue(toMeta(data == null ? null : data.meta));
                        loadedOnce = true;
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        if (!loadedOnce) {
                            error.setValue(true);
                        }
                    }
                });
    }

    /** "Không quan tâm": backend loại sự kiện khỏi gợi ý 30 ngày. */
    public void dismiss(RecommendationUiModel item) {
        List<RecommendationUiModel> current = copyOf(items.getValue());
        int index = current.indexOf(item);
        if (index < 0) {
            return;
        }
        dismissed = current.remove(index);
        dismissedIndex = index;
        items.setValue(current);
        repository.dismiss(item.event.id, new ApiCallback<RecommendationDtos.DismissResult>() {
            @Override
            public void onSuccess(@Nullable RecommendationDtos.DismissResult data,
                                  @Nullable JsonElement meta) {
                dismissed = null;
                dismissedIndex = -1;
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                // Không khôi phục để tránh nhấp nháy; người dùng có thể tải lại.
            }
        });
    }

    public void undoDismiss() {
        if (dismissed == null) {
            return;
        }
        List<RecommendationUiModel> current = copyOf(items.getValue());
        int index = Math.max(0, Math.min(dismissedIndex, current.size()));
        current.add(index, dismissed);
        items.setValue(current);
        dismissed = null;
        dismissedIndex = -1;
    }

    @NonNull
    private static Meta toMeta(@Nullable RecommendationDtos.Meta source) {
        if (source == null) {
            return new Meta(System.currentTimeMillis(), false, false);
        }
        return new Meta(DateTimeUtils.toMillis(source.generatedAt),
                source.personalized, source.isFallback);
    }

    private List<EventCategory> collectCategories(List<RecommendationUiModel> values) {
        Set<EventCategory> unique = new LinkedHashSet<>();
        for (RecommendationUiModel item : values) {
            unique.add(item.event.category);
            if (unique.size() >= MAX_PREFERENCE_CHIPS) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private List<RecommendationUiModel> copyOf(List<RecommendationUiModel> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
