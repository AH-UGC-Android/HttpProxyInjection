package com.autohome.httpdns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * 监听网络状态变化
 * Created by yuanxx on 2017/9/27.
 */

public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";
    private Context mContext;

    Runnable updateCache = new Runnable() {
        @Override
        public void run() {
            // TODO: 2017/9/27 以www.weather.com.cn为例刷新缓存，实际项目中需改为自己的域名
            OkHttpUtils.getIpByHostAsync(mContext, "www.weather.com.cn");
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            mContext = context;
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
            if (activeNetwork != null) { // connected to the internet
                if (activeNetwork.isConnected()) {
                    //在网络类型变化时，如4G切换到wifi，不同wifi间切换等，需要重新执行HttpDNS请求刷新本地缓存
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        Log.i(TAG, "连接到WiFi");
                        new Thread(updateCache).start();
                    } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                        Log.i(TAG, "链接到移动网络");
                        new Thread(updateCache).start();
                    }
                } else {
                    Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                }
            } else {
                Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
            }
        }
    }
}
