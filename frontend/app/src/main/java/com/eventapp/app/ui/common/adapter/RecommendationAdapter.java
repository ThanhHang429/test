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
import com.eventapp.app.ui.user.home.RecommendationUiModel;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** FR-U07 cards. Only opens event detail; it never starts a purchase or registration. */
public class RecommendationAdapter
        extends RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder> {

    public interface OnRecommendationActionListener {
        void onRecommendationOpen(RecommendationUiModel item);

        void onRecommendationDismiss(RecommendationUiModel item);
    }

    private final List<RecommendationUiModel> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("dd/MM/yyyy • HH:mm", new Locale("vi", "VN"));

    private OnRecommendationActionListener listener;

    public void setOnRecommendationActionListener(OnRecommendationActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RecommendationUiModel> values) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        notifyDataSetChanged();
    }

    public RecommendationUiModel getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_recommend, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        RecommendationUiModel item = items.get(position);
        EventUiModel event = item.event;
        Context context = holder.itemView.getContext();

        bindCover(holder.cover, event);
        bindStatus(holder.status, event);
        holder.title.setText(event.title);
        holder.time.setText(timeFormat.format(new Date(event.startAt)));
        holder.location.setText(formatLocation(context, event));
        if (event.free) {
            holder.price.setFree();
        } else {
            holder.price.setPriceFrom(event.priceFrom);
        }

        boolean aiSourced = item.source == RecommendationUiModel.Source.AI;
        holder.aiBadge.setVisibility(aiSourced ? View.VISIBLE : View.GONE);
        if (aiSourced) {
            holder.aiBadge.setScore(event.aiScore);
        }
        String reason = TextUtils.isEmpty(item.reason) ? "" : item.reason;
        holder.reason.setText(aiSourced ? reason
                : context.getString(R.string.hc_rec_source_rule, reason));
        holder.reasonRow.setVisibility(reason.isEmpty() ? View.GONE : View.VISIBLE);

        holder.itemView.setContentDescription(event.title);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationOpen(item);
            }
        });
        holder.open.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationOpen(item);
            }
        });
        holder.dismiss.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationDismiss(item);
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

    private void bindStatus(StatusChip chip, EventUiModel event) {
        Context context = chip.getContext();
        if (event.availability == EventUiModel.Availability.SOLD_OUT) {
            chip.setStatus(context.getString(R.string.hc_event_sold_out), StatusChip.Tone.MUTED);
        } else if (event.availability == EventUiModel.Availability.FEW_LEFT) {
            chip.setStatus(context.getString(R.string.hc_event_few_left), StatusChip.Tone.WARNING);
        } else if (event.free) {
            chip.setStatus(context.getString(R.string.hc_event_free), StatusChip.Tone.SUCCESS);
        } else {
            chip.setVisibility(View.GONE);
            return;
        }
        chip.setVisibility(View.VISIBLE);
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

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final StatusChip status;
        final AiBadge aiBadge;
        final TextView title;
        final TextView time;
        final TextView location;
        final PriceTextView price;
        final TextView reason;
        final View reasonRow;
        final MaterialButton open;
        final ImageButton dismiss;

        RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.ivRecCover);
            status = itemView.findViewById(R.id.chipRecStatus);
            aiBadge = itemView.findViewById(R.id.aiBadgeRec);
            title = itemView.findViewById(R.id.tvRecEventTitle);
            time = itemView.findViewById(R.id.tvRecEventTime);
            location = itemView.findViewById(R.id.tvRecEventLocation);
            price = itemView.findViewById(R.id.tvRecEventPrice);
            reason = itemView.findViewById(R.id.tvRecReason);
            reasonRow = itemView.findViewById(R.id.layoutRecReason);
            open = itemView.findViewById(R.id.btnRecOpen);
            dismiss = itemView.findViewById(R.id.btnRecNotInterested);
        }
    }
}
