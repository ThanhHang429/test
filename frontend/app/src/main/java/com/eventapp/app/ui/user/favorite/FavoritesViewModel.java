package com.eventapp.app.ui.user.favorite;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.user.home.EventUiModel;
import com.eventapp.app.ui.user.home.SampleEventData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FR-U06 state holder. Favourites are read-only when offline and removal is the only local
 * mutation; availability labels always come from the event snapshot.
 */
public class FavoritesViewModel extends ViewModel {

    public enum Filter {
        ALL,
        UPCOMING,
        PAST
    }

    private static final int MAX_SUGGESTIONS = 8;

    private final MutableLiveData<List<FavoriteUiModel>> items =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<EventUiModel>> suggestions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> alertsEnabled = new MutableLiveData<>(true);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<FavoriteUiModel> saved = new ArrayList<>();
    private List<EventUiModel> catalogue = new ArrayList<>();
    private Filter filter = Filter.ALL;

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

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter == null ? Filter.ALL : filter;
        publish();
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::bindSampleData, 320L);
    }

    public void refresh() {
        load();
    }

    public void setAlertsEnabled(boolean enabled) {
        alertsEnabled.setValue(enabled);
    }

    public void remove(FavoriteUiModel item) {
        int index = indexOf(item);
        if (index < 0) {
            return;
        }
        saved.remove(index);
        publish();
    }

    public void restore(FavoriteUiModel item) {
        if (item == null) {
            return;
        }
        if (item.event != null && findSaved(item.event.id) != null) {
            return;
        }
        saved.add(0, item);
        publish();
    }

    public void addToFavorites(EventUiModel event) {
        if (event == null || findSaved(event.id) != null) {
            return;
        }
        event.favorite = true;
        saved.add(0, FavoriteUiModel.from(event, System.currentTimeMillis()));
        publish();
    }

    public void removeEvent(long eventId) {
        FavoriteUiModel item = findSaved(eventId);
        if (item != null) {
            remove(item);
        }
    }

    private void bindSampleData() {
        catalogue = SampleEventData.createEvents();
        saved.clear();
        saved.addAll(SampleFavoriteData.create(catalogue));
        loading.setValue(false);
        publish();
    }

    private void publish() {
        long now = System.currentTimeMillis();
        List<FavoriteUiModel> visible = new ArrayList<>();
        for (FavoriteUiModel item : saved) {
            if (matchesFilter(item, now)) {
                visible.add(item);
            }
        }
        items.setValue(visible);
        suggestions.setValue(buildSuggestions());
    }

    private boolean matchesFilter(FavoriteUiModel item, long now) {
        if (filter == Filter.ALL) {
            return true;
        }
        boolean ended = item.state == FavoriteUiModel.State.ENDED
                || (item.event != null && item.event.endAt > 0L && item.event.endAt < now);
        if (filter == Filter.PAST) {
            return ended;
        }
        return !ended && item.state != FavoriteUiModel.State.UNAVAILABLE;
    }

    private List<EventUiModel> buildSuggestions() {
        List<EventUiModel> result = new ArrayList<>();
        for (EventUiModel event : catalogue) {
            if (event.availability == EventUiModel.Availability.CANCELLED || findSaved(event.id) != null) {
                continue;
            }
            result.add(event);
        }
        Collections.sort(result, (left, right) -> Integer.compare(
                right.aiScore == null ? -1 : right.aiScore,
                left.aiScore == null ? -1 : left.aiScore));
        if (result.size() > MAX_SUGGESTIONS) {
            return new ArrayList<>(result.subList(0, MAX_SUGGESTIONS));
        }
        return result;
    }

    private FavoriteUiModel findSaved(long eventId) {
        for (FavoriteUiModel item : saved) {
            if (item.event != null && item.event.id == eventId) {
                return item;
            }
        }
        return null;
    }

    private int indexOf(FavoriteUiModel item) {
        for (int i = 0; i < saved.size(); i++) {
            FavoriteUiModel current = saved.get(i);
            if (current == item) {
                return i;
            }
            if (current.event == null && item.event == null
                    && current.savedAt == item.savedAt) {
                return i;
            }
            if (current.event != null && item.event != null
                    && current.event.id == item.event.id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
