package com.eventapp.app.ui.organizer.tools;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.model.DashboardUiModel;
import com.eventapp.app.ui.organizer.event.OrganizerEventDetailFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FR-O06 / O09 / O27 — màn "Công cụ" của khu nhà tổ chức.
 *
 * <p>Hai lối vào dùng API thật: trợ lý AI viết mô tả (cần tên + danh mục + 3 điểm nổi bật) và
 * phân tích/phân loại một sự kiện có trong tổng quan 30 ngày gần nhất.</p>
 */
public class OrganizerToolsFragment extends Fragment {

    private OrganizerToolsViewModel viewModel;
    private ToolEventAdapter adapter;
    private LinearProgressIndicator progress;
    private TextView errorView;
    private TextView emptyView;
    private View retryButton;

    private List<CatalogDtos.CategoryNode> categoryTree = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_organizer_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OrganizerToolsViewModel.class);

        progress = view.findViewById(R.id.progressTools);
        errorView = view.findViewById(R.id.tvToolsError);
        emptyView = view.findViewById(R.id.tvToolsEmpty);
        retryButton = view.findViewById(R.id.btnToolsRetry);

        RecyclerView list = view.findViewById(R.id.rvToolsEvents);
        adapter = new ToolEventAdapter();
        adapter.setOnEventListener(event -> openEvent(event.id, event.title));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setItemAnimator(null);
        list.setAdapter(adapter);

        retryButton.setOnClickListener(v -> viewModel.refresh());
        view.findViewById(R.id.btnToolsAiDescription)
                .setOnClickListener(v -> openBriefDialog());

        observe();
    }

    private void observe() {
        viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
            adapter.submitList(events);
            boolean empty = events == null || events.isEmpty();
            emptyView.setVisibility(empty && !Boolean.TRUE.equals(viewModel.getLoading().getValue())
                    ? View.VISIBLE : View.GONE);
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(),
                nodes -> categoryTree = nodes == null ? new ArrayList<>() : nodes);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            boolean failed = Boolean.TRUE.equals(error);
            errorView.setVisibility(failed ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(failed ? View.VISIBLE : View.GONE);
        });
    }

    private void openEvent(long eventId, @Nullable String title) {
        Bundle args = new Bundle();
        args.putLong(OrganizerEventDetailFragment.ARG_EVENT_ID, eventId);
        args.putString(OrganizerEventDetailFragment.ARG_EVENT_TITLE, title);
        navigate(R.id.organizer_event_detailFragment, args);
    }

    /** Hộp thoại nhập brief rồi mở màn AI viết mô tả với dữ liệu hợp lệ. */
    private void openBriefDialog() {
        if (categoryTree.isEmpty()) {
            Snackbar.make(requireView(), R.string.org_tools_error, Snackbar.LENGTH_SHORT).show();
            return;
        }
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_ai_brief, null, false);
        ChipGroup categories = content.findViewById(R.id.chipGroupBriefCategory);
        TextView error = content.findViewById(R.id.tvBriefError);
        TextInputEditText titleInput = content.findViewById(R.id.etBriefTitle);
        TextInputEditText pointsInput = content.findViewById(R.id.etBriefPoints);
        TextInputEditText audienceInput = content.findViewById(R.id.etBriefAudience);

        List<CatalogDtos.CategoryNode> flat = flatten(categoryTree);
        for (CatalogDtos.CategoryNode node : flat) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(node.name);
            chip.setTag(node.id);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(com.eventapp.app.R.color.ds_chip_bg);
            categories.addView(chip);
        }
        if (categories.getChildCount() > 0) {
            ((Chip) categories.getChildAt(0)).setChecked(true);
        }

        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setView(content)
                        .setNegativeButton(R.string.org_ai_brief_cancel, null)
                        .setPositiveButton(R.string.org_ai_brief_generate, null)
                        .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = textOf(titleInput);
                    List<String> points = splitPoints(textOf(pointsInput));
                    int checkedId = categories.getCheckedChipId();
                    Long categoryId = null;
                    if (checkedId != View.NO_ID) {
                        Chip chip = categories.findViewById(checkedId);
                        if (chip != null && chip.getTag() instanceof Long) {
                            categoryId = (Long) chip.getTag();
                        }
                    }
                    String problem = validate(title, categoryId, points);
                    if (problem != null) {
                        error.setText(problem);
                        error.setVisibility(View.VISIBLE);
                        return;
                    }
                    dialog.dismiss();
                    Bundle args = new Bundle();
                    args.putString("arg_event_title", title);
                    args.putLong("arg_category_id", categoryId);
                    args.putString("arg_format", "OFFLINE");
                    args.putStringArray("arg_key_points", points.toArray(new String[0]));
                    args.putString("arg_audience", textOf(audienceInput));
                    navigate(R.id.ai_descriptionFragment, args);
                }));

        dialog.show();
    }

    @Nullable
    private String validate(@NonNull String title, @Nullable Long categoryId,
                            @NonNull List<String> points) {
        if (title.length() < 5) {
            return getString(R.string.org_ai_brief_missing_title_value);
        }
        if (categoryId == null || categoryId <= 0L) {
            return getString(R.string.org_ai_brief_missing_category);
        }
        if (points.size() < 3) {
            return getString(R.string.org_ai_brief_missing_points);
        }
        return null;
    }

    @NonNull
    private static List<CatalogDtos.CategoryNode> flatten(
            @NonNull List<CatalogDtos.CategoryNode> nodes) {
        List<CatalogDtos.CategoryNode> values = new ArrayList<>();
        for (CatalogDtos.CategoryNode node : nodes) {
            if (node == null || node.name == null) {
                continue;
            }
            values.add(node);
            if (node.children != null) {
                for (CatalogDtos.CategoryNode child : node.children) {
                    if (child != null && child.name != null) {
                        values.add(child);
                    }
                }
            }
        }
        return values;
    }

    @NonNull
    private static List<String> splitPoints(@NonNull String raw) {
        List<String> values = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String value = line.trim();
            if (!value.isEmpty() && value.length() >= 3) {
                values.add(value.length() > 200 ? value.substring(0, 200) : value);
            }
            if (values.size() >= 10) {
                break;
            }
        }
        return values;
    }

    @NonNull
    private static String textOf(@Nullable TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private void navigate(int destination, @Nullable Bundle args) {
        try {
            if (args == null) {
                Navigation.findNavController(requireView()).navigate(destination);
            } else {
                Navigation.findNavController(requireView()).navigate(destination, args);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Snackbar.make(requireView(), R.string.org_tools_error, Snackbar.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ adapter

    private static final class ToolEventAdapter
            extends RecyclerView.Adapter<ToolEventAdapter.Holder> {

        interface OnEventListener {
            void onEvent(DashboardUiModel.TopEvent event);
        }

        private final List<DashboardUiModel.TopEvent> items = new ArrayList<>();
        private OnEventListener listener;

        void setOnEventListener(OnEventListener listener) {
            this.listener = listener;
        }

        void submitList(@Nullable List<DashboardUiModel.TopEvent> values) {
            items.clear();
            if (values != null) {
                items.addAll(values);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_org_tool_event, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            DashboardUiModel.TopEvent event = items.get(position);
            holder.title.setText(event.title);
            holder.meta.setText(String.format(Locale.US, "%d đăng ký · %s",
                    event.registrations, PriceTextView.formatVnd(event.verifiedRevenue)));
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEvent(event);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView meta;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tvToolEventTitle);
                meta = itemView.findViewById(R.id.tvToolEventMeta);
            }
        }
    }
}
