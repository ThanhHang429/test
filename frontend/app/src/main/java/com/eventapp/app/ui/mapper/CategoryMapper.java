package com.eventapp.app.ui.mapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.ui.model.CategoryNodeUiModel;

import java.util.ArrayList;
import java.util.List;

/** Chuyển cây danh mục công khai từ DTO sang UI model. */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    @NonNull
    public static List<CategoryNodeUiModel> fromTree(
            @Nullable List<CatalogDtos.CategoryNode> nodes) {
        List<CategoryNodeUiModel> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (CatalogDtos.CategoryNode node : nodes) {
            if (node != null) {
                result.add(from(node));
            }
        }
        return result;
    }

    @NonNull
    private static CategoryNodeUiModel from(@NonNull CatalogDtos.CategoryNode node) {
        return new CategoryNodeUiModel(
                node.id,
                node.name == null ? "" : node.name,
                fromTree(node.children));
    }
}
