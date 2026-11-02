package com.eventapp.app.ui.organizer.classification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.remote.dto.classification.ClassificationDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.OrganizerRepository;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-O09 — xem và cập nhật danh mục chính/phụ, nhãn phụ của một sự kiện.
 *
 * <p>Server là nguồn sự thật: sau khi lưu, trạng thái được thay bằng phản hồi của API. Đổi danh mục
 * chính của sự kiện đã duyệt sẽ bật cờ {@code requiresReReview}.</p>
 */
public class ClassificationViewModel extends ViewModel {

    public static final int MAX_SECONDARY = 2;
    public static final int MAX_TAGS = 5;

    public static class State {
        public long eventId;
        public long primaryId;
        public final List<Long> secondaryIds = new ArrayList<>();
        public final List<String> tags = new ArrayList<>();
        public boolean requiresReReview;
        public boolean locked;
    }

    private final OrganizerRepository organizer = ServiceLocator.get(OrganizerRepository.class);
    private final CategoryRepository categories = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<List<CatalogDtos.CategoryNode>> categoryTree =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ClassificationDtos.TagSuggestion>> suggestions =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private long eventId = -1L;

    public LiveData<State> getState() {
        return state;
    }

    public LiveData<List<CatalogDtos.CategoryNode>> getCategories() {
        return categoryTree;
    }

    public LiveData<List<ClassificationDtos.TagSuggestion>> getSuggestions() {
        return suggestions;
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

    public LiveData<String> getMessage() {
        return message;
    }

    public void load(long eventId) {
        this.eventId = eventId;
        if (eventId <= 0L) {
            loading.setValue(false);
            error.setValue(true);
            return;
        }
        loading.setValue(true);
        error.setValue(false);
        loadCategories();
        loadSuggestions();
        organizer.classification(eventId,
                new ApiCallback<ClassificationDtos.Classification>() {
                    @Override
                    public void onSuccess(@Nullable ClassificationDtos.Classification data,
                                          @Nullable JsonElement meta) {
                        loading.setValue(false);
                        if (data == null) {
                            error.setValue(true);
                            return;
                        }
                        state.setValue(map(data));
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(true);
                    }
                });
    }

    public void retry() {
        load(eventId);
    }

    private void loadCategories() {
        categories.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                  @Nullable JsonElement meta) {
                categoryTree.setValue(data == null ? new ArrayList<>() : data);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                // Không chặn màn hình: chỉ thiếu danh sách để chọn.
            }
        });
    }

    private void loadSuggestions() {
        organizer.suggestTags(null, 10,
                new ApiCallback<List<ClassificationDtos.TagSuggestion>>() {
                    @Override
                    public void onSuccess(
                            @Nullable List<ClassificationDtos.TagSuggestion> data,
                            @Nullable JsonElement meta) {
                        suggestions.setValue(data == null ? new ArrayList<>() : data);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        suggestions.setValue(new ArrayList<>());
                    }
                });
    }

    public void selectPrimary(long categoryId) {
        State current = state.getValue();
        if (current == null || current.locked) {
            return;
        }
        current.primaryId = categoryId;
        current.secondaryIds.remove(categoryId);
        state.setValue(current);
    }

    /** @return thông điệp lỗi khi vượt giới hạn 2 danh mục phụ, ngược lại null. */
    @Nullable
    public String toggleSecondary(long categoryId) {
        State current = state.getValue();
        if (current == null || current.locked) {
            return null;
        }
        if (categoryId == current.primaryId) {
            return null;
        }
        if (current.secondaryIds.contains(categoryId)) {
            current.secondaryIds.remove(categoryId);
        } else {
            if (current.secondaryIds.size() >= MAX_SECONDARY) {
                return "MAX_SECONDARY";
            }
            current.secondaryIds.add(categoryId);
        }
        state.setValue(current);
        return null;
    }

    /** @return thông điệp lỗi khi nhãn không hợp lệ/vượt giới hạn, ngược lại null. */
    @Nullable
    public String addTag(@Nullable String raw) {
        State current = state.getValue();
        if (current == null || current.locked) {
            return null;
        }
        String tag = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (tag.length() < 2 || tag.length() > 30) {
            return "INVALID";
        }
        for (String existing : current.tags) {
            if (existing.equalsIgnoreCase(tag)) {
                return null;
            }
        }
        if (current.tags.size() >= MAX_TAGS) {
            return "MAX_TAGS";
        }
        current.tags.add(tag);
        state.setValue(current);
        return null;
    }

    public void removeTag(@Nullable String tag) {
        State current = state.getValue();
        if (current == null || current.locked || tag == null) {
            return;
        }
        current.tags.remove(tag);
        state.setValue(current);
    }

    public void save() {
        State current = state.getValue();
        if (current == null || current.locked || Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        if (current.primaryId <= 0L) {
            message.setValue("Vui lòng chọn danh mục chính.");
            return;
        }
        saving.setValue(true);
        ClassificationDtos.UpdateRequest request = new ClassificationDtos.UpdateRequest();
        request.primaryCategoryId = current.primaryId;
        request.secondaryCategoryIds = new ArrayList<>(current.secondaryIds);
        request.tags = new ArrayList<>(current.tags);
        organizer.updateClassification(eventId, request,
                new ApiCallback<ClassificationDtos.Classification>() {
                    @Override
                    public void onSuccess(@Nullable ClassificationDtos.Classification data,
                                          @Nullable JsonElement meta) {
                        saving.setValue(false);
                        if (data != null) {
                            state.setValue(map(data));
                        }
                        message.setValue("SAVED");
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        saving.setValue(false);
                        if (exception.getHttpCode() == 409) {
                            State locked = state.getValue();
                            if (locked != null) {
                                locked.locked = true;
                                state.setValue(locked);
                            }
                        }
                        message.setValue(exception.getUserMessage());
                    }
                });
    }

    public void consumeMessage() {
        message.setValue(null);
    }

    @NonNull
    private State map(@NonNull ClassificationDtos.Classification dto) {
        State value = new State();
        value.eventId = dto.eventId;
        value.primaryId = dto.primaryCategory == null ? 0L : dto.primaryCategory.id;
        if (dto.secondaryCategories != null) {
            for (ClassificationDtos.CategoryBrief category : dto.secondaryCategories) {
                if (category != null) {
                    value.secondaryIds.add(category.id);
                }
            }
        }
        if (dto.tags != null) {
            value.tags.addAll(dto.tags);
        }
        value.requiresReReview = dto.requiresReReview;
        return value;
    }
}
