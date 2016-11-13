package com.amap.util;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public interface ApiService {
    @GET("api/coordinate/")
    Call<CoordinateGson> getCoordinateData(@Query("role") String role);
}
