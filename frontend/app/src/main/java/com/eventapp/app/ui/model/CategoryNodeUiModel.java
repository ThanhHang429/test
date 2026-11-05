package com.eventapp.app.ui.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Một danh mục trong cây công khai dùng cho chip lọc và sở thích. */
public final class CategoryNodeUiModel {

    public final long id;
    public final String name;
    public final List<CategoryNodeUiModel> children;

    public CategoryNodeUiModel(long id, @NonNull String name,
                               @NonNull List<CategoryNodeUiModel> children) {
        this.id = id;
        this.name = name;
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }
}
