package com.eventapp.app.ui.user.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ItemUserEventFeaturedBinding;
import com.eventapp.app.databinding.ItemUserEventListBinding;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

/** Adapter cho thẻ sự kiện: kiểu nổi bật (ngang) và kiểu danh sách dọc. */
public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(@NonNull DemoEvent event);

        void onSaveChanged(@NonNull DemoEvent event, boolean saved);
    }

    private static final int TYPE_FEATURED = 0;
    private static final int TYPE_LIST = 1;

    private final boolean featured;
    private final Listener listener;
    private final List<DemoEvent> items = new ArrayList<>();

    public EventAdapter(boolean featured, @NonNull Listener listener) {
        this.featured = featured;
        this.listener = listener;
    }

    public void setItems(List<DemoEvent> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return featured ? TYPE_FEATURED : TYPE_LIST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FEATURED) {
            return new FeaturedBindingHolder(
                    ItemUserEventFeaturedBinding.inflate(inflater, parent, false));
        }
        return new ListBindingHolder(ItemUserEventListBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DemoEvent event = items.get(position);
        if (holder instanceof FeaturedBindingHolder) {
            bindFeatured(((FeaturedBindingHolder) holder).binding, event);
        } else if (holder instanceof ListBindingHolder) {
            bindList(((ListBindingHolder) holder).binding, event);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindFeatured(ItemUserEventFeaturedBinding b, DemoEvent event) {
        Context context = b.getRoot().getContext();
        b.cover.setBackgroundResource(event.coverRes);
        b.ivIcon.setImageResource(event.iconRes);
        b.tvCategory.setText(event.categoryLabel);
        b.tvTitle.setText(event.title);
        b.tvDate.setText(UserFmt.dateTime(event.startAt));
        b.tvVenue.setText(event.venue);
        bindPrice(context, b.tvPrice, event);
        b.tvRemaining.setText(event.remaining > 0
                ? context.getString(R.string.user_remaining, event.remaining)
                : context.getString(R.string.user_sold_out));
        b.tvTag.setText(event.tag);
        b.tvTag.setVisibility(event.tag.isEmpty() ? View.GONE : View.VISIBLE);
        bindSave(context, b.btnSave, event, true);

        b.getRoot().setOnClickListener(v -> listener.onOpen(event));
    }

    private void bindList(ItemUserEventListBinding b, DemoEvent event) {
        Context context = b.getRoot().getContext();
        b.cover.setBackgroundResource(event.coverRes);
        b.ivIcon.setImageResource(event.iconRes);
        b.tvCategory.setText(event.categoryLabel);
        b.tvTitle.setText(event.title);
        b.tvDate.setText(UserFmt.dateTime(event.startAt));
        b.tvVenue.setText(event.venue);
        bindPrice(context, b.tvPrice, event);
        b.tvTag.setText(event.tag);
        b.tvTag.setVisibility(event.tag.isEmpty() ? View.GONE : View.VISIBLE);
        bindSave(context, b.btnSave, event, false);

        b.getRoot().setOnClickListener(v -> listener.onOpen(event));
    }

    private void bindPrice(Context context, android.widget.TextView view, DemoEvent event) {
        if (event.isFree()) {
            view.setText(R.string.user_free);
            view.setTextColor(ContextCompat.getColor(context, R.color.user_success_text));
        } else {
            view.setText(context.getString(R.string.user_from_price, UserFmt.money(event.priceFrom)));
            view.setTextColor(ContextCompat.getColor(context, R.color.user_primary));
        }
    }

    private void bindSave(Context context, android.widget.ImageButton button,
                          DemoEvent event, boolean onCover) {
        boolean saved = DemoContent.get().isSaved(event.id);
        button.setImageResource(saved ? R.drawable.ic_ds_heart_filled : R.drawable.ic_user_heart);
        int tint = saved
                ? R.color.user_danger
                : (onCover ? R.color.user_text : R.color.user_text_dim);
        button.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, tint)));
        button.setContentDescription(context.getString(
                saved ? R.string.user_unsave : R.string.user_save));
        button.setOnClickListener(v -> {
            boolean nowSaved = DemoContent.get().toggleSaved(event.id);
            if (listener != null) {
                listener.onSaveChanged(event, nowSaved);
            }
        });
    }

    private static class FeaturedBindingHolder extends RecyclerView.ViewHolder {
        final ItemUserEventFeaturedBinding binding;

        FeaturedBindingHolder(ItemUserEventFeaturedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class ListBindingHolder extends RecyclerView.ViewHolder {
        final ItemUserEventListBinding binding;

        ListBindingHolder(ItemUserEventListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
