package com.eventapp.app.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Danh sách kèm meta phân trang mà backend trả về. */
public final class Paged<T> {

    @NonNull
    public final List<T> items;
    @Nullable
    public final ResponseWrapper.PageMeta meta;

    public Paged(@Nullable List<T> items, @Nullable JsonElement meta) {
        this.items = items == null ? new ArrayList<>() : items;
        this.meta = ResponseWrapper.asPageMeta(meta);
    }

    public boolean hasNext() {
        return meta != null && meta.hasNext;
    }

    public long totalItems() {
        return meta == null ? items.size() : meta.totalItems;
    }
}
