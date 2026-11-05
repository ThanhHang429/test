package com.eventapp.app.ui.organizer.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.ui.mapper.CategoryMapper;
import com.eventapp.app.ui.model.CategoryNodeUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Form tạo sự kiện bước 1: danh mục lấy từ API để khớp dữ liệu Admin. */
public class OrgCreateEventViewModel extends ViewModel {

    private final CategoryRepository repository = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<List<CategoryNodeUiModel>> categories =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<CategoryNodeUiModel>> getCategories() {
        return categories;
    }

    public void loadCategories() {
        repository.tree(new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(@Nullable List<CatalogDtos.CategoryNode> data,
                                  @Nullable JsonElement meta) {
                categories.setValue(CategoryMapper.fromTree(data));
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                categories.setValue(new ArrayList<>());
            }
        });
    }
}
