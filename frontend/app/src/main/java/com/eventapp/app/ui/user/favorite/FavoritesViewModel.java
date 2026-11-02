package com.eventapp.app.ui.user.favorite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;
import com.eventapp.app.data.repository.AiRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.mapper.PageMappers;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.model.FavoriteUiModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FR-U06 — danh sách đã lưu lấy từ {@code GET /me/favorites} và thao tác thêm/bỏ qua
 * {@code PUT/DELETE /me/favorites/{eventId}}.
 *
 * <p>Bộ lọc Sắp diễn ra / Đã qua do backend quyết định (tham số {@code filter}) để không lệch
 * khi danh sách dài hơn một trang.</p>
 */
public class FavoritesViewModel extends ViewModel {

    public enum Filter {
        ALL,
        UPCOMING,
        PAST
    }

    private static final int MAX_SUGGESTIONS = 8;
    private static final int PAGE_SIZE = 50;

    private final FavoriteRepository favorites = ServiceLocator.get(FavoriteRepository.class);
    private final AiRepository ai = ServiceLocator.get(AiRepository.class);

    private final MutableLiveData<List<FavoriteUiModel>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> suggestions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> alertsEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private final List<FavoriteUiModel> saved = new ArrayList<>();
    private Filter filter = Filter.ALL;
    private boolean listLoaded;

    public FavoritesViewModel() {
        load();
    }

    public LiveData<List<FavoriteUiModel>> getItems() {
        return items;
    }

    public LiveData<List<EventUiModel>> getSuggestions() {
        return suggestions;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Boolean> getAlertsEnabled() {
        return alertsEnabled;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        Filter next = filter == null ? Filter.ALL : filter;
        if (next == this.filter) {
            return;
        }
        this.filter = next;
        load();
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        loadList();
        loadSuggestions();
    }

    public void refresh() {
        load();
    }

    public void setAlertsEnabled(boolean enabled) {
        alertsEnabled.setValue(enabled);
    }

    /** Bỏ yêu thích: cập nhật lạc quan rồi gọi API, lỗi thì hoàn tác. */
    public void remove(@NonNull FavoriteUiModel item) {
        final int index = indexOf(item);
        if (index < 0) {
            return;
        }
        final FavoriteUiModel removed = saved.remove(index);
        publish();
        favorites.remove(removed.eventId,
                new ApiCallback<FavoriteDtos.ToggleResult>() {
                    @Override
                    public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        message.setValue("Đã bỏ khỏi yêu thích");
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        saved.add(Math.min(index, saved.size()), removed);
                        publish();
                        message.setValue(exception.getUserMessage());
                    }
                });
    }

    /** Hoàn tác thao tác xoá (nút UNDO trên Snackbar). */
    public void restore(@Nullable FavoriteUiModel item) {
        if (item == null || item.event == null) {
            return;
        }
        if (findSaved(item.event.id) != null) {
            return;
        }
        item.event.favorite = true;
        saved.add(0, item);
        publish();
        favorites.add(item.event.id, new ApiCallback<FavoriteDtos.ToggleResult>() {
            @Override
            public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                  @Nullable com.google.gson.JsonElement meta) {
                message.setValue("Đã khôi phục yêu thích");
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                saved.remove(item);
                publish();
                message.setValue(exception.getUserMessage());
            }
        });
    }

    public void addToFavorites(@Nullable EventUiModel event) {
        if (event == null || findSaved(event.id) != null) {
            return;
        }
        event.favorite = true;
        FavoriteUiModel item = FavoriteUiModel.from(event, System.currentTimeMillis());
        saved.add(0, item);
        publish();
        favorites.add(event.id, new ApiCallback<FavoriteDtos.ToggleResult>() {
            @Override
            public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                  @Nullable com.google.gson.JsonElement meta) {
                message.setValue("Đã lưu sự kiện");
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                event.favorite = false;
                saved.remove(item);
                publish();
                message.setValue(favoriteMessage(exception));
            }
        });
    }

    public void removeEvent(long eventId) {
        FavoriteUiModel item = findSaved(eventId);
        if (item != null) {
            remove(item);
        }
    }

    private void loadList() {
        favorites.list(filter.name(), 0, PAGE_SIZE,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        saved.clear();
                        Set<Long> seen = new HashSet<>();
                        if (data != null) {
                            for (FavoriteDtos.Item dto : data.items) {
                                if (dto == null || !seen.add(dto.eventId)) {
                                    continue;
                                }
                                saved.add(PageMappers.favorite(dto));
                            }
                        }
                        listLoaded = true;
                        loading.setValue(false);
                        publish();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(!listLoaded);
                        message.setValue(exception.getUserMessage());
                    }
                });
    }

    private void loadSuggestions() {
        ai.recommendations(MAX_SUGGESTIONS, false,
                new ApiCallback<RecommendationDtos.ListResult>() {
                    @Override
                    public void onSuccess(@Nullable RecommendationDtos.ListResult data,
                                          @Nullable com.google.gson.JsonElement meta) {
                        List<EventUiModel> result = new ArrayList<>();
                        if (data != null && data.items != null) {
                            for (RecommendationDtos.Item item : data.items) {
                                if (item == null || item.event == null
                                        || findSaved(item.event.id) != null) {
                                    continue;
                                }
                                EventUiModel event = EventMapper.fromCard(item.event);
                                if (event.availability == EventUiModel.Availability.CANCELLED) {
                                    continue;
                                }
                                result.add(event);
                            }
                        }
                        suggestions.setValue(result);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        suggestions.setValue(new ArrayList<>());
                    }
                });
    }

    private void publish() {
        items.setValue(new ArrayList<>(saved));
    }

    @Nullable
    private FavoriteUiModel findSaved(long eventId) {
        for (FavoriteUiModel item : saved) {
            if (item.event != null && item.event.id == eventId) {
                return item;
            }
        }
        return null;
    }

    private int indexOf(@NonNull FavoriteUiModel item) {
        for (int i = 0; i < saved.size(); i++) {
            FavoriteUiModel current = saved.get(i);
            if (current == item) {
                return i;
            }
            if (current.event != null && item.event != null
                    && current.event.id == item.event.id) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private static String favoriteMessage(@NonNull ApiException exception) {
        if (exception.getHttpCode() == 409
                || "FAVORITE_LIMIT_REACHED".equalsIgnoreCase(exception.getErrorCode())) {
            return "Bạn đã đạt giới hạn sự kiện yêu thích.";
        }
        return exception.getUserMessage();
    }
}
