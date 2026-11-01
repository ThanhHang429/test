package com.eventapp.app.ui.admin.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.SeverityChip;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.ContentRuleUiModel;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

/** FR-A09 banned keyword / suspicious domain row with an on-off switch. */
public class ContentRuleAdapter
        extends RecyclerView.Adapter<ContentRuleAdapter.RuleViewHolder> {

    public interface OnRuleActionListener {
        void onToggle(ContentRuleUiModel rule, boolean active);

        void onDelete(ContentRuleUiModel rule);
    }

    private final List<ContentRuleUiModel> items = new ArrayList<>();
    private OnRuleActionListener listener;

    public void setOnRuleActionListener(OnRuleActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ContentRuleUiModel> next) {
        List<ContentRuleUiModel> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
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
                ContentRuleUiModel oldItem = items.get(oldPosition);
                ContentRuleUiModel newItem = incoming.get(newPosition);
                return oldItem.term.equals(newItem.term)
                        && oldItem.active == newItem.active
                        && oldItem.severity == newItem.severity
                        && oldItem.scopes.equals(newItem.scopes);
            }
        });
        items.clear();
        items.addAll(incoming);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content_rule, parent, false);
        return new RuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class RuleViewHolder extends RecyclerView.ViewHolder {

        private final TextView term;
        private final TextView meta;
        private final StatusChip severity;
        private final MaterialSwitch active;
        private final ImageButton delete;

        RuleViewHolder(@NonNull View itemView) {
            super(itemView);
            term = itemView.findViewById(R.id.tvRuleTerm);
            meta = itemView.findViewById(R.id.tvRuleMeta);
            severity = itemView.findViewById(R.id.chipRuleSeverity);
            active = itemView.findViewById(R.id.switchRuleActive);
            delete = itemView.findViewById(R.id.btnRuleDelete);
        }

        void bind(ContentRuleUiModel item) {
            term.setText(item.term);

            String kind = itemView.getContext().getString(
                    item.kind == ContentRuleUiModel.Kind.DOMAIN
                            ? R.string.hc_cat_kind_domain
                            : R.string.hc_cat_kind_keyword);
            String scopes = item.scopesLabel();
            meta.setText(scopes.isEmpty() ? kind : kind + " · " + scopes);
            SeverityChip.bind(severity, item.severity.name());

            active.setOnCheckedChangeListener(null);
            active.setChecked(item.active);
            active.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggle(item, isChecked);
                }
            });

            delete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(item);
                }
            });
        }
    }
}
