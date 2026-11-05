package com.eventapp.app.ui.organizer.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.databinding.ItemOrgEventBinding;
import com.eventapp.app.ui.organizer.model.OrgEvent;
import com.eventapp.app.ui.organizer.util.OrgFormat;
import com.eventapp.app.ui.organizer.util.OrgUi;

import java.util.ArrayList;
import java.util.List;

/** Danh sách sự kiện của nhà tổ chức. */
public class OrgEventAdapter extends RecyclerView.Adapter<OrgEventAdapter.VH> {

    public interface Listener {
        void onOpen(OrgEvent event);

        void onManageTickets(OrgEvent event);
    }

    private final List<OrgEvent> items = new ArrayList<>();
    private final Listener listener;

    public OrgEventAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(List<OrgEvent> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOrgEventBinding binding = ItemOrgEventBinding.inflate(
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

        private final ItemOrgEventBinding binding;

        VH(ItemOrgEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrgEvent event) {
            binding.cover.setBackgroundResource(event.coverRes());
            binding.tvCategory.setText(event.code);
            binding.tvStatus.setText(event.statusLabel());
            OrgUi.applyPill(binding.tvStatus, event.tone());
            binding.tvTitle.setText(event.title);
            binding.tvMeta.setText(OrgFormat.dateRange(event.startAt, event.endAt));
            binding.tvLocation.setText(OrgFormat.location(event.venueName, event.city));
            binding.tvSold.setText(OrgFormat.count(event.ticketsSold) + " / "
                    + OrgFormat.count(event.totalTickets()) + " vé");
            binding.tvRevenue.setText(event.revenue > 0
                    ? OrgFormat.compactMoney(event.revenue) : "—");
            binding.tvPercent.setText(event.percent() + "%");
            binding.progress.setProgressCompat(event.percent(), false);

            binding.getRoot().setOnClickListener(v -> listener.onOpen(event));
            binding.tvOpenDetail.setOnClickListener(v -> listener.onOpen(event));
            binding.tvManageTickets.setOnClickListener(v -> listener.onManageTickets(event));
        }
    }
}
