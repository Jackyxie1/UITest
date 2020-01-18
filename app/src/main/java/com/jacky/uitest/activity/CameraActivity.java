package com.jacky.uitest.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.jacky.uitest.App;
import com.jacky.uitest.R;
import com.jacky.uitest.callback.OcrCallback;
import com.jacky.uitest.utils.ModeUtil;
import com.jacky.uitest.view.CameraView;
import com.jacky.uitest.view.MyImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends BaseActivity implements OcrCallback, Handler.Callback, View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    //TAG
    private static final String TAG = "CameraActivity";
    //global variable
    private String ocrResult, responseTimes, ocrTimes;
    private String selectedMode;
    private String ok;
    private boolean isStop;
    //view initialize
    CameraView cameraView;
    MyImageView imageView;
    Button stop, start, setDefault, returnDefault;
    RadioGroup modeGroup;
    //serial port
    private static UsbSerialPort touchPort, otherPort;
    private SerialInputOutputManager IOManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    //others var
    Handler mHandler;
    static Handler receiverHandler;
    ModeUtil modeUtil;
    CameraModeListener cmListener;
    //serial port command
    private byte[] setDefaultCoordination = new byte[]{0x47, 0x39, 0x32, 0x58, 0x30, 0x59, 0x30, 0x5A, 0x30, 0x0A};
    private byte[] returnDefaultOrigin = new byte[]{0x47, 0x30, 0x30, 0x58, 0x30, 0x59, 0x30, 0x5A, 0x30, 0x0A};
    private byte[] goToSettings = new byte[]{};
    private byte[] modeWifi_1 = new byte[]{};
    private byte[] modeWifi_2 = new byte[]{};
    private byte[] modeWifi_3 = new byte[]{};
    private byte[] modeWifi_4 = new byte[]{};
    private byte[] modeWifi_5 = new byte[]{};

    private List<String> modes = new ArrayList<>();
    private List<String> colors = new ArrayList<>();

    private static final int DEFAULT_BAUD_RATE = 115200;

    //handler msg.what
    private static final int MSG_MODE_WIFI = 1000;
    private static final int MSG_MODE_WIFI_1 = 1001;
    private static final int MSG_MODE_WIFI_2 = 1002;
    private static final int MSG_MODE_WIFI_3 = 1003;
    private static final int MSG_MODE_WIFI_4 = 1004;
    private static final int MSG_MODE_CANCEL = -1;

    static {
        System.loadLibrary("native-lib");
    }

    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(final byte[] data) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    read(data);
                    Log.d(TAG, "ok: " + ok);
                }
            });
        }

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "runner stop");
            ok = "error";
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        initViews();
        getModes();
        addRb();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != touchPort)
            connect(touchPort);
        if (null != otherPort)
            connect(otherPort);
//        resetIOManagerState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != touchPort) {
            try {
                touchPort.close();
            } catch (IOException e) {
            }
            touchPort = null;
        }
//        stopIOManager();
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
        mHandler = new Handler(this);

        modeGroup = findViewById(R.id.mode_group);


        stop = findViewById(R.id.stop);
        start = findViewById(R.id.start);
        setDefault = findViewById(R.id.set_default_coordination);
        returnDefault = findViewById(R.id.return_to_origin);

        stop.setOnClickListener(this);
        start.setOnClickListener(this);
        setDefault.setOnClickListener(this);
        returnDefault.setOnClickListener(this);
        modeGroup.setOnCheckedChangeListener(this);
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
        if (null != selectedMode)
            start(selectedMode);
    }

    @Override
    public void onResponseTimes(String time) {

    }

    @Override
    public void onOcrTimes(String time) {
        ocrTimes = time;
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (!isStop)
            switch (msg.what) {
                case MSG_MODE_WIFI:
                    writeToTouchPort(goToSettings);
                    handlerMove(MSG_MODE_WIFI_1, MSG_MODE_WIFI);
                    break;
                case MSG_MODE_WIFI_1:
                    writeToTouchPort(modeWifi_1);
                    handlerMove(MSG_MODE_WIFI_2, MSG_MODE_WIFI_1);
                    break;
                case MSG_MODE_WIFI_2:
                    writeToTouchPort(modeWifi_2);
                    handlerMove(MSG_MODE_WIFI_3, MSG_MODE_WIFI_2);
                    break;
                case MSG_MODE_WIFI_3:
                    writeToTouchPort(modeWifi_3);
                    handlerMove(MSG_MODE_WIFI_4, MSG_MODE_WIFI_3);
                    break;
                case MSG_MODE_WIFI_4:
                    writeToTouchPort(modeWifi_4);
                    handlerMove(MSG_MODE_WIFI_1, MSG_MODE_WIFI_4);
                    break;
                default:
                    break;
            }
        return false;
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder builder;
        switch (v.getId()) {
            case R.id.stop:
                builder = createAlertDialog(CameraActivity.this, "提示", "是否要暂停机器");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (null != mHandler)
                            mHandler.removeCallbacksAndMessages(null);
                        isStop = true;
                        setFullScreen();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setFullScreen();
                    }
                });
                builder.show();
                break;
            case R.id.start:
                isStop = false;
                if (null != selectedMode)
                    start(selectedMode);
                break;
            case R.id.set_default_coordination:
                builder = createAlertDialog(CameraActivity.this, "提示", "是否要设置坐标原点");
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (null == touchPort) {
                            Log.d(TAG, "touch port is null");
                            return;
                        }
                        try {
                            touchPort.write(setDefaultCoordination, 100);
                        } catch (IOException e) {
                        }
                        setFullScreen();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setFullScreen();
                    }
                });
                builder.show();
                break;
            case R.id.return_to_origin:
                if (null == touchPort) {
                    Log.d(TAG, "touch port is null");
                    return;
                }
                try {
                    Log.d(TAG, "touchPort: " + touchPort);
                    touchPort.write(returnDefaultOrigin, 100);
                } catch (IOException e) {
                }
                break;

        }

    }

    @Override
    public void onCheckedChanged(final RadioGroup group, int checkedId) {
        final RadioButton rb = group.findViewById(checkedId);
        AlertDialog.Builder builder = createAlertDialog(CameraActivity.this, "注意", "确认选择模式：" + rb.getText().toString() + " ？");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedMode = rb.getText().toString();
                setFullScreen();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setFullScreen();
            }
        });
        builder.show();
    }

    private void setFullScreen() {
        if (Build.VERSION.SDK_INT > 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private AlertDialog.Builder createAlertDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        return builder;
    }

    private void connect(UsbSerialPort port) {
        final UsbManager mUsbManager = (UsbManager) App.getContext().getSystemService(USB_SERVICE);
        UsbDeviceConnection connection = null;
        if (null != mUsbManager)
            connection = mUsbManager.openDevice(port.getDriver().getDevice());
        if (null == connection) {
            Log.d(TAG, "can not get connection");
            ToastUtils.showShort("connection failed");
            return;
        }
        try {
            port.open(connection);
            port.setParameters(DEFAULT_BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            ToastUtils.showShort("Error: " + e.getMessage());
            try {
                port.close();
            } catch (IOException e1) {
            }
            port = null;
        }
    }

    private void getModes() {
        modeUtil = new ModeUtil();
        modeUtil.addMode();
        modes = modeUtil.modes;
        modeUtil.addColor();
        colors = modeUtil.colors;

    }

    private void addRb() {
        for (int i = 0, j = 0; i < modes.size(); i++, j++) {
            RadioButton rb = new RadioButton(CameraActivity.this);
            rb.setButtonDrawable(null);
            rb.setPadding(5, 5, 5, 5);
            rb.setBackground(getBackgroundSelector(Color.parseColor(colors.get(j))));
            rb.setText(modes.get(i));
            rb.setTextSize(14);
            rb.setTextColor(getTextColorSelector());
            rb.setPaddingRelative(50,25,50,25);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            modeGroup.addView(rb, params);
            if (j == colors.size() - 1) j = 0;
        }
    }

    private void read(byte[] data) {
        char[] tmp = ConvertUtils.bytes2Chars(data);
        ok = Arrays.toString(tmp);
    }

    private boolean isOk(String ok) {
        if (ok.equals("ok")) {
            ok = "";
            return true;
        }
        return false;
    }

    private void writeToTouchPort(byte[] data) {
        if (null != touchPort)
            try {
                touchPort.write(data, 100);
            } catch (IOException e) {
            }
    }

    private void handlerMove(int next, int current) {
        if (isOk(ok))
            mHandler.sendEmptyMessage(next);
        else
            mHandler.sendEmptyMessage(current);
    }

    private void start(String mode) {
        switch (mode) {
            case "TEST":
                ToastUtils.showLong("test success");
                break;
            case "WIFI_TEST":
                if (ocrResult.contains("Wi-Fi"))
                    mHandler.sendEmptyMessage(MSG_MODE_WIFI);
                break;
            default:
                break;
        }
    }

    private void startIOManager() {
        if (null != touchPort) {
            Log.d(TAG, "start IOManager");
            IOManager = new SerialInputOutputManager(touchPort, mListener);
            mExecutor.submit(IOManager);
        }
    }

    private void stopIOManager() {
        if (null != IOManager) {
            Log.d(TAG, "stop IOManager");
            IOManager.stop();
            IOManager = null;
        }
    }

    private void resetIOManagerState() {
        stopIOManager();
        startIOManager();
    }

    interface CameraModeListener {
        void onCameraMode(int facing);
    }

    private void setCameraModeListener(CameraModeListener cmListener) {
        this.cmListener = cmListener;
    }

    public void setModeListener(CameraModeListener cmListener) {
        setCameraModeListener(cmListener);
    }

    private StateListDrawable getBackgroundSelector(int color) {
        GradientDrawable normal = new GradientDrawable();
        GradientDrawable pressed = new GradientDrawable();
        StateListDrawable modeSelector = new StateListDrawable();

        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setStroke(SizeUtils.dp2px(1), Color.parseColor("#EFFF31"));
        normal.setColor(Color.parseColor("#000000"));
        normal.setCornerRadius(SizeUtils.dp2px(5));

        pressed.setShape(GradientDrawable.RECTANGLE);
        pressed.setStroke(SizeUtils.dp2px(1), Color.parseColor("#EFFF31"));
        normal.setColor(color);
        pressed.setCornerRadius(SizeUtils.dp2px(5));

        modeSelector.addState(new int[]{android.R.attr.state_checked}, normal);
        modeSelector.addState(new int[]{}, pressed);
        return modeSelector;
    }

    private ColorStateList getTextColorSelector() {
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_checked};
        states[1] = new int[]{};
        int normal = Color.parseColor("#000000");
        int pressed = Color.parseColor("#FFFFFF");
        int[] colors = new int[]{pressed, normal};
        return new ColorStateList(states, colors);
    }

}
