package com.eventapp.app.ui.user.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.core.network.ApiException;
import com.eventapp.app.data.di.ServiceLocator;
import com.eventapp.app.data.repository.TicketRepository;
import com.eventapp.app.ui.model.TicketUiModel;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/** Vé của tôi (FR-U12) — đồng bộ danh sách vé & mã QR thật từ Backend `/api/v1/tickets`. */
public class TicketsViewModel extends ViewModel {

    private final TicketRepository ticketRepository = ServiceLocator.get(TicketRepository.class);

    private final MutableLiveData<List<TicketUiModel>> ticketsLive =
            new MutableLiveData<>(new ArrayList<>(ticketRepository.getCachedTickets()));
    private final MutableLiveData<Boolean> loadingLive = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public LiveData<List<TicketUiModel>> getTickets() {
        return ticketsLive;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLive;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public void loadTickets() {
        loadingLive.setValue(true);
        ticketRepository.getMyTickets("ALL", 0, 50, new ApiCallback<List<TicketUiModel>>() {
            @Override
            public void onSuccess(@Nullable List<TicketUiModel> data, @Nullable JsonElement meta) {
                loadingLive.setValue(false);
                ticketsLive.setValue(data != null ? data : new ArrayList<>());
            }

            @Override
            public void onError(@NonNull ApiException error) {
                loadingLive.setValue(false);
                errorLive.setValue(error.getUserMessage());
                List<TicketUiModel> cached = ticketRepository.getCachedTickets();
                if (!cached.isEmpty()) {
                    ticketsLive.setValue(cached);
                }
            }
        });
    }

    public List<TicketUiModel> tickets(TicketUiModel.Status status) {
        List<TicketUiModel> all = ticketsLive.getValue();
        List<TicketUiModel> filtered = new ArrayList<>();
        if (all == null) {
            return filtered;
        }
        for (TicketUiModel ticket : all) {
            if (ticket.status == status) {
                filtered.add(ticket);
            }
        }
        return filtered;
    }

    public int count(TicketUiModel.Status status) {
        List<TicketUiModel> all = ticketsLive.getValue();
        if (all == null) {
            return 0;
        }
        int total = 0;
        for (TicketUiModel ticket : all) {
            if (ticket.status == status) {
                total += Math.max(1, ticket.quantity);
            }
        }
        return total;
    }
}
