package com.eventapp.app.ui.user.ticket;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.fake.SampleTicketData;
import com.eventapp.app.ui.model.TicketUiModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dữ liệu cho "Vé của tôi": FR-U14 (vé đã mua) và FR-U18 (lịch sự kiện) gộp thành hai tab.
 * Tạm dùng dữ liệu mẫu cho tới khi repository đơn hàng được nối.
 */
public class MyTicketsViewModel extends ViewModel {

    private final MutableLiveData<List<TicketUiModel>> upcomingTickets = new MutableLiveData<>();
    private final MutableLiveData<List<TicketUiModel>> pastTickets = new MutableLiveData<>();

    public MyTicketsViewModel() {
        load();
    }

    public LiveData<List<TicketUiModel>> getUpcomingTickets() {
        return upcomingTickets;
    }

    public LiveData<List<TicketUiModel>> getPastTickets() {
        return pastTickets;
    }

    private void load() {
        List<TicketUiModel> all = SampleTicketData.createTickets();

        List<TicketUiModel> upcoming = new ArrayList<>();
        List<TicketUiModel> past = new ArrayList<>();
        for (TicketUiModel ticket : all) {
            if (ticket.isUpcoming()) {
                upcoming.add(ticket);
            } else {
                past.add(ticket);
            }
        }
        // Sắp diễn ra: gần nhất lên đầu. Đã qua: mới nhất lên đầu.
        Collections.sort(upcoming, (left, right) -> Long.compare(left.startAt, right.startAt));
        Collections.sort(past, (left, right) -> Long.compare(right.startAt, left.startAt));

        upcomingTickets.setValue(upcoming);
        pastTickets.setValue(past);
    }
}
