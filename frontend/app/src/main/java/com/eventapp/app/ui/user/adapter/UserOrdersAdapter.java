package com.eventapp.app.ui.user.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.data.remote.dto.order.OrderDtos;
import com.eventapp.app.databinding.ItemUserOrderBinding;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

public class UserOrdersAdapter extends RecyclerView.Adapter<UserOrdersAdapter.ViewHolder> {

    public interface Listener {
        void onOrderClick(OrderDtos.Order order);
        void onViewTicketsClick(OrderDtos.Order order);
        void onContinuePaymentClick(OrderDtos.Order order);
        void onCancelOrderClick(OrderDtos.Order order);
    }

    private final Listener listener;
    private final List<OrderDtos.Order> items = new ArrayList<>();

    public UserOrdersAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<OrderDtos.Order> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserOrderBinding binding = ItemUserOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserOrderBinding binding;

        ViewHolder(ItemUserOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(OrderDtos.Order order) {
            Context ctx = itemView.getContext();
            binding.tvOrderCode.setText(order.orderCode != null ? order.orderCode : ("Đơn #" + order.orderId));
            binding.tvEventTitle.setText(order.eventTitle != null && !order.eventTitle.isEmpty()
                    ? order.eventTitle : "Đơn vé sự kiện");

            String loc = (order.venueName != null ? order.venueName : "")
                    + (order.city != null && !order.city.isEmpty() ? (" • " + order.city) : "");
            StringBuilder metaText = new StringBuilder(loc.isEmpty() ? "Đơn đặt vé" : loc);
            if (order.paymentStatus != null && !order.paymentStatus.isEmpty()) {
                metaText.append(" · TT thanh toán: ").append(order.paymentStatus);
            } else if (order.paymentMethod != null && !order.paymentMethod.isEmpty()) {
                metaText.append(" · ").append(order.paymentMethod);
            }
            if (order.payments != null && !order.payments.isEmpty()) {
                int failedCount = 0;
                for (OrderDtos.PaymentAttempt p : order.payments) {
                    if ("FAILED".equalsIgnoreCase(p.status)) {
                        failedCount++;
                    }
                }
                if (failedCount > 0) {
                    metaText.append(" (").append(failedCount).append(" GD lỗi)");
                }
            }
            binding.tvEventMeta.setText(metaText.toString());

            int qty = order.ticketsIssued > 0 ? order.ticketsIssued : 1;
            if (order.items != null && !order.items.isEmpty()) {
                int sum = 0;
                for (OrderDtos.Item it : order.items) sum += it.quantity;
                if (sum > 0) qty = sum;
            }
            binding.tvTicketCount.setText("Số lượng: " + qty + " vé");

            double total = order.totalAmount != null ? order.totalAmount.doubleValue() : 0.0;
            binding.tvTotalAmount.setText(UserFmt.amount((long) total));

            binding.btnPayNow.setVisibility(View.GONE);
            binding.btnCancelOrder.setVisibility(View.GONE);
            binding.btnViewTickets.setVisibility(View.GONE);

            String status = order.status != null ? order.status.toUpperCase() : "PAID";
            switch (status) {
                case "PAID":
                case "COMPLETED":
                    binding.tvOrderStatus.setText("ĐÃ THANH TOÁN");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_success);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_success_text));
                    binding.btnViewTickets.setVisibility(View.VISIBLE);
                    break;
                case "PENDING_PAYMENT":
                case "PENDING":
                    binding.tvOrderStatus.setText("CHỜ THANH TOÁN");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_warning);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_warning_text));
                    binding.btnPayNow.setVisibility(View.VISIBLE);
                    binding.btnCancelOrder.setVisibility(View.VISIBLE);
                    break;
                case "REFUND_PENDING":
                    binding.tvOrderStatus.setText("CHỜ HOÀN TIỀN");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_warning);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_warning_text));
                    break;
                case "REFUNDED":
                    binding.tvOrderStatus.setText("ĐÃ HOÀN TIỀN");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_info);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_primary));
                    break;
                case "EXPIRED":
                    binding.tvOrderStatus.setText("HẾT HẠN");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_danger);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_danger));
                    break;
                case "CANCELLED":
                    binding.tvOrderStatus.setText("ĐÃ HỦY");
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_danger);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_danger));
                    break;
                default:
                    binding.tvOrderStatus.setText(status);
                    binding.tvOrderStatus.setBackgroundResource(R.drawable.bg_user_badge_neutral);
                    binding.tvOrderStatus.setTextColor(ctx.getColor(R.color.user_text_dim));
                    break;
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOrderClick(order);
            });
            binding.btnViewTickets.setOnClickListener(v -> {
                if (listener != null) listener.onViewTicketsClick(order);
            });
            binding.btnPayNow.setOnClickListener(v -> {
                if (listener != null) listener.onContinuePaymentClick(order);
            });
            binding.btnCancelOrder.setOnClickListener(v -> {
                if (listener != null) listener.onCancelOrderClick(order);
            });
        }
    }
}
