package com.autohome.httpdns;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Created by zhchyu999 on 2017/8/19.
 */

public class InjectionInterceptor implements Interceptor {
    public static final String TAG="InjectionInterceptor";
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request=chain.request();
        Log.d(TAG,request.url().encodedPath());
//        request.body().
        Response response= chain.proceed(request);
//        Log.d(TAG,response.body().string());
        return response;
    }


    public static String bodyToString(final RequestBody request){
        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            if(copy != null)
                copy.writeTo(buffer);
            else
                return "";
            return buffer.readUtf8();
        }
        catch (final IOException e) {
            return "did not work";
        }
    }
}
