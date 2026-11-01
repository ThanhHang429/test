package com.eventapp.app.ui.common.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.TicketUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Danh sách vé của người dùng (FR-U14) dùng chung cho cả tab sắp diễn ra và đã qua. */
public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(TicketUiModel ticket);
    }

    private static final String[] WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN"));
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN"));

    private final List<TicketUiModel> items = new ArrayList<>();

    @Nullable
    private OnTicketClickListener listener;

    public void setOnTicketClickListener(@Nullable OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@Nullable List<TicketUiModel> tickets) {
        items.clear();
        if (tickets != null) {
            items.addAll(tickets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketUiModel ticket = items.get(position);
        holder.bind(ticket);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTicketClick(ticket);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {

        private final TextView day;
        private final TextView month;
        private final TextView title;
        private final TextView time;
        private final TextView venue;
        private final TextView meta;
        private final StatusChip status;
        private final PriceTextView price;

        TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.tvTicketDay);
            month = itemView.findViewById(R.id.tvTicketMonth);
            title = itemView.findViewById(R.id.tvTicketTitle);
            time = itemView.findViewById(R.id.tvTicketTime);
            venue = itemView.findViewById(R.id.tvTicketVenue);
            meta = itemView.findViewById(R.id.tvTicketMeta);
            status = itemView.findViewById(R.id.chipTicketStatus);
            price = itemView.findViewById(R.id.tvTicketPrice);
        }

        void bind(TicketUiModel ticket) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(ticket.startAt);

            day.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            month.setText(itemView.getContext()
                    .getString(R.string.hc_tickets_month, calendar.get(Calendar.MONTH) + 1));
            title.setText(ticket.eventTitle);
            time.setText(formatTime(ticket.startAt));
            venue.setText(ticket.venue);
            meta.setText(ticket.ticketType.isEmpty()
                    ? itemView.getContext().getString(R.string.hc_tickets_meta_no_type,
                    ticket.quantity)
                    : itemView.getContext().getString(R.string.hc_tickets_meta,
                    ticket.quantity, ticket.ticketType));
            bindStatus(ticket.status);

            if (ticket.totalPrice <= 0L) {
                price.setFree();
            } else {
                price.setPrice(ticket.totalPrice);
            }
            itemView.setContentDescription(ticket.eventTitle + ", " + time.getText());
        }

        private void bindStatus(TicketUiModel.Status status) {
            switch (status) {
                case PAST:
                    this.status.setStatus(
                            itemView.getContext().getString(R.string.hc_tickets_status_past),
                            StatusChip.Tone.MUTED);
                    break;
                case CANCELLED:
                    this.status.setStatus(
                            itemView.getContext().getString(R.string.hc_tickets_status_cancelled),
                            StatusChip.Tone.DANGER);
                    break;
                case UPCOMING:
                default:
                    this.status.setStatus(
                            itemView.getContext().getString(R.string.hc_tickets_status_upcoming),
                            StatusChip.Tone.PRIMARY);
                    break;
            }
        }

        /** Định dạng "T7, 16/11 • 19:00" — thống nhất với card ở trang Khám phá. */
        private String formatTime(long startAt) {
            Calendar event = Calendar.getInstance();
            event.setTimeInMillis(startAt);
            String weekday = WEEKDAYS[event.get(Calendar.DAY_OF_WEEK) - 1];
            Date date = new Date(startAt);
            return weekday + ", " + DATE_FORMAT.format(date) + " • " + TIME_FORMAT.format(date);
        }
    }
}
