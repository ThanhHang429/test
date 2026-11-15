package com.eventapp.app.ui.user.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.data.remote.dto.wallet.WalletTransactionDto;
import com.eventapp.app.databinding.ItemWalletTransactionBinding;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private final List<WalletTransactionDto> items = new ArrayList<>();

    public void submitList(List<WalletTransactionDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalletTransactionBinding binding = ItemWalletTransactionBinding.inflate(
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemWalletTransactionBinding binding;

        public ViewHolder(@NonNull ItemWalletTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WalletTransactionDto item) {
            boolean isIncoming = "IN".equalsIgnoreCase(item.direction);

            // Tiêu đề giao dịch
            if (item.note != null && !item.note.isEmpty()) {
                binding.tvTxTitle.setText(item.note);
            } else if ("DEPOSIT".equalsIgnoreCase(item.transactionType)) {
                binding.tvTxTitle.setText("Nạp tiền vào ví");
            } else if ("PAYMENT".equalsIgnoreCase(item.transactionType)) {
                binding.tvTxTitle.setText("Thanh toán vé đơn hàng");
            } else {
                binding.tvTxTitle.setText("Giao dịch ví");
            }

            // Thời gian
            binding.tvTxTime.setText(item.createdAt != null ? item.createdAt.replace("T", " ").substring(0, Math.min(16, item.createdAt.length())) : "");

            // Số tiền và màu sắc
            long amountVal = Math.round(item.amount);
            if (isIncoming) {
                binding.tvTxAmount.setText("+" + UserFmt.amount(amountVal));
                binding.tvTxAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.user_success));
                binding.ivTxIcon.setImageResource(R.drawable.ic_common_chevron_down);
                binding.ivTxIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.user_success));
            } else {
                binding.tvTxAmount.setText("-" + UserFmt.amount(amountVal));
                binding.tvTxAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.user_danger));
                binding.ivTxIcon.setImageResource(R.drawable.ic_common_chevron_up);
                binding.ivTxIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.user_danger));
            }

            // Số dư sau giao dịch
            long balanceAfterVal = Math.round(item.balanceAfter);
            binding.tvTxBalanceAfter.setText("Số dư: " + UserFmt.amount(balanceAfterVal));
        }
    }
}
