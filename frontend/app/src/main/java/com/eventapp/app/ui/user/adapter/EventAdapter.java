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
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

/** Adapter cho thẻ sự kiện: kiểu nổi bật (ngang) và kiểu danh sách dọc. */
public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(@NonNull EventUiModel event);

        void onToggleFavorite(@NonNull EventUiModel event);
    }

    private static final int TYPE_FEATURED = 0;
    private static final int TYPE_LIST = 1;

    private final boolean featured;
    private final Listener listener;
    private final List<EventUiModel> items = new ArrayList<>();

    public EventAdapter(boolean featured, @NonNull Listener listener) {
        this.featured = featured;
        this.listener = listener;
    }

    public void setItems(List<EventUiModel> data) {
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
        EventUiModel event = items.get(position);
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

    private void bindFeatured(ItemUserEventFeaturedBinding b, EventUiModel event) {
        Context context = b.getRoot().getContext();
        b.cover.setBackgroundResource(EventCategoryUi.coverRes(event.category));
        b.ivIcon.setImageResource(EventCategoryUi.iconRes(event.category));
        b.tvCategory.setText(EventCategoryUi.label(context, event.category));
        b.tvTitle.setText(event.title);
        b.tvDate.setText(UserFmt.dateTime(event.startAt));
        b.tvVenue.setText(venue(event));
        bindPrice(context, b.tvPrice, event);
        b.tvRemaining.setText(statusText(context, event));
        String badge = badge(event);
        b.tvTag.setText(badge);
        b.tvTag.setVisibility(badge.isEmpty() ? View.GONE : View.VISIBLE);
        bindSave(context, b.btnSave, event, true);

        b.getRoot().setOnClickListener(v -> listener.onOpen(event));
    }

    private void bindList(ItemUserEventListBinding b, EventUiModel event) {
        Context context = b.getRoot().getContext();
        b.cover.setBackgroundResource(EventCategoryUi.coverRes(event.category));
        b.ivIcon.setImageResource(EventCategoryUi.iconRes(event.category));
        b.tvCategory.setText(EventCategoryUi.label(context, event.category));
        b.tvTitle.setText(event.title);
        b.tvDate.setText(UserFmt.dateTime(event.startAt));
        b.tvVenue.setText(venue(event));
        bindPrice(context, b.tvPrice, event);
        String badge = badge(event);
        b.tvTag.setText(badge);
        b.tvTag.setVisibility(badge.isEmpty() ? View.GONE : View.VISIBLE);
        bindSave(context, b.btnSave, event, false);

        b.getRoot().setOnClickListener(v -> listener.onOpen(event));
    }

    private String venue(EventUiModel event) {
        if (event.venue == null || event.venue.isEmpty()) {
            return event.city == null ? "" : event.city;
        }
        return event.city == null || event.city.isEmpty()
                ? event.venue : event.venue + ", " + event.city;
    }

    private String badge(EventUiModel event) {
        if (event.statusLabel != null && !event.statusLabel.isEmpty()) {
            return event.statusLabel;
        }
        return event.tags.isEmpty() ? "" : event.tags.get(0);
    }

    private String statusText(Context context, EventUiModel event) {
        if (event.availability == EventUiModel.Availability.SOLD_OUT) {
            return context.getString(R.string.user_sold_out);
        }
        if (event.statusLabel != null && !event.statusLabel.isEmpty()) {
            return event.statusLabel;
        }
        return event.online ? context.getString(R.string.user_online) : "";
    }

    private void bindPrice(Context context, android.widget.TextView view, EventUiModel event) {
        if (event.availability == EventUiModel.Availability.SOLD_OUT) {
            view.setText(R.string.user_sold_out_short);
            view.setTextColor(ContextCompat.getColor(context, R.color.user_text_muted));
        } else if (event.free) {
            view.setText(R.string.user_free);
            view.setTextColor(ContextCompat.getColor(context, R.color.user_success_text));
        } else {
            view.setText(context.getString(R.string.user_from_price, UserFmt.money(event.priceFrom)));
            view.setTextColor(ContextCompat.getColor(context, R.color.user_primary));
        }
    }

    private void bindSave(Context context, android.widget.ImageButton button,
                          EventUiModel event, boolean onCover) {
        boolean saved = event.favorite;
        button.setImageResource(saved ? R.drawable.ic_ds_heart_filled : R.drawable.ic_user_heart);
        int tint = saved
                ? R.color.user_danger
                : (onCover ? R.color.user_text : R.color.user_text_dim);
        button.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, tint)));
        button.setContentDescription(context.getString(
                saved ? R.string.user_unsave : R.string.user_save));
        button.setOnClickListener(v -> listener.onToggleFavorite(event));
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
