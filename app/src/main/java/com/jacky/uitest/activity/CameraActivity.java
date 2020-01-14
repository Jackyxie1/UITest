package com.jacky.uitest.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.jacky.uitest.R;
import com.jacky.uitest.utils.OcrUtil;
import com.jacky.uitest.view.CameraView;
import com.jacky.uitest.view.MyImageView;

public class CameraActivity extends BaseActivity {

    private static UsbSerialPort touchPort, otherPort;
    CameraView cameraView;
    MyImageView imageView;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraView = findViewById(R.id.camera_view);
        imageView = findViewById(R.id.my_image_view);
        cameraView.setTag(imageView);
    }

    public static void setPorts(UsbSerialPort touchPort, UsbSerialPort otherPort) {
        CameraActivity.touchPort = touchPort;
        CameraActivity.otherPort = otherPort;
    }


}
