package com.eventapp.app.ui.common.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.eventapp.app.R;
import com.eventapp.app.ui.common.view.AiBadge;
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.user.home.EventCategoryUi;
import com.eventapp.app.ui.user.home.EventUiModel;

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
        AI
    }

    public interface OnEventActionListener {
        void onEventClick(EventUiModel event);

        void onFavoriteClick(EventUiModel event, int position);
    }

    private static final int TYPE_LIST = 0;
    private static final int TYPE_FEATURED = 1;
    private static final int TYPE_FOOTER = 2;

    private final Mode mode;
    private final List<EventUiModel> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));

    private OnEventActionListener listener;
    private boolean loadingMore;

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
        return mode == Mode.LIST ? TYPE_LIST : TYPE_FEATURED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            return new EventViewHolder(inflater.inflate(R.layout.item_loading_footer, parent, false),
                    TYPE_FOOTER);
        }
        if (viewType == TYPE_LIST) {
            return new EventViewHolder(inflater.inflate(R.layout.item_event, parent, false),
                    TYPE_LIST);
        }
        return new EventViewHolder(inflater.inflate(R.layout.item_event_featured, parent, false),
                TYPE_FEATURED);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EventViewHolder viewHolder = (EventViewHolder) holder;
        if (viewHolder.type == TYPE_FOOTER) {
            return;
        }
        EventUiModel event = items.get(position);
        if (viewHolder.type == TYPE_LIST) {
            bindList(viewHolder, event, position);
        } else {
            bindFeatured(viewHolder, event, position);
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
        holder.time.setText(formatTime(holder.itemView.getContext(), event.startAt));
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
        holder.time.setText(formatTime(holder.itemView.getContext(), event.startAt));
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

    private void bindCover(ImageView cover, EventUiModel event) {
        if (cover == null) {
            return;
        }
        Context context = cover.getContext();
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{EventCategoryUi.gradientStart(context, event.category),
                        EventCategoryUi.gradientEnd(context, event.category)});
        gradient.setCornerRadius(dp(context, 12));
        cover.setBackground(gradient);
        cover.setImageResource(EventCategoryUi.iconRes(event.category));
        cover.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.ds_white)));
        cover.setAlpha(0.92f);
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
                chip.setStatus(chip.getContext().getString(R.string.hc_event_few_left),
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

    private String formatTime(Context context, long startAt) {
        Calendar now = Calendar.getInstance();
        Calendar event = Calendar.getInstance();
        event.setTimeInMillis(startAt);
        boolean sameDay = now.get(Calendar.YEAR) == event.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == event.get(Calendar.DAY_OF_YEAR);
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        boolean isTomorrow = tomorrow.get(Calendar.YEAR) == event.get(Calendar.YEAR)
                && tomorrow.get(Calendar.DAY_OF_YEAR) == event.get(Calendar.DAY_OF_YEAR);
        String clock = timeFormat.format(new Date(startAt));
        if (sameDay) {
            return context.getString(R.string.hc_event_today, clock);
        }
        if (isTomorrow) {
            return context.getString(R.string.hc_event_tomorrow, clock);
        }
        return dateFormat.format(new Date(startAt)) + " • " + clock;
    }

    private String formatLocation(Context context, EventUiModel event) {
        if (event.online) {
            return context.getString(R.string.hc_event_online);
        }
        if (TextUtils.isEmpty(event.venue)) {
            return event.city;
        }
        if (TextUtils.isEmpty(event.city)) {
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
