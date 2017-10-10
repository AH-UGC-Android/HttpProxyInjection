package tn.ln.httpproxyinjection;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tn.ln.httpproxyinjection.net.HttpClient;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button requestBtn_1;
    private TextView text;

    private static final String TAG = "MainActivity";
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestBtn_1 = (Button) findViewById(R.id.request_btn_1);
        text = (TextView) findViewById(R.id.text);
        requestBtn_1.setOnClickListener(this);
    }

    private void request_1() {

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

        HttpClient.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.d("MainActivity", "fail");
                Log.i(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("MainActivity", "success");
                Log.i("MainActivity", response.body().string());

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.request_btn_1:
                request_1();
                break;
        }
    }
}
