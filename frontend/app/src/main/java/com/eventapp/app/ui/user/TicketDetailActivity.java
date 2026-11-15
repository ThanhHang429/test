package com.eventapp.app.ui.user;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.ActivityUserTicketDetailBinding;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.util.UserFmt;
import com.eventapp.app.ui.user.viewmodel.TicketDetailViewModel;

/** Chi tiết vé kèm mã QR check-in thật từ Backend (`qrToken`) khớp với máy quét Ban tổ chức. */
public class TicketDetailActivity extends AppCompatActivity {

    private static final String EXTRA_TICKET_ID = "ticket_id";
    private static final String EXTRA_CODE = "ticket_code";

    private ActivityUserTicketDetailBinding binding;
    private TicketUiModel ticket;
    private TicketDetailViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int seconds = 60;
    private int tick;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (binding == null || ticket == null) {
                return;
            }
            // Chỉ xoay vòng mã demo khi không có qrToken thật từ server
            if (ticket.qrToken != null && !ticket.qrToken.isEmpty()) {
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
        start(context, -1L, code);
    }

    public static void start(Context context, long ticketId, @Nullable String code) {
        Intent intent = new Intent(context, TicketDetailActivity.class);
        intent.putExtra(EXTRA_TICKET_ID, ticketId);
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

        applyInsets();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnShareEvent.setOnClickListener(v -> share());
        binding.btnCalendar.setOnClickListener(v -> addToCalendar());

        viewModel = new ViewModelProvider(this).get(TicketDetailViewModel.class);
        viewModel.getTicket().observe(this, data -> {
            if (data != null) {
                ticket = data;
                bindTicket();
                restartTickerIfNeeded();
            }
        });
        viewModel.getError().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        long ticketId = getIntent().getLongExtra(EXTRA_TICKET_ID, -1L);
        String code = getIntent().getStringExtra(EXTRA_CODE);
        viewModel.load(ticketId, code);
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
        if (ticket == null) {
            return;
        }
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
        binding.tvFullVenue.setText(ticket.city != null && !ticket.city.isEmpty()
                ? ticket.venue + ", " + ticket.city : ticket.venue);

        boolean hasServerQr = ticket.qrToken != null && !ticket.qrToken.isEmpty();

        switch (ticket.status) {
            case USED:
                binding.tvStatus.setText(R.string.user_ticket_status_used);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_neutral);
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.user_text_muted));
                binding.tvNote.setText(R.string.user_ticket_used_note);
                binding.tvRefresh.setText(R.string.user_ticket_used_note);
                binding.qrCode.setPayload(hasServerQr ? ticket.qrToken : ticket.code);
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
                binding.qrCode.setPayload(hasServerQr ? ticket.qrToken : ticket.code);
                binding.qrCode.setAlpha(0.25f);
                binding.btnCalendar.setEnabled(false);
                binding.btnCalendar.setAlpha(0.5f);
                break;
            default:
                binding.tvStatus.setText(R.string.user_ticket_ready);
                binding.tvStatus.setBackgroundResource(R.drawable.bg_user_badge_success);
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.user_success_text));
                binding.qrCode.setAlpha(1.0f);
                if (hasServerQr) {
                    binding.tvRefresh.setText("Mã QR xác thực check-in (Đồng bộ hệ thống)");
                    binding.qrCode.setPayload(ticket.qrToken);
                } else {
                    binding.tvRefresh.setText(getString(R.string.user_ticket_refresh, seconds));
                    binding.qrCode.setPayload(ticket.code + "#0");
                }
                break;
        }
    }

    private void restartTickerIfNeeded() {
        handler.removeCallbacks(ticker);
        if (ticket != null
                && ticket.status == TicketUiModel.Status.UPCOMING
                && (ticket.qrToken == null || ticket.qrToken.isEmpty())) {
            handler.postDelayed(ticker, 1000L);
        }
    }

    private void addToCalendar() {
        if (ticket == null) return;
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
        if (ticket == null) return;
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
        restartTickerIfNeeded();
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
