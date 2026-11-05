package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;
import com.eventapp.app.ui.model.AdminCategoryUiModel;

import java.util.ArrayList;
import java.util.List;

/** Chuyển cây danh mục quản trị sang AdminCategoryUiModel. */
public final class AdminCategoryMapper {

    private AdminCategoryMapper() {
    }

    @NonNull
    public static List<AdminCategoryUiModel> fromTree(
            @Nullable List<AdminCategoryDtos.Category> items) {
        List<AdminCategoryUiModel> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (AdminCategoryDtos.Category item : items) {
            if (item != null) {
                result.add(from(item));
            }
        }
        return result;
    }

    @NonNull
    private static AdminCategoryUiModel from(@NonNull AdminCategoryDtos.Category item) {
        return new AdminCategoryUiModel(
                item.id,
                item.code,
                item.name,
                item.slug,
                item.description,
                item.parentCategoryId,
                item.active,
                item.sortOrder,
                item.eventCount,
                fromTree(item.children),
                DateTimeUtils.toMillis(item.updatedAt));
    }
}
