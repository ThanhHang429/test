package com.eventapp.app.ui.user.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.data.DemoContent;

import java.util.List;

/** Vé của tôi (FR-U14) — bọc nguồn dữ liệu mẫu qua ViewModel. */
public class TicketsViewModel extends ViewModel {

    public List<TicketUiModel> tickets(TicketUiModel.Status status) {
        return DemoContent.get().tickets(status);
    }

    public int count(TicketUiModel.Status status) {
        return DemoContent.get().ticketsByStatusCount(status);
    }

    public void addListener(Runnable listener) {
        DemoContent.get().addListener(listener);
    }

    public void removeListener(Runnable listener) {
        DemoContent.get().removeListener(listener);
    }
}
