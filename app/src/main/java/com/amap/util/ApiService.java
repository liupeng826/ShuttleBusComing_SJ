package com.amap.util;


import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public interface ApiService {
    @GET("api/coordinate/")
    Observable<CoordinateGson> getCoordinateData(@Query("role") String role);
}
