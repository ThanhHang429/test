package com.eventapp.app.ui.organizer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.databinding.ItemOrgOrderBinding;
import com.eventapp.app.ui.organizer.model.OrgOrder;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;

import java.util.ArrayList;
import java.util.List;

/** Danh sách đơn hàng của nhà tổ chức. */
public class OrgOrderAdapter extends RecyclerView.Adapter<OrgOrderAdapter.VH> {

    public interface Listener {
        void onOpen(OrgOrder order);
    }

    private final List<OrgOrder> items = new ArrayList<>();
    private final Listener listener;

    public OrgOrderAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(List<OrgOrder> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrgOrderBinding binding = ItemOrgOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class VH extends RecyclerView.ViewHolder {

        private final ItemOrgOrderBinding binding;

        VH(ItemOrgOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrgOrder order) {
            binding.tvOrderCode.setText("#" + order.code);
            binding.tvOrderAmount.setText(order.amount > 0
                    ? OrgFormat.money(order.amount) : "Miễn phí");
            binding.tvOrderStatus.setText(order.statusLabel);
            OrgUi.applyPill(binding.tvOrderStatus, order.tone);
            binding.tvOrderTime.setText(order.time);
            binding.tvBuyer.setText(order.buyer);
            binding.tvOrderEvent.setText(order.event);
            binding.tvTicketType.setText(order.ticketType);
            binding.tvPayment.setText(order.payment);
            binding.getRoot().setOnClickListener(v -> listener.onOpen(order));
        }
    }
}
