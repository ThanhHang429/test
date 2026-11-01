package com.eventapp.app.ui.admin.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.FaqUiModel;

import java.util.ArrayList;
import java.util.List;

/** FR-A09 guidance content row; tapping opens the editor. */
public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    public interface OnFaqClickListener {
        void onFaqClick(FaqUiModel faq);
    }

    private final List<FaqUiModel> items = new ArrayList<>();
    private OnFaqClickListener listener;

    public void setOnFaqClickListener(OnFaqClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FaqUiModel> next) {
        List<FaqUiModel> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
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
                FaqUiModel oldItem = items.get(oldPosition);
                FaqUiModel newItem = incoming.get(newPosition);
                return oldItem.title.equals(newItem.title)
                        && oldItem.content.equals(newItem.content)
                        && oldItem.visible == newItem.visible;
            }
        });
        items.clear();
        items.addAll(incoming);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public FaqViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq, parent, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaqViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FaqViewHolder extends RecyclerView.ViewHolder {

        private final TextView order;
        private final TextView title;
        private final TextView preview;
        private final StatusChip status;

        FaqViewHolder(@NonNull View itemView) {
            super(itemView);
            order = itemView.findViewById(R.id.tvFaqOrder);
            title = itemView.findViewById(R.id.tvFaqTitle);
            preview = itemView.findViewById(R.id.tvFaqPreview);
            status = itemView.findViewById(R.id.chipFaqStatus);
        }

        void bind(FaqUiModel item) {
            order.setText(itemView.getContext().getString(R.string.hc_cat_faq_order, item.sortOrder));
            title.setText(item.title);
            preview.setText(item.content);
            status.setStatus(
                    itemView.getContext().getString(item.visible
                            ? R.string.hc_cat_faq_shown
                            : R.string.hc_cat_faq_hidden),
                    item.visible ? StatusChip.Tone.SUCCESS : StatusChip.Tone.MUTED);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFaqClick(item);
                }
            });
        }
    }
}
