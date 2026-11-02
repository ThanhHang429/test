package com.eventapp.app.ui.user.home.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.adapter.EventCoverRenderer;
import com.eventapp.app.core.enums.EventCategory;
import com.eventapp.app.ui.user.home.EventDetailViewModel;

import java.util.ArrayList;
import java.util.List;

/** Swipeable gallery placeholder for FR-U05. Glide will replace the generated panel later. */
public class EventGalleryAdapter extends RecyclerView.Adapter<EventGalleryAdapter.GalleryViewHolder> {

    public interface OnGalleryClickListener {
        void onGalleryClick(EventDetailViewModel.GalleryItem item, int position);
    }

    private final List<EventDetailViewModel.GalleryItem> items = new ArrayList<>();
    private EventCategory category = EventCategory.OTHER;
    private OnGalleryClickListener listener;

    public void setCategory(EventCategory category) {
        this.category = category == null ? EventCategory.OTHER : category;
    }

    public void setOnGalleryClickListener(OnGalleryClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<EventDetailViewModel.GalleryItem> gallery) {
        items.clear();
        if (gallery != null) {
            items.addAll(gallery);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_gallery, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        EventDetailViewModel.GalleryItem item = items.get(position);
        Context context = holder.itemView.getContext();
        EventCoverRenderer.showPlaceholder(holder.image, category, 16);
        if (item.url != null && !item.url.trim().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.image)
                    .load(item.url)
                    .thumbnail(0.25f)
                    .centerCrop()
                    .into(holder.image);
        }
        holder.label.setText(item.label);
        holder.itemView.setContentDescription(context.getString(
                R.string.hc_detail_gallery_item, position + 1));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGalleryClick(item, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView label;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivDetailGallery);
            label = itemView.findViewById(R.id.tvDetailGalleryLabel);
        }
    }
}
