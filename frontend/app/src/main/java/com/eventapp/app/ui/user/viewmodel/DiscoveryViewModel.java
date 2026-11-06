package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.ui.mapper.CategoryMapper;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.model.UserFilters;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Trạng thái tab Khám phá: danh mục, thành phố, tìm kiếm phân trang và yêu thích. */
public class DiscoveryViewModel extends ViewModel {

    private static final int PAGE_SIZE = 20;

    private final CategoryRepository categoryRepository =
            ServiceLocator.get(CategoryRepository.class);
    private final EventRepository eventRepository = ServiceLocator.get(EventRepository.class);
    private final FavoriteRepository favoriteRepository =
            ServiceLocator.get(FavoriteRepository.class);

    private final MutableLiveData<List<CategoryNodeUiModel>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> cities =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> events =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasNext = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<ApiException> favoriteError = new MutableLiveData<>();

    private final Set<Long> favoriteRequests = new HashSet<>();
    private final FavoriteRepository.FavoriteListener favoriteListener =
            (eventId, favorite) -> updateFavorite(eventId, favorite);

    private UserFilters filters = new UserFilters();
    private String query = "";
    private int page;

    public DiscoveryViewModel() {
        favoriteRepository.addListener(favoriteListener);
    }

    public LiveData<List<CategoryNodeUiModel>> getCategories() {
        return categories;
    }

    public LiveData<List<String>> getCities() {
        return cities;
    }

    public LiveData<List<EventUiModel>> getEvents() {
        return events;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getHasNext() {
        return hasNext;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<ApiException> getFavoriteError() {
        return favoriteError;
    }

    public void loadCategories() {
        categoryRepository.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                  @Nullable JsonElement meta) {
                categories.setValue(CategoryMapper.fromTree(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                categories.setValue(new ArrayList<>());
            }
        });
    }

    public void loadCities() {
        eventRepository.cities(new ApiCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> data, @Nullable JsonElement meta) {
                cities.setValue(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                cities.setValue(new ArrayList<>());
            }
        });
    }

    public void search(@NonNull UserFilters nextFilters, @Nullable String nextQuery) {
        filters = nextFilters.copy();
        query = nextQuery == null ? "" : nextQuery.trim();
        page = 0;
        error.setValue(null);
        events.setValue(new ArrayList<>());
        loading.setValue(true);
        loadPage(0, true);
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(loading.getValue())
                || !Boolean.TRUE.equals(hasNext.getValue())) {
            return;
        }
        loading.setValue(true);
        loadPage(page + 1, false);
    }

    public void toggleFavorite(@NonNull EventUiModel event) {
        if (!favoriteRequests.add(event.id)) {
            return;
        }
        boolean target = !event.favorite;
        event.favorite = target;
        publishEvents();

        ApiCallback<FavoriteDtos.ToggleResult> callback =
                new ApiCallback<FavoriteDtos.ToggleResult>() {
                    @Override
                    public void onSuccess(@Nullable FavoriteDtos.ToggleResult data,
                                          @Nullable JsonElement meta) {
                        favoriteRequests.remove(event.id);
                        if (data != null) {
                            updateFavorite(event.id, data.isFavorite);
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        favoriteRequests.remove(event.id);
                        event.favorite = !target;
                        publishEvents();
                        favoriteError.setValue(exception);
                    }
                };
        if (target) {
            favoriteRepository.add(event.id, callback);
        } else {
            favoriteRepository.remove(event.id, callback);
        }
    }

    public void consumeFavoriteError() {
        favoriteError.setValue(null);
    }

    private void loadPage(int targetPage, boolean replace) {
        eventRepository.search(buildQuery(targetPage),
                new ApiCallback<Paged<DiscoveryDtos.EventCard>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<DiscoveryDtos.EventCard> data,
                                          @Nullable JsonElement meta) {
                        List<EventUiModel> result = new ArrayList<>();
                        if (!replace && events.getValue() != null) {
                            result.addAll(events.getValue());
                        }
                        if (data != null) {
                            for (DiscoveryDtos.EventCard card : data.items) {
                                result.add(EventMapper.fromCard(card));
                            }
                        }
                        events.setValue(result);
                        page = targetPage;
                        hasNext.setValue(data != null && data.hasNext());
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                        if (replace) {
                            events.setValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFinish() {
                        loading.setValue(false);
                    }
                });
    }

    @NonNull
    private EventRepository.SearchQuery buildQuery(int targetPage) {
        EventRepository.SearchQuery request = new EventRepository.SearchQuery();
        request.q = query.isEmpty() ? null : query;
        request.categoryIds = filters.categoryId == null
                ? null : Collections.singletonList(filters.categoryId);
        request.dateFrom = filters.dateFrom;
        request.dateTo = filters.dateTo;
        request.city = filters.city;
        request.priceType = filters.priceType;
        request.priceMin = filters.priceMin;
        request.priceMax = filters.priceMax;
        request.sort = filters.sort == null
                ? (query.isEmpty() ? UserFilters.SORT_UPCOMING : UserFilters.SORT_RELEVANCE)
                : filters.sort;
        request.page = targetPage;
        request.size = PAGE_SIZE;
        return request;
    }

    private void updateFavorite(long eventId, boolean favorite) {
        List<EventUiModel> current = events.getValue();
        if (current == null) {
            return;
        }
        boolean changed = false;
        for (EventUiModel event : current) {
            if (event.id == eventId && event.favorite != favorite) {
                event.favorite = favorite;
                changed = true;
            }
        }
        if (changed) {
            publishEvents();
        }
    }

    private void publishEvents() {
        List<EventUiModel> current = events.getValue();
        events.setValue(current == null ? new ArrayList<>() : new ArrayList<>(current));
    }

    @Override
    protected void onCleared() {
        favoriteRepository.removeListener(favoriteListener);
        super.onCleared();
    }
}
