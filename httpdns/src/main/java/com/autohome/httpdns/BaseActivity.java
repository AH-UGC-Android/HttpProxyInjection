package com.autohome.httpdns;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * Created by yuanxx on 2017/9/28.
 */

public class BaseActivity extends Activity implements SensorEventListener {
    private static final String TAG = "BaseActivity";
    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private PopupWindow mPopupWindow;
    private static final int UPDATE_INTERVAL = 100;//检测的时间间隔
    private long mLastUpdateTime;//上一次检测的时间
    private float mLastX, mLastY, mLastZ;//上一次检测时，加速度在x、y、z方向上的分量，用于和当前加速度比较求差。
    private int shakeThreshold = 2000;//摇晃检测阈值，决定了对摇晃的敏感程度，越小越敏感。

    @Override
    protected void onStart() {
        super.onStart();
        //获取 SensorManager 负责管理传感器
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        if (mSensorManager != null) {
            //获取加速度传感器
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mAccelerometerSensor != null) {
                mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        // 务必要在pause中注销 mSensorManager
        // 否则会造成界面退出后摇一摇依旧生效的bug
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long currentTime = System.currentTimeMillis();
        long diffTime = currentTime - mLastUpdateTime;
        if (diffTime < UPDATE_INTERVAL) {
            return;
        }
        mLastUpdateTime = currentTime;
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        float deltaX = x - mLastX;
        float deltaY = y - mLastY;
        float deltaZ = z - mLastZ;
        mLastX = x;
        mLastY = y;
        mLastZ = z;
        float delta = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000);
        // 当加速度的差值大于指定的阈值，认为这是一个摇晃
        if (delta > shakeThreshold && mPopupWindow == null) {
            Log.i(TAG, "onSensorChanged: 摇一摇");
            showPopupWindow();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void showPopupWindow() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popupwindow_host_config, null);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.popupwindow_radiogroup);
        final RadioButton radio_debug = (RadioButton) view.findViewById(R.id.popupwindow_type_debug);
        final RadioButton radio_pre_release = (RadioButton) view.findViewById(R.id.popupwindow_type_pre_release);
        final RadioButton radio_release = (RadioButton) view.findViewById(R.id.popupwindow_type_release);
        int type = PreferencesUtils.getInt(BaseActivity.this, Constant.TYPE_DEBUG_RELEASE, -1);
        Log.i(TAG, "showPopupWindow: type=" + type);
        switch (type) {
            case 0:
                radio_debug.setChecked(true);
                break;
            case 1:
                radio_pre_release.setChecked(true);
                break;
            case 2:
                radio_release.setChecked(true);
                break;
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                if (radio_debug.getId() == i) {
                    PreferencesUtils.putInt(BaseActivity.this, Constant.TYPE_DEBUG_RELEASE, 0);
                    Toast.makeText(BaseActivity.this, "测试环境", Toast.LENGTH_LONG).show();
                } else if (radio_pre_release.getId() == i) {
                    PreferencesUtils.putInt(BaseActivity.this, Constant.TYPE_DEBUG_RELEASE, 1);
                    Toast.makeText(BaseActivity.this, "线上测试", Toast.LENGTH_LONG).show();
                } else if (radio_release.getId() == i) {
                    PreferencesUtils.putInt(BaseActivity.this, Constant.TYPE_DEBUG_RELEASE, 2);
                    Toast.makeText(BaseActivity.this, "线上环境", Toast.LENGTH_LONG).show();
                }
                mPopupWindow.dismiss();
            }
        });


        mPopupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        mPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mPopupWindow = null;
            }
        });
    }

}
