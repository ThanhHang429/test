package com.eventapp.app.ui.user;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
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

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserTicketDetailBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.util.UserFmt;

/** Chi tiết vé kèm mã QR động (mô phỏng) dùng để check-in. */
public class TicketDetailActivity extends AppCompatActivity {

    private static final String EXTRA_CODE = "ticket_code";

    private ActivityUserTicketDetailBinding binding;
    private TicketUiModel ticket;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int seconds = 60;
    private int tick;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (binding == null) {
                return;
            }
            seconds--;
            if (seconds <= 0) {
                seconds = 60;
                tick++;
                binding.qrCode.setPayload(ticket.code + "#" + tick);
            }
            binding.tvRefresh.setText(getString(R.string.user_ticket_refresh, seconds));
            handler.postDelayed(this, 1000L);
        }
    };

    public static void start(Context context, String code) {
        Intent intent = new Intent(context, TicketDetailActivity.class);
        intent.putExtra(EXTRA_CODE, code);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityUserTicketDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        ticket = DemoContent.get().ticket(getIntent().getStringExtra(EXTRA_CODE));
        if (ticket == null) {
            finish();
            return;
        }

        applyInsets();
        bindTicket();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnShareEvent.setOnClickListener(v -> share());
        binding.btnCalendar.setOnClickListener(v -> addToCalendar());
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.ticketRoot, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) binding.topBar.getLayoutParams();
            params.height = dp(60) + bars.top;
            binding.topBar.setLayoutParams(params);
            binding.topBar.setPadding(dp(16), bars.top + dp(6), dp(16), dp(6));
            binding.scroll.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });
    }

    private void bindTicket() {
        binding.cover.setBackgroundResource(ticket.coverRes());
        binding.ivIcon.setImageResource(ticket.iconRes());
        binding.tvDate.setText(UserFmt.dateShort(ticket.startAt) + " • "
                + UserFmt.time(ticket.startAt));
        binding.tvTitle.setText(ticket.eventTitle);
        binding.tvVenue.setText(ticket.venue);
        binding.tvCode.setText(ticket.code);
        binding.tvType.setText(ticket.ticketType);
        binding.tvQuantity.setText(String.valueOf(ticket.quantity));
        binding.tvTime.setText(UserFmt.dateLong(ticket.startAt) + "\n"
                + UserFmt.time(ticket.startAt) + " – " + UserFmt.time(ticket.endAt));
        binding.tvFullVenue.setText(ticket.venue + ", " + ticket.city);

        switch (ticket.status) {
            case USED:
                binding.tvStatus.setText(R.string.user_ticket_status_used);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_neutral);
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.user_text_muted));
                binding.tvNote.setText(R.string.user_ticket_used_note);
                binding.tvRefresh.setText(R.string.user_ticket_used_note);
                binding.qrCode.setAlpha(0.25f);
                binding.btnCalendar.setEnabled(false);
                binding.btnCalendar.setAlpha(0.5f);
                break;
            case CANCELLED:
                binding.tvStatus.setText(R.string.user_ticket_status_cancelled);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_danger);
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.user_danger_text));
                binding.tvNote.setText(R.string.user_ticket_cancelled_note);
                binding.tvRefresh.setText(R.string.user_ticket_cancelled_note);
                binding.qrCode.setAlpha(0.25f);
                binding.btnCalendar.setEnabled(false);
                binding.btnCalendar.setAlpha(0.5f);
                break;
            default:
                binding.tvStatus.setText(R.string.user_ticket_ready);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_success);
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.user_success_text));
                binding.tvRefresh.setText(getString(R.string.user_ticket_refresh, seconds));
                binding.qrCode.setPayload(ticket.code + "#0");
                break;
        }
    }

    private void addToCalendar() {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, ticket.eventTitle)
                .putExtra(CalendarContract.Events.EVENT_LOCATION,
                        ticket.venue + ", " + ticket.city)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, ticket.startAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, ticket.endAt);
        try {
            startActivity(intent);
            SnackPill.show(binding.getRoot(), getString(R.string.user_added_calendar), dp(96));
        } catch (ActivityNotFoundException exception) {
            SnackPill.show(binding.getRoot(), getString(R.string.user_coming_soon_toast), dp(96));
        }
    }

    private void share() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, ticket.eventTitle);
        send.putExtra(Intent.EXTRA_TEXT, ticket.eventTitle + "\n"
                + UserFmt.dateTime(ticket.startAt) + "\n" + ticket.venue);
        startActivity(Intent.createChooser(send, getString(R.string.user_share_chooser)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ticket != null && ticket.status == TicketUiModel.Status.UPCOMING) {
            handler.removeCallbacks(ticker);
            handler.postDelayed(ticker, 1000L);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
