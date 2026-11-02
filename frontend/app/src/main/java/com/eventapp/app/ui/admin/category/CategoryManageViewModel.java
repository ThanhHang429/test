package com.eventapp.app.ui.admin.category;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.R;
import com.eventapp.app.core.di.ServiceLocator;
import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.core.util.DateTimeUtils;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;
import com.eventapp.app.data.repository.CategoryRepository;
import com.eventapp.app.ui.model.CategoryUiModel;
import com.eventapp.app.ui.model.ContentRuleUiModel;
import com.eventapp.app.ui.model.FaqUiModel;
import com.google.gson.JsonElement;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FR-A09 — cây danh mục lấy từ {@code GET /admin/categories?includeInactive=true}.
 *
 * <p>Thêm/sửa/ẩn/hiện/sắp xếp đều gọi API thật. Bộ lọc từ khoá và FAQ trong thiết kế cũ không có
 * API backend nên các danh sách đó luôn rỗng và giao diện đã ẩn hai tab tương ứng.</p>
 */
public class CategoryManageViewModel extends ViewModel {

    public static final int MAX_ACTIVE_CATEGORIES = 50;
    public static final int MAX_CONTENT_RULES = 500;

    public enum Action {
        SAVED,
        HIDDEN,
        SHOWN,
        REORDERED,
        DUPLICATE,
        RULE_ADDED,
        RULE_TOGGLED,
        RULE_DELETED,
        RULE_RESTORED,
        FAQ_SAVED,
        FAQ_DELETED
    }

    public static class ActionEvent {
        public final Action action;
        public final ContentRuleUiModel deletedRule;
        public final String message;

        ActionEvent(Action action, ContentRuleUiModel deletedRule) {
            this(action, deletedRule, null);
        }

        ActionEvent(Action action, ContentRuleUiModel deletedRule, @Nullable String message) {
            this.action = action;
            this.deletedRule = deletedRule;
            this.message = message;
        }
    }

    private final CategoryRepository repository = ServiceLocator.get(CategoryRepository.class);

    private final MutableLiveData<List<CategoryUiModel>> categories =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContentRuleUiModel>> keywords =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContentRuleUiModel>> domains =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<FaqUiModel>> faqs = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> error = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> activeCategoryCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> ruleCount = new MutableLiveData<>(0);
    private final MutableLiveData<ActionEvent> actionEvent = new MutableLiveData<>();

    private final List<CategoryUiModel> allCategories = new ArrayList<>();
    private final Map<Long, Long> versionById = new HashMap<>();
    private final Map<Long, String> codeById = new HashMap<>();

    public CategoryManageViewModel() {
        load();
    }

    public LiveData<List<CategoryUiModel>> getCategories() {
        return categories;
    }

    public LiveData<List<ContentRuleUiModel>> getKeywords() {
        return keywords;
    }

    public LiveData<List<ContentRuleUiModel>> getDomains() {
        return domains;
    }

    public LiveData<List<FaqUiModel>> getFaqs() {
        return faqs;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getError() {
        return error;
    }

    public LiveData<Integer> getActiveCategoryCount() {
        return activeCategoryCount;
    }

    public LiveData<Integer> getRuleCount() {
        return ruleCount;
    }

    public LiveData<ActionEvent> getActionEvent() {
        return actionEvent;
    }

    public void load() {
        loading.setValue(true);
        error.setValue(false);
        repository.adminTree(true,
                new ApiCallback<List<AdminCategoryDtos.Category>>() {
                    @Override
                    public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                          @Nullable JsonElement meta) {
                        allCategories.clear();
                        versionById.clear();
                        codeById.clear();
                        if (data != null) {
                            for (AdminCategoryDtos.Category root : data) {
                                addCategory(root, 0L);
                                if (root.children != null) {
                                    for (AdminCategoryDtos.Category child : root.children) {
                                        addCategory(child, root.id);
                                    }
                                }
                            }
                        }
                        rebuildCategories();
                        publishCounts();
                        loading.setValue(false);
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        loading.setValue(false);
                        error.setValue(true);
                    }
                });
    }

    public void retry() {
        load();
    }

    private void addCategory(@Nullable AdminCategoryDtos.Category dto, long parentId) {
        if (dto == null || dto.name == null) {
            return;
        }
        long parent = dto.parentCategoryId == null ? parentId : dto.parentCategoryId;
        allCategories.add(new CategoryUiModel(
                dto.id,
                dto.name,
                dto.slug,
                parent,
                iconFor(dto),
                colorFor(dto),
                dto.active ? CategoryUiModel.Status.ACTIVE : CategoryUiModel.Status.HIDDEN,
                (int) Math.min(Integer.MAX_VALUE, dto.eventCount),
                (int) (DateTimeUtils.toMillis(dto.updatedAt) % Integer.MAX_VALUE)));
        versionById.put(dto.id, DateTimeUtils.toMillis(dto.updatedAt));
        codeById.put(dto.id, dto.code);
    }

    public int childCount(long categoryId) {
        int count = 0;
        for (CategoryUiModel item : allCategories) {
            if (item.parentId == categoryId) {
                count++;
            }
        }
        return count;
    }

    public void saveCategory(long id, String name, long parentId, int iconRes, int colorRes,
                             boolean active) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 120) {
            actionEvent.setValue(new ActionEvent(Action.DUPLICATE, null,
                    "Tên danh mục phải có 1–120 ký tự."));
            return;
        }

        if (id == 0L) {
            AdminCategoryDtos.CreateRequest request = new AdminCategoryDtos.CreateRequest();
            request.categoryCode = uniqueCode(trimmed);
            request.categoryName = trimmed;
            request.slug = slugify(trimmed);
            request.parentCategoryId = parentId > 0L ? parentId : null;
            request.sortOrder = nextSortOrder();
            submitCreate(request);
            return;
        }

        CategoryUiModel current = findById(id);
        if (current == null) {
            return;
        }
        AdminCategoryDtos.UpdateRequest request = new AdminCategoryDtos.UpdateRequest();
        request.categoryName = trimmed;
        request.slug = current.slug;
        request.version = versionById.get(id);
        repository.update(id, request, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                if (active == (current.status == CategoryUiModel.Status.ACTIVE)) {
                    actionEvent.setValue(new ActionEvent(Action.SAVED, null));
                    load();
                } else {
                    changeVisibility(id, active,
                            current.status == CategoryUiModel.Status.ACTIVE);
                }
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                actionEvent.setValue(new ActionEvent(
                        exception.getHttpCode() == 409 ? Action.DUPLICATE : Action.SAVED, null,
                        exception.getUserMessage()));
            }
        });
    }

    private void submitCreate(@NonNull AdminCategoryDtos.CreateRequest request) {
        repository.create(request, new ApiCallback<AdminCategoryDtos.Category>() {
            @Override
            public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                  @Nullable JsonElement meta) {
                actionEvent.setValue(new ActionEvent(Action.SAVED, null));
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                actionEvent.setValue(new ActionEvent(
                        exception.getHttpCode() == 409 ? Action.DUPLICATE : Action.SAVED, null,
                        exception.getUserMessage()));
            }
        });
    }

    private void changeVisibility(long id, boolean show, boolean wasActive) {
        if (show) {
            repository.unhide(id, new ApiCallback<AdminCategoryDtos.Category>() {
                @Override
                public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                      @Nullable JsonElement meta) {
                    actionEvent.setValue(new ActionEvent(
                            wasActive ? Action.SAVED : Action.SHOWN, null));
                    load();
                }

                @Override
                public void onError(@NonNull ApiException exception) {
                    actionEvent.setValue(new ActionEvent(Action.SAVED, null,
                            exception.getUserMessage()));
                }
            });
            return;
        }
        repository.hide(id, "Ẩn từ ứng dụng quản trị",
                new ApiCallback<AdminCategoryDtos.Category>() {
                    @Override
                    public void onSuccess(@Nullable AdminCategoryDtos.Category data,
                                          @Nullable JsonElement meta) {
                        actionEvent.setValue(new ActionEvent(Action.HIDDEN, null));
                        load();
                    }

                    @Override
                    public void onError(@NonNull ApiException exception) {
                        actionEvent.setValue(new ActionEvent(Action.SAVED, null,
                                exception.getUserMessage()));
                    }
                });
    }

    public void moveCategory(long fromId, long toId) {
        List<CategoryUiModel> roots = new ArrayList<>();
        for (CategoryUiModel item : allCategories) {
            if (item.isRoot()) {
                roots.add(item);
            }
        }
        int from = indexOf(roots, fromId);
        int to = indexOf(roots, toId);
        if (from < 0 || to < 0 || from == to) {
            return;
        }
        roots.add(to, roots.remove(from));

        List<AdminCategoryDtos.OrderItem> items = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            items.add(new AdminCategoryDtos.OrderItem(roots.get(i).id, i + 1));
        }
        repository.reorder(items, new ApiCallback<List<AdminCategoryDtos.Category>>() {
            @Override
            public void onSuccess(@Nullable List<AdminCategoryDtos.Category> data,
                                  @Nullable JsonElement meta) {
                actionEvent.setValue(new ActionEvent(Action.REORDERED, null));
                load();
            }

            @Override
            public void onError(@NonNull ApiException exception) {
                actionEvent.setValue(new ActionEvent(Action.REORDERED, null,
                        exception.getUserMessage()));
                load();
            }
        });
    }

    // Các hàm của tab từ khoá/FAQ giữ lại để không phá vỡ giao diện, nhưng backend chưa có API.

    public void addRule(String term, ContentRuleUiModel.Kind kind,
                        ContentRuleUiModel.MatchType matchType, List<String> scopes,
                        ContentRuleUiModel.Severity severity) {
        actionEvent.setValue(new ActionEvent(Action.RULE_ADDED, null,
                "Bộ lọc nội dung chưa có API backend."));
    }

    public void toggleRule(long id, boolean active) {
    }

    public void deleteRule(long id) {
    }

    public void restoreRule(ContentRuleUiModel rule) {
    }

    public void saveFaq(long id, String title, String content, boolean visible) {
        actionEvent.setValue(new ActionEvent(Action.FAQ_SAVED, null,
                "FAQ chưa có API backend."));
    }

    public void deleteFaq(long id) {
    }

    private void rebuildCategories() {
        List<CategoryUiModel> flat = new ArrayList<>();
        for (CategoryUiModel root : allCategories) {
            if (!root.isRoot()) {
                continue;
            }
            flat.add(root);
            for (CategoryUiModel child : allCategories) {
                if (child.parentId == root.id) {
                    flat.add(child);
                }
            }
        }
        categories.setValue(flat);
    }

    private void publishCounts() {
        int active = 0;
        for (CategoryUiModel item : allCategories) {
            if (item.status == CategoryUiModel.Status.ACTIVE) {
                active++;
            }
        }
        activeCategoryCount.setValue(active);
        ruleCount.setValue(0);
    }

    @Nullable
    private CategoryUiModel findById(long id) {
        for (CategoryUiModel item : allCategories) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    private int nextSortOrder() {
        int max = 0;
        for (CategoryUiModel item : allCategories) {
            if (item.isRoot()) {
                max++;
            }
        }
        return max + 1;
    }

    @NonNull
    private String uniqueCode(@NonNull String name) {
        String base = slugify(name).replace('-', '_').toUpperCase(Locale.ROOT);
        if (base.isEmpty()) {
            base = "CATEGORY";
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        String code = base;
        int suffix = 2;
        while (codeExists(code)) {
            code = base + "_" + suffix;
            suffix++;
        }
        return code;
    }

    private boolean codeExists(@NonNull String code) {
        for (String existing : codeById.values()) {
            if (code.equalsIgnoreCase(existing)) {
                return true;
            }
        }
        return false;
    }

    private int indexOf(List<CategoryUiModel> items, long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private static String slugify(@NonNull String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private static int iconFor(AdminCategoryDtos.Category dto) {
        String code = dto.code == null ? "" : dto.code.toUpperCase(Locale.ROOT);
        if (code.contains("MUSIC")) {
            return R.drawable.ic_ds_mic;
        }
        if (code.contains("TECH")) {
            return R.drawable.ic_ds_chart;
        }
        if (code.contains("SPORT")) {
            return R.drawable.ic_ds_ticket;
        }
        if (code.contains("ART")) {
            return R.drawable.ic_ds_image;
        }
        if (code.contains("FOOD")) {
            return R.drawable.ic_ds_payment;
        }
        return R.drawable.ic_ds_event;
    }

    private static int colorFor(AdminCategoryDtos.Category dto) {
        String code = dto.code == null ? "" : dto.code.toUpperCase(Locale.ROOT);
        if (code.contains("TECH")) {
            return R.color.ds_secondary;
        }
        if (code.contains("SPORT")) {
            return R.color.ds_organizer;
        }
        if (code.contains("ART")) {
            return R.color.ds_info;
        }
        return R.color.ds_primary;
    }
}
