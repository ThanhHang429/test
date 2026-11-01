package com.eventapp.app.ui.admin.category.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.CategoryUiModel;

import java.util.ArrayList;
import java.util.List;

/** FR-A09 category tree row: drag handle, icon, name/slug, usage and status. */
public class CategoryTreeAdapter
        extends RecyclerView.Adapter<CategoryTreeAdapter.CategoryViewHolder> {

    public interface OnCategoryActionListener {
        void onEdit(CategoryUiModel category);

        void onStartDrag(RecyclerView.ViewHolder holder);
    }

    private final List<CategoryUiModel> items = new ArrayList<>();
    private OnCategoryActionListener listener;

    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CategoryUiModel> next) {
        List<CategoryUiModel> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return incoming.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return items.get(oldPosition).id == incoming.get(newPosition).id;
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                CategoryUiModel oldItem = items.get(oldPosition);
                CategoryUiModel newItem = incoming.get(newPosition);
                return oldItem.name.equals(newItem.name)
                        && oldItem.parentId == newItem.parentId
                        && oldItem.status == newItem.status
                        && oldItem.usageCount == newItem.usageCount
                        && oldItem.iconRes == newItem.iconRes
                        && oldItem.colorRes == newItem.colorRes;
            }
        });
        items.clear();
        items.addAll(incoming);
        diff.dispatchUpdatesTo(this);
    }

    public CategoryUiModel getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final ImageView dragHandle;
        private final ImageView icon;
        private final TextView name;
        private final TextView slug;
        private final TextView usage;
        private final StatusChip status;
        private final ImageButton edit;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.ivCategoryDrag);
            icon = itemView.findViewById(R.id.ivCategoryIcon);
            name = itemView.findViewById(R.id.tvCategoryName);
            slug = itemView.findViewById(R.id.tvCategorySlug);
            usage = itemView.findViewById(R.id.tvCategoryUsage);
            status = itemView.findViewById(R.id.chipCategoryStatus);
            edit = itemView.findViewById(R.id.btnCategoryEdit);
        }

        // Tay nắm kéo không phải vùng bấm: listener chỉ bắt ACTION_DOWN để bắt đầu kéo và
        // không tiêu thụ sự kiện, nên không cần performClick.
        @SuppressLint("ClickableViewAccessibility")
        void bind(CategoryUiModel item) {
            int indent = item.isRoot() ? 8 : 36;
            float density = itemView.getResources().getDisplayMetrics().density;
            itemView.setPadding(Math.round(indent * density), 0,
                    Math.round(8 * density), 0);

            dragHandle.setVisibility(item.isRoot() ? View.VISIBLE : View.INVISIBLE);
            icon.setImageResource(item.iconRes);
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(itemView.getContext(),
                            item.colorRes)));
            name.setText(item.name);
            slug.setText(itemView.getContext().getString(R.string.hc_cat_slug, item.slug));
            usage.setText(itemView.getContext().getString(R.string.hc_cat_usage, item.usageCount));
            status.setStatus(
                    itemView.getContext().getString(item.status == CategoryUiModel.Status.ACTIVE
                            ? R.string.hc_cat_status_active
                            : R.string.hc_cat_status_hidden),
                    item.status == CategoryUiModel.Status.ACTIVE
                            ? StatusChip.Tone.SUCCESS
                            : StatusChip.Tone.MUTED);
            edit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(item);
                }
            });
            dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                        && listener != null && item.isRoot()) {
                    listener.onStartDrag(CategoryViewHolder.this);
                }
                return false;
            });
        }
    }
}
