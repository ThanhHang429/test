package com.eventapp.app.ui.user.order;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.fake.SampleOrderFactory;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

/** Danh sách loại vé có thể chọn ở màn thanh toán (UI demo, dữ liệu từ factory). */
public final class TicketOptionAdapter extends RecyclerView.Adapter<TicketOptionAdapter.Holder> {

    interface OnTicketSelectedListener {
        void onTicketSelected(int position);
    }

    private final List<SampleOrderFactory.TicketOption> items = new ArrayList<>();
    private int selectedIndex = 0;
    private OnTicketSelectedListener listener;

    void submit(List<SampleOrderFactory.TicketOption> options, int selected) {
        items.clear();
        if (options != null) {
            items.addAll(options);
        }
        selectedIndex = Math.max(0, Math.min(selected, items.size() - 1));
        notifyDataSetChanged();
    }

    void setSelectedIndex(int index) {
        int next = Math.max(0, Math.min(index, items.size() - 1));
        if (next == selectedIndex) {
            return;
        }
        int previous = selectedIndex;
        selectedIndex = next;
        notifyItemChanged(previous);
        notifyItemChanged(selectedIndex);
    }

    void setOnTicketSelectedListener(OnTicketSelectedListener value) {
        listener = value;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_ticket_option, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SampleOrderFactory.TicketOption option = items.get(position);
        boolean isSelected = position == selectedIndex;

        holder.name.setText(option.name);
        if (option.description == null || option.description.trim().isEmpty()) {
            holder.description.setVisibility(View.GONE);
        } else {
            holder.description.setVisibility(View.VISIBLE);
            holder.description.setText(option.description);
        }
        holder.remaining.setText(holder.itemView.getContext()
                .getString(R.string.hc_checkout_remaining, option.remaining));
        holder.remaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                option.remaining <= 10 ? R.color.ds_warning_dark : R.color.ds_text_muted));
        holder.price.setPrice(option.price);
        holder.radio.setChecked(isSelected);
        holder.card.setChecked(isSelected);
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.hc_checkout_ticket_a11y, option.name,
                PriceTextView.formatVnd(option.price),
                holder.itemView.getContext().getString(R.string.hc_checkout_remaining,
                        option.remaining)));
        holder.itemView.setOnClickListener(v -> {
            int current = holder.getBindingAdapterPosition();
            if (current == RecyclerView.NO_POSITION || listener == null) {
                return;
            }
            listener.onTicketSelected(current);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final MaterialRadioButton radio;
        final TextView name;
        final TextView description;
        final TextView remaining;
        final PriceTextView price;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardOrderTicket);
            radio = itemView.findViewById(R.id.radioOrderTicket);
            name = itemView.findViewById(R.id.tvOrderTicketName);
            description = itemView.findViewById(R.id.tvOrderTicketDescription);
            remaining = itemView.findViewById(R.id.tvOrderTicketRemaining);
            price = itemView.findViewById(R.id.tvOrderTicketPrice);
        }
    }
}
