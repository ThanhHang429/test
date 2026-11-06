package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.catalog.CatalogDtos;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/** Danh mục công khai (FR-U04). */
public interface CategoryApi {

    @GET("categories")
    Call<ResponseWrapper.Envelope<List<CatalogDtos.CategoryNode>>> tree();
}
