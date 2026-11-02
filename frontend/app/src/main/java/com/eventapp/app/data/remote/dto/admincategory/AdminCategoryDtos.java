package com.eventapp.app.data.remote.dto.admincategory;

import java.util.ArrayList;
import java.util.List;

/** Quản lý danh mục (FR-A09). */
public final class AdminCategoryDtos {

    private AdminCategoryDtos() {
    }

    public static class Category {
        public long id;
        public String code;
        public String name;
        public String slug;
        public String description;
        public Long parentCategoryId;
        public boolean active;
        public int sortOrder;
        public long eventCount;
        public List<Category> children = new ArrayList<>();
        public String updatedAt;
    }

    public static class CreateRequest {
        public String categoryCode;
        public String categoryName;
        public String slug;
        public String description;
        public Long parentCategoryId;
        public Integer sortOrder;
    }

    public static class UpdateRequest {
        public String categoryName;
        public String slug;
        public String description;
        public Integer sortOrder;
        public Long version;
    }

    public static class HideRequest {
        public String reason;

        public HideRequest(String reason) {
            this.reason = reason;
        }
    }

    public static class OrderItem {
        public long categoryId;
        public int sortOrder;

        public OrderItem(long categoryId, int sortOrder) {
            this.categoryId = categoryId;
            this.sortOrder = sortOrder;
        }
    }

    public static class OrderRequest {
        public List<OrderItem> items = new ArrayList<>();

        public OrderRequest(List<OrderItem> items) {
            this.items = items;
        }
    }
}
