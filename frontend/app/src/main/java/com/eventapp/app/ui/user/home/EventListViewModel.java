package com.eventapp.app.ui.user.home;


import com.eventapp.app.data.model.EventFilterCriteria;
import com.eventapp.app.ui.fake.SampleEventData;
import com.eventapp.app.ui.model.EventUiModel;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Search/filter/pagination state for FR-U04. The API integration will replace the sample list
 * and the simulated delay while keeping this public contract for the screen.
 */
public class EventListViewModel extends ViewModel {

    private static final int PAGE_SIZE = 6;

    private final MutableLiveData<List<EventUiModel>> events = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);

    private final Handler handler = new Handler(Looper.getMainLooper());

    private EventFilterCriteria criteria = new EventFilterCriteria();
    private String query = "";
    private List<EventUiModel> filtered = new ArrayList<>();
    private int page;

    public LiveData<List<EventUiModel>> getEvents() {
        return events;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getLoadingMore() {
        return loadingMore;
    }

    public LiveData<Boolean> getError() {
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
        error.setValue(false);
        loading.setValue(true);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            List<EventUiModel> all = SampleEventData.createEvents();
            filtered = filter(all);
            page = 0;
            events.setValue(pageItems());
            hasMore.setValue(filtered.size() > events.getValue().size());
            loading.setValue(false);
        }, 280L);
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
        handler.postDelayed(() -> {
            page++;
            List<EventUiModel> current = events.getValue() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(events.getValue());
            int from = page * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, filtered.size());
            if (from < to && from >= 0) {
                current.addAll(filtered.subList(from, to));
            }
            events.setValue(current);
            hasMore.setValue(to < filtered.size());
            loadingMore.setValue(false);
        }, 240L);
    }

    private List<EventUiModel> pageItems() {
        int to = Math.min(PAGE_SIZE, filtered.size());
        return new ArrayList<>(filtered.subList(0, to));
    }

    private List<EventUiModel> filter(List<EventUiModel> all) {
        List<EventUiModel> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (EventUiModel event : all) {
            // U04-R03: cancelled events never appear in search results.
            if (event.availability == EventUiModel.Availability.CANCELLED) {
                continue;
            }
            // includePast=false is the public catalogue default.
            if (event.endAt > 0L && event.endAt < now) {
                continue;
            }
            if (!matchesQuery(event)) {
                continue;
            }
            if (!matchesCategory(event)
                    || !matchesTime(event)
                    || !matchesCity(event)
                    || !matchesFormat(event)
                    || !matchesPrice(event)
                    || !matchesAvailability(event)) {
                continue;
            }
            result.add(event);
        }
        sort(result);
        return result;
    }

    private boolean matchesQuery(EventUiModel event) {
        String needle = normalize(query);
        if (needle.length() < 2) {
            return true;
        }
        if (contains(event.title, needle)
                || contains(event.description, needle)
                || contains(event.organizer, needle)
                || contains(event.venue, needle)
                || contains(event.city, needle)) {
            return true;
        }
        for (String tag : event.tags) {
            if (contains(tag, needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCategory(EventUiModel event) {
        return criteria.categoryIds.isEmpty()
                || criteria.categoryIds.contains(event.categoryId());
    }

    private boolean matchesTime(EventUiModel event) {
        if (criteria.timePreset == EventFilterCriteria.TimePreset.ANY) {
            return true;
        }
        long from;
        long to;
        Calendar calendar = Calendar.getInstance();
        switch (criteria.timePreset) {
            case TODAY:
                from = startOfDay(calendar);
                to = from + 24L * 60L * 60L * 1000L - 1L;
                break;
            case THIS_WEEK:
                from = startOfWeek(calendar);
                to = from + 7L * 24L * 60L * 60L * 1000L - 1L;
                break;
            case THIS_MONTH:
                from = startOfMonth(calendar);
                Calendar monthEnd = Calendar.getInstance();
                monthEnd.setTimeInMillis(from);
                monthEnd.add(Calendar.MONTH, 1);
                to = monthEnd.getTimeInMillis() - 1L;
                break;
            case CUSTOM:
                from = criteria.dateFrom;
                to = criteria.dateTo;
                break;
            case ANY:
            default:
                return true;
        }
        if (from > 0L && event.startAt < from) {
            return false;
        }
        return to <= 0L || event.startAt <= to;
    }

    private boolean matchesCity(EventUiModel event) {
        return criteria.city.trim().isEmpty()
                || event.city.equalsIgnoreCase(criteria.city.trim());
    }

    private boolean matchesFormat(EventUiModel event) {
        switch (criteria.format) {
            case ONSITE:
                return !event.online;
            case ONLINE:
                return event.online;
            case ANY:
            default:
                return true;
        }
    }

    private boolean matchesPrice(EventUiModel event) {
        if (criteria.priceType == EventFilterCriteria.PriceType.FREE && !event.free) {
            return false;
        }
        if (criteria.priceType == EventFilterCriteria.PriceType.PAID && event.free) {
            return false;
        }
        if (criteria.priceRangeSet) {
            return event.priceFrom >= criteria.priceMin && event.priceFrom <= criteria.priceMax;
        }
        return true;
    }

    private boolean matchesAvailability(EventUiModel event) {
        if (!criteria.availableOnly) {
            return true;
        }
        return event.availability == EventUiModel.Availability.AVAILABLE
                || event.availability == EventUiModel.Availability.FEW_LEFT;
    }

    private void sort(List<EventUiModel> result) {
        switch (criteria.sort) {
            case NEWEST:
                Collections.sort(result, (left, right) -> Long.compare(right.id, left.id));
                break;
            case PRICE_ASC:
                Collections.sort(result, (left, right) -> Long.compare(left.priceFrom, right.priceFrom));
                break;
            case PRICE_DESC:
                Collections.sort(result, (left, right) -> Long.compare(right.priceFrom, left.priceFrom));
                break;
            case POPULAR:
                Collections.sort(result, (left, right) ->
                        Integer.compare(right.reviewCount, left.reviewCount));
                break;
            case START_TIME_ASC:
            default:
                Collections.sort(result, (left, right) ->
                        Long.compare(left.startAt, right.startAt));
                break;
        }
    }

    private static boolean contains(String value, String needle) {
        return value != null && normalize(value).contains(needle);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        decomposed = decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return decomposed.toLowerCase(Locale.ROOT).replace('đ', 'd').trim();
    }

    private static long startOfDay(Calendar calendar) {
        Calendar copy = Calendar.getInstance();
        copy.setTimeInMillis(calendar.getTimeInMillis());
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    private static long startOfWeek(Calendar calendar) {
        Calendar copy = Calendar.getInstance();
        copy.setTimeInMillis(calendar.getTimeInMillis());
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        copy.set(Calendar.DAY_OF_WEEK, copy.getFirstDayOfWeek());
        return copy.getTimeInMillis();
    }

    private static long startOfMonth(Calendar calendar) {
        Calendar copy = Calendar.getInstance();
        copy.setTimeInMillis(calendar.getTimeInMillis());
        copy.set(Calendar.DAY_OF_MONTH, 1);
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
