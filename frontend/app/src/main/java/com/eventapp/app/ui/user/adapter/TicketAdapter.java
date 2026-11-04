package com.eventapp.app.ui.user.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ItemUserTicketBinding;
import com.eventapp.app.ui.user.model.DemoTicket;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

/** Danh sách vé trong tab Vé của tôi. */
public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull DemoTicket ticket);
    }

    private final Listener listener;
    private final List<DemoTicket> items = new ArrayList<>();

    public TicketAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<DemoTicket> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemUserTicketBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ItemUserTicketBinding b = holder.binding;
        DemoTicket ticket = items.get(position);
        Context context = b.getRoot().getContext();

        b.cover.setBackgroundResource(ticket.event.coverRes);
        b.ivIcon.setImageResource(ticket.event.iconRes);
        b.tvCode.setText(ticket.code);
        b.tvDate.setText(UserFmt.dateShort(ticket.event.startAt) + " • "
                + UserFmt.time(ticket.event.startAt));
        b.tvTitle.setText(ticket.event.title);
        b.tvVenue.setText(ticket.event.venue);
        b.tvType.setText(ticket.typeName + " × " + ticket.quantity);
        b.tvPrice.setText(UserFmt.money(ticket.total));

        bindStatus(context, b.tvStatus, ticket);

        boolean live = ticket.status == DemoTicket.Status.UPCOMING;
        b.btnOpen.setText(live ? R.string.user_view_ticket : R.string.user_view_ticket_detail);
        b.btnOpen.setAlpha(live ? 1f : 0.72f);

        if (ticket.status == DemoTicket.Status.CANCELLED) {
            b.tvPrice.setPaintFlags(b.tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            b.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.user_text_dim));
        } else {
            b.tvPrice.setPaintFlags(b.tvPrice.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            b.tvPrice.setTextColor(ContextCompat.getColor(context, R.color.user_primary));
        }

        View.OnClickListener open = v -> listener.onOpen(ticket);
        b.getRoot().setOnClickListener(open);
        b.btnOpen.setOnClickListener(open);
    }

    private void bindStatus(Context context, android.widget.TextView view, DemoTicket ticket) {
        switch (ticket.status) {
            case USED:
                view.setText(R.string.user_ticket_status_used);
                view.setBackgroundResource(R.drawable.bg_user_badge_neutral);
                view.setTextColor(ContextCompat.getColor(context, R.color.user_text_muted));
                break;
            case CANCELLED:
                view.setText(R.string.user_ticket_status_cancelled);
                view.setBackgroundResource(R.drawable.bg_user_badge_danger);
                view.setTextColor(ContextCompat.getColor(context, R.color.user_danger_text));
                break;
            default:
                view.setText(R.string.user_ticket_status_valid);
                view.setBackgroundResource(R.drawable.bg_user_badge_success);
                view.setTextColor(ContextCompat.getColor(context, R.color.user_success_text));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemUserTicketBinding binding;

        Holder(ItemUserTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
