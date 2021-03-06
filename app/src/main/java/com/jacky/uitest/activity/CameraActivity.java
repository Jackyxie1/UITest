package com.jacky.uitest.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
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
import com.jacky.uitest.App;
import com.jacky.uitest.R;
import com.jacky.uitest.callback.OcrCallback;
import com.jacky.uitest.utils.ModeUtil;
import com.jacky.uitest.view.CameraView;
import com.jacky.uitest.view.MyImageView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends BaseActivity implements OcrCallback, Handler.Callback, View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    //TAG
    private static final String TAG = "CameraActivity";
    //global variable
    private String ocrResult = " ", responseTimes, ocrTimes;
    private String selectedMode = null;
    private String ok = "6F6B0D0A";
    private boolean isStop = true, isOk, isStart = false;
    private static final int BUFFER_SIZE = 4096;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    //view initialize
    CameraView cameraView;
    MyImageView imageView;
    Button stop, start, setDefault, returnDefault, test, xTestP, xTestN;
    RadioGroup modeGroup, cameraModeGroup;
    //serial port
    private static UsbSerialPort touchPort, otherPort;
    //others var
    Handler mHandler;
    static Handler receiverHandler;
    ModeUtil modeUtil;
    //serial port command
    private byte[] setDefaultCoordination = new byte[]{0x47, 0x39, 0x32, 0x58, 0x30, 0x59, 0x30, 0x5A, 0x30, 0x0A};
    private byte[] returnDefaultOrigin = new byte[]{0x47, 0x30, 0x30, 0x58, 0x30, 0x59, 0x30, 0x5A, 0x30, 0x0A};
    private byte[] testByte = new byte[]{0x47, 0x30, 0x30, 0x58, 0x30, 0x30, 0x59, 0x2D, 0x36, 0x30, 0x35, 0x5A, 0x30, 0x35, 0x0A};
    private byte[] testByteXP = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x38, 0x30, 0x59, 0x2D, 0x34, 0x38, 0x35, 0x5A, 0x2D, 0x30, 0x30, 0x0A};
    private byte[] testByteXN = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x31, 0x31, 0x30, 0x59, 0x2D, 0x35, 0x35, 0x37, 0x5A, 0x2D, 0x30, 0x30, 0x0A};
    private byte[] goToSettings = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x30, 0x30, 0x59, 0x2D, 0x36, 0x30, 0x35, 0x5A, 0x30, 0x30, 0x0A};//前往截图键
    private byte[] modeWifi_1 = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x30, 0x30, 0x59, 0x2D, 0x34, 0x38, 0x35, 0x5A, 0x30, 0x30, 0x0A};//前往Wi-Fi
    private byte[] modeWifi_2 = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x30, 0x30, 0x59, 0x2D, 0x34, 0x38, 0x35, 0x5A, 0x30, 0x30, 0x0A};//前往SSK
    private byte[] modeWifi_3 = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x31, 0x31, 0x30, 0x59, 0x2D, 0x35, 0x35, 0x37, 0x5A, 0x30, 0x30, 0x0A};//前往CANCEL键
    private byte[] modeWifi_4 = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x38, 0x30, 0x59, 0x2D, 0x36, 0x30, 0x35, 0x5A, 0x30, 0x30, 0x0A};//前往back键
    private byte[] pressDown = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x30, 0x30, 0x59, 0x2D, 0x30, 0x30, 0x30, 0x5A, 0x2D, 0x31, 0x35, 0x0A};
    private byte[] pressUp = new byte[]{0x47, 0x30, 0x30, 0x58, 0x2D, 0x30, 0x30, 0x30, 0x59, 0x2D, 0x30, 0x30, 0x30, 0x5A, 0x2D, 0x30, 0x0A};
    private List<String> modes = new ArrayList<>();
    private List<String> colors = new ArrayList<>();

    private static final int DEFAULT_BAUD_RATE = 115200;

    //handler msg.what
    private static final int MSG_MODE_WIFI = 1000;
    private static final int MSG_MODE_WIFI_1 = 1001;
    private static final int MSG_MODE_WIFI_2 = 1002;
    private static final int MSG_MODE_WIFI_3 = 1003;
    private static final int MSG_MODE_WIFI_4 = 1004;
    private static final int MSG_MODE_PRESS_DOWN = 998;
    private static final int MSG_MODE_PRESS_UP = 999;
    private static final int MSG_MODE_RETURN_TO_ORIGIN = 997;
    private static final int MSG_MODE_CANCEL = -1;

    static {
        System.loadLibrary("native-lib");
    }

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
        cameraModeGroup = findViewById(R.id.camera_mode);


        stop = findViewById(R.id.stop);
        start = findViewById(R.id.start);
        test = findViewById(R.id.test);
        xTestP = findViewById(R.id.x_test_p);
        xTestN = findViewById(R.id.x_test_n);
        setDefault = findViewById(R.id.set_default_coordination);
        returnDefault = findViewById(R.id.return_to_origin);

        stop.setOnClickListener(this);
        start.setOnClickListener(this);
        test.setOnClickListener(this);
        xTestP.setOnClickListener(this);
        xTestN.setOnClickListener(this);
        setDefault.setOnClickListener(this);
        returnDefault.setOnClickListener(this);
        modeGroup.setOnCheckedChangeListener(this);
        cameraModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.front_camera:
                        cameraView.setVisibility(View.GONE);
                        cameraView.setFacing(1);
                        cameraView.setVisibility(View.VISIBLE);
                        break;
                    case R.id.back_camera:
                        cameraView.setVisibility(View.GONE);
                        cameraView.setFacing(0);
                        cameraView.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        });
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
        if (null != selectedMode && !isStart) {
            start(selectedMode);
            isStart = true;
        }
    }

    @Override
    public void onResponseTimes(String time) {

    }

    @Override
    public void onOcrTimes(String time) {
        ocrTimes = time;
    }

    int mode = -1;

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_MODE_WIFI:
                if (!isStop) {
                    mode = 0;
                    writeToTouchPort(goToSettings);
                    if (read())
                        mHandler.sendEmptyMessage(MSG_MODE_PRESS_DOWN);
                }
                break;
            case MSG_MODE_WIFI_1:
                if (ocrResult.contains("Wi-Fi") && !isStop) {
                    mode = 1;
                    writeToTouchPort(modeWifi_1);
                    if (read())
                        mHandler.sendEmptyMessageDelayed(MSG_MODE_PRESS_DOWN, 2000);
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_1, 2000);
                }
                break;
            case MSG_MODE_WIFI_2:
                if (ocrResult.contains("SSK") && !isStop) {
                    mode = 2;
                    writeToTouchPort(modeWifi_2);
                    if (read())
                        mHandler.sendEmptyMessageDelayed(MSG_MODE_PRESS_DOWN, 2000);
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_2, 2000);
                }
                break;
            case MSG_MODE_WIFI_3:
                if (ocrResult.contains("CANCEL") && !isStop) {
                    mode = 3;
                    writeToTouchPort(modeWifi_3);
                    if (read())
                        mHandler.sendEmptyMessageDelayed(MSG_MODE_PRESS_DOWN, 2000);
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_3, 2000);
                }
                break;
            case MSG_MODE_WIFI_4:
                if (ocrResult.contains("SSK") && !isStop) {
                    mode = 4;
                    writeToTouchPort(modeWifi_4);
                    if (read())
                        mHandler.sendEmptyMessageDelayed(MSG_MODE_PRESS_DOWN, 2000);
                } else {
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_4, 2000);
                }
                break;
            case MSG_MODE_PRESS_DOWN:
                switch (mode) {
                    case 0:
                        changePressByte(goToSettings);
                        writeToTouchPort(pressDown);
                        if (read())
                            mHandler.sendEmptyMessageDelayed(MSG_MODE_PRESS_UP, 4000);
                        break;
                    case 1:
                        changePressByte(modeWifi_1);
                        writeToTouchPort(pressDown);
                        if (read())
                            mHandler.sendEmptyMessage(MSG_MODE_PRESS_UP);
                        break;
                    case 2:
                        changePressByte(modeWifi_2);
                        writeToTouchPort(pressDown);
                        if (read())
                            mHandler.sendEmptyMessage(MSG_MODE_PRESS_UP);
                        break;
                    case 3:
                        changePressByte(modeWifi_3);
                        writeToTouchPort(pressDown);
                        if (read())
                            mHandler.sendEmptyMessage(MSG_MODE_PRESS_UP);
                        break;
                    case 4:
                        changePressByte(modeWifi_4);
                        writeToTouchPort(pressDown);
                        if (read())
                            mHandler.sendEmptyMessage(MSG_MODE_PRESS_UP);
                        break;

                }
                break;
            case MSG_MODE_PRESS_UP:
                writeToTouchPort(pressUp);
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_RETURN_TO_ORIGIN, 3000);
                break;
            case MSG_MODE_RETURN_TO_ORIGIN:
                returnToOrigin(mode);
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
            case R.id.test:
                writeToTouchPort(testByte);
                isOk = read();
                Log.d("ok", "return ok? " + isOk + " return hex string: " + ok);
                break;
            case R.id.x_test_p:
                writeToTouchPort(testByteXP);
                isOk = read();
                Log.d("ok", "return ok? " + isOk + " return hex string: " + ok);
                break;
            case R.id.x_test_n:
                writeToTouchPort(testByteXN);
                isOk = read();
                Log.d("ok", "return ok? " + isOk + " return hex string: " + ok);
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
                port = null;
            }
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
            rb.setPaddingRelative(50, 25, 50, 25);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(5, 5, 5, 5);
            modeGroup.addView(rb, params);
            if (j == colors.size() - 1) j = 0;
        }
    }

    private boolean read() {
        //handle incoming data
        if (null != touchPort)
            try {
                int len = touchPort.read(readBuffer.array(), 0);
                if (len < 0) return false;
                else {
                    Log.d(TAG, "read data len = " + len);
                    final byte[] tmp = new byte[len];
                    readBuffer.get(tmp, 0, len);
                    readBuffer.clear();
                    String strTmp = ConvertUtils.bytes2HexString(tmp);
                    int length = strTmp.length();
                    Log.d("ok", "return data to hex string: " + strTmp.substring(length - 8, length));
                    return TextUtils.equals(ok, strTmp.substring(length - 8, length));
                }
            } catch (IOException e) {
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

    private void handlerMoveDelayed(int next, int current) {
        if (isOk)
            mHandler.sendEmptyMessageDelayed(next, 3000);
        else
            mHandler.sendEmptyMessageDelayed(current, 2000);
    }

    private void handlerMove(int next, int current) {
        if (isOk)
            mHandler.sendEmptyMessage(next);
        else mHandler.sendEmptyMessageDelayed(current, 2000);
    }

    private void start(String mode) {
        switch (mode) {
            case "TEST":
                ToastUtils.showShort("test success");
                break;
            case "WIFI_TEST":
//                if (ocrResult.contains("Wi-Fi"))
                mHandler.sendEmptyMessage(MSG_MODE_WIFI);
                break;
            default:
                break;
        }
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

    private void changePressByte(byte[] superByte) {
//        pressUp[5] = superByte[5];
//        pressUp[6] = superByte[6];
//        pressUp[7] = superByte[7];
//        pressUp[10] = superByte[10];
//        pressUp[11] = superByte[11];
//        pressUp[12] = superByte[12];
        System.arraycopy(superByte, 5, pressUp, 5, 8);

//        pressDown[5] = superByte[5];
//        pressDown[6] = superByte[6];
//        pressDown[7] = superByte[7];
//        pressDown[10] = superByte[10];
//        pressDown[11] = superByte[11];
//        pressDown[12] = superByte[12];
        System.arraycopy(superByte, 5, pressDown, 5, 8);
    }

    private void resetPressByte() {
//        pressUp[5] = 0x30;
//        pressUp[6] = 0x30;
//        pressUp[7] = 0x30;
//        pressUp[10] = 0x30;
//        pressUp[11] = 0x30;
//        pressUp[12] = 0x30;
        for (int i = 5; i <= 12; i++) {
            pressUp[i] = 0x30;
        }

//        pressDown[5] = 0x30;
//        pressDown[6] = 0x30;
//        pressDown[7] = 0x30;
//        pressDown[10] = 0x30;
//        pressDown[11] = 0x30;
//        pressDown[12] = 0x30;
        for (int i = 5; i <= 12; i++) {
            pressDown[i] = 0x30;
        }
    }

    private void returnToOrigin(int mode) {
        switch (mode) {
            case 0:
                writeToTouchPort(returnDefaultOrigin);
//                isOk = read();
                resetPressByte();
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_1, 3000);
//                handlerMoveDelayed(MSG_MODE_WIFI_1, MSG_MODE_RETURN_TO_ORIGIN);
                break;
            case 1:
                writeToTouchPort(returnDefaultOrigin);
//                isOk = read();
                resetPressByte();
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_2, 3000);
//                handlerMoveDelayed(MSG_MODE_WIFI_2, MSG_MODE_RETURN_TO_ORIGIN);
                break;
            case 2:
                writeToTouchPort(returnDefaultOrigin);
//                isOk = read();
                resetPressByte();
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_3, 3000);
//                handlerMoveDelayed(MSG_MODE_WIFI_3, MSG_MODE_RETURN_TO_ORIGIN);
                break;
            case 3:
                writeToTouchPort(returnDefaultOrigin);
//                isOk = read();
                resetPressByte();
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_4, 3000);
//                handlerMoveDelayed(MSG_MODE_WIFI_4, MSG_MODE_RETURN_TO_ORIGIN);
                break;
            case 4:
                writeToTouchPort(returnDefaultOrigin);
//                isOk = read();
                resetPressByte();
                if (read())
                    mHandler.sendEmptyMessageDelayed(MSG_MODE_WIFI_1, 3000);
//                handlerMoveDelayed(MSG_MODE_WIFI_1, MSG_MODE_RETURN_TO_ORIGIN);
                isStart = false;
                break;
        }
    }

}
