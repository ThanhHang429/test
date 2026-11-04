package com.eventapp.app.ui.user.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.databinding.ItemUserNotificationBinding;
import com.eventapp.app.ui.user.model.DemoNotification;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

/** Danh sách thông báo trong trung tâm thông báo. */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull DemoNotification notification);
    }

    private final Listener listener;
    private final List<DemoNotification> items = new ArrayList<>();

    public NotificationAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<DemoNotification> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemUserNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ItemUserNotificationBinding b = holder.binding;
        DemoNotification item = items.get(position);
        Context context = b.getRoot().getContext();

        b.iconFrame.setBackgroundResource(item.iconBackgroundRes);
        b.ivIcon.setImageResource(item.iconRes);
        b.ivIcon.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, item.iconTintRes)));
        b.tvTitle.setText(item.title);
        b.tvBody.setText(item.body);
        b.tvTime.setText(UserFmt.relative(item.createdAt));
        b.unreadDot.setVisibility(item.unread ? View.VISIBLE : View.INVISIBLE);
        b.getRoot().setOnClickListener(v -> listener.onOpen(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemUserNotificationBinding binding;

        Holder(ItemUserNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
