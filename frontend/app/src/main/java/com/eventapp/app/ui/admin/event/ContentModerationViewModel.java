package com.eventapp.app.ui.admin.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.ModerationUiModel;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FR-A07 + FR-A14 — cờ kiểm duyệt và hàng chờ đánh giá, dữ liệu từ API thật.
 *
 * <p>Cờ được backend suy ra từ đánh giá PENDING và báo cáo trong audit log; mọi thao tác duyệt/ẩn
 * đều ghi audit phía server. Sau thao tác màn hình tải lại để phản ánh trạng thái chính thức.</p>
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
        public final String message;

        ActionEvent(Action action, long itemId, boolean undoable) {
            this(action, itemId, undoable, null);
        }

        ActionEvent(Action action, long itemId, boolean undoable, @Nullable String message) {
            this.action = action;
            this.itemId = itemId;
            this.undoable = undoable;
            this.message = message;
        }
    }

    private static final int PAGE_SIZE = 50;
    private static final String DEFAULT_REASON = "OFFENSIVE";

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

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

    private final List<ModerationUiModel> allFlags = new ArrayList<>();
    private final List<ReviewUiModel> allReviews = new ArrayList<>();
    private final Map<Long, String> flagIds = new HashMap<>();

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
        loadFlags();
        loadReviews();
    }

    public void refresh() {
        load();
    }

    public void retry() {
        load();
    }

    private void loadFlags() {
        repository.flags("ALL", 0, PAGE_SIZE,
                new ApiCallback<Paged<ReviewDtos.ModerationFlag>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.ModerationFlag> data,
                                          @Nullable JsonElement meta) {
                        allFlags.clear();
                        flagIds.clear();
                        if (data != null) {
                            for (ReviewDtos.ModerationFlag dto : data.items) {
                                ModerationUiModel mapped = mapFlag(dto);
                                if (mapped != null) {
                                    allFlags.add(mapped);
                                    if (dto.id != null) {
                                        flagIds.put(mapped.id, dto.id);
                                    }
                                }
                            }
                        }
                        publishAll();
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(true);
                    }
                });
    }

    private void loadReviews() {
        repository.adminReviews(null, null, null, 0, PAGE_SIZE,
                new ApiCallback<Paged<ReviewDtos.AdminReview>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.AdminReview> data,
                                          @Nullable JsonElement meta) {
                        allReviews.clear();
                        if (data != null) {
                            for (ReviewDtos.AdminReview dto : data.items) {
                                allReviews.add(ReviewMapper.fromAdmin(dto));
                            }
                        }
                        publishReviews();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        // Tab cờ vẫn dùng được khi hàng chờ đánh giá lỗi.
                    }
                });
    }

    @Nullable
    private ModerationUiModel mapFlag(@Nullable ReviewDtos.ModerationFlag dto) {
        if (dto == null) {
            return null;
        }
        return new ModerationUiModel.Builder()
                .id(dto.targetId)
                .targetId(dto.targetId)
                .targetType(ModerationUiModel.TargetType.REVIEW)
                .targetName(dto.eventTitle)
                .severity(severity(dto.severity))
                .status(ModerationUiModel.Status.OPEN)
                .source("REPORTED".equalsIgnoreCase(dto.reasonCode)
                        ? ModerationUiModel.Source.AI : ModerationUiModel.Source.RULE)
                .confidence(0)
                .reason(dto.reasonLabel == null ? dto.reasonCode : dto.reasonLabel)
                .evidence(dto.evidence)
                .createdAt(DateTimeUtils.toMillis(dto.createdAt))
                .build();
    }

    public void confirmFlag(long id) {
        ModerationUiModel flag = findFlag(id);
        if (flag == null || !flag.isOpen()) {
            return;
        }
        repository.confirmFlag(flagIdOf(id),
                new ReviewDtos.ModerationAction(DEFAULT_REASON),
                new ApiCallback<ReviewDtos.AdminReview>() {
                    @Override
                    public void onSuccess(@Nullable ReviewDtos.AdminReview data,
                                          @Nullable JsonElement meta) {
                        flag.status = ModerationUiModel.Status.CONFIRMED;
                        flag.reviewedAt = System.currentTimeMillis();
                        handledTodayValue++;
                        publishAll();
                        actionEvent.setValue(new ActionEvent(Action.CONFIRM, id, true));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        actionEvent.setValue(new ActionEvent(Action.CONFIRM, id, false,
                                exception.getUserMessage()));
                    }
                });
    }

    public void dismissFlag(long id, String note) {
        ModerationUiModel flag = findFlag(id);
        if (flag == null || !flag.isOpen()) {
            return;
        }
        repository.dismissFlag(flagIdOf(id),
                new ReviewDtos.ModerationAction("OTHER", note),
                new ApiCallback<ReviewDtos.AdminReview>() {
                    @Override
                    public void onSuccess(@Nullable ReviewDtos.AdminReview data,
                                          @Nullable JsonElement meta) {
                        flag.status = ModerationUiModel.Status.DISMISSED;
                        flag.reviewedAt = System.currentTimeMillis();
                        flag.reviewNote = note == null ? "" : note;
                        handledTodayValue++;
                        publishAll();
                        actionEvent.setValue(new ActionEvent(Action.DISMISS, id, true));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        actionEvent.setValue(new ActionEvent(Action.DISMISS, id, false,
                                exception.getUserMessage()));
                    }
                });
    }

    /** Quét lại nội dung 30 ngày gần nhất theo bộ lọc hiện tại của server. */
    public void rescanFlag(long id) {
        repository.rescan(30, new ApiCallback<ReviewDtos.RescanResult>() {
            @Override
            public void onSuccess(@Nullable ReviewDtos.RescanResult data,
                                  @Nullable JsonElement meta) {
                actionEvent.setValue(new ActionEvent(Action.RESCAN, id, false,
                        data == null ? null : "Đã quét " + data.scanned + " đánh giá, phát hiện "
                                + data.flagged + " nội dung."));
                loadFlags();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                actionEvent.setValue(new ActionEvent(Action.RESCAN, id, false,
                        exception.getUserMessage()));
            }
        });
    }

    public void hideReview(long id, String reasonCode, String note) {
        applyReviewAction(id, reasonCode, note, Action.HIDE, true);
    }

    public void keepReview(long id, String note) {
        repository.approve(id, new ReviewDtos.ModerationAction(reasonOr(note, "OTHER"), note),
                new ApiCallback<ReviewDtos.AdminReview>() {
                    @Override
                    public void onSuccess(@Nullable ReviewDtos.AdminReview data,
                                          @Nullable JsonElement meta) {
                        handledTodayValue++;
                        removeReviewLocally(id);
                        actionEvent.setValue(new ActionEvent(Action.KEEP, id, false));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        actionEvent.setValue(new ActionEvent(Action.KEEP, id, false,
                                exception.getUserMessage()));
                    }
                });
    }

    public void requestEditReview(long id, String reasonCode, String note) {
        applyReviewAction(id, reasonCode, note, Action.REQUEST_EDIT, true);
    }

    public void undoLast() {
        // Backend không có API hoàn tác; tải lại để hiển thị trạng thái chính thức.
        actionEvent.setValue(new ActionEvent(Action.UNDO, 0L, false));
        load();
    }

    public void consumeActionEvent() {
        actionEvent.setValue(null);
    }

    @Nullable
    public ModerationUiModel findFlag(long id) {
        for (ModerationUiModel flag : allFlags) {
            if (flag.id == id) {
                return flag;
            }
        }
        return null;
    }

    @Nullable
    public ReviewUiModel findReview(long id) {
        for (ReviewUiModel review : allReviews) {
            if (review.id == id) {
                return review;
            }
        }
        return null;
    }

    private void applyReviewAction(long id, @Nullable String reasonCode, @Nullable String note,
                                   @NonNull Action action, boolean hide) {
        ReviewDtos.ModerationAction request =
                new ReviewDtos.ModerationAction(reasonOr(note, reasonCode), note);
        ApiCallback<ReviewDtos.AdminReview> callback = new ApiCallback<ReviewDtos.AdminReview>() {
            @Override
            public void onSuccess(@Nullable ReviewDtos.AdminReview data,
                                  @Nullable JsonElement meta) {
                handledTodayValue++;
                removeReviewLocally(id);
                actionEvent.setValue(new ActionEvent(action, id, false));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                actionEvent.setValue(new ActionEvent(action, id, false,
                        exception.getUserMessage()));
            }
        };
        if (hide) {
            repository.hide(id, request, callback);
        } else {
            repository.restore(id, request, callback);
        }
    }

    @NonNull
    private static String reasonOr(@Nullable String note, @Nullable String reasonCode) {
        if (reasonCode != null && !reasonCode.trim().isEmpty()) {
            return reasonCode.trim().toUpperCase(Locale.ROOT);
        }
        return DEFAULT_REASON;
    }

    @NonNull
    private String flagIdOf(long id) {
        String raw = flagIds.get(id);
        return raw == null ? "review:" + id : raw;
    }

    private void removeReviewLocally(long id) {
        for (int i = 0; i < allReviews.size(); i++) {
            if (allReviews.get(i).id == id) {
                allReviews.remove(i);
                break;
            }
        }
        publishAll();
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

    @NonNull
    private static ModerationUiModel.Severity severity(@Nullable String raw) {
        if (raw == null) {
            return ModerationUiModel.Severity.LOW;
        }
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "HIGH":
                return ModerationUiModel.Severity.HIGH;
            case "MEDIUM":
                return ModerationUiModel.Severity.MEDIUM;
            case "LOW":
            default:
                return ModerationUiModel.Severity.LOW;
        }
    }
}
