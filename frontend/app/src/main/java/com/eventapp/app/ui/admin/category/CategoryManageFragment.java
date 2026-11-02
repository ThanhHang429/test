package com.eventapp.app.ui.admin.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.admin.category.adapter.CategoryTreeAdapter;
import com.eventapp.app.ui.admin.category.adapter.ContentRuleAdapter;
import com.eventapp.app.ui.admin.category.adapter.FaqAdapter;
import com.eventapp.app.ui.common.view.EmptyStateView;
import com.eventapp.app.ui.common.view.ErrorStateView;
import com.eventapp.app.ui.common.view.SkeletonLayout;
import com.eventapp.app.ui.model.CategoryUiModel;
import com.eventapp.app.ui.model.ContentRuleUiModel;
import com.eventapp.app.ui.model.FaqUiModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * FR-A09: one destination with three tabs — category tree (A09-R01), banned keywords and
 * suspicious domains, guidance content. The tree supports drag reorder for root categories; the
 * edit form keeps the optimistic version check contract in the ViewModel.
 */
public class CategoryManageFragment extends Fragment {

    private CategoryManageViewModel viewModel;
    private CategoryTreeAdapter categoryAdapter;
    private ContentRuleAdapter ruleAdapter;
    private FaqAdapter faqAdapter;

    private TabLayout tabs;
    private TextView subtitle;
    private TextView ruleCountView;
    private View groupCategories;
    private View groupRules;
    private View groupFaqs;
    private RecyclerView categoriesList;
    private RecyclerView rulesList;
    private RecyclerView faqsList;
    private EmptyStateView emptyCategories;
    private EmptyStateView emptyRules;
    private EmptyStateView emptyFaqs;
    private SkeletonLayout skeleton;
    private ErrorStateView errorState;

    private TextInputLayout ruleLayout;
    private TextInputEditText ruleInput;
    private ChipGroup ruleKindGroup;
    private ChipGroup ruleMatchGroup;
    private ChipGroup ruleScopeGroup;
    private ChipGroup ruleSeverityGroup;

    private ItemTouchHelper touchHelper;
    private List<CategoryUiModel> currentCategories = new ArrayList<>();
    private List<ContentRuleUiModel> currentKeywords = new ArrayList<>();
    private List<ContentRuleUiModel> currentDomains = new ArrayList<>();
    private long pendingFromId;
    private long pendingToId;
    private boolean bindingTabs;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_category_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CategoryManageViewModel.class);
        bindViews(view);
        setupToolbar();
        setupTabs();
        setupCategoryList();
        setupRuleForm();
        setupFaqList();
        setupDialogs();
        observe();
    }

    private void bindViews(View view) {
        tabs = view.findViewById(R.id.tabsCategory);
        subtitle = view.findViewById(R.id.tvCatCount);
        ruleCountView = view.findViewById(R.id.tvRuleCount);
        groupCategories = view.findViewById(R.id.groupCategories);
        groupRules = view.findViewById(R.id.groupRules);
        groupFaqs = view.findViewById(R.id.groupFaqs);
        categoriesList = view.findViewById(R.id.rvCategories);
        rulesList = view.findViewById(R.id.rvRules);
        faqsList = view.findViewById(R.id.rvFaqs);
        emptyCategories = view.findViewById(R.id.emptyCategories);
        emptyRules = view.findViewById(R.id.emptyRules);
        emptyFaqs = view.findViewById(R.id.emptyFaqs);
        skeleton = view.findViewById(R.id.skeletonCategory);
        errorState = view.findViewById(R.id.errorCategory);
        ruleLayout = view.findViewById(R.id.layoutRuleTerm);
        ruleInput = view.findViewById(R.id.etRuleTerm);
        ruleKindGroup = view.findViewById(R.id.chipGroupRuleKind);
        ruleMatchGroup = view.findViewById(R.id.chipGroupRuleMatch);
        ruleScopeGroup = view.findViewById(R.id.chipGroupRuleScope);
        ruleSeverityGroup = view.findViewById(R.id.chipGroupRuleSeverity);
    }

    private void setupToolbar() {
        // Màn này là tab cấp 1 của bottom navigation nên không có nút quay lại.
    }

    private void setupTabs() {
        // Backend chỉ có API danh mục; tab bộ lọc nội dung và FAQ không có dữ liệu nên ẩn.
        tabs.setVisibility(View.GONE);
        groupRules.setVisibility(View.GONE);
        groupFaqs.setVisibility(View.GONE);
        groupCategories.setVisibility(View.VISIBLE);
        tabs.addTab(tabs.newTab().setText(R.string.hc_cat_tab_categories));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!bindingTabs) {
                    showTab(tab.getPosition());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void showTab(int position) {
        groupCategories.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        groupRules.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        groupFaqs.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }

    private void setupCategoryList() {
        categoryAdapter = new CategoryTreeAdapter();
        categoryAdapter.setOnCategoryActionListener(new CategoryTreeAdapter.OnCategoryActionListener() {
            @Override
            public void onEdit(CategoryUiModel category) {
                showCategoryForm(category);
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder holder) {
                if (touchHelper != null) {
                    touchHelper.startDrag(holder);
                }
            }
        });
        categoriesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoriesList.setAdapter(categoryAdapter);

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                CategoryUiModel source = categoryAdapter.getItem(from);
                CategoryUiModel destination = categoryAdapter.getItem(to);
                if (source == null || destination == null
                        || !source.isRoot() || !destination.isRoot()) {
                    return false;
                }
                pendingFromId = source.id;
                pendingToId = destination.id;
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onMoved(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder, int fromPos,
                                @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                if (pendingFromId != 0L && pendingToId != 0L) {
                    viewModel.moveCategory(pendingFromId, pendingToId);
                    pendingFromId = 0L;
                    pendingToId = 0L;
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
        });
        touchHelper.attachToRecyclerView(categoriesList);

        requireView().findViewById(R.id.fabAddCategory).setOnClickListener(v ->
                showCategoryForm(null));

        emptyCategories.setIcon(R.drawable.ic_ds_event);
        emptyCategories.setTitle(R.string.hc_cat_empty_title);
        emptyCategories.setMessage(R.string.hc_cat_empty_message);
        emptyCategories.showAction(false);
    }

    private void setupRuleForm() {
        ruleAdapter = new ContentRuleAdapter();
        ruleAdapter.setOnRuleActionListener(new ContentRuleAdapter.OnRuleActionListener() {
            @Override
            public void onToggle(ContentRuleUiModel rule, boolean active) {
                viewModel.toggleRule(rule.id, active);
            }

            @Override
            public void onDelete(ContentRuleUiModel rule) {
                viewModel.deleteRule(rule.id);
            }
        });
        rulesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        rulesList.setAdapter(ruleAdapter);

        emptyRules.setIcon(R.drawable.ic_ds_flag);
        emptyRules.setTitle(R.string.hc_cat_rule_empty_title);
        emptyRules.setMessage(R.string.hc_cat_rule_empty_message);
        emptyRules.showAction(false);

        ruleKindGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean domain = checkedIds.contains(R.id.chipRuleKindDomain);
            ruleMatchGroup.setVisibility(domain ? View.GONE : View.VISIBLE);
            ruleScopeGroup.setVisibility(domain ? View.GONE : View.VISIBLE);
            ruleLayout.setHint(domain
                    ? R.string.hc_cat_kind_domain
                    : R.string.hc_cat_rule_hint);
            renderRules();
        });

        MaterialButton addRule = requireView().findViewById(R.id.btnAddRule);
        addRule.setOnClickListener(v -> submitRule());
    }

    private void submitRule() {
        ruleLayout.setError(null);
        String term = ruleInput.getText() == null ? "" : ruleInput.getText().toString().trim();
        if (term.length() < 2 || term.length() > 50) {
            ruleLayout.setError(getString(R.string.hc_cat_rule_error));
            return;
        }

        boolean domain = ruleKindGroup.getCheckedChipId() == R.id.chipRuleKindDomain;
        ContentRuleUiModel.Kind kind = domain
                ? ContentRuleUiModel.Kind.DOMAIN
                : ContentRuleUiModel.Kind.KEYWORD;
        ContentRuleUiModel.MatchType matchType =
                ruleMatchGroup.getCheckedChipId() == R.id.chipMatchWord
                        ? ContentRuleUiModel.MatchType.WORD
                        : ContentRuleUiModel.MatchType.CONTAINS;

        List<String> scopes = new ArrayList<>();
        if (!domain) {
            if (((Chip) requireView().findViewById(R.id.chipScopeEventText)).isChecked()) {
                scopes.add("EVENT_TEXT");
            }
            if (((Chip) requireView().findViewById(R.id.chipScopeTag)).isChecked()) {
                scopes.add("TAG");
            }
            if (((Chip) requireView().findViewById(R.id.chipScopeReview)).isChecked()) {
                scopes.add("REVIEW");
            }
        }

        ContentRuleUiModel.Severity severity = ContentRuleUiModel.Severity.MEDIUM;
        int checkedSeverity = ruleSeverityGroup.getCheckedChipId();
        if (checkedSeverity == R.id.chipSeverityLow) {
            severity = ContentRuleUiModel.Severity.LOW;
        } else if (checkedSeverity == R.id.chipSeverityHigh) {
            severity = ContentRuleUiModel.Severity.HIGH;
        }

        viewModel.addRule(term, kind, matchType, scopes, severity);
        ruleInput.setText("");
    }

    private void setupFaqList() {
        faqAdapter = new FaqAdapter();
        faqAdapter.setOnFaqClickListener(this::showFaqEditor);
        faqsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        faqsList.setAdapter(faqAdapter);

        emptyFaqs.setIcon(R.drawable.ic_ds_info);
        emptyFaqs.setTitle(R.string.hc_cat_faq_empty_title);
        emptyFaqs.setMessage(R.string.hc_cat_faq_empty_message);
        emptyFaqs.showAction(false);

        FloatingActionButton addFaq = requireView().findViewById(R.id.fabAddFaq);
        addFaq.setOnClickListener(v -> showFaqEditor(null));
    }

    private void setupDialogs() {
        getParentFragmentManager().setFragmentResultListener(CategoryFormDialog.REQUEST_KEY,
                getViewLifecycleOwner(), (requestKey, bundle) -> onCategorySaved(bundle));
        getParentFragmentManager().setFragmentResultListener(FaqEditorDialog.REQUEST_KEY,
                getViewLifecycleOwner(), (requestKey, bundle) -> {
                    viewModel.saveFaq(bundle.getLong(FaqEditorDialog.RESULT_ID),
                            bundle.getString(FaqEditorDialog.RESULT_TITLE),
                            bundle.getString(FaqEditorDialog.RESULT_CONTENT),
                            bundle.getBoolean(FaqEditorDialog.RESULT_VISIBLE));
                });
    }

    private void onCategorySaved(Bundle bundle) {
        long id = bundle.getLong(CategoryFormDialog.RESULT_ID);
        String name = bundle.getString(CategoryFormDialog.RESULT_NAME, "");
        long parentId = bundle.getLong(CategoryFormDialog.RESULT_PARENT_ID);
        int iconRes = bundle.getInt(CategoryFormDialog.RESULT_ICON);
        int colorRes = bundle.getInt(CategoryFormDialog.RESULT_COLOR);
        boolean active = bundle.getBoolean(CategoryFormDialog.RESULT_ACTIVE, true);

        CategoryUiModel current = findCategory(id);
        boolean hiding = current != null
                && current.status == CategoryUiModel.Status.ACTIVE && !active;
        if (!hiding) {
            viewModel.saveCategory(id, name, parentId, iconRes, colorRes, active);
            return;
        }

        String message = getString(R.string.hc_cat_hide_message, current.usageCount);
        int children = viewModel.childCount(id);
        if (children > 0) {
            message += "\n\n" + getString(R.string.hc_cat_hide_children, children);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.hc_cat_hide_title)
                .setMessage(message)
                .setNegativeButton(R.string.hc_cat_cancel, null)
                .setPositiveButton(R.string.hc_cat_hide_confirm, (dialog, which) ->
                        viewModel.saveCategory(id, name, parentId, iconRes, colorRes, false))
                .show();
    }

    private void showCategoryForm(@Nullable CategoryUiModel category) {
        long[] parentIds = new long[currentCategories.size()];
        String[] parentNames = new String[currentCategories.size()];
        int count = 0;
        for (CategoryUiModel item : currentCategories) {
            if (item.isRoot()) {
                parentIds[count] = item.id;
                parentNames[count] = item.name;
                count++;
            }
        }
        long[] ids = new long[count];
        String[] names = new String[count];
        System.arraycopy(parentIds, 0, ids, 0, count);
        System.arraycopy(parentNames, 0, names, 0, count);

        CategoryFormDialog dialog = category == null
                ? CategoryFormDialog.newInstance(0L, "", 0L, R.drawable.ic_ds_event,
                R.color.ds_primary, true, ids, names)
                : CategoryFormDialog.newInstance(category.id, category.name, category.parentId,
                category.iconRes, category.colorRes,
                category.status == CategoryUiModel.Status.ACTIVE, ids, names);
        dialog.show(getParentFragmentManager(), "category_form");
    }

    private void showFaqEditor(@Nullable FaqUiModel faq) {
        FaqEditorDialog dialog = faq == null
                ? FaqEditorDialog.newInstance(0L, "", "", true)
                : FaqEditorDialog.newInstance(faq.id, faq.title, faq.content, faq.visible);
        dialog.show(getParentFragmentManager(), "faq_editor");
    }

    private CategoryUiModel findCategory(long id) {
        for (CategoryUiModel item : currentCategories) {
            if (item.id == id) {
                return item;
            }
        }
        return null;
    }

    private void observe() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), items -> {
            currentCategories = items == null ? new ArrayList<>() : items;
            categoryAdapter.submitList(currentCategories);
            emptyCategories.setVisibility(currentCategories.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getKeywords().observe(getViewLifecycleOwner(), items -> {
            currentKeywords = items == null ? new ArrayList<>() : items;
            renderRules();
        });
        viewModel.getDomains().observe(getViewLifecycleOwner(), items -> {
            currentDomains = items == null ? new ArrayList<>() : items;
            renderRules();
        });
        viewModel.getFaqs().observe(getViewLifecycleOwner(), items -> {
            faqAdapter.submitList(items);
            emptyFaqs.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getActiveCategoryCount().observe(getViewLifecycleOwner(), active -> {
            Integer rules = viewModel.getRuleCount().getValue();
            renderCounts(active == null ? 0 : active, rules == null ? 0 : rules);
        });
        viewModel.getRuleCount().observe(getViewLifecycleOwner(), rules -> {
            Integer active = viewModel.getActiveCategoryCount().getValue();
            renderCounts(active == null ? 0 : active, rules == null ? 0 : rules);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = loading != null && loading;
            skeleton.setVisibility(busy && firstLoad ? View.VISIBLE : View.GONE);
            if (!busy) {
                firstLoad = false;
            }
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean failed = error != null && error;
            errorState.setVisibility(failed ? View.VISIBLE : View.GONE);
        });
        errorState.setOnRetryClickListener(v -> viewModel.retry());
        viewModel.getActionEvent().observe(getViewLifecycleOwner(), this::showActionMessage);
    }

    private void renderCounts(int activeCategories, int rules) {
        subtitle.setText(getString(R.string.hc_cat_count, activeCategories,
                CategoryManageViewModel.MAX_ACTIVE_CATEGORIES, rules,
                CategoryManageViewModel.MAX_CONTENT_RULES));
        ruleCountView.setText(getString(R.string.hc_cat_rule_count, rules,
                CategoryManageViewModel.MAX_CONTENT_RULES));
    }

    private boolean isKeywordKind() {
        return ruleKindGroup.getCheckedChipId() != R.id.chipRuleKindDomain;
    }

    private void renderRules() {
        List<ContentRuleUiModel> items =
                isKeywordKind() ? currentKeywords : currentDomains;
        ruleAdapter.submitList(items);
        emptyRules.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showActionMessage(CategoryManageViewModel.ActionEvent event) {
        if (event == null || event.action == null) {
            return;
        }
        if (event.message != null && !event.message.isEmpty()) {
            snackbar(event.message);
            return;
        }
        switch (event.action) {
            case SAVED:
                snackbar(getString(R.string.hc_cat_saved));
                break;
            case HIDDEN:
                snackbar(getString(R.string.hc_cat_hidden));
                break;
            case SHOWN:
                snackbar(getString(R.string.hc_cat_shown));
                break;
            case REORDERED:
                snackbar(getString(R.string.hc_cat_reordered));
                break;
            case DUPLICATE:
                snackbar(getString(R.string.hc_cat_duplicate));
                break;
            case RULE_ADDED:
                snackbar(getString(R.string.hc_cat_rule_added));
                break;
            case RULE_TOGGLED:
                break;
            case RULE_DELETED:
                Snackbar.make(requireView(), R.string.hc_cat_rule_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.hc_cat_rule_undo,
                                v -> viewModel.restoreRule(event.deletedRule))
                        .show();
                break;
            case RULE_RESTORED:
                snackbar(getString(R.string.hc_cat_rule_restored));
                break;
            case FAQ_SAVED:
                snackbar(getString(R.string.hc_cat_faq_saved));
                break;
            case FAQ_DELETED:
                break;
            default:
                break;
        }
    }

    private void snackbar(String message) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
    }
}
