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

import java.util.List;

/** Chi tiết vé (FR-U12) — tải vé & qrToken thật từ Backend `/api/v1/tickets`. */
public class TicketDetailViewModel extends ViewModel {

    private final TicketRepository ticketRepository = ServiceLocator.get(TicketRepository.class);
    private final MutableLiveData<TicketUiModel> ticketLive = new MutableLiveData<>();
    private final MutableLiveData<String> errorLive = new MutableLiveData<>();

    public LiveData<TicketUiModel> getTicket() {
        return ticketLive;
    }

    public LiveData<String> getError() {
        return errorLive;
    }

    public void load(long ticketId, @Nullable String ticketCode) {
        TicketUiModel cached = ticketId > 0
                ? ticketRepository.findCachedById(ticketId)
                : ticketRepository.findCachedByCode(ticketCode);
        if (cached != null) {
            ticketLive.setValue(cached);
        }

        if (ticketId > 0) {
            ticketRepository.getTicketDetail(ticketId, new ApiCallback<TicketUiModel>() {
                @Override
                public void onSuccess(@Nullable TicketUiModel data, @Nullable JsonElement meta) {
                    if (data != null) {
                        ticketLive.setValue(data);
                    } else if (cached == null) {
                        fallbackByCode(ticketCode);
                    }
                }

                @Override
                public void onError(@NonNull ApiException error) {
                    if (cached == null) {
                        fallbackByCode(ticketCode);
                    }
                }
            });
            return;
        }

        ticketRepository.getMyTickets("ALL", 0, 50, new ApiCallback<List<TicketUiModel>>() {
            @Override
            public void onSuccess(@Nullable List<TicketUiModel> data, @Nullable JsonElement meta) {
                TicketUiModel match = findMatch(data, ticketCode);
                if (match != null) {
                    ticketLive.setValue(match);
                } else {
                    fallbackByCode(ticketCode);
                }
            }

            @Override
            public void onError(@NonNull ApiException error) {
                fallbackByCode(ticketCode);
            }
        });
    }

    @Nullable
    private TicketUiModel findMatch(@Nullable List<TicketUiModel> list, @Nullable String code) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (code != null && !code.trim().isEmpty()) {
            String target = code.trim();
            for (TicketUiModel t : list) {
                if (target.equalsIgnoreCase(t.code) || target.equalsIgnoreCase(t.orderCode)) {
                    return t;
                }
            }
            return null;
        }
        return list.get(0);
    }

    private void fallbackByCode(@Nullable String ticketCode) {
        if (ticketLive.getValue() == null) {
            errorLive.setValue("Không tìm thấy thông tin vé điện tử"
                    + (ticketCode != null && !ticketCode.trim().isEmpty() ? " (" + ticketCode + ")" : "") + ".");
        }
    }
}
