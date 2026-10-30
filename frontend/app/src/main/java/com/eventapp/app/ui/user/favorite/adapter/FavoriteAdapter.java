package com.eventapp.app.ui.user.favorite.adapter;

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
import com.eventapp.app.ui.common.view.PriceTextView;
import com.eventapp.app.ui.common.view.StatusChip;
import com.eventapp.app.ui.model.FavoriteUiModel;
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Saved-event cards for FR-U06. Unavailable rows never render event identity. */
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavoriteViewHolder> {

    public interface OnFavoriteActionListener {
        void onFavoriteOpen(FavoriteUiModel item);

        void onFavoriteRemove(FavoriteUiModel item);
    }

    private final List<FavoriteUiModel> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("dd/MM/yyyy • HH:mm", new Locale("vi", "VN"));

    private OnFavoriteActionListener listener;

    public void setOnFavoriteActionListener(OnFavoriteActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<FavoriteUiModel> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        notifyDataSetChanged();
    }

    public FavoriteUiModel getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        FavoriteUiModel item = items.get(position);
        boolean unavailable = item.isUnavailable();
        holder.content.setVisibility(unavailable ? View.GONE : View.VISIBLE);
        holder.unavailable.setVisibility(unavailable ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (!unavailable && listener != null) {
                listener.onFavoriteOpen(item);
            }
        });

        if (unavailable) {
            holder.itemView.setContentDescription(
                    holder.itemView.getContext().getString(R.string.hc_fav_unavailable));
            holder.remove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteRemove(item);
                }
            });
            return;
        }

        EventUiModel event = item.event;
        bindCover(holder.cover, event);
        bindStatus(holder, item);
        holder.time.setText(timeFormat.format(new Date(event.startAt)));
        holder.title.setText(event.title);
        holder.location.setText(formatLocation(holder.itemView.getContext(), event));
        if (event.free) {
            holder.price.setFree();
        } else {
            holder.price.setPriceFrom(event.priceFrom);
        }
        bindAction(holder.action, item);
        holder.itemView.setContentDescription(event.title);

        holder.favorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteRemove(item);
            }
        });
        holder.action.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteOpen(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindCover(ImageView cover, EventUiModel event) {
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

    private void bindStatus(FavoriteViewHolder holder, FavoriteUiModel item) {
        EventUiModel event = item.event;
        Context context = holder.itemView.getContext();
        if (item.state == FavoriteUiModel.State.CANCELLED) {
            holder.status.setStatus(context.getString(R.string.hc_event_cancelled),
                    StatusChip.Tone.DANGER);
        } else if (item.state == FavoriteUiModel.State.ENDED) {
            holder.status.setStatus(context.getString(R.string.hc_fav_state_ended),
                    StatusChip.Tone.MUTED);
        } else if (event.availability == EventUiModel.Availability.SOLD_OUT) {
            holder.status.setStatus(context.getString(R.string.hc_event_sold_out),
                    StatusChip.Tone.MUTED);
        } else if (event.availability == EventUiModel.Availability.FEW_LEFT) {
            holder.status.setStatus(context.getString(R.string.hc_event_few_left),
                    StatusChip.Tone.WARNING);
        } else if (event.free) {
            holder.status.setStatus(context.getString(R.string.hc_fav_state_free),
                    StatusChip.Tone.SUCCESS);
        } else {
            holder.status.setVisibility(View.GONE);
            return;
        }
        holder.status.setVisibility(View.VISIBLE);
    }

    private void bindAction(MaterialButton button, FavoriteUiModel item) {
        EventUiModel event = item.event;
        Context context = button.getContext();
        boolean detailOnly = item.state != FavoriteUiModel.State.UPCOMING
                || event.availability == EventUiModel.Availability.SOLD_OUT
                || event.availability == EventUiModel.Availability.CANCELLED;
        int label;
        if (detailOnly) {
            label = R.string.hc_fav_cta_detail;
        } else if (event.free) {
            label = R.string.hc_fav_cta_register;
        } else {
            label = R.string.hc_fav_cta_buy;
        }
        button.setText(label);
        int background = ContextCompat.getColor(context,
                detailOnly ? R.color.ds_surface : R.color.ds_primary);
        int foreground = ContextCompat.getColor(context,
                detailOnly ? R.color.ds_primary : R.color.ds_white);
        int stroke = ContextCompat.getColor(context,
                detailOnly ? R.color.ds_outline_strong : R.color.ds_primary);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(foreground);
        button.setStrokeColor(ColorStateList.valueOf(stroke));
        button.setContentDescription(context.getString(label) + ", " + event.title);
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

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        final View content;
        final View unavailable;
        final ImageView cover;
        final StatusChip status;
        final TextView time;
        final TextView title;
        final TextView location;
        final PriceTextView price;
        final ImageButton favorite;
        final MaterialButton action;
        final MaterialButton remove;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.layoutFavContent);
            unavailable = itemView.findViewById(R.id.layoutFavUnavailable);
            cover = itemView.findViewById(R.id.ivFavCover);
            status = itemView.findViewById(R.id.chipFavStatus);
            time = itemView.findViewById(R.id.tvFavTimePill);
            title = itemView.findViewById(R.id.tvFavTitle);
            location = itemView.findViewById(R.id.tvFavLocation);
            price = itemView.findViewById(R.id.tvFavPrice);
            favorite = itemView.findViewById(R.id.btnFavHeart);
            action = itemView.findViewById(R.id.btnFavAction);
            remove = itemView.findViewById(R.id.btnFavRemove);
        }
    }
}
