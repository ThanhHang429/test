package com.eventapp.app.data.remote.dto.catalog;

import java.util.ArrayList;
import java.util.List;

/** Danh mục sự kiện công khai (GET /categories) — cây tối đa 2 cấp. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public static class CategoryNode {
        public long id;
        public String code;
        public String name;
        public String slug;
        public String description;
        public Long parentId;
        public int sortOrder;
        public List<CategoryNode> children = new ArrayList<>();
    }
}
