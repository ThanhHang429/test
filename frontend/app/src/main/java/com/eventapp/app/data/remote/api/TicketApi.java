package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.ticket.TicketDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Ví vé điện tử thật của người dùng (FR-U12) — khớp với TicketController ở Backend.
 */
public interface TicketApi {

    @GET("tickets")
    Call<ResponseWrapper.Envelope<List<TicketDtos.Ticket>>> list(
            @Query("status") String status,
            @Query("page") int page,
            @Query("size") int size
    );

    @GET("tickets/{ticketId}")
    Call<ResponseWrapper.Envelope<TicketDtos.Ticket>> get(@Path("ticketId") long ticketId);
}
