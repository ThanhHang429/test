package com.eventapp.app.data.remote.api;

import com.eventapp.app.core.network.ResponseWrapper;
import com.eventapp.app.data.remote.dto.auth.DevAccountDto;
import com.eventapp.app.data.remote.dto.auth.DevTokenDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Xác thực.
 *
 * <p>Backend hiện chỉ triển khai module Khám phá/AI/Tích hợp nên <b>chưa có</b> API đăng ký,
 * đăng nhập bằng mật khẩu hay làm mới token. Hai endpoint dưới đây chỉ tồn tại ở profile
 * {@code dev} và cấp JWT thật cho một {@code userId} có sẵn trong DB — đủ để app đi qua đúng
 * chuỗi xác thực (Bearer token) như bản thật.</p>
 */
public interface AuthApi {

    /** Danh sách tài khoản seed để chọn khi đăng nhập. */
    @GET("dev/accounts")
    Call<ResponseWrapper.Envelope<List<DevAccountDto>>> accounts();

    /** Cấp JWT cho một userId. */
    @POST("dev/token")
    Call<ResponseWrapper.Envelope<DevTokenDto>> token(@Query("userId") long userId);
}
