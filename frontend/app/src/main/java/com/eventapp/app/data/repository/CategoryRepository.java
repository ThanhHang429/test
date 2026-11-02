package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.network.Paged;
import com.eventapp.app.data.remote.api.AdminCategoryApi;
import com.eventapp.app.data.remote.api.CategoryApi;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Danh mục công khai + quản lý danh mục cho admin.
 *
 * <p>Cây danh mục ít đổi nhưng được nhiều màn hình hỏi (trang chủ, sở thích, công cụ organizer,
 * quản trị). Repository giữ bản sao 60 giây và gộp các lời gọi song song thành một request, nhờ đó
 * chuyển tab qua lại không phát sinh thêm round-trip.</p>
 */
public class CategoryRepository {

    private static final long TREE_TTL_MS = 60_000L;

    private final CategoryApi publicApi;
    private final AdminCategoryApi adminApi;

    private final List<ApiCallback<List<CatalogDtos.CategoryNode>>> waiting = new ArrayList<>();
    private List<CatalogDtos.CategoryNode> cachedTree;
    private long cachedAt;
    private boolean treeInFlight;

    public CategoryRepository(@NonNull CategoryApi publicApi, @NonNull AdminCategoryApi adminApi) {
        this.publicApi = publicApi;
        this.adminApi = adminApi;
    }

    public void tree(@NonNull ApiCallback<List<CatalogDtos.CategoryNode>> callback) {
        callback.onStart();
        List<CatalogDtos.CategoryNode> cached = cachedTree();
        if (cached != null) {
            callback.onSuccess(cached, null);
            callback.onFinish();
            return;
        }
        waiting.add(callback);
        if (treeInFlight) {
            return;
        }
        treeInFlight = true;
        ApiCallback.enqueue(publicApi.tree(), new ApiCallback<List<CatalogDtos.CategoryNode>>() {
            @Override
            public void onSuccess(List<CatalogDtos.CategoryNode> data, JsonElement meta) {
                cachedTree = data;
                cachedAt = System.currentTimeMillis();
                treeInFlight = false;
                flush(data, null);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                treeInFlight = false;
                flush(null, exception);
            }
        });
    }

    /** Gọi sau khi admin sửa danh mục để lần đọc sau lấy dữ liệu mới. */
    public void invalidateTree() {
        cachedTree = null;
        cachedAt = 0L;
    }

    @Nullable
    private List<CatalogDtos.CategoryNode> cachedTree() {
        if (cachedTree == null) {
            return null;
        }
        if (System.currentTimeMillis() - cachedAt > TREE_TTL_MS) {
            cachedTree = null;
            return null;
        }
        return cachedTree;
    }

    private void flush(@Nullable List<CatalogDtos.CategoryNode> data, @Nullable ApiException error) {
        List<ApiCallback<List<CatalogDtos.CategoryNode>>> callbacks = new ArrayList<>(waiting);
        waiting.clear();
        for (ApiCallback<List<CatalogDtos.CategoryNode>> callback : callbacks) {
            if (error == null) {
                callback.onSuccess(data, null);
            } else {
                callback.onError(error);
            }
            callback.onFinish();
        }
    }

    public void adminTree(boolean includeInactive,
                          @NonNull ApiCallback<List<AdminCategoryDtos.Category>> callback) {
        ApiCallback.enqueue(adminApi.tree(includeInactive), callback);
    }

    public void create(@NonNull AdminCategoryDtos.CreateRequest request,
                       @NonNull ApiCallback<AdminCategoryDtos.Category> callback) {
        ApiCallback.enqueue(adminApi.create(request), invalidating(callback));
    }

    public void update(long id, @NonNull AdminCategoryDtos.UpdateRequest request,
                       @NonNull ApiCallback<AdminCategoryDtos.Category> callback) {
        ApiCallback.enqueue(adminApi.update(id, request), invalidating(callback));
    }

    public void hide(long id, @NonNull String reason,
                     @NonNull ApiCallback<AdminCategoryDtos.Category> callback) {
        ApiCallback.enqueue(adminApi.hide(id, new AdminCategoryDtos.HideRequest(reason)),
                invalidating(callback));
    }

    public void unhide(long id, @NonNull ApiCallback<AdminCategoryDtos.Category> callback) {
        ApiCallback.enqueue(adminApi.unhide(id), invalidating(callback));
    }

    public void reorder(@NonNull List<AdminCategoryDtos.OrderItem> items,
                        @NonNull ApiCallback<List<AdminCategoryDtos.Category>> callback) {
        ApiCallback.enqueue(adminApi.reorder(new AdminCategoryDtos.OrderRequest(items)),
                invalidating(callback));
    }

    /** Bỏ cache sau khi ghi thành công để lần đọc sau thấy dữ liệu mới. */
    @NonNull
    private <T> ApiCallback<T> invalidating(@NonNull ApiCallback<T> target) {
        return new ApiCallback.Delegate<T>(target) {
            @Override
            public void onSuccess(T data, JsonElement meta) {
                invalidateTree();
                target.onSuccess(data, meta);
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                target.onError(exception);
            }
        };
    }

    /** Tiện ích: meta phân trang cho danh sách admin (không phân trang ở backend). */
    @NonNull
    public static Paged<AdminCategoryDtos.Category> paged(
            @Nullable List<AdminCategoryDtos.Category> items, @Nullable JsonElement meta) {
        return new Paged<>(items, meta);
    }
}
