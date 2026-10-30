package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.AiBadge;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shared event list adapter for Home, EventList and Favorites (FR-U04/U06).
 * One adapter covers the row and featured-card presentations to avoid duplicate bindings.
 */
public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public enum Mode {
        LIST,
        FEATURED,
        AI,
        HERO,
        TRENDING
    }

    public interface OnEventActionListener {
        void onEventClick(EventUiModel event);

        void onFavoriteClick(EventUiModel event, int position);
    }

    private static final int TYPE_LIST = 0;
    private static final int TYPE_FEATURED = 1;
    private static final int TYPE_FOOTER = 2;
    private static final int TYPE_HERO = 3;
    private static final int TYPE_TRENDING = 4;
    private static final int TYPE_AI = 5;

    private final Mode mode;
    private final List<EventUiModel> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));

    private OnEventActionListener listener;
    private boolean loadingMore;
    /** Thành phố đang chọn ở bộ lọc; trùng với city của sự kiện thì ẩn phần thành phố. */
    private String selectedCity;

    private static final String[] WEEKDAYS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    public EventAdapter() {
        this(Mode.LIST);
    }

    public EventAdapter(Mode mode) {
        this.mode = mode == null ? Mode.LIST : mode;
        setHasStableIds(false);
    }

    public void setOnEventActionListener(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void setSelectedCity(@Nullable String selectedCity) {
        this.selectedCity = selectedCity;
    }

    public void submitList(List<EventUiModel> events) {
        items.clear();
        if (events != null) {
            items.addAll(events);
        }
        notifyDataSetChanged();
    }

    public List<EventUiModel> getItems() {
        return new ArrayList<>(items);
    }

    public EventUiModel getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    public void setLoadingMore(boolean loadingMore) {
        if (this.loadingMore == loadingMore) {
            return;
        }
        this.loadingMore = loadingMore;
        if (loadingMore) {
            notifyItemInserted(items.size());
        } else {
            notifyItemRemoved(items.size());
        }
    }

    public boolean isLoadingMore() {
        return loadingMore;
    }

    public void toggleFavorite(int position) {
        EventUiModel event = getItem(position);
        if (event == null) {
            return;
        }
        event.favorite = !event.favorite;
        notifyItemChanged(position);
        if (listener != null) {
            listener.onFavoriteClick(event, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (loadingMore && position == items.size()) {
            return TYPE_FOOTER;
        }
        switch (mode) {
            case HERO:
                return TYPE_HERO;
            case TRENDING:
                return TYPE_TRENDING;
            case AI:
                return TYPE_AI;
            case FEATURED:
                return TYPE_FEATURED;
            case LIST:
            default:
                return TYPE_LIST;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_FOOTER:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_loading_footer, parent, false), TYPE_FOOTER);
            case TYPE_HERO:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_home_hero, parent, false), TYPE_HERO);
            case TYPE_TRENDING:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_home_trending, parent, false), TYPE_TRENDING);
            case TYPE_AI:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_home_ai, parent, false), TYPE_AI);
            case TYPE_FEATURED:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_event_featured, parent, false),
                        TYPE_FEATURED);
            case TYPE_LIST:
            default:
                return new EventViewHolder(
                        inflater.inflate(R.layout.item_event, parent, false), TYPE_LIST);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EventViewHolder viewHolder = (EventViewHolder) holder;
        if (viewHolder.type == TYPE_FOOTER) {
            return;
        }
        EventUiModel event = items.get(position);
        switch (viewHolder.type) {
            case TYPE_HERO:
                bindHero(viewHolder, event, position);
                break;
            case TYPE_TRENDING:
                bindTrending(viewHolder, event, position);
                break;
            case TYPE_AI:
                bindAi(viewHolder, event, position);
                break;
            case TYPE_FEATURED:
                bindFeatured(viewHolder, event, position);
                break;
            case TYPE_LIST:
            default:
                bindList(viewHolder, event, position);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + (loadingMore ? 1 : 0);
    }

    private void bindList(EventViewHolder holder, EventUiModel event, int position) {
        bindCover(holder.cover, event);
        bindStatus(holder.status, event);
        holder.title.setText(event.title);
        holder.time.setText(formatTime(event.startAt));
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        bindPrice(holder.price, event);
        bindRating(holder.rating, holder.ratingStar, event);
        bindFavorite(holder.favorite, event);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
        holder.itemView.setContentDescription(event.title);
        holder.favorite.setOnClickListener(v -> toggleFavorite(position));
    }

    private void bindFeatured(EventViewHolder holder, EventUiModel event, int position) {
        bindCover(holder.cover, event);
        if (holder.category != null) {
            holder.category.setText(EventCategoryUi.label(holder.itemView.getContext(),
                    event.category));
        }
        if (holder.aiBadge != null) {
            boolean showAi = mode == Mode.AI && (event.aiScore != null
                    || (event.aiReason != null && !event.aiReason.trim().isEmpty()));
            holder.aiBadge.setVisibility(showAi ? View.VISIBLE : View.GONE);
            if (showAi) {
                holder.aiBadge.setScore(event.aiScore);
                holder.aiBadge.setReason(event.aiReason);
            }
        }
        holder.title.setText(event.title);
        holder.time.setText(formatTime(event.startAt));
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        bindPrice(holder.price, event);
        bindFavorite(holder.favorite, event);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
        holder.itemView.setContentDescription(event.title);
        holder.favorite.setOnClickListener(v -> toggleFavorite(position));
    }

    private void bindHero(EventViewHolder holder, EventUiModel event, int position) {
        bindCover(holder.cover, event, 16);
        bindStatus(holder.status, event);
        holder.title.setText(event.title);
        holder.time.setText(formatTime(event.startAt));
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        bindPrice(holder.price, event);
        bindFavorite(holder.favorite, event);
        bindClicks(holder, event, position);
    }

    private void bindTrending(EventViewHolder holder, EventUiModel event, int position) {
        bindCover(holder.cover, event, 16);
        bindStatus(holder.status, event);
        if (holder.category != null) {
            holder.category.setText(EventCategoryUi.label(holder.itemView.getContext(),
                    event.category));
        }
        holder.title.setText(event.title);
        holder.time.setText(formatTime(event.startAt));
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        bindPrice(holder.price, event);
        bindFavorite(holder.favorite, event);
        bindClicks(holder, event, position);
    }

    private void bindAi(EventViewHolder holder, EventUiModel event, int position) {
        bindCover(holder.cover, event, 12);
        if (holder.aiBadge != null) {
            boolean hasReason = event.aiReason != null && !event.aiReason.trim().isEmpty();
            boolean showAi = event.aiScore != null || hasReason;
            holder.aiBadge.setVisibility(showAi ? View.VISIBLE : View.GONE);
            if (showAi) {
                holder.aiBadge.setScore(event.aiScore);
                holder.aiBadge.setReason(event.aiReason);
            }
        }
        holder.title.setText(event.title);
        holder.time.setText(formatTime(event.startAt));
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        bindPrice(holder.price, event);
        bindClicks(holder, event, position);
    }

    private void bindClicks(EventViewHolder holder, EventUiModel event, int position) {
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
        holder.itemView.setContentDescription(event.title);
        if (holder.favorite != null) {
            holder.favorite.setOnClickListener(v -> toggleFavorite(position));
        }
    }

    private void bindCover(ImageView cover, EventUiModel event) {
        bindCover(cover, event, 12);
    }

    private void bindCover(ImageView cover, EventUiModel event, int radiusDp) {
        if (cover == null) {
            return;
        }
        if (!TextUtils.isEmpty(event.coverUrl)) {
            cover.setBackground(EventCoverRenderer.gradient(cover.getContext(),
                    event.category, radiusDp));
            cover.setImageTintList(null);
            cover.setAlpha(1f);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(cover)
                    .load(event.coverUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(cover);
            return;
        }
        Glide.with(cover).clear(cover);
        EventCoverRenderer.showPlaceholder(cover, event.category, radiusDp);
    }

    private void bindStatus(StatusChip chip, EventUiModel event) {
        if (chip == null) {
            return;
        }
        switch (event.availability) {
            case CANCELLED:
                chip.setStatus(chip.getContext().getString(R.string.hc_event_cancelled),
                        StatusChip.Tone.DANGER);
                chip.setVisibility(View.VISIBLE);
                break;
            case SOLD_OUT:
                chip.setStatus(chip.getContext().getString(R.string.hc_event_sold_out),
                        StatusChip.Tone.MUTED);
                chip.setVisibility(View.VISIBLE);
                break;
            case FEW_LEFT:
                chip.setStatus(chip.getContext().getString(
                                event.remainingTickets > 0
                                        ? R.string.hc_event_remaining
                                        : R.string.hc_event_few_left,
                                event.remainingTickets),
                        StatusChip.Tone.WARNING);
                chip.setVisibility(View.VISIBLE);
                break;
            case AVAILABLE:
            case UNKNOWN:
            default:
                chip.setVisibility(View.GONE);
                break;
        }
    }

    private void bindPrice(PriceTextView price, EventUiModel event) {
        if (price == null) {
            return;
        }
        if (event.free) {
            price.setFree();
        } else {
            price.setPriceFrom(event.priceFrom);
        }
    }

    private void bindRating(TextView rating, ImageView star, EventUiModel event) {
        if (rating == null) {
            return;
        }
        boolean hasRating = event.reviewCount > 0 && event.rating > 0f;
        rating.setVisibility(hasRating ? View.VISIBLE : View.GONE);
        if (star != null) {
            star.setVisibility(hasRating ? View.VISIBLE : View.GONE);
        }
        if (hasRating) {
            rating.setText(String.format(new Locale("vi", "VN"), "%.1f (%d)",
                    event.rating, event.reviewCount));
        }
    }

    private void bindFavorite(ImageButton button, EventUiModel event) {
        if (button == null) {
            return;
        }
        button.setImageResource(event.favorite
                ? R.drawable.ic_ds_heart_filled
                : R.drawable.ic_ds_heart);
        button.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                button.getContext(),
                event.favorite ? R.color.ds_cta : R.color.ds_text_muted)));
        button.setContentDescription(button.getContext().getString(
                event.favorite ? R.string.hc_event_favorite_remove
                        : R.string.hc_event_favorite_add));
    }

    /** Định dạng thống nhất: "T7, 16/11 • 14:00". */
    private String formatTime(long startAt) {
        Calendar event = Calendar.getInstance();
        event.setTimeInMillis(startAt);
        String weekday = WEEKDAYS[event.get(Calendar.DAY_OF_WEEK) - 1];
        Date date = new Date(startAt);
        return weekday + ", " + dateFormat.format(date) + " • " + timeFormat.format(date);
    }

    private String formatLocation(Context context, EventUiModel event) {
        if (event.online) {
            return context.getString(R.string.hc_event_online);
        }
        if (TextUtils.isEmpty(event.venue)) {
            return event.city;
        }
        if (TextUtils.isEmpty(event.city) || event.city.equalsIgnoreCase(selectedCity)) {
            return event.venue;
        }
        return event.venue + ", " + event.city;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static class EventViewHolder extends RecyclerView.ViewHolder {
        final int type;
        ImageView cover;
        StatusChip status;
        TextView title;
        TextView time;
        TextView location;
        PriceTextView price;
        TextView rating;
        ImageView ratingStar;
        ImageButton favorite;
        TextView category;
        AiBadge aiBadge;

        EventViewHolder(@NonNull View itemView, int type) {
            super(itemView);
            this.type = type;
            if (type == TYPE_FOOTER) {
                return;
            }
            cover = itemView.findViewById(R.id.ivEventCover);
            status = itemView.findViewById(R.id.chipEventStatus);
            title = itemView.findViewById(R.id.tvEventTitle);
            time = itemView.findViewById(R.id.tvEventTime);
            location = itemView.findViewById(R.id.tvEventLocation);
            price = itemView.findViewById(R.id.tvEventPrice);
            rating = itemView.findViewById(R.id.tvEventRating);
            ratingStar = itemView.findViewById(R.id.ivEventRatingStar);
            favorite = itemView.findViewById(R.id.btnEventFavorite);
            category = itemView.findViewById(R.id.tvEventCategory);
            aiBadge = itemView.findViewById(R.id.aiBadgeEvent);
        }
    }
}
