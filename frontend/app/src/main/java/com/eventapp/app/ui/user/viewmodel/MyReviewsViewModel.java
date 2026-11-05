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
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.ui.mapper.ReviewMapper;
import com.eventapp.app.ui.model.ReviewUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Danh sách đánh giá của tôi ở mọi trạng thái duyệt (FR-U20). */
public class MyReviewsViewModel extends ViewModel {

    private static final int PAGE_SIZE = 50;

    private final ReviewRepository repository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<List<ReviewUiModel>> reviews =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    public LiveData<List<ReviewUiModel>> getReviews() {
        return reviews;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public void load() {
        loading.setValue(true);
        repository.mine(0, PAGE_SIZE, new ApiCallback<Paged<ReviewDtos.MyReview>>() {
            @Override
            public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                  @Nullable JsonElement meta) {
                List<ReviewUiModel> result = new ArrayList<>();
                if (data != null) {
                    for (ReviewDtos.MyReview dto : data.items) {
                        result.add(ReviewMapper.fromMine(dto));
                    }
                }
                reviews.setValue(result);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }

            @Override
            public void onFinish() {
                loading.setValue(false);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }
}
