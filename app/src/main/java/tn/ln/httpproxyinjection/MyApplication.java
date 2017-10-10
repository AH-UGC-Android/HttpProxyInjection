package tn.ln.httpproxyinjection;

import android.app.Application;
import android.util.Log;

/**
 * Created by yuanxx on 2017/9/25.
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication instance = null;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");
        instance = this;
    }

}
