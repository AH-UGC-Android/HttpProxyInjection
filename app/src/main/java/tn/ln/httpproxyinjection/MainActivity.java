package tn.ln.httpproxyinjection;

import android.os.Bundle;
import android.util.Log;

import com.autohome.httpdns.BaseActivity;
import com.autohome.httpdns.OkHttpUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        client = new OkHttpClient.Builder()
//                .addNetworkInterceptor(new InjectionInterceptor())
//                .dns(OkHttpDns.getInstance(this))
//                .build();

        client = OkHttpUtils.getOkHttpClient(this);
        try {
            run("http://www.weather.com.cn/data/sk/101190408.html");
//            run("https://www.baidu.com");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

//        client.
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("MainActivity", response.body().string());
            }
        });
    }
}
