package com.eventapp.app.ui.user.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.user.home.EventDetailViewModel;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Ticket option row for FR-U05. Values come from the detail view model, never from cache math. */
public class EventTicketAdapter extends RecyclerView.Adapter<EventTicketAdapter.TicketViewHolder> {

    public interface OnTicketSelectedListener {
        void onTicketSelected(int position);
    }

    private final List<EventDetailViewModel.TicketOption> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
    private int selectedIndex;
    private OnTicketSelectedListener listener;

    public void setOnTicketSelectedListener(OnTicketSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<EventDetailViewModel.TicketOption> tickets, int selectedIndex) {
        items.clear();
        if (tickets != null) {
            items.addAll(tickets);
        }
        this.selectedIndex = selectedIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_ticket_option, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        EventDetailViewModel.TicketOption ticket = items.get(position);
        Context context = holder.itemView.getContext();
        boolean selected = position == selectedIndex;

        holder.card.setChecked(selected);
        holder.card.setStrokeWidth(dp(context, selected ? 2 : 1));
        holder.card.setStrokeColor(selected
                ? ContextCompat.getColor(context, R.color.ds_primary)
                : ContextCompat.getColor(context, R.color.ds_outline));
        holder.radio.setChecked(selected);
        holder.name.setText(ticket.name);
        holder.description.setVisibility(ticket.description == null
                || ticket.description.trim().isEmpty() ? View.GONE : View.VISIBLE);
        holder.description.setText(ticket.description);
        holder.price.setText(ticket.price <= 0L
                ? context.getString(R.string.hc_detail_ticket_free)
                : PriceTextView.formatVnd(ticket.price));
        holder.saleWindow.setText(context.getString(R.string.hc_detail_ticket_sale_window,
                dateFormat.format(new Date(ticket.saleStart)),
                dateFormat.format(new Date(ticket.saleEnd))));
        bindStatus(holder.status, ticket.status);
        bindRemaining(holder.remaining, ticket);
        holder.card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTicketSelected(position);
            }
        });
        holder.itemView.setContentDescription(ticket.name + ", " + holder.price.getText());
    }

    private void bindStatus(StatusChip chip, EventDetailViewModel.TicketSaleStatus status) {
        Context context = chip.getContext();
        switch (status) {
            case NOT_STARTED:
                chip.setStatus(context.getString(R.string.hc_detail_ticket_status_not_started),
                        StatusChip.Tone.WARNING);
                break;
            case ENDED:
                chip.setStatus(context.getString(R.string.hc_detail_ticket_status_ended),
                        StatusChip.Tone.MUTED);
                break;
            case SOLD_OUT:
                chip.setStatus(context.getString(R.string.hc_detail_ticket_status_sold_out),
                        StatusChip.Tone.DANGER);
                break;
            case ON_SALE:
            default:
                chip.setStatus(context.getString(R.string.hc_detail_ticket_status_on_sale),
                        StatusChip.Tone.SUCCESS);
                break;
        }
    }

    private void bindRemaining(TextView view, EventDetailViewModel.TicketOption ticket) {
        if (ticket.status == EventDetailViewModel.TicketSaleStatus.SOLD_OUT) {
            view.setText(R.string.hc_detail_ticket_sold_out);
            view.setVisibility(View.VISIBLE);
            return;
        }
        if (ticket.status == EventDetailViewModel.TicketSaleStatus.ENDED) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        if (ticket.remaining <= 10) {
            view.setText(R.string.hc_detail_ticket_few);
        } else {
            view.setText(view.getContext().getString(
                    R.string.hc_detail_ticket_remaining, ticket.remaining));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final RadioButton radio;
        final TextView name;
        final TextView description;
        final TextView saleWindow;
        final TextView remaining;
        final TextView price;
        final StatusChip status;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            radio = itemView.findViewById(R.id.radioDetailTicket);
            name = itemView.findViewById(R.id.tvDetailTicketName);
            description = itemView.findViewById(R.id.tvDetailTicketDescription);
            saleWindow = itemView.findViewById(R.id.tvDetailTicketSaleWindow);
            remaining = itemView.findViewById(R.id.tvDetailTicketRemaining);
            price = itemView.findViewById(R.id.tvDetailTicketPrice);
            status = itemView.findViewById(R.id.chipDetailTicketStatus);
        }
    }
}
