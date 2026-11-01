package com.eventapp.app.ui.admin.category;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.fake.SampleCategoryData;
import com.eventapp.app.ui.model.CategoryUiModel;
import com.eventapp.app.ui.model.ContentRuleUiModel;
import com.eventapp.app.ui.model.FaqUiModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FR-A09 state holder: category tree, banned keywords/suspicious domains and guidance content.
 * Every action mutates the in-memory catalogue only; the backend contract is not wired yet.
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

        ActionEvent(Action action, ContentRuleUiModel deletedRule) {
            this.action = action;
            this.deletedRule = deletedRule;
        }
    }

    private static final long LOAD_DELAY_MS = 320L;

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

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<CategoryUiModel> allCategories = new ArrayList<>();
    private final List<ContentRuleUiModel> allRules = new ArrayList<>();
    private final List<FaqUiModel> allFaqs = new ArrayList<>();
    private long nextId = 1000L;

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
        handler.postDelayed(() -> {
            allCategories.clear();
            allCategories.addAll(SampleCategoryData.categories());
            allRules.clear();
            allRules.addAll(SampleCategoryData.rules());
            allFaqs.clear();
            allFaqs.addAll(SampleCategoryData.faqs());
            rebuildCategories();
            publishRules();
            faqs.setValue(new ArrayList<>(allFaqs));
            publishCounts();
            loading.setValue(false);
        }, LOAD_DELAY_MS);
    }

    public void retry() {
        load();
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
        if (isDuplicate(trimmed, parentId, id)) {
            actionEvent.setValue(new ActionEvent(Action.DUPLICATE, null));
            return;
        }
        CategoryUiModel.Status status =
                active ? CategoryUiModel.Status.ACTIVE : CategoryUiModel.Status.HIDDEN;

        if (id == 0L) {
            CategoryUiModel created = new CategoryUiModel(nextId++, trimmed, slugify(trimmed),
                    parentId, iconRes, colorRes, status, 0, 1);
            allCategories.add(created);
            rebuildCategories();
            publishCounts();
            actionEvent.setValue(new ActionEvent(
                    active ? Action.SAVED : Action.HIDDEN, null));
            return;
        }

        for (int i = 0; i < allCategories.size(); i++) {
            CategoryUiModel item = allCategories.get(i);
            if (item.id != id) {
                continue;
            }
            CategoryUiModel updated = item
                    .withContent(trimmed, parentId, iconRes, colorRes)
                    .withStatus(status);
            allCategories.set(i, updated);
            rebuildCategories();
            publishCounts();
            Action action = status == CategoryUiModel.Status.ACTIVE
                    ? (item.status == status ? Action.SAVED : Action.SHOWN)
                    : Action.HIDDEN;
            actionEvent.setValue(new ActionEvent(action, null));
            return;
        }
    }

    public void moveCategory(long fromId, long toId) {
        List<CategoryUiModel> snapshot = new ArrayList<>(allCategories);
        List<CategoryUiModel> roots = new ArrayList<>();
        for (CategoryUiModel item : snapshot) {
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

        allCategories.clear();
        for (CategoryUiModel root : roots) {
            allCategories.add(root);
            for (CategoryUiModel child : snapshot) {
                if (child.parentId == root.id) {
                    allCategories.add(child);
                }
            }
        }
        rebuildCategories();
        actionEvent.setValue(new ActionEvent(Action.REORDERED, null));
    }

    public void addRule(String term, ContentRuleUiModel.Kind kind,
                        ContentRuleUiModel.MatchType matchType, List<String> scopes,
                        ContentRuleUiModel.Severity severity) {
        String trimmed = term == null ? "" : term.trim();
        if (trimmed.length() < 2 || trimmed.length() > 50 || isDuplicateRule(trimmed)) {
            return;
        }
        allRules.add(new ContentRuleUiModel(nextId++, trimmed, kind, matchType, scopes,
                severity, true));
        publishRules();
        publishCounts();
        actionEvent.setValue(new ActionEvent(Action.RULE_ADDED, null));
    }

    public void toggleRule(long id, boolean active) {
        for (int i = 0; i < allRules.size(); i++) {
            ContentRuleUiModel item = allRules.get(i);
            if (item.id == id) {
                allRules.set(i, item.withActive(active));
                publishRules();
                actionEvent.setValue(new ActionEvent(Action.RULE_TOGGLED, null));
                return;
            }
        }
    }

    public void deleteRule(long id) {
        for (int i = 0; i < allRules.size(); i++) {
            ContentRuleUiModel item = allRules.get(i);
            if (item.id == id) {
                allRules.remove(i);
                publishRules();
                publishCounts();
                actionEvent.setValue(new ActionEvent(Action.RULE_DELETED, item));
                return;
            }
        }
    }

    public void restoreRule(ContentRuleUiModel rule) {
        if (rule == null) {
            return;
        }
        allRules.add(rule);
        publishRules();
        publishCounts();
        actionEvent.setValue(new ActionEvent(Action.RULE_RESTORED, null));
    }

    public void saveFaq(long id, String title, String content, boolean visible) {
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedContent = content == null ? "" : content.trim();
        if (id == 0L) {
            allFaqs.add(new FaqUiModel(nextId++, trimmedTitle, trimmedContent, visible,
                    allFaqs.size() + 1));
        } else {
            for (int i = 0; i < allFaqs.size(); i++) {
                FaqUiModel item = allFaqs.get(i);
                if (item.id == id) {
                    allFaqs.set(i, item.withContent(trimmedTitle, trimmedContent, visible));
                    break;
                }
            }
        }
        faqs.setValue(new ArrayList<>(allFaqs));
        actionEvent.setValue(new ActionEvent(Action.FAQ_SAVED, null));
    }

    public void deleteFaq(long id) {
        for (int i = 0; i < allFaqs.size(); i++) {
            if (allFaqs.get(i).id == id) {
                allFaqs.remove(i);
                faqs.setValue(new ArrayList<>(allFaqs));
                actionEvent.setValue(new ActionEvent(Action.FAQ_DELETED, null));
                return;
            }
        }
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

    private void publishRules() {
        List<ContentRuleUiModel> keywordItems = new ArrayList<>();
        List<ContentRuleUiModel> domainItems = new ArrayList<>();
        for (ContentRuleUiModel rule : allRules) {
            if (rule.kind == ContentRuleUiModel.Kind.DOMAIN) {
                domainItems.add(rule);
            } else {
                keywordItems.add(rule);
            }
        }
        keywords.setValue(keywordItems);
        domains.setValue(domainItems);
    }

    private void publishCounts() {
        int active = 0;
        for (CategoryUiModel item : allCategories) {
            if (item.status == CategoryUiModel.Status.ACTIVE) {
                active++;
            }
        }
        activeCategoryCount.setValue(active);
        ruleCount.setValue(allRules.size());
    }

    private boolean isDuplicate(String name, long parentId, long ignoreId) {
        String key = name.toLowerCase(Locale.ROOT);
        for (CategoryUiModel item : allCategories) {
            if (item.id != ignoreId && item.parentId == parentId
                    && item.name.toLowerCase(Locale.ROOT).equals(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicateRule(String term) {
        String key = term.toLowerCase(Locale.ROOT);
        for (ContentRuleUiModel rule : allRules) {
            if (rule.term.toLowerCase(Locale.ROOT).equals(key)) {
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

    private String slugify(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
