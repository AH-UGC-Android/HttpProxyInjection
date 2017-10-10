package tn.ln.httpproxyinjection;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

/**
 * Created by yuanxx on 2017/9/22.
 */

public class OkHttpDns implements Dns {
    private static final String TAG = "OkHttpDns";
    private static OkHttpDns instance = null;
    private Context mContext;

    private OkHttpDns(Context context) {
        mContext = context;
    }


    public static OkHttpDns getInstance(Context context) {
        if (instance == null) {
            instance = new OkHttpDns(context);
        }
        return instance;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        Log.i(TAG, "lookup() called with: hostname = [" + hostname + "]");
        if (TextUtils.isEmpty(hostname)) {
            throw new UnknownHostException("hostname == null");
        }
        String ip = "";
        int type = PreferencesUtils.getInt(mContext, Constant.TYPE_DEBUG_RELEASE, -1);
        Log.i(TAG, "lookup: type=" + type);
        //测试环境和线上测试环境用xml文件中配置的ip
        if (type == 0 || type == 1) {
            ip = XmlParser.getInstance(mContext).getIP(String.valueOf(type), hostname);
        } else {
            //线上环境通过异步解析接口获取ip
            ip = OkHttpUtils.getIpByHostAsync(mContext, hostname);
        }
        Log.i(TAG, "lookup: ip=" + ip);
        if (TextUtils.isEmpty(ip)) {
            //如果返回ip为空,或者使用代理，走系统DNS服务解析域名
            Log.e(TAG, "lookup: IP无效");
            return Dns.SYSTEM.lookup(hostname);
        }
        return Arrays.asList(InetAddress.getAllByName(ip));
    }
}
