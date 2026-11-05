package com.eventapp.app.ui.user.viewmodel;

import androidx.lifecycle.ViewModel;

import com.eventapp.app.ui.model.TicketUiModel;
import com.eventapp.app.ui.user.data.DemoContent;

/** Thanh toán đặt vé (FR-U14). */
public class CheckoutViewModel extends ViewModel {
    public TicketUiModel purchase(TicketUiModel.Builder snapshot) {
        return DemoContent.get().purchase(snapshot);
    }
}
