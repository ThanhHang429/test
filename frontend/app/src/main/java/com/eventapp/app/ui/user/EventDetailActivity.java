package com.eventapp.app.ui.user;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserEventDetailBinding;
import com.eventapp.app.databinding.ViewUserTicketTypeBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.List;

/** Chi tiết sự kiện + chọn số lượng vé và chuyển sang bước xác nhận đặt vé. */
public class EventDetailActivity extends AppCompatActivity {

    private static final String EXTRA_EVENT_ID = "event_id";

    private ActivityUserEventDetailBinding binding;
    private DemoEvent event;
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

        event = DemoContent.get().event(getIntent().getLongExtra(EXTRA_EVENT_ID, -1L));
        if (event == null) {
            finish();
            return;
        }

        applyInsets();
        bindEvent();
        buildTicketTypes();

        binding.scroll.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, scrollY, oldX, oldY) -> {
                    float fade = 1f - Math.min(1f, scrollY / (float) dp(110));
                    binding.heroInfo.setAlpha(fade);
                    binding.heroInfo.setVisibility(fade <= 0f ? View.INVISIBLE : View.VISIBLE);
                });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnSave.setOnClickListener(v -> toggleSave());
        binding.tvDirections.setOnClickListener(v -> openDirections());
        binding.btnBook.setOnClickListener(v -> book());
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
        binding.heroCover.setBackgroundResource(event.coverRes);
        binding.heroIcon.setImageResource(event.iconRes);
        binding.tvCategory.setText(event.categoryLabel);
        binding.tvTag.setText(event.tag);
        binding.tvTag.setVisibility(event.tag.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvTitle.setText(event.title);
        binding.tvHeroDate.setText(UserFmt.dateTime(event.startAt));
        binding.tvTime.setText(UserFmt.dateLong(event.startAt) + "\n"
                + UserFmt.time(event.startAt) + " – " + UserFmt.time(event.endAt));
        binding.tvVenue.setText(event.venue + ", " + event.city);
        binding.tvOrganizer.setText(event.organizer);
        binding.tvDescription.setText(event.description);
        updateSaveIcon();
    }

    private void buildTicketTypes() {
        binding.ticketTypeList.removeAllViews();
        typeRows.clear();
        quantities = new int[event.ticketTypes.size()];

        for (int i = 0; i < event.ticketTypes.size(); i++) {
            final int index = i;
            DemoEvent.TicketType type = event.ticketTypes.get(i);
            ViewUserTicketTypeBinding row = ViewUserTicketTypeBinding.inflate(
                    getLayoutInflater(), binding.ticketTypeList, false);
            row.tvTypeName.setText(type.name);
            row.tvPrice.setText(UserFmt.money(type.price));
            row.tvNote.setText(type.note);
            row.tvNote.setVisibility(type.note.isEmpty() ? View.GONE : View.VISIBLE);
            row.tvPopular.setVisibility(type.popular && !type.soldOut() ? View.VISIBLE : View.GONE);
            row.tvRemaining.setText(type.soldOut()
                    ? getString(R.string.user_sold_out_short)
                    : getString(R.string.user_ticket_remaining, type.remaining));

            if (type.soldOut()) {
                row.getRoot().setAlpha(0.55f);
                row.btnPlus.setEnabled(false);
                row.btnMinus.setEnabled(false);
            } else {
                row.btnPlus.setOnClickListener(v -> {
                    if (quantities[index] < Math.min(type.remaining, 10)) {
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

    private void renderRow(ViewUserTicketTypeBinding row, DemoEvent.TicketType type, int qty) {
        row.tvQty.setText(String.valueOf(qty));
        boolean canMinus = qty > 0;
        boolean canPlus = !type.soldOut() && qty < Math.min(type.remaining, 10);
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
        boolean soldOut = event.cheapestAvailable() == null;
        binding.btnBook.setEnabled(!soldOut);
        binding.btnBook.setText(soldOut ? R.string.user_sold_out_short : R.string.user_book_now);
        binding.btnBook.setAlpha(soldOut ? 0.5f : (count == 0 ? 0.55f : 1f));
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
        CheckoutActivity.start(this, event.id, quantities);
    }

    private void toggleSave() {
        boolean saved = DemoContent.get().toggleSaved(event.id);
        updateSaveIcon();
        SnackPill.show(binding.getRoot(), getString(
                saved ? R.string.user_saved_toast : R.string.user_unsaved_toast), dp(96));
    }

    private void updateSaveIcon() {
        boolean saved = DemoContent.get().isSaved(event.id);
        binding.btnSave.setImageResource(saved
                ? R.drawable.ic_ds_heart_filled : R.drawable.ic_user_heart);
        binding.btnSave.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                saved ? R.color.user_danger : R.color.user_text)));
        binding.btnSave.setContentDescription(getString(
                saved ? R.string.user_unsave : R.string.user_save));
    }

    private void share() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, event.title);
        send.putExtra(Intent.EXTRA_TEXT, event.title + "\n"
                + UserFmt.dateTime(event.startAt) + "\n" + event.venue);
        startActivity(Intent.createChooser(send, getString(R.string.user_share_chooser)));
    }

    private void openDirections() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=" + Uri.encode(event.venue + ", " + event.city)));
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
