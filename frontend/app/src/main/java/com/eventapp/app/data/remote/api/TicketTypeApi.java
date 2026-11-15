package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeRequest;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * REST API Client cho module Quản lý Loại Vé (FR-O10, FR-O11, FR-O12).
 */
public interface TicketTypeApi {

    /** Lấy danh sách loại vé của một sự kiện */
    @GET("events/{eventId}/ticket-types")
    Call<ResponseWrapper.Envelope<List<TicketTypeResponse>>> getTicketTypes(@Path("eventId") long eventId);

    /** Organizer tạo loại vé mới cho sự kiện */
    @POST("organizer/events/{eventId}/ticket-types")
    Call<ResponseWrapper.Envelope<TicketTypeResponse>> createTicketType(
            @Path("eventId") long eventId,
            @Body TicketTypeRequest request
    );

    /** Organizer cập nhật thông tin loại vé */
    @PUT("organizer/ticket-types/{ticketTypeId}")
    Call<ResponseWrapper.Envelope<TicketTypeResponse>> updateTicketType(
            @Path("ticketTypeId") long ticketTypeId,
            @Body TicketTypeRequest request
    );

    /** Organizer xóa loại vé (chỉ xóa được khi chưa bán vé nào) */
    @DELETE("organizer/ticket-types/{ticketTypeId}")
    Call<ResponseWrapper.Envelope<Boolean>> deleteTicketType(@Path("ticketTypeId") long ticketTypeId);
}
