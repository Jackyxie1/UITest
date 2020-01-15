package com.jacky.uitest.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.jacky.uitest.R;
import com.jacky.uitest.callback.OcrCallback;
import com.jacky.uitest.view.CameraView;
import com.jacky.uitest.view.MyImageView;

public class CameraActivity extends BaseActivity implements OcrCallback {

    private static final String TAG = "CameraActivity";
    private static UsbSerialPort touchPort, otherPort;
    CameraView cameraView;
    MyImageView imageView;
    private String ocrResult, responseTimes, ocrTimes;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initViews();
    }

    private void initViews() {
        cameraView = findViewById(R.id.camera_view);
        imageView = findViewById(R.id.my_image_view);
        cameraView.setTag(imageView);
        String ENG = "英文识别";
        if (TextUtils.equals(getIntent().getStringExtra("english"), ENG))
            cameraView.setOcrMode(ENG);
        String CN = "中文识别";
        if (TextUtils.equals(getIntent().getStringExtra("chinese"), CN))
            cameraView.setOcrMode(CN);
        cameraView.setListener(this);
    }

    public static void setPorts(UsbSerialPort touchPort, UsbSerialPort otherPort) {
        CameraActivity.touchPort = touchPort;
        CameraActivity.otherPort = otherPort;
    }


    @Override
    public void onResult(String result) {
        if (null == result) {
            Log.d(TAG, "ocr error");
            return;
        }
        ocrResult = result;
    }

    @Override
    public void onResponseTimes(String time) {

    }

    @Override
    public void onOcrTimes(String time) {
        ocrTimes = time;
    }
}
