package com.android.rapido.Utils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by anuj on 28/05/17.
 */

public class NetworkUtils {

    public static OkHttpClient createClient() {
        return new OkHttpClient.
                Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}
