package com.eventapp.app.ui.admin.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.ui.mapper.AdminCategoryMapper;
import com.eventapp.app.ui.model.AdminCategoryUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Quản lý danh mục sự kiện: tải cây, CRUD và sắp xếp thứ tự (FR-A09). */
public class AdminCategoriesViewModel extends ViewModel {

    public enum Notice {
        CREATED,
        UPDATED,
        DELETED,
        HIDDEN,
        UNHIDDEN
    }

    private final CategoryRepository repository = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<List<AdminCategoryUiModel>> roots =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ApiException> error = new MutableLiveData<>();
    private final MutableLiveData<Notice> notice = new MutableLiveData<>();

    public LiveData<List<AdminCategoryUiModel>> getRoots() {
        return roots;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<ApiException> getError() {
        return error;
    }

    public LiveData<Notice> getNotice() {
        return notice;
    }

    public void load() {
        loading.setValue(true);
        repository.adminTree(true, new ApiCallback<List<AdminCategoryDtos.Category>>() {
            @Override
            public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                  @Nullable JsonElement meta) {
                roots.setValue(AdminCategoryMapper.fromTree(data));
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

    public void move(@NonNull List<AdminCategoryUiModel> siblings, int index, int delta) {
        int target = index + delta;
        if (target < 0 || target >= siblings.size()) {
            return;
        }
        AdminCategoryUiModel moving = siblings.remove(index);
        siblings.add(target, moving);

        List<AdminCategoryDtos.OrderItem> items = new ArrayList<>();
        for (int i = 0; i < siblings.size(); i++) {
            items.add(new AdminCategoryDtos.OrderItem(siblings.get(i).id, i));
        }
        repository.reorder(items, new ApiCallback<List<AdminCategoryDtos.Category>>() {
            @Override
            public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                  @Nullable JsonElement meta) {
                roots.setValue(AdminCategoryMapper.fromTree(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
                load();
            }
        });
    }

    public void create(@NonNull String code, @NonNull String name, @Nullable String slug,
                       @Nullable String description, @Nullable Long parentId, int sortOrder) {
        AdminCategoryDtos.CreateRequest request = new AdminCategoryDtos.CreateRequest();
        request.categoryCode = code;
        request.categoryName = name;
        request.slug = slug;
        request.description = description;
        request.parentCategoryId = parentId;
        request.sortOrder = sortOrder;
        repository.create(request, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                notice.setValue(Notice.CREATED);
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void update(long id, @NonNull String name, @Nullable String slug,
                       @Nullable String description, int sortOrder, long version) {
        AdminCategoryDtos.UpdateRequest request = new AdminCategoryDtos.UpdateRequest();
        request.categoryName = name;
        request.slug = slug;
        request.description = description;
        request.sortOrder = sortOrder;
        request.version = version;
        repository.update(id, request, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                notice.setValue(Notice.UPDATED);
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void delete(long categoryId) {
        repository.delete(categoryId, new ApiCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void data, @Nullable JsonElement meta) {
                notice.setValue(Notice.DELETED);
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void hide(long categoryId, @NonNull String reason) {
        repository.hide(categoryId, reason, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                notice.setValue(Notice.HIDDEN);
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void unhide(long categoryId) {
        repository.unhide(categoryId, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                notice.setValue(Notice.UNHIDDEN);
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                error.setValue(exception);
            }
        });
    }

    public void consumeError() {
        error.setValue(null);
    }

    public void consumeNotice() {
        notice.setValue(null);
    }
}
