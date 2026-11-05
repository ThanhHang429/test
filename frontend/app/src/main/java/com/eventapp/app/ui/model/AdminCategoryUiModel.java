package com.eventapp.app.ui.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Danh mục sự kiện trong cây quản trị (FR-A09). */
public final class AdminCategoryUiModel {

    public final long id;
    public final String code;
    public final String name;
    public final String slug;
    public final String description;
    @Nullable
    public final Long parentCategoryId;
    public final boolean active;
    public final int sortOrder;
    public final long eventCount;
    public final List<AdminCategoryUiModel> children;
    public final long updatedAtMillis;

    public AdminCategoryUiModel(long id, @Nullable String code, @Nullable String name,
                                @Nullable String slug, @Nullable String description,
                                @Nullable Long parentCategoryId, boolean active, int sortOrder,
                                long eventCount, @Nullable List<AdminCategoryUiModel> children,
                                long updatedAtMillis) {
        this.id = id;
        this.code = code == null ? "" : code;
        this.name = name == null ? "" : name;
        this.slug = slug == null ? "" : slug;
        this.description = description == null ? "" : description;
        this.parentCategoryId = parentCategoryId;
        this.active = active;
        this.sortOrder = sortOrder;
        this.eventCount = eventCount;
        this.children = children == null ? new ArrayList<>() : children;
        this.updatedAtMillis = updatedAtMillis;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @NonNull
    public List<AdminCategoryUiModel> childList() {
        return children;
    }
}
