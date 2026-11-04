package com.eventapp.app.ui.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.eventapp.app.R;
import com.eventapp.app.databinding.FragmentUserCalendarBinding;
import com.eventapp.app.databinding.ItemUserCalendarEventBinding;
import com.eventapp.app.databinding.ItemUserDayBinding;
import com.eventapp.app.ui.common.view.NavContentInset;
import com.eventapp.app.ui.common.view.SnackPill;
import com.eventapp.app.ui.host.UserHostActivity;
import com.eventapp.app.ui.user.data.DemoContent;
import com.eventapp.app.ui.user.model.DemoEvent;
import com.eventapp.app.ui.user.model.DemoTicket;
import com.eventapp.app.ui.user.util.UserFmt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tab Lịch: dải ngày trong tuần và danh sách sự kiện sắp tới được nhóm theo ngày. */
public class CalendarFragment extends Fragment {

    private FragmentUserCalendarBinding binding;
    private final List<ItemUserDayBinding> dayBindings = new ArrayList<>();
    private final Map<Long, View> headers = new LinkedHashMap<>();
    private long selectedDayKey = -1L;

    private final Runnable dataObserver = this::build;

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
        build();
    }

    private void build() {
        if (binding == null) {
            return;
        }
        List<DemoEvent> events = upcomingEvents();
        binding.tvSub.setText(getString(R.string.user_calendar_sub, events.size()));
        binding.emptyState.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);

        buildWeek(events);
        buildAgenda(events);
    }

    private void buildWeek(List<DemoEvent> events) {
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
            boolean hasEvent = hasEventOn(events, time);
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
        // Đánh dấu ô vừa chọn bằng cách dò lại theo nội dung ngày.
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

    private void buildAgenda(List<DemoEvent> events) {
        binding.agendaList.removeAllViews();
        headers.clear();
        long lastKey = -1L;

        for (DemoEvent event : events) {
            long key = dayKey(event.startAt);
            if (key != lastKey) {
                lastKey = key;
                TextView header = new TextView(requireContext());
                boolean today = UserFmt.isToday(event.startAt);
                String prefix = today ? getString(R.string.user_today) + " · " : "";
                header.setText(prefix + UserFmt.dateLong(event.startAt).toUpperCase());
                header.setTextAppearance(R.style.TextAppearance_EventApp_User_Label);
                header.setTextColor(ContextCompat.getColor(requireContext(),
                        today ? R.color.user_primary : R.color.user_text_muted));
                header.setPadding(0, dp(18), 0, dp(8));
                binding.agendaList.addView(header);
                headers.put(key, header);
            }

            DemoTicket ticket = ticketFor(event.id);
            ItemUserCalendarEventBinding item = ItemUserCalendarEventBinding.inflate(
                    LayoutInflater.from(requireContext()), binding.agendaList, false);
            item.tvTime.setText(UserFmt.time(event.startAt) + " – " + UserFmt.time(event.endAt));
            item.tvTitle.setText(event.title);
            item.tvVenue.setText(event.venue);

            if (ticket != null) {
                item.tvStatus.setText(R.string.user_ticket_status_valid);
                item.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.user_success_text));
                item.tvTicketInfo.setText(ticket.typeName + " × " + ticket.quantity
                        + " · #" + ticket.code);
                item.tvOpen.setText(R.string.user_open_ticket);
                item.tvOpen.setOnClickListener(v ->
                        TicketDetailActivity.start(requireContext(), ticket.code));
            } else {
                item.tvStatus.setText(R.string.user_saved_label);
                item.tvStatus.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.user_primary));
                item.tvTicketInfo.setText(R.string.user_reminder);
                item.tvOpen.setText(R.string.user_detail);
                item.tvOpen.setOnClickListener(v ->
                        EventDetailActivity.start(requireContext(), event.id));
            }

            item.getRoot().setOnClickListener(v ->
                    EventDetailActivity.start(requireContext(), event.id));
            binding.agendaList.addView(item.getRoot());
        }
    }

    private List<DemoEvent> upcomingEvents() {
        long now = System.currentTimeMillis();
        Map<Long, DemoEvent> unique = new LinkedHashMap<>();
        for (DemoTicket ticket : DemoContent.get().tickets()) {
            if (ticket.status == DemoTicket.Status.UPCOMING && ticket.event.startAt >= now) {
                unique.put(ticket.event.id, ticket.event);
            }
        }
        for (DemoEvent event : DemoContent.get().savedEvents()) {
            if (event.startAt >= now) {
                unique.put(event.id, event);
            }
        }
        List<DemoEvent> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> Long.compare(a.startAt, b.startAt));
        return result;
    }

    private DemoTicket ticketFor(long eventId) {
        for (DemoTicket ticket : DemoContent.get().tickets()) {
            if (ticket.status == DemoTicket.Status.UPCOMING && ticket.event.id == eventId) {
                return ticket;
            }
        }
        return null;
    }

    private boolean hasEventOn(List<DemoEvent> events, long day) {
        long key = dayKey(day);
        for (DemoEvent event : events) {
            if (dayKey(event.startAt) == key) {
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
    public void onStart() {
        super.onStart();
        DemoContent.get().addListener(dataObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        build();
    }

    @Override
    public void onStop() {
        DemoContent.get().removeListener(dataObserver);
        super.onStop();
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
