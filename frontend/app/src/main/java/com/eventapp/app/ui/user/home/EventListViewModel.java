package com.eventapp.app.ui.user.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.model.EventFilterCriteria;
import com.eventapp.app.data.remote.dto.discovery.DiscoveryDtos;
import com.eventapp.app.data.repository.EventRepository;
import com.eventapp.app.ui.mapper.EventMapper;
import com.eventapp.app.ui.model.EventUiModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Tìm kiếm/lọc/phân trang FR-U04 — mọi trạng thái lấy từ {@code GET /events}.
 *
 * <p>Bộ lọc trên UI được chuyển nguyên sang tham số backend; app không lọc lại danh sách đã
 * phân trang (sẽ sai khi có nhiều trang).</p>
 */
public class EventListViewModel extends ViewModel {

    private static final int PAGE_SIZE = 10;

    private final EventRepository repository = ServiceLocator.get(EventRepository.class);

    private final MutableLiveData<List<EventUiModel>> events = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);

    private EventFilterCriteria criteria = new EventFilterCriteria();
    private String query = "";
    private int page;
    private boolean firstPageLoaded;

    public LiveData<List<EventUiModel>> getEvents() {
        return events;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }

    public EventFilterCriteria getCriteria() {
        return criteria.copy();
    }

    public String getQuery() {
        return query;
    }

    public void applyFilters(EventFilterCriteria criteria, String query) {
        this.criteria = criteria == null ? new EventFilterCriteria() : criteria.copy();
        this.query = query == null ? "" : query.trim();
        refresh();
    }

    public void refresh() {
        page = 0;
        firstPageLoaded = false;
        error.setValue(null);
        loading.setValue(true);
        loadPage(0, false);
    }

    public void retry() {
        refresh();
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(loading.getValue())
                || Boolean.TRUE.equals(loadingMore.getValue())
                || !Boolean.TRUE.equals(hasMore.getValue())) {
            return;
        }
        loadingMore.setValue(true);
        loadPage(page + 1, true);
    }

    private void loadPage(int targetPage, boolean append) {
        EventRepository.SearchQuery request = toQuery(targetPage);
        repository.search(request, new ApiCallback<Paged<DiscoveryDtos.EventCard>>() {
            @Override
            public void onSuccess(Paged<DiscoveryDtos.EventCard> data, com.google.gson.JsonElement meta) {
                List<EventUiModel> mapped = new ArrayList<>();
                if (data != null) {
                    for (DiscoveryDtos.EventCard card : data.items) {
                        mapped.add(EventMapper.fromCard(card));
                    }
                }
                if (append) {
                    List<EventUiModel> merged = events.getValue() == null
                            ? new ArrayList<>() : new ArrayList<>(events.getValue());
                    merged.addAll(mapped);
                    events.setValue(merged);
                } else {
                    events.setValue(mapped);
                    firstPageLoaded = true;
                }
                page = targetPage;
                hasMore.setValue(data != null && data.hasNext());
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
                if (!append) {
                    events.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFinish() {
                loading.setValue(false);
                loadingMore.setValue(false);
            }
        });
    }

    @NonNull
    private EventRepository.SearchQuery toQuery(int targetPage) {
        EventRepository.SearchQuery request = new EventRepository.SearchQuery();
        request.q = query;
        request.categoryIds = new ArrayList<>(criteria.categoryIds);
        request.city = criteria.city;
        request.format = criteria.format == EventFilterCriteria.Format.ONSITE ? "OFFLINE"
                : criteria.format == EventFilterCriteria.Format.ONLINE ? "ONLINE" : null;
        request.priceType = criteria.priceType == EventFilterCriteria.PriceType.FREE ? "FREE"
                : criteria.priceType == EventFilterCriteria.PriceType.PAID ? "PAID" : null;
        if (criteria.priceRangeSet) {
            request.priceMin = criteria.priceMin > 0 ? criteria.priceMin : null;
            request.priceMax = criteria.priceMax > 0
                    && criteria.priceMax < EventFilterCriteria.MAX_PRICE
                    ? criteria.priceMax : null;
        }
        request.availableOnly = criteria.availableOnly;
        request.sort = sortKey(criteria.sort);
        request.page = targetPage;
        request.size = PAGE_SIZE;
        applyTimeRange(request);
        return request;
    }

    private void applyTimeRange(@NonNull EventRepository.SearchQuery request) {
        Calendar calendar = Calendar.getInstance();
        switch (criteria.timePreset) {
            case TODAY:
                request.dateFrom = isoDay(calendar.getTimeInMillis());
                request.dateTo = request.dateFrom;
                break;
            case THIS_WEEK:
                request.dateFrom = isoDay(calendar.getTimeInMillis());
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                request.dateTo = isoDay(calendar.getTimeInMillis());
                break;
            case THIS_MONTH:
                request.dateFrom = isoDay(calendar.getTimeInMillis());
                calendar.add(Calendar.MONTH, 1);
                request.dateTo = isoDay(calendar.getTimeInMillis());
                break;
            case CUSTOM:
                request.dateFrom = isoDay(criteria.dateFrom);
                request.dateTo = isoDay(criteria.dateTo);
                break;
            case ANY:
            default:
                break;
        }
    }

    private String isoDay(long millis) {
        if (millis <= 0L) {
            return null;
        }
        String iso = DateTimeUtils.toIso(millis);
        return iso == null ? null : iso.substring(0, 10);
    }

    @NonNull
    private static String sortKey(@NonNull EventFilterCriteria.Sort sort) {
        switch (sort) {
            case NEWEST:
                return "NEWEST";
            case PRICE_ASC:
                return "PRICE_ASC";
            case PRICE_DESC:
                return "PRICE_DESC";
            case POPULAR:
                return "POPULAR";
            case START_TIME_ASC:
            default:
                return "START_TIME_ASC";
        }
    }
}
