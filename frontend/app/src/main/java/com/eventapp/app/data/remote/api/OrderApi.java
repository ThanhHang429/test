package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.order.OrderDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * API Đơn hàng (FR-U10, FR-U16, FR-O17).
 */
public interface OrderApi {

    /** Lấy danh sách đơn hàng của người dùng đang đăng nhập (FR-U16). */
    @GET("orders")
    Call<ResponseWrapper.Envelope<List<OrderDtos.Order>>> getMyOrders();

    /** Lấy chi tiết một đơn hàng kèm danh sách vé (FR-U16). */
    @GET("orders/{orderId}")
    Call<ResponseWrapper.Envelope<OrderDtos.Order>> getOrderDetail(@Path("orderId") long orderId);

    /** Tạo đơn hàng mới khi thanh toán (FR-U10). */
    @POST("orders")
    Call<ResponseWrapper.Envelope<OrderDtos.Order>> createOrder(@Body OrderDtos.CreateRequest request);

    /** Huỷ đơn hàng đang chờ thanh toán (FR-U10). */
    @POST("orders/{orderId}/cancel")
    Call<ResponseWrapper.Envelope<OrderDtos.Order>> cancelOrder(@Path("orderId") long orderId);

    /** Dành cho Organizer: Lấy danh sách đơn hàng của sự kiện (FR-O17). */
    @GET("organizer/orders")
    Call<ResponseWrapper.Envelope<List<OrderDtos.Order>>> getOrganizerOrders(
            @Query("status") String status,
            @Query("query") String query
    );

    /** Dành cho Organizer: Xem chi tiết 1 đơn hàng theo mã đơn (FR-O17). */
    @GET("organizer/orders/{orderCode}")
    Call<ResponseWrapper.Envelope<OrderDtos.Order>> getOrganizerOrderDetail(
            @Path("orderCode") String orderCode
    );
}
