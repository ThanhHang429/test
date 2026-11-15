package com.eventapp.app.data.remote.dto.tickettype;

import com.google.gson.annotations.SerializedName;

/**
 * DTO Trả về thông tin loại vé từ Backend (FR-O10, FR-O11, FR-O12).
 */
public class TicketTypeResponse {

    @SerializedName("ticketTypeId")
    public Long ticketTypeId;

    @SerializedName("eventId")
    public Long eventId;

    @SerializedName("ticketTypeName")
    public String ticketTypeName;

    @SerializedName("description")
    public String description;

    @SerializedName("price")
    public double price;

    @SerializedName("currency")
    public String currency = "VND";

    @SerializedName("totalQuantity")
    public int totalQuantity;

    @SerializedName("quantityReserved")
    public int quantityReserved;

    @SerializedName("quantitySold")
    public int quantitySold;

    @SerializedName("remainingQuantity")
    public int remainingQuantity;

    @SerializedName("minPerOrder")
    public int minPerOrder = 1;

    @SerializedName("maxPerUser")
    public int maxPerUser = 10;

    @SerializedName("salesStartAt")
    public String salesStartAt;

    @SerializedName("salesEndAt")
    public String salesEndAt;

    @SerializedName("status")
    public String status = "ON_SALE";
}
