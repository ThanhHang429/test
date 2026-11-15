package com.eventapp.app.data.repository;

import androidx.annotation.NonNull;

import com.eventapp.app.core.network.ApiCallback;
import com.eventapp.app.data.remote.api.TicketTypeApi;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeRequest;
import com.eventapp.app.data.remote.dto.tickettype.TicketTypeResponse;

import java.util.List;

/**
 * Repository xử lý dữ liệu loại vé (FR-O10, FR-O11, FR-O12).
 */
public class TicketTypeRepository {

    private final TicketTypeApi api;

    public TicketTypeRepository(@NonNull TicketTypeApi api) {
        this.api = api;
    }

    /** Lấy danh sách loại vé của sự kiện */
    public void getTicketTypes(long eventId, @NonNull ApiCallback<List<TicketTypeResponse>> callback) {
        ApiCallback.enqueue(api.getTicketTypes(eventId), callback);
    }

    /** Organizer tạo loại vé mới */
    public void createTicketType(long eventId, @NonNull TicketTypeRequest request,
                                 @NonNull ApiCallback<TicketTypeResponse> callback) {
        ApiCallback.enqueue(api.createTicketType(eventId, request), callback);
    }

    /** Organizer cập nhật thông tin loại vé */
    public void updateTicketType(long ticketTypeId, @NonNull TicketTypeRequest request,
                                 @NonNull ApiCallback<TicketTypeResponse> callback) {
        ApiCallback.enqueue(api.updateTicketType(ticketTypeId, request), callback);
    }

    /** Organizer xóa loại vé */
    public void deleteTicketType(long ticketTypeId, @NonNull ApiCallback<Boolean> callback) {
        ApiCallback.enqueue(api.deleteTicketType(ticketTypeId), callback);
    }
}
