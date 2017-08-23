package tn.ln.httpproxyinjection;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhchyu999 on 2017/8/19.
 */

public class InjectionInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request=chain.request();
        request.body().
        Response response= chain.proceed(request);
        return response;
    }
}
