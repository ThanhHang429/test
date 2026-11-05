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
import com.eventapp.app.data.remote.dto.favorite.FavoriteDtos;
import com.eventapp.app.data.remote.dto.preference.PreferenceDtos;
import com.eventapp.app.data.remote.dto.review.ReviewDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.data.repository.FavoriteRepository;
import com.eventapp.app.data.repository.ReviewRepository;
import com.eventapp.app.data.repository.UserRepository;
import com.eventapp.app.ui.mapper.CategoryMapper;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.eventapp.app.ui.user.data.DemoContent;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Trạng thái tab Tài khoản: sở thích, thống kê và lưu tuỳ chọn cá nhân hoá. */
public class AccountViewModel extends ViewModel {

    private static final int MAX_CATEGORIES = 10;

    private final CategoryRepository categoryRepository =
            ServiceLocator.get(CategoryRepository.class);
    private final UserRepository userRepository = ServiceLocator.get(UserRepository.class);
    private final FavoriteRepository favoriteRepository =
            ServiceLocator.get(FavoriteRepository.class);
    private final ReviewRepository reviewRepository = ServiceLocator.get(ReviewRepository.class);

    private final MutableLiveData<List<CategoryNodeUiModel>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Long>> selectedCategories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> savedCount = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> reviewCount = new MutableLiveData<>(0L);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();

    private PreferenceDtos.Preference preference;

    public LiveData<List<CategoryNodeUiModel>> getCategories() {
        return categories;
    }

    public LiveData<List<Long>> getSelectedCategories() {
        return selectedCategories;
    }

    public LiveData<Long> getSavedCount() {
        return savedCount;
    }

    public LiveData<Long> getReviewCount() {
        return reviewCount;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public int purchasedCount() {
        return DemoContent.get().purchasedCount();
    }

    public void load() {
        loadCategories();
        loadPreference();
        refreshStats();
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
                // Không có danh mục thì giữ khung sở thích trống.
            }
        });
    }

    public void loadPreference() {
        userRepository.preferences(new ApiCallback<PreferenceDtos.Preference>() {
            @Override
            public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                  @Nullable JsonElement meta) {
                if (data == null) {
                    return;
                }
                preference = data;
                selectedCategories.setValue(data.categoryIds == null
                        ? new ArrayList<>() : new ArrayList<>(data.categoryIds));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    /**
     * Bật/tắt một danh mục sở thích và lưu ngay.
     *
     * @return {@code false} khi đã chọn đủ {@value #MAX_CATEGORIES} danh mục.
     */
    public boolean toggleCategory(long categoryId) {
        List<Long> current = selectedCategories.getValue() == null
                ? new ArrayList<>() : new ArrayList<>(selectedCategories.getValue());
        boolean nowSelected = !current.contains(categoryId);
        if (nowSelected) {
            if (current.size() >= MAX_CATEGORIES) {
                return false;
            }
            current.add(categoryId);
        } else {
            current.remove(categoryId);
        }
        selectedCategories.setValue(current);
        savePreferences(current);
        return true;
    }

    public void refreshStats() {
        savedCount.setValue(0L);
        reviewCount.setValue(0L);

        favoriteRepository.list("ALL", 0, 1,
                new ApiCallback<Paged<FavoriteDtos.Item>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<FavoriteDtos.Item> data,
                                          @Nullable JsonElement meta) {
                        savedCount.setValue(data == null ? 0L : data.totalItems());
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                    }
                });

        reviewRepository.mine(0, 1,
                new ApiCallback<Paged<ReviewDtos.MyReview>>() {
                    @Override
                    public void onSuccess(@Nullable Paged<ReviewDtos.MyReview> data,
                                          @Nullable JsonElement meta) {
                        reviewCount.setValue(data == null ? 0L : data.totalItems());
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                    }
                });
    }

    public void consumeError() {
        error.setValue(null);
    }

    private void savePreferences(List<Long> categoryIds) {
        PreferenceDtos.UpdateRequest request = new PreferenceDtos.UpdateRequest();
        request.categoryIds = new ArrayList<>(categoryIds);
        request.version = preference == null ? null : preference.version;
        if (preference != null) {
            request.eventFormat = preference.eventFormat;
            request.priceMode = preference.priceMode;
            request.maxBudgetVnd = preference.maxBudgetVnd;
            request.preferredCities = preference.preferredCities;
            request.preferredDays = preference.preferredDays;
            request.personalizationEnabled = preference.personalizationEnabled;
            request.onboardingCompleted = true;
        } else {
            request.onboardingCompleted = true;
        }

        userRepository.savePreferences(request,
                new ApiCallback<PreferenceDtos.Preference>() {
                    @Override
                    public void onSuccess(@Nullable PreferenceDtos.Preference data,
                                          @Nullable JsonElement meta) {
                        if (data != null) {
                            preference = data;
                        }
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        error.setValue(exception);
                        loadPreference();
                    }
                });
    }
}
