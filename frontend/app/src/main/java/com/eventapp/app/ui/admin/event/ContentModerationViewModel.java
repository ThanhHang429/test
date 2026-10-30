package com.eventapp.app.ui.admin.event;


import com.eventapp.app.ui.fake.SampleModerationData;
import com.eventapp.app.ui.model.ModerationUiModel;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.model.ReviewUiModel;
import com.eventapp.app.ui.fake.SampleReviewData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * FR-A07 + FR-A14 state holder. Both lanes share one screen: flags keep their status locally,
 * review rows leave the queue after an action and can be restored from the Snackbar undo.
 */
public class ContentModerationViewModel extends ViewModel {

    public enum StatusFilter {
        OPEN,
        CONFIRMED,
        DISMISSED,
        HANDLED,
        ALL
    }

    public enum Action {
        CONFIRM,
        DISMISS,
        RESCAN,
        HIDE,
        KEEP,
        REQUEST_EDIT,
        UNDO
    }

    public static class ActionEvent {
        public final Action action;
        public final long itemId;
        public final boolean undoable;

        ActionEvent(Action action, long itemId, boolean undoable) {
            this.action = action;
            this.itemId = itemId;
            this.undoable = undoable;
        }
    }

    private static final long LOAD_DELAY_MS = 320L;
    private static final int MAX_UNDO = 20;

    private final MutableLiveData<List<ModerationUiModel>> flags =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ReviewUiModel>> reviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> selectedTab = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> openCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> highCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> handledToday = new MutableLiveData<>(0);
    private final MutableLiveData<ActionEvent> actionEvent = new MutableLiveData<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<ModerationUiModel> allFlags = new ArrayList<>();
    private final List<ReviewUiModel> allReviews = new ArrayList<>();
    private final List<UndoEntry> undoStack = new ArrayList<>();

    private ModerationUiModel.TargetType targetFilter;
    private ModerationUiModel.Severity severityFilter;
    private StatusFilter statusFilter = StatusFilter.OPEN;
    private ReviewUiModel.Source reviewSourceFilter;
    private int handledTodayValue;

    public ContentModerationViewModel() {
        load();
    }

    public LiveData<List<ModerationUiModel>> getFlags() {
        return flags;
    }

    public LiveData<List<ReviewUiModel>> getReviews() {
        return reviews;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Integer> getSelectedTab() {
        return selectedTab;
    }

    public LiveData<Integer> getOpenCount() {
        return openCount;
    }

    public LiveData<Integer> getHighCount() {
        return highCount;
    }

    public LiveData<Integer> getHandledToday() {
        return handledToday;
    }

    public LiveData<ActionEvent> getActionEvent() {
        return actionEvent;
    }

    public ModerationUiModel.TargetType getTargetFilter() {
        return targetFilter;
    }

    public ModerationUiModel.Severity getSeverityFilter() {
        return severityFilter;
    }

    public StatusFilter getStatusFilter() {
        return statusFilter;
    }

    public ReviewUiModel.Source getReviewSourceFilter() {
        return reviewSourceFilter;
    }

    public void selectTab(int tab) {
        selectedTab.setValue(tab == 1 ? 1 : 0);
    }

    public void setTargetFilter(ModerationUiModel.TargetType filter) {
        targetFilter = filter;
        publishFlags();
    }

    public void setSeverityFilter(ModerationUiModel.Severity filter) {
        severityFilter = filter;
        publishFlags();
    }

    public void setStatusFilter(StatusFilter filter) {
        statusFilter = filter == null ? StatusFilter.OPEN : filter;
        publishFlags();
    }

    public void setReviewSourceFilter(ReviewUiModel.Source filter) {
        reviewSourceFilter = filter;
        publishReviews();
    }

    public void showHandled() {
        statusFilter = StatusFilter.HANDLED;
        publishFlags();
    }

    public void clearReviewSourceFilter() {
        reviewSourceFilter = null;
        publishReviews();
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::bindSampleData, LOAD_DELAY_MS);
    }

    public void refresh() {
        load();
    }

    public void retry() {
        load();
    }

    public void confirmFlag(long id) {
        ModerationUiModel flag = findFlag(id);
        if (flag == null || !flag.isOpen()) {
            return;
        }
        pushFlagUndo(flag);
        flag.status = ModerationUiModel.Status.CONFIRMED;
        flag.reviewedAt = System.currentTimeMillis();
        handledTodayValue++;
        publishAll();
        actionEvent.setValue(new ActionEvent(Action.CONFIRM, id, true));
    }

    public void dismissFlag(long id, String note) {
        ModerationUiModel flag = findFlag(id);
        if (flag == null || !flag.isOpen()) {
            return;
        }
        pushFlagUndo(flag);
        flag.status = ModerationUiModel.Status.DISMISSED;
        flag.reviewedAt = System.currentTimeMillis();
        flag.reviewNote = note == null ? "" : note;
        handledTodayValue++;
        publishAll();
        actionEvent.setValue(new ActionEvent(Action.DISMISS, id, true));
    }

    public void rescanFlag(long id) {
        ModerationUiModel flag = findFlag(id);
        if (flag == null) {
            return;
        }
        flag.unchecked = false;
        flag.contentChanged = false;
        if (flag.confidence < 80) {
            flag.confidence = 80;
        }
        publishFlags();
        actionEvent.setValue(new ActionEvent(Action.RESCAN, id, false));
    }

    public void hideReview(long id, String reasonCode, String note) {
        removeReview(id, Action.HIDE);
    }

    public void keepReview(long id, String note) {
        removeReview(id, Action.KEEP);
    }

    public void requestEditReview(long id, String reasonCode, String note) {
        removeReview(id, Action.REQUEST_EDIT);
    }

    public void undoLast() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoEntry entry = undoStack.remove(undoStack.size() - 1);
        if (entry.flag != null) {
            entry.flag.status = entry.previousFlagStatus;
            entry.flag.reviewedAt = entry.previousReviewedAt;
            entry.flag.reviewNote = entry.previousNote;
        } else if (entry.review != null) {
            int index = Math.min(entry.reviewIndex, allReviews.size());
            allReviews.add(index, entry.review);
        }
        if (entry.countedHandled && handledTodayValue > 0) {
            handledTodayValue--;
        }
        publishAll();
        actionEvent.setValue(new ActionEvent(Action.UNDO, entry.itemId, false));
    }

    public void consumeActionEvent() {
        actionEvent.setValue(null);
    }

    public ModerationUiModel findFlag(long id) {
        for (ModerationUiModel flag : allFlags) {
            if (flag.id == id) {
                return flag;
            }
        }
        return null;
    }

    public ReviewUiModel findReview(long id) {
        for (ReviewUiModel review : allReviews) {
            if (review.id == id) {
                return review;
            }
        }
        return null;
    }

    private void bindSampleData() {
        allFlags.clear();
        allFlags.addAll(SampleModerationData.createFlags());
        allReviews.clear();
        allReviews.addAll(SampleReviewData.moderationQueue());
        undoStack.clear();
        handledTodayValue = SampleModerationData.handledTodayBase() + countHandledToday();
        publishAll();
        loading.setValue(false);
    }

    private void removeReview(long id, Action action) {
        int index = indexOfReview(id);
        if (index < 0) {
            return;
        }
        ReviewUiModel review = allReviews.remove(index);
        pushReviewUndo(review, index);
        handledTodayValue++;
        publishAll();
        actionEvent.setValue(new ActionEvent(action, id, true));
    }

    private void publishAll() {
        publishFlags();
        publishReviews();
        publishCounts();
    }

    private void publishFlags() {
        List<ModerationUiModel> visible = new ArrayList<>();
        for (ModerationUiModel flag : allFlags) {
            if (targetFilter != null && flag.targetType != targetFilter) {
                continue;
            }
            if (severityFilter != null && flag.severity != severityFilter) {
                continue;
            }
            if (!matchesStatus(flag.status)) {
                continue;
            }
            visible.add(flag);
        }
        flags.setValue(visible);
    }

    private boolean matchesStatus(ModerationUiModel.Status status) {
        switch (statusFilter) {
            case CONFIRMED:
                return status == ModerationUiModel.Status.CONFIRMED;
            case DISMISSED:
                return status == ModerationUiModel.Status.DISMISSED;
            case HANDLED:
                return status != ModerationUiModel.Status.OPEN;
            case ALL:
                return true;
            case OPEN:
            default:
                return status == ModerationUiModel.Status.OPEN;
        }
    }

    private void publishReviews() {
        List<ReviewUiModel> visible = new ArrayList<>();
        for (ReviewUiModel review : allReviews) {
            if (reviewSourceFilter == null || review.source == reviewSourceFilter) {
                visible.add(review);
            }
        }
        reviews.setValue(visible);
    }

    private void publishCounts() {
        int open = 0;
        int high = 0;
        for (ModerationUiModel flag : allFlags) {
            if (!flag.isOpen()) {
                continue;
            }
            open++;
            if (flag.severity == ModerationUiModel.Severity.HIGH) {
                high++;
            }
        }
        openCount.setValue(open);
        highCount.setValue(high);
        handledToday.setValue(handledTodayValue);
    }

    private int countHandledToday() {
        int count = 0;
        for (ModerationUiModel flag : allFlags) {
            if (flag.status != ModerationUiModel.Status.OPEN && isToday(flag.reviewedAt)) {
                count++;
            }
        }
        return count;
    }

    private boolean isToday(long timestamp) {
        if (timestamp <= 0L) {
            return false;
        }
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(timestamp);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
    }

    private void pushFlagUndo(ModerationUiModel flag) {
        UndoEntry entry = new UndoEntry(flag);
        entry.previousFlagStatus = flag.status;
        entry.previousReviewedAt = flag.reviewedAt;
        entry.previousNote = flag.reviewNote;
        entry.countedHandled = true;
        addUndo(entry);
    }

    private void pushReviewUndo(ReviewUiModel review, int index) {
        UndoEntry entry = new UndoEntry(review);
        entry.reviewIndex = index;
        entry.countedHandled = true;
        addUndo(entry);
    }

    private void addUndo(UndoEntry entry) {
        undoStack.add(entry);
        while (undoStack.size() > MAX_UNDO) {
            undoStack.remove(0);
        }
    }

    private int indexOfReview(long id) {
        for (int i = 0; i < allReviews.size(); i++) {
            if (allReviews.get(i).id == id) {
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

    private static class UndoEntry {
        final long itemId;
        final ModerationUiModel flag;
        final ReviewUiModel review;
        ModerationUiModel.Status previousFlagStatus;
        long previousReviewedAt;
        String previousNote = "";
        int reviewIndex;
        boolean countedHandled;

        UndoEntry(ModerationUiModel flag) {
            this.flag = flag;
            this.review = null;
            this.itemId = flag.id;
        }

        UndoEntry(ReviewUiModel review) {
            this.flag = null;
            this.review = review;
            this.itemId = review.id;
        }
    }
}
