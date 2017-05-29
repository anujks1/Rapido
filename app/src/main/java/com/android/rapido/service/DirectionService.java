package com.android.rapido.service;

import com.android.rapido.Utils.NetworkUtils;
import com.google.gson.JsonElement;

import org.json.JSONObject;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

/**
 * Created by anuj on 28/05/17.
 */

public interface DirectionService {

    String BASE_URL="https://maps.googleapis.com/maps/api/directions/";

    @Headers({"Accept: application/json", "content-type: application/x-www-form-urlencoded"})
    @GET("json")
    Observable<JsonElement> getDirection(@Query("origin") String origin, @Query("destination") String destination ,@Query("alternatives") boolean alternative);

    class Creator {
        public static DirectionService getService() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(NetworkUtils.createClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();
            return retrofit.create(DirectionService.class);
        }
    }
}
