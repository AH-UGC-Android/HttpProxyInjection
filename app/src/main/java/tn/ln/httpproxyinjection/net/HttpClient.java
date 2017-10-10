package tn.ln.httpproxyinjection.net;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by heshiqi on 17/9/25.
 */

public class HttpClient {

    private static final int CONNECT_TIME_OUT = 30;
    private static final int READ_TIME_OUT = 20;

    private static OkHttpClient OKHTTPCLIENT;

    public static OkHttpClient getClient() {

        return getOkHttpClient();
    }


    private static OkHttpClient getOkHttpClient() {
        final OkHttpClient okHttpClient = OKHTTPCLIENT;
        if (okHttpClient == null) {
            synchronized (HttpClient.class) {
                if (okHttpClient == null) {
                    HttpLogInterceptor logInterceptor=new HttpLogInterceptor();
                    logInterceptor.setLevel(HttpLogInterceptor.LEVEL_BODY);
                    OKHTTPCLIENT = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS).
                                    readTimeout(READ_TIME_OUT, TimeUnit.SECONDS)
                            .addInterceptor(logInterceptor)
                            .addNetworkInterceptor(new NetworkInterceptor())
                            .build();
                }
            }
        }
        return OKHTTPCLIENT;
    }


}
