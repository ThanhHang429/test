package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserCalendarBinding;
import com.eventapp.app.databinding.ItemUserCalendarEventBinding;
import com.eventapp.app.databinding.ItemUserDayBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.model.EventUiModel;
import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.util.UserFmt;
import com.eventapp.app.ui.user.viewmodel.CalendarViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tab Lịch: dải ngày trong tuần và danh sách sự kiện sắp tới được nhóm theo ngày. */
public class CalendarFragment extends Fragment {

    private FragmentUserCalendarBinding binding;
    private CalendarViewModel viewModel;
    private final List<ItemUserDayBinding> dayBindings = new ArrayList<>();
    private final Map<Long, View> headers = new LinkedHashMap<>();
    private long selectedDayKey = -1L;

    /** Một dòng lịch: vé đã mua hoặc sự kiện đã lưu. */
    private static final class CalendarItem {
        final long eventId;
        final String title;
        final String venue;
        final long startAt;
        final long endAt;
        final TicketUiModel ticket;

        CalendarItem(long eventId, String title, String venue, long startAt, long endAt,
                     @Nullable TicketUiModel ticket) {
            this.eventId = eventId;
            this.title = title;
            this.venue = venue;
            this.startAt = startAt;
            this.endAt = endAt;
            this.ticket = ticket;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        NavContentInset.apply(binding.getRoot());
        binding.tvMonth.setText(UserFmt.monthYear(System.currentTimeMillis()));
        binding.btnToday.setOnClickListener(v -> scrollToToday());
        binding.btnExplore.setOnClickListener(v -> {
            if (getActivity() instanceof UserHostActivity) {
                ((UserHostActivity) getActivity()).openTab(UserBottomNavView.TAB_DISCOVERY);
            }
        });
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
        viewModel.getSavedEvents().observe(getViewLifecycleOwner(), data -> renderAll());
        build(upcomingItems());
        viewModel.loadSaved();
    }

    private void renderAll() {
        if (binding == null) {
            return;
        }
        List<CalendarItem> items = upcomingItems();
        List<EventUiModel> saved = viewModel.getSavedEvents().getValue();
        if (saved != null) {
            for (EventUiModel event : saved) {
                if (containsEvent(items, event.id)) {
                    continue;
                }
                items.add(new CalendarItem(event.id, event.title, venue(event),
                        event.startAt, event.endAt, null));
            }
        }
        items.sort((a, b) -> Long.compare(a.startAt, b.startAt));
        build(items);
    }

    private boolean containsEvent(List<CalendarItem> items, long eventId) {
        for (CalendarItem item : items) {
            if (item.eventId == eventId) {
                return true;
            }
        }
        return false;
    }

    private String venue(EventUiModel event) {
        if (event.venue == null || event.venue.isEmpty()) {
            return event.city == null ? "" : event.city;
        }
        return event.city == null || event.city.isEmpty()
                ? event.venue : event.venue + ", " + event.city;
    }

    private void build(List<CalendarItem> items) {
        if (binding == null) {
            return;
        }
        binding.tvSub.setText(getString(R.string.user_calendar_sub, items.size()));
        binding.emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);

        buildWeek(items);
        buildAgenda(items);
    }

    private void buildWeek(List<CalendarItem> items) {
        binding.weekStrip.removeAllViews();
        dayBindings.clear();

        Calendar monday = Calendar.getInstance();
        monday.set(Calendar.HOUR_OF_DAY, 0);
        monday.set(Calendar.MINUTE, 0);
        monday.set(Calendar.SECOND, 0);
        monday.set(Calendar.MILLISECOND, 0);
        int offset = (monday.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        monday.add(Calendar.DAY_OF_YEAR, -offset);

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) monday.clone();
            day.add(Calendar.DAY_OF_YEAR, i);
            long time = day.getTimeInMillis();

            ItemUserDayBinding item = ItemUserDayBinding.inflate(
                    LayoutInflater.from(requireContext()), binding.weekStrip, false);
            item.tvDay.setText(UserFmt.dayOfWeekShort(time));
            item.tvDate.setText(UserFmt.dayNumber(time));
            boolean hasEvent = hasEventOn(items, time);
            item.dot.setVisibility(hasEvent ? View.VISIBLE : View.INVISIBLE);

            boolean today = UserFmt.isToday(time);
            item.getRoot().setSelected(today);
            if (today) {
                selectedDayKey = dayKey(time);
            }

            final long dayTime = time;
            item.getRoot().setOnClickListener(v -> onDayClick(dayTime, hasEvent));
            binding.weekStrip.addView(item.getRoot());
            dayBindings.add(item);
        }
    }

    private void onDayClick(long time, boolean hasEvent) {
        long key = dayKey(time);
        View header = headers.get(key);
        if (!hasEvent || header == null) {
            SnackPill.show(binding.getRoot(), getString(R.string.user_no_events_day), dp(112));
            return;
        }
        selectedDayKey = key;
        for (ItemUserDayBinding item : dayBindings) {
            item.getRoot().setSelected(false);
        }
        for (ItemUserDayBinding item : dayBindings) {
            if (UserFmt.dayNumber(time).contentEquals(item.tvDate.getText())) {
                item.getRoot().setSelected(true);
                break;
            }
        }
        binding.scroll.smoothScrollTo(0, binding.agendaList.getTop() + header.getTop() - dp(8));
    }

    private void scrollToToday() {
        long today = dayKey(System.currentTimeMillis());
        View header = headers.get(today);
        if (header != null) {
            binding.scroll.smoothScrollTo(0, binding.agendaList.getTop() + header.getTop() - dp(8));
        } else if (!headers.isEmpty()) {
            View first = headers.values().iterator().next();
            binding.scroll.smoothScrollTo(0, binding.agendaList.getTop() + first.getTop() - dp(8));
        }
    }

    private void buildAgenda(List<CalendarItem> items) {
        binding.agendaList.removeAllViews();
        headers.clear();
        long lastKey = -1L;

        for (CalendarItem item : items) {
            long key = dayKey(item.startAt);
            if (key != lastKey) {
                lastKey = key;
                TextView header = new TextView(requireContext());
                boolean today = UserFmt.isToday(item.startAt);
                String prefix = today ? getString(R.string.user_today) + " · " : "";
                header.setText(prefix + UserFmt.dateLong(item.startAt).toUpperCase());
                header.setTextAppearance(R.style.TextAppearance_EventApp_User_Label);
                header.setTextColor(ContextCompat.getColor(requireContext(),
                        today ? R.color.user_primary : R.color.user_text_muted));
                header.setPadding(0, dp(18), 0, dp(8));
                binding.agendaList.addView(header);
                headers.put(key, header);
            }

            ItemUserCalendarEventBinding row = ItemUserCalendarEventBinding.inflate(
                    LayoutInflater.from(requireContext()), binding.agendaList, false);
            row.tvTime.setText(UserFmt.time(item.startAt) + " – " + UserFmt.time(item.endAt));
            row.tvTitle.setText(item.title);
            row.tvVenue.setText(item.venue);

            if (item.ticket != null) {
                row.tvStatus.setText(R.string.user_ticket_status_valid);
                row.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.user_success_text));
                row.tvTicketInfo.setText(item.ticket.ticketType + " × " + item.ticket.quantity
                        + " · #" + item.ticket.code);
                row.tvOpen.setText(R.string.user_open_ticket);
                row.tvOpen.setOnClickListener(v ->
                        TicketDetailActivity.start(requireContext(), item.ticket.code));
            } else {
                row.tvStatus.setText(R.string.user_saved_label);
                row.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.user_primary));
                row.tvTicketInfo.setText(R.string.user_reminder);
                row.tvOpen.setText(R.string.user_detail);
                row.tvOpen.setOnClickListener(v ->
                        EventDetailActivity.start(requireContext(), item.eventId));
            }

            row.getRoot().setOnClickListener(v ->
                    EventDetailActivity.start(requireContext(), item.eventId));
            binding.agendaList.addView(row.getRoot());
        }
    }

    private List<CalendarItem> upcomingItems() {
        long now = System.currentTimeMillis();
        List<CalendarItem> result = new ArrayList<>();
        for (TicketUiModel ticket : DemoContent.get().tickets()) {
            if (ticket.status == TicketUiModel.Status.UPCOMING && ticket.startAt >= now) {
                result.add(new CalendarItem(ticket.eventId, ticket.eventTitle, ticket.venue,
                        ticket.startAt, ticket.endAt, ticket));
            }
        }
        return result;
    }

    private boolean hasEventOn(List<CalendarItem> items, long day) {
        long key = dayKey(day);
        for (CalendarItem item : items) {
            if (dayKey(item.startAt) == key) {
                return true;
            }
        }
        return false;
    }

    private long dayKey(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            renderAll();
            viewModel.loadSaved();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
