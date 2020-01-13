package com.jacky.uitest.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.jacky.uitest.R;

public class CameraActivity extends BaseActivity {

    private static UsbSerialPort touchPort, otherPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
    }

    public static void setPorts(UsbSerialPort touchPort, UsbSerialPort otherPort) {
        CameraActivity.touchPort = touchPort;
        CameraActivity.otherPort = otherPort;
    }


}
