package com.eventapp.app.data.remote.dto.tickettype;

import com.google.gson.annotations.SerializedName;

/**
 * DTO Gửi lên để tạo hoặc chỉnh sửa loại vé (FR-O10, FR-O11, FR-O12).
 */
public class TicketTypeRequest {

    @SerializedName("ticketTypeName")
    public String ticketTypeName;

    @SerializedName("description")
    public String description;

    @SerializedName("price")
    public Double price;

    @SerializedName("currency")
    public String currency = "VND";

    @SerializedName(value = "quantityTotal", alternate = {"totalQuantity"})
    public Integer totalQuantity;

    @SerializedName("minPerOrder")
    public Integer minPerOrder = 1;

    @SerializedName("maxPerUser")
    public Integer maxPerUser = 10;

    @SerializedName(value = "saleStartAt", alternate = {"salesStartAt"})
    public String salesStartAt;

    @SerializedName(value = "saleEndAt", alternate = {"salesEndAt"})
    public String salesEndAt;

    public TicketTypeRequest() {}

    public TicketTypeRequest(String name, String desc, Double price, Integer totalQty,
                             Integer minPerOrder, Integer maxPerUser) {
        this.ticketTypeName = name;
        this.description = desc;
        this.price = price;
        this.totalQuantity = totalQty;
        this.minPerOrder = minPerOrder;
        this.maxPerUser = maxPerUser;
    }
}
