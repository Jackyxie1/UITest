package com.jacky.uitest.activity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.jacky.uitest.App;
import com.jacky.uitest.R;
import com.jacky.uitest.adapter.SerialPortAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SerialPortActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener, Handler.Callback {

    private static final String TAG = "SerialPortActivity";
    private static final String INTENT_ACTION_USB_GRANT = "com.jacky.UITest.GRANT_USB";

    private static final String TOUCH_PID = "7523";
    private static final String TOUCH_VID = "1a86";
    private static final String TEST_PID = "6001";
    private static final String TEST_VID = "403";
    private static final String TEST_PID_2 = "2303";
    private static final String TEST_VID_2 = "67b";

    private static final int DEFAULT_BAUD_RATE = 115200;

    private static final int MSG_RESET_SERIAL_INFO = 1000;

    ListView serialPortListView;
    Button refreshList, ocrStart, forceStart, touchConnect, otherConnect;

    SerialPortAdapter.ViewHolder viewHolder;

    private SerialPortAdapter mAdapter;
    private UsbSerialPort touchPort, otherPort;
    private UsbManager mUsbManager;
    static Handler mHandler;
    public static UsbDeviceReceiver mUsbDeviceReceiver;

    private boolean isTouchConnected, isOtherConnected, isChecked;

    private List<UsbSerialPort> portList = new ArrayList<>();


    public void setTouchPort(UsbSerialPort touchPort) {
        this.touchPort = touchPort;
    }

    public void setOtherPort(UsbSerialPort otherPort) {
        this.otherPort = otherPort;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_port);

        initViews();
        initListView();
    }

    private void initListView() {
        mUsbManager = (UsbManager) App.getContext().getSystemService(Context.USB_SERVICE);
        mAdapter = new SerialPortAdapter();
        portList = mAdapter.getUsbSerialPortList();
        mHandler = new Handler(this);
        serialPortListView.setAdapter(mAdapter);
        serialPortListView.setOnItemClickListener(this);

    }

    private void initViews() {
        serialPortListView = findViewById(R.id.serial_port_list);
        refreshList = findViewById(R.id.refresh_device_list);
        ocrStart = findViewById(R.id.ocr_start);
        forceStart = findViewById(R.id.force_start);
        touchConnect = findViewById(R.id.touch);
        otherConnect = findViewById(R.id.other);


        refreshList.setOnClickListener(this);
        ocrStart.setOnClickListener(this);
        forceStart.setOnClickListener(this);
        touchConnect.setOnClickListener(this);
        otherConnect.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
        mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != touchPort) {
            try {
                touchPort.close();
            } catch (IOException e) {
            }
            touchPort = null;
        }

        if (null != otherPort) {
            try {
                touchPort.close();
            } catch (IOException e) {
            }
            otherPort = null;
        }
        unRegisterReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterReceiver();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        viewHolder = (SerialPortAdapter.ViewHolder) view.getTag();
        viewHolder.serialPortItem = view.findViewById(R.id.serial_port_item);
        Log.d(TAG, "selected item: 第" + (position + 1) + "个串口");

        if (position >= portList.size()) {
            Log.d(TAG, "Illegal position");
            return;
        }

        UsbSerialPort port = portList.get(position);
        final UsbDevice device = port.getDriver().getDevice();

        String PIDOfItem = Integer.toHexString(device.getProductId());
        String VIDOfItem = Integer.toHexString(device.getVendorId());
        switch (PIDOfItem) {
            case TOUCH_PID:
            case TEST_PID:
            case TEST_PID_2:
                viewHolder.serialPortItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            touchPort = portList.get(position);
                            final UsbDevice device = touchPort.getDriver().getDevice();
                            if (!mUsbManager.hasPermission(device)) {
                                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(SerialPortActivity.this, 0, new Intent(INTENT_ACTION_USB_GRANT), 0);
                                mUsbManager.requestPermission(device, usbPermissionIntent);
                            } else {
                                setTouchPort(touchPort);
                            }
                        }
                        if (!isChecked) {
                            Log.d(TAG, "touch serial port release");
                            ToastUtils.showShort("touch serial port release");
                            setTouchPort(null);
//                            otherPort = null;
                            isTouchConnected = false;
                            mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
                        }
                    }
                });
                if (!viewHolder.serialPortItem.isChecked()) {
                    viewHolder.serialPortItem.setChecked(true);
                } else {
                    if (TextUtils.equals(touchConnect.getText(), getResources().getString(R.string.touch_connect))) {
                        ToastUtils.showShort("please disconnect touch serial port");
                    } else {
                        viewHolder.serialPortItem.setChecked(false);
                    }
                }
                break;
            default:
                viewHolder.serialPortItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            otherPort = portList.get(position);
                            final UsbDevice device = otherPort.getDriver().getDevice();
                            if (!mUsbManager.hasPermission(device)) {
                                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(SerialPortActivity.this, 0, new Intent(INTENT_ACTION_USB_GRANT), 0);
                                mUsbManager.requestPermission(device, usbPermissionIntent);
                            } else {
                                setOtherPort(otherPort);
                            }
                        }
                        if (!isChecked) {
                            Log.d(TAG, "other serial port release");
                            ToastUtils.showShort("other serial port release");
                            setOtherPort(null);
                            isOtherConnected = false;
                            mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
                        }
                    }
                });
                if (!viewHolder.serialPortItem.isChecked()) {
                    viewHolder.serialPortItem.setChecked(true);
                } else {
                    if (TextUtils.equals(otherConnect.getText(), getResources().getString(R.string.other_connect))) {
                        ToastUtils.showShort("please disconnect other serial port");
                    } else {
                        viewHolder.serialPortItem.setChecked(false);
                    }
                }
                break;
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.refresh_device_list:
                refreshList();
                break;
            case R.id.ocr_start:
                if (!TextUtils.equals(touchConnect.getText(), getResources().getString(R.string.touch_connect))) {
                    ToastUtils.showShort("请至少连接笔串口");
                    return;
                }
                setPorts(touchPort, otherPort);
                startCameraActivity();
                break;
            case R.id.touch:
                touchStateChange();
                break;
            case R.id.other:
                otherStateChange();
                break;
            case R.id.force_start:
                if (TextUtils.equals(touchConnect.getText(), getResources().getString(R.string.touch_connect))) {
                    ToastUtils.showShort("当前已连接串口，请直接开始识别");
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(SerialPortActivity.this);
                builder.setTitle("警告");
                builder.setMessage("确定要强行进入识别页吗？这将无法使用串口功能");
                builder.setCancelable(false);
                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startCameraActivity();
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
            default:
                break;
        }

    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {

        switch (msg.what) {
            case MSG_RESET_SERIAL_INFO:
                if (null == touchPort || null == otherPort) {
                    Log.d(TAG, "have not select any serial port, ready to refresh list");
                    refreshList();
                } else {
                    if (isTouchConnected) {
                        Log.d(TAG, "touch serial device is connecting");
                    } else {
                        Log.d(TAG, "touch serial device found but not connect");
                    }

                    if (isOtherConnected) {
                        Log.d(TAG, "other serial device is connecting");
                    } else {
                        Log.d(TAG, "other serial port found but not connect");
                    }
                }
                break;
            default:
                break;
        }
        return false;
    }

    public static class UsbDeviceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) {
                return;
            }
            String action = intent.getAction();
            if (TextUtils.equals(INTENT_ACTION_USB_GRANT, action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "permission grant");
                } else {
                    Log.d(TAG, "permission denied");
                }
            }
            if (TextUtils.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED, action)) {
                Log.d(TAG, "usb attached");
                mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
            } else if (TextUtils.equals(UsbManager.ACTION_USB_DEVICE_DETACHED, action)) {
                Log.d(TAG, "usb detached");
                mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
            }
        }

    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(INTENT_ACTION_USB_GRANT);
        mUsbDeviceReceiver = new UsbDeviceReceiver();
        registerReceiver(mUsbDeviceReceiver, filter);
    }

    private void unRegisterReceiver() {
        if (null != mUsbDeviceReceiver) {
            unregisterReceiver(mUsbDeviceReceiver);
            mUsbDeviceReceiver = null;
        }
    }

    private void touchStateChange() {
        if (null == touchPort) {
            Log.d(TAG, "touch serial port is not checked");
            ToastUtils.showShort("touch serial port is not checked");
            touchConnect.setText(getResources().getString(R.string.touch_not_connect));
        } else {
            if (isTouchConnected) {
                isTouchConnected = false;
                touchConnect.setText(getResources().getString(R.string.touch_not_connect));
            } else {
                isTouchConnected = true;
                touchConnect.setText(getResources().getString(R.string.touch_connect));
            }
        }
    }

    private void otherStateChange() {
        if (null == otherPort) {
            Log.d(TAG, "other serial port is not checked");
            ToastUtils.showShort("other serial port is not checked");
            otherConnect.setText(getResources().getString(R.string.other_not_connect));
        } else {
            if (isOtherConnected) {
                isOtherConnected = false;
                otherConnect.setText(getResources().getString(R.string.other_not_connect));
            } else {
                isOtherConnected = true;
                otherConnect.setText(getResources().getString(R.string.other_connect));
            }
        }
    }

    private void refreshList() {
        ThreadUtils.executeBySingle(new ThreadUtils.SimpleTask<List<UsbSerialPort>>() {
            @Override
            public List<UsbSerialPort> doInBackground() throws Throwable {
                UsbManager mUsbManager = (UsbManager) App.getContext().getSystemService(Context.USB_SERVICE);
                List<UsbSerialDriver> drivers = null;
                if (null != mUsbManager) {
                    drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                }

                final List<UsbSerialPort> result = new ArrayList<>();
                if (null != drivers)
                    for (final UsbSerialDriver driver : drivers) {
                        final List<UsbSerialPort> ports = driver.getPorts();
                        result.addAll(ports);
                    }
                return result;
            }

            @Override
            public void onSuccess(List<UsbSerialPort> result) {
                mAdapter.setUsbDevices(result);
                ToastUtils.showShort(result.size() + " device(s) found");
            }
        });
    }

    private void setPorts(UsbSerialPort touchPort, UsbSerialPort otherPort) {
        CameraActivity.setPorts(touchPort, otherPort);
    }

    private void startCameraActivity() {
        Intent intent = new Intent(SerialPortActivity.this, CameraActivity.class);
        intent.putExtra("chinese", getIntent().getStringExtra("chinese"));
        intent.putExtra("english", getIntent().getStringExtra("english"));
        startActivity(intent);
    }

}
