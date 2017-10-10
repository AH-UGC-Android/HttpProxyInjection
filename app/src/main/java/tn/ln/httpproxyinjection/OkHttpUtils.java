package tn.ln.httpproxyinjection;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by yuanxx on 2017/9/25.
 */

public class OkHttpUtils {
    private static final String TAG = "OkHttpUtils";
    private static OkHttpClient mOkHttpClient;
    private static OkHttpClient mDnsclient;
    private static Dispatcher mDispatcher;

    /**
     * 通过域名异步获取IP地址
     *
     * @param hostname
     * @return
     */
    public static synchronized String getIpByHostAsync(Context context,String hostname) {
        String decryptedIP = "";
        String encryptedHostName = EncryptUtils.encrypt(Constant.TENCENT_HTTPDNS_KEY, hostname);
        HttpUrl httpUrl = new HttpUrl.Builder().scheme(Constant.TENCENT_HTTPDNS_SCHEME)
                .host(Constant.TENCENT_HTTPDNS_IP)
                .addPathSegment("d")
                .addQueryParameter("dn", encryptedHostName)
                .addQueryParameter("id", Constant.TENCENT_HTTPDNS_ID)
                .addQueryParameter("ttl", String.valueOf(Constant.HTTPDNS_TTL))
                .build();
        Log.i(TAG, "lookup: httpUrl=" + httpUrl.toString());
        Request dnsRequest = new Request.Builder().url(httpUrl).get().build();
        try {
            String ip = OkHttpUtils.getHTTPDnsClient(context).newCall(dnsRequest).execute().body().string();
            if (!TextUtils.isEmpty(ip)) {
                decryptedIP = EncryptUtils.decrypt(Constant.TENCENT_HTTPDNS_KEY, ip);
                Log.i(TAG, "getIpByHostAsync: decryptedIP=" + decryptedIP);
                if (decryptedIP.contains(";")) {
                    //取第一个ip
                    decryptedIP = decryptedIP.substring(0, decryptedIP.indexOf(";"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return decryptedIP;
    }

    /**
     * OkHttp client for httpDNS
     * DNS懒更新策略
     *
     * @return
     */
    public static synchronized OkHttpClient getHTTPDnsClient(Context context) {
        if (mDnsclient == null) {
            final File cacheDir = context.getExternalCacheDir();
            mDnsclient = new OkHttpClient.Builder().dispatcher(getDispatcher())
                    .addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response originalResponse = chain.proceed(chain.request());
                            return originalResponse.newBuilder()
                                    //缓存时间建议设置为120s至600s，不可低于60s,建议在75%TTL时就开始进行域名解析
                                    // 复用OkHttp中的cache
                                    .header("Cache-Control", "max-age=" + Constant.HTTPDNS_TTL * 0.75).build();
                        }
                    })
                    //5MB的文件缓存
                    .cache(new Cache(new File(cacheDir, "httpdns"), 5 * 1024 * 1024))
                    //超时时间建议为5s
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build();
        }
        return mDnsclient;
    }

    /**
     * @return OkHttpClient
     */
    public static synchronized OkHttpClient getOkHttpClient(Context context) {
        if (mOkHttpClient == null) {
            final File cacheDir = context.getExternalCacheDir();
//            mOkHttpClient = new OkHttpClient.Builder()
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addNetworkInterceptor(new InjectionInterceptor())
                    .cache(new Cache(new File(cacheDir, "okhttp"), 60 * 1024 * 1024))
                    .dispatcher(getDispatcher());
            //如果设置代理，走系统DNS服务解析域名;否则使用HTTPDNS
            if (!detectIfProxyExist()) {
                builder.dns(OkHttpDns.getInstance(context));
            }
            mOkHttpClient = builder.build();
        }
        return mOkHttpClient;
    }

    public static synchronized Dispatcher getDispatcher() {
        if (mDispatcher == null) {
            mDispatcher = new Dispatcher();
        }
        return mDispatcher;
    }

    /**
     * 检测系统是否已经设置代理
     */
    public static boolean detectIfProxyExist() {
        boolean IS_ICS_OR_LATER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
        String proxyHost;
        int proxyPort;
        if (IS_ICS_OR_LATER) {
            proxyHost = System.getProperty("http.proxyHost");
            String port = System.getProperty("http.proxyPort");
            proxyPort = Integer.parseInt(port != null ? port : "-1");
        } else {
            proxyHost = android.net.Proxy.getHost(MyApplication.getInstance());
            proxyPort = android.net.Proxy.getPort(MyApplication.getInstance());
        }
        return proxyHost != null && proxyPort != -1;
    }
}
