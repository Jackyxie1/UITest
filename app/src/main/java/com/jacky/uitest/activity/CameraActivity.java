package com.jacky.uitest.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.jacky.uitest.R;

public class CameraActivity extends BaseActivity {

    private static UsbSerialPort touchPort, otherPort;
    TextView test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        test = findViewById(R.id.test);
        setText();
    }

    public static void setPorts(UsbSerialPort touchPort, UsbSerialPort otherPort) {
        CameraActivity.touchPort = touchPort;
        CameraActivity.otherPort = otherPort;
    }

    private void setText() {
        String cn, eng;
        cn = getIntent().getStringExtra("chinese");
        eng = getIntent().getStringExtra("english");
        if (cn != null && cn.length() > 0) {
            test.setText(cn);
        } else if (eng != null && eng.length() > 0) {
            test.setText(eng);
        }
    }

}
