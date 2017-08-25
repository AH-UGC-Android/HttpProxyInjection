package tn.ln.httpproxyinjection;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    OkHttpClient client ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new InjectionInterceptor())
                .build();


        try {
            run("http://www.weather.com.cn/data/sk/101190408.html");
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

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("MainActivity","ok");
                Log.d("MainActivity",response.body().string());
            }
        });
        ;
    }
}
