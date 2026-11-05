package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.admincategory.AdminCategoryDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Quản lý danh mục (FR-A09). */
public interface AdminCategoryApi {

    @GET("admin/categories")
    Call<ResponseWrapper.Envelope<List<AdminCategoryDtos.Category>>> tree(
            @Query("includeInactive") boolean includeInactive);

    @POST("admin/categories")
    Call<ResponseWrapper.Envelope<AdminCategoryDtos.Category>> create(
            @Body AdminCategoryDtos.CreateRequest request);

    @PUT("admin/categories/{categoryId}")
    Call<ResponseWrapper.Envelope<AdminCategoryDtos.Category>> update(
            @Path("categoryId") long categoryId,
            @Body AdminCategoryDtos.UpdateRequest request);

    @POST("admin/categories/{categoryId}/hide")
    Call<ResponseWrapper.Envelope<AdminCategoryDtos.Category>> hide(
            @Path("categoryId") long categoryId,
            @Body AdminCategoryDtos.HideRequest request);

    @POST("admin/categories/{categoryId}/unhide")
    Call<ResponseWrapper.Envelope<AdminCategoryDtos.Category>> unhide(
            @Path("categoryId") long categoryId);

    @PUT("admin/categories/order")
    Call<ResponseWrapper.Envelope<List<AdminCategoryDtos.Category>>> reorder(
            @Body AdminCategoryDtos.OrderRequest request);

    @DELETE("admin/categories/{categoryId}")
    Call<ResponseWrapper.Envelope<Void>> delete(@Path("categoryId") long categoryId);
}
