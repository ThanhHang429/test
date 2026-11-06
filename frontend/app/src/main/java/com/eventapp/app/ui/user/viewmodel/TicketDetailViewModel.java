package com.eventapp.app.ui.user.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.data.DemoContent;

/** Chi tiết vé (FR-U14). */
public class TicketDetailViewModel extends ViewModel {
    public TicketUiModel ticket(String code) {
        return DemoContent.get().ticket(code);
    }
}
