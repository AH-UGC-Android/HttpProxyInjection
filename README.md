## HttpDns+OkHttp实践，并加入日志打印及摇一摇切换环境功能

### * HttpDns+OkHttp实践
HttpDNS基于Http协议向腾讯云的DNS服务器发送域名解析请求，替代了基于DNS协议向运营商LocalDNS发起解析请求的传统方式，可以避免LocalDNS造成的域名劫持和跨网访问问题，解决移动互联网服务中域名解析异常带来的困扰。

OkHttp默认使用系统DNS服务InetAddress进行域名解析，但同时也暴露了自定义DNS服务的接口，通过该接口我们可以优雅地使用HttpDns。

1. 自定义DNS接口，代码见OkHttpDns.java。
	
2. 创建OkHttpClient，代码见OkHttpUtils.getOkHttpClient(context)。

### * 日志打印
通过设置HttpLogInterceptor，打印网络请求及响应过程中的各种信息，并可以设置日志打印的级别。

### * 摇一摇切换环境
在R.xml.host_config中配置测试、线上测试、线上环境的域名及IP地址，摇一摇时弹出PopupWindow可以选择环境类型并保存在本地，在解析DNS时读取本地保存的环境类型，测试及线上测试环境直接返回xml文件中配置的IP，线上环境通过腾讯云异步解析接口获取IP。

host_config.xml

```
<resources>
    <!--测试环境-->
    <config
        flag="0"
        host="www.weather.com.cn"
        ip="60.9.1.129"
        type="debug"/>
    <!--线上测试环境-->
    <config
        flag="1"
        host="www.weather.com.cn"
        ip="60.9.1.130"
        type="pre_release"/>
    <!--线上环境-->
    <config
        flag="2"
        host="www.weather.com.cn"
        ip="111.206.239.19"
        type="release"/>
</resources>
```

OkHttpDns.java

```
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
```