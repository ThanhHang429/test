package com.eventapp.app.ui.user.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.google.gson.JsonElement;

import java.util.UUID;

/**
 * FR-U20 — gửi/sửa đánh giá qua {@code POST /events/{id}/reviews} và
 * {@code PUT /events/{id}/reviews/me}.
 *
 * <p>Nội dung có từ khoá cấm/link/SĐT sẽ được backend giữ ở trạng thái chờ duyệt; client chỉ phản
 * ánh lại bằng cờ {@code held} trong kết quả.</p>
 */
public class WriteReviewViewModel extends ViewModel {

    public static class Result {
        public final boolean success;
        public final boolean held;

        Result(boolean success, boolean held) {
            this.success = success;
            this.held = held;
        }
    }

    private static final int MAX_TEXT = 2000;

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<Boolean> submitting = new MutableLiveData<>(false);
    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String idempotencyKey;

    public LiveData<Boolean> getSubmitting() {
        return submitting;
    }

    public LiveData<Result> getResult() {
        return result;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void submit(long eventId, float rating, String comment, boolean editMode,
                       @Nullable Long version) {
        if (Boolean.TRUE.equals(submitting.getValue())) {
            return;
        }
        error.setValue(null);
        result.setValue(null);
        int stars = Math.round(rating);
        if (stars < 1 || stars > 5) {
            error.setValue("Vui lòng chọn số sao");
            return;
        }
        String normalized = comment == null ? "" : comment.trim();
        if (normalized.length() > MAX_TEXT) {
            error.setValue("Nhận xét tối đa " + MAX_TEXT + " ký tự");
            return;
        }
        if (eventId <= 0L) {
            error.setValue("Không xác định được sự kiện");
            return;
        }

        submitting.setValue(true);
        ApiCallback<ReviewDtos.Mutation> callback = new ApiCallback<ReviewDtos.Mutation>() {
            @Override
            public void onSuccess(@Nullable ReviewDtos.Mutation data,
                                  @Nullable JsonElement meta) {
                boolean held = data == null || data.review == null
                        || !"VISIBLE".equalsIgnoreCase(data.review.status);
                result.setValue(new Result(true, held));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception.getUserMessage());
            }

            @Override
            public void onFinish() {
                submitting.setValue(false);
            }
        };

        if (editMode) {
            ReviewDtos.UpdateRequest request = new ReviewDtos.UpdateRequest();
            request.rating = stars;
            request.text = normalized;
            request.version = version;
            repository.update(eventId, request, callback);
            return;
        }

        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        ReviewDtos.CreateRequest request = new ReviewDtos.CreateRequest();
        request.rating = stars;
        request.text = normalized;
        repository.create(eventId, idempotencyKey, request, callback);
    }

    public void resetResult() {
        result.setValue(null);
        error.setValue(null);
        idempotencyKey = null;
    }
}
