package com.eventapp.app.ui.model;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * Category row for FR-A09. {@code parentId == 0} means a root category; the tree has at most two
 * levels, so children never have children of their own.
 */
public class CategoryUiModel {

    public enum Status {
        ACTIVE,
        HIDDEN
    }

    public final long id;
    public final String name;
    public final String slug;
    public final long parentId;
    @DrawableRes
    public final int iconRes;
    @ColorRes
    public final int colorRes;
    public final Status status;
    public final int usageCount;
    public final int version;

    public CategoryUiModel(long id, String name, String slug, long parentId,
                           @DrawableRes int iconRes, @ColorRes int colorRes,
                           Status status, int usageCount, int version) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.parentId = parentId;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
        this.status = status;
        this.usageCount = usageCount;
        this.version = version;
    }

    public boolean isRoot() {
        return parentId == 0L;
    }

    public CategoryUiModel withStatus(Status nextStatus) {
        return new CategoryUiModel(id, name, slug, parentId, iconRes, colorRes,
                nextStatus, usageCount, version + 1);
    }

    public CategoryUiModel withContent(String nextName, long nextParentId,
                                       @DrawableRes int nextIconRes, @ColorRes int nextColorRes) {
        return new CategoryUiModel(id, nextName, slug, nextParentId, nextIconRes, nextColorRes,
                status, usageCount, version + 1);
    }
}
