package com.eventapp.app.ui.user.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.remote.dto.recommendation.RecommendationDtos;
import com.eventapp.app.data.repository.AiRepository;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.mapper.PageMappers;
import com.eventapp.app.ui.model.EventUiModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Home data for FR-U04/U07.
 *
 * <p>Nguồn dữ liệu là API thật: gợi ý AI cho khối đầu trang, danh sách sắp diễn ra cho khối
 * "Sắp diễn ra", và các truy vấn sắp theo độ phổ biến cho "Thịnh hành". Một API lỗi không làm
 * hỏng cả trang — từng khối tự cập nhật khi có dữ liệu.</p>
 */
public class HomeViewModel extends ViewModel {

    private static final int AI_LIMIT = 6;
    private static final int HERO_LIMIT = 6;
    private static final int TRENDING_LIMIT = 4;
    private static final int UPCOMING_LIMIT = 6;

    private final EventRepository events = ServiceLocator.get(EventRepository.class);
    private final AiRepository ai = ServiceLocator.get(AiRepository.class);
    private final CategoryRepository categories = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<List<EventUiModel>> aiEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> featuredEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> upcomingEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> trendingEvents = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<List<CatalogDtos.CategoryNode>> categoryTree =
            new MutableLiveData<>(new ArrayList<>());
    private final Map<String, Long> categoryIds = new LinkedHashMap<>();

    private int pendingCalls;

    public HomeViewModel() {
        load();
    }

    public LiveData<List<EventUiModel>> getAiEvents() {
        return aiEvents;
    }

    public LiveData<List<EventUiModel>> getFeaturedEvents() {
        return featuredEvents;
    }

    public LiveData<List<EventUiModel>> getUpcomingEvents() {
        return upcomingEvents;
    }

    public LiveData<List<EventUiModel>> getTrendingEvents() {
        return trendingEvents;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    /** Cây danh mục thật (id 9–24) để đầu trang hiển thị đúng danh mục backend. */
    public LiveData<List<CatalogDtos.CategoryNode>> getCategories() {
        return categoryTree;
    }

    /** Id danh mục backend theo tên, dùng cho hàng icon danh mục ở trang chủ. */
    public long categoryId(@Nullable String backendName) {
        Long id = backendName == null ? null : categoryIds.get(backendName.toLowerCase(java.util.Locale.ROOT));
        return id == null ? 0L : id;
    }

    public void refresh() {
        load();
    }

    public void load() {
        loading.setValue(true);
        error.setValue(null);
        // Chỉ chờ 3 khối lõi (danh mục, sắp diễn ra, thịnh hành). Khối gợi ý AI tốn thời gian
        // nhất (backend có thể gọi mô hình) nên cập nhật riêng khi có kết quả, không giữ
        // skeleton của cả trang.
        pendingCalls = 3;

        loadCategories();
        loadRecommendations();
        loadUpcoming();
        loadPopular();
    }

    private void loadCategories() {
        categories.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(List<CatalogDtos.CategoryNode> data, com.google.gson.JsonElement meta) {
                categoryIds.clear();
                if (data != null) {
                    for (CatalogDtos.CategoryNode node : data) {
                        if (node.name != null) {
                            categoryIds.put(node.name.toLowerCase(java.util.Locale.ROOT), node.id);
                        }
                        if (node.children != null) {
                            for (CatalogDtos.CategoryNode child : node.children) {
                                if (child.name != null) {
                                    categoryIds.put(child.name.toLowerCase(java.util.Locale.ROOT), child.id);
                                }
                            }
                        }
                    }
                    categoryTree.setValue(data);
                }
                finishOne();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                finishOne();
            }
        });
    }

    private void loadRecommendations() {
        ai.recommendations(AI_LIMIT, false,
                new ApiCallback<RecommendationDtos.ListResult>() {
                    @Override
                    public void onSuccess(RecommendationDtos.ListResult data, com.google.gson.JsonElement meta) {
                        List<EventUiModel> mapped = new ArrayList<>();
                        if (data != null && data.items != null) {
                            for (RecommendationDtos.Item item : data.items) {
                                EventUiModel event = PageMappers.recommendation(item).event;
                                mapped.add(new EventUiModel.Builder()
                                        .id(event.id)
                                        .title(event.title)
                                        .description(event.description)
                                        .coverUrl(event.coverUrl)
                                        .venue(event.venue)
                                        .city(event.city)
                                        .startAt(event.startAt)
                                        .endAt(event.endAt)
                                        .online(event.online)
                                        .priceFrom(event.priceFrom)
                                        .free(event.free)
                                        .availability(event.availability)
                                        .category(event.category)
                                        .rating(event.rating)
                                        .reviewCount(event.reviewCount)
                                        .aiScore(item.reasonSource != null ? 90 : 70)
                                        .aiReason(item.reason)
                                        .favorite(event.favorite)
                                        .build());
                            }
                        }
                        aiEvents.setValue(mapped);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        // Gợi ý lỗi không chặn phần còn lại của trang.
                        aiEvents.setValue(Collections.emptyList());
                    }
                });
    }

    private void loadUpcoming() {
        EventRepository.SearchQuery query = new EventRepository.SearchQuery();
        query.sort = "START_TIME_ASC";
        query.size = UPCOMING_LIMIT;
        events.search(query, new ApiCallback<Paged<DiscoveryDtos.EventCard>>() {
            @Override
            public void onSuccess(Paged<DiscoveryDtos.EventCard> data, com.google.gson.JsonElement meta) {
                upcomingEvents.setValue(map(data == null ? null : data.items));
                finishOne();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                reportError(exception);
                finishOne();
            }
        });
    }

    private void loadPopular() {
        EventRepository.SearchQuery query = new EventRepository.SearchQuery();
        query.sort = "POPULAR";
        query.size = HERO_LIMIT;
        events.search(query, new ApiCallback<Paged<DiscoveryDtos.EventCard>>() {
            @Override
            public void onSuccess(Paged<DiscoveryDtos.EventCard> data, com.google.gson.JsonElement meta) {
                List<EventUiModel> mapped = map(data == null ? null : data.items);
                featuredEvents.setValue(limit(mapped, HERO_LIMIT));
                List<EventUiModel> trending = new ArrayList<>(mapped);
                Collections.sort(trending, Comparator.comparingLong(event -> event.startAt));
                trendingEvents.setValue(limit(trending, TRENDING_LIMIT));
                finishOne();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                reportError(exception);
                finishOne();
            }
        });
    }

    private void finishOne() {
        pendingCalls--;
        if (pendingCalls <= 0) {
            loading.setValue(false);
        }
    }

    private void reportError(@NonNull ApiException exception) {
        if (error.getValue() == null) {
            error.setValue(exception);
        }
    }

    @NonNull
    private static List<EventUiModel> map(@Nullable List<DiscoveryDtos.EventCard> cards) {
        List<EventUiModel> mapped = new ArrayList<>();
        if (cards != null) {
            for (DiscoveryDtos.EventCard card : cards) {
                mapped.add(EventMapper.fromCard(card));
            }
        }
        return mapped;
    }

    @NonNull
    private static List<EventUiModel> limit(@NonNull List<EventUiModel> source, int max) {
        if (source.size() <= max) {
            return source;
        }
        return new ArrayList<>(source.subList(0, max));
    }
}
