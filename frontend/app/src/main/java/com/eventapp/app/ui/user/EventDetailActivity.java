package com.eventapp.app.ui.user;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserEventDetailBinding;
import com.eventapp.app.databinding.ViewUserTicketTypeBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.model.EventCategoryUi;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.user.viewmodel.EventDetailViewModel;
import com.eventapp.app.ui.user.util.StarText;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Chi tiết sự kiện + chọn số lượng vé và chuyển sang bước xác nhận đặt vé. */
public class EventDetailActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityUserEventDetailBinding binding;
    private EventDetailViewModel viewModel;
    private EventUiModel event;
    private boolean savePending;
    private boolean saveTarget;
    private int[] quantities = new int[0];
    private final List<ViewUserTicketTypeBinding> typeRows = new ArrayList<>();

    public static void start(Context context, long eventId) {
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserEventDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        applyInsets();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnSave.setOnClickListener(v -> toggleSave());
        binding.tvDirections.setOnClickListener(v -> openDirections());
        binding.btnBook.setOnClickListener(v -> book());
        binding.cardReviews.setOnClickListener(v -> {
            if (event != null) {
                EventReviewsActivity.start(this, event.id, event.title);
            }
        });
        binding.btnBook.setEnabled(false);

        viewModel = new ViewModelProvider(this).get(EventDetailViewModel.class);
        observe();

        binding.scroll.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, scrollY, oldX, oldY) -> {
                    float fade = 1f - Math.min(1f, scrollY / (float) dp(110));
                    binding.heroInfo.setAlpha(fade);
                    binding.heroInfo.setVisibility(fade <= 0f ? View.INVISIBLE : View.VISIBLE);
                });

        load();
    }

    private void observe() {
        viewModel.getEvent().observe(this, data -> {
            if (data == null || isFinishing() || isDestroyed()) {
                return;
            }
            event = data;
            bindEvent();
            buildTicketTypes();
        });
        viewModel.getLoadError().observe(this, error -> {
            if (error == null || isFinishing() || isDestroyed()) {
                return;
            }
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
            finish();
        });
        viewModel.getFavorite().observe(this, saved -> {
            if (saved == null || event == null) {
                return;
            }
            savePending = false;
            event.favorite = saved;
            updateSaveIcon();
        });
        viewModel.getFavoriteError().observe(this, error -> {
            if (error == null) {
                return;
            }
            viewModel.consumeFavoriteError();
            savePending = false;
            if (event == null) {
                return;
            }
            event.favorite = !saveTarget;
            updateSaveIcon();
            SnackPill.show(binding.getRoot(), error.getUserMessage(), dp(96));
        });
    }

    private void load() {
        viewModel.load(getIntent().getLongExtra(EXTRA_EVENT_ID, -1L));
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.detailRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.heroControls.setPadding(binding.heroControls.getPaddingStart(),
                    bars.top + dp(10), binding.heroControls.getPaddingEnd(), 0);
            binding.bottomBar.setPadding(binding.bottomBar.getPaddingStart(),
                    binding.bottomBar.getPaddingTop(), binding.bottomBar.getPaddingEnd(),
                    dp(12) + bars.bottom);
            binding.scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
    }

    private void bindEvent() {
        binding.heroCover.setBackgroundResource(EventCategoryUi.coverRes(event.category));
        binding.heroIcon.setImageResource(EventCategoryUi.iconRes(event.category));
        binding.tvCategory.setText(EventCategoryUi.label(this, event.category));
        String tag = event.statusLabel == null ? "" : event.statusLabel;
        binding.tvTag.setText(tag);
        binding.tvTag.setVisibility(tag.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvTitle.setText(event.title);
        binding.tvHeroDate.setText(UserFmt.dateTime(event.startAt));
        binding.tvTime.setText(UserFmt.dateLong(event.startAt) + "\n"
                + UserFmt.time(event.startAt) + " – " + UserFmt.time(event.endAt));
        binding.tvVenue.setText(venue());
        binding.tvOrganizer.setText(event.organizer);
        binding.tvDescription.setText(event.description);
        bindReviewSummary();
        updateSaveIcon();
    }

    private void bindReviewSummary() {
        StarText.apply(binding.tvReviewStars, event.rating);
        binding.tvReviewSummary.setText(event.reviewCount <= 0
                ? getString(R.string.user_reviews_none)
                : String.format(Locale.US, "%.1f", event.rating).replace('.', ',')
                + " · " + getString(R.string.user_reviews_total, event.reviewCount));
    }

    private void buildTicketTypes() {
        binding.ticketTypeList.removeAllViews();
        typeRows.clear();
        quantities = new int[event.ticketTypes.size()];

        for (int i = 0; i < event.ticketTypes.size(); i++) {
            final int index = i;
            EventUiModel.TicketType type = event.ticketTypes.get(i);
            ViewUserTicketTypeBinding row = ViewUserTicketTypeBinding.inflate(
                    getLayoutInflater(), binding.ticketTypeList, false);
            row.tvTypeName.setText(type.name);
            row.tvPrice.setText(type.isFree() ? getString(R.string.user_free)
                    : UserFmt.money(type.price));
            row.tvNote.setText(type.description);
            row.tvNote.setVisibility(type.description.isEmpty() ? View.GONE : View.VISIBLE);
            row.tvPopular.setVisibility(index == 0 && !type.soldOut() ? View.VISIBLE : View.GONE);
            row.tvRemaining.setText(remainingLabel(type));

            if (type.soldOut() || !type.onSale()) {
                row.getRoot().setAlpha(0.55f);
                row.btnPlus.setEnabled(false);
                row.btnMinus.setEnabled(false);
            } else {
                row.btnPlus.setOnClickListener(v -> {
                    if (quantities[index] < maxQuantity(type)) {
                        quantities[index]++;
                        renderRow(row, type, quantities[index]);
                        updateTotal();
                    }
                });
                row.btnMinus.setOnClickListener(v -> {
                    if (quantities[index] > 0) {
                        quantities[index]--;
                        renderRow(row, type, quantities[index]);
                        updateTotal();
                    }
                });
            }

            renderRow(row, type, 0);
            binding.ticketTypeList.addView(row.getRoot());
            typeRows.add(row);
        }
        updateTotal();
    }

    private String remainingLabel(EventUiModel.TicketType type) {
        if (type.soldOut()) {
            return getString(R.string.user_sold_out_short);
        }
        if (type.remaining == null) {
            return "";
        }
        return getString(R.string.user_ticket_remaining, type.remaining);
    }

    private int maxQuantity(EventUiModel.TicketType type) {
        int limit = type.maxPerUser;
        if (type.remaining != null) {
            limit = Math.min(limit, type.remaining);
        }
        return Math.max(1, limit);
    }

    private void renderRow(ViewUserTicketTypeBinding row, EventUiModel.TicketType type, int qty) {
        row.tvQty.setText(String.valueOf(qty));
        boolean canMinus = qty > 0;
        boolean canPlus = !type.soldOut() && type.onSale() && qty < maxQuantity(type);
        row.btnMinus.setEnabled(canMinus);
        row.btnMinus.setAlpha(canMinus ? 1f : 0.35f);
        row.btnPlus.setEnabled(canPlus);
        row.btnPlus.setAlpha(canPlus ? 1f : 0.35f);
    }

    private void updateTotal() {
        long total = 0L;
        int count = 0;
        for (int i = 0; i < quantities.length; i++) {
            total += event.ticketTypes.get(i).price * quantities[i];
            count += quantities[i];
        }
        binding.tvTotal.setText(UserFmt.amount(total));
        boolean blocked = event.disabledReason != null && !event.disabledReason.isEmpty();
        binding.btnBook.setEnabled(!blocked);
        binding.btnBook.setText(blocked ? blockedLabel(event.disabledReason)
                : R.string.user_book_now);
        binding.btnBook.setAlpha(blocked ? 0.5f : (count == 0 ? 0.55f : 1f));
    }

    private int blockedLabel(String reason) {
        switch (reason) {
            case "SOLD_OUT":
                return R.string.user_sold_out_short;
            case "SALES_NOT_STARTED":
                return R.string.user_sales_not_started;
            case "SALES_ENDED":
                return R.string.user_sales_ended;
            case "EVENT_CANCELLED":
                return R.string.user_event_cancelled;
            case "EVENT_COMPLETED":
                return R.string.user_event_completed;
            case "ALREADY_REGISTERED":
                return R.string.user_already_registered;
            default:
                return R.string.user_book_now;
        }
    }

    private void book() {
        int count = 0;
        for (int qty : quantities) {
            count += qty;
        }
        if (count == 0) {
            SnackPill.show(binding.getRoot(), getString(R.string.user_select_ticket_first), dp(96));
            return;
        }
        String[] names = new String[event.ticketTypes.size()];
        long[] prices = new long[event.ticketTypes.size()];
        for (int i = 0; i < event.ticketTypes.size(); i++) {
            names[i] = event.ticketTypes.get(i).name;
            prices[i] = event.ticketTypes.get(i).price;
        }
        CheckoutActivity.start(this, event.id, event.title, venue(), event.city,
                event.startAt, event.endAt, event.category.ordinal(),
                names, prices, quantities);
    }

    private void toggleSave() {
        if (event == null || savePending) {
            return;
        }
        savePending = true;
        saveTarget = !event.favorite;
        // Đổi icon ngay để thao tác lưu/bỏ lưu có phản hồi tức thì.
        event.favorite = saveTarget;
        updateSaveIcon();
        SnackPill.show(binding.getRoot(), getString(
                saveTarget ? R.string.user_saved_toast : R.string.user_unsaved_toast), dp(96));
        viewModel.toggleFavorite(event.id, saveTarget);
    }

    private void updateSaveIcon() {
        boolean saved = event != null && event.favorite;
        binding.btnSave.setImageResource(saved
                ? R.drawable.ic_ds_heart_filled : R.drawable.ic_user_heart);
        binding.btnSave.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                saved ? R.color.user_danger : R.color.user_text)));
        binding.btnSave.setContentDescription(getString(
                saved ? R.string.user_unsave : R.string.user_save));
    }

    private String venue() {
        if (event == null) {
            return "";
        }
        if (event.venue == null || event.venue.isEmpty()) {
            return event.city == null ? "" : event.city;
        }
        return event.city == null || event.city.isEmpty()
                ? event.venue : event.venue + ", " + event.city;
    }

    private void share() {
        if (event == null) {
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, event.title);
        send.putExtra(Intent.EXTRA_TEXT, event.title + "\n"
                + UserFmt.dateTime(event.startAt) + "\n" + venue());
        startActivity(Intent.createChooser(send, getString(R.string.user_share_chooser)));
    }

    private void openDirections() {
        if (event == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=" + Uri.encode(venue())));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            SnackPill.show(binding.getRoot(), getString(R.string.user_coming_soon_toast), dp(96));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
