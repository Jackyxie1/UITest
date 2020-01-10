package com.jacky.uitest.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.jacky.uitest.App;
import com.jacky.uitest.R;
import com.jacky.uitest.adapter.SerialPortAdapter;

import java.util.ArrayList;
import java.util.List;

public class SerialPortActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener, Handler.Callback {

    private static final String TAG = "SerialPortActivity";

    private static final String PEN_PID = "7523";
    private static final String PEN_VID = "1a86";

    private static final String INTENT_ACTION_USB_GRANT = "com.jacky.UITest.GRANT_USB";

    ListView serialPortListView;
    Button refreshList, ocrStart, penConnect, otherConnect;

    SerialPortAdapter.ViewHolder viewHolder;

    private SerialPortAdapter mAdapter;
    private List<UsbSerialPort> mPortList;

    private UsbSerialPort touchPort, otherPort;
    private UsbManager mUsbManager;
    static Handler mHandler;
    private UsbDeviceReceiver mUsbDeviceReceiver;

    private boolean isPenConnected, isOtherConnected;

    private static final int MSG_RESET_SERIAL_INFO = 1000;

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

        initView();

        refreshList = findViewById(R.id.refresh_device_list);
        ocrStart = findViewById(R.id.ocr_start);
        refreshList.setOnClickListener(this);
        ocrStart.setOnClickListener(this);


    }

    private void initView() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mAdapter = new SerialPortAdapter();
        mHandler = new Handler(this);
        mPortList = mAdapter.getUsbSerialPortList();
        serialPortListView.setAdapter(mAdapter);
        serialPortListView.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        viewHolder = (SerialPortAdapter.ViewHolder) view.getTag();
        viewHolder.serialPortItem = view.findViewById(R.id.serial_port_item);
        Log.d(TAG, "selected item: 第 " + position + " 个串口");

        if (position >= mPortList.size()) {
            Log.d(TAG, "Illegal position");
            return;
        }
        final UsbDevice device = mPortList.get(position).getDriver().getDevice();
        String PIDOfItem = Integer.toHexString(device.getProductId());
        String VIDOfItem = Integer.toHexString(device.getVendorId());
        switch (PIDOfItem) {
            case PEN_PID:
                viewHolder.serialPortItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            touchPort = mPortList.get(position);
                            final UsbDevice device = mPortList.get(position).getDriver().getDevice();
                            if (!mUsbManager.hasPermission(device)) {
                                PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(SerialPortActivity.this, 0, new Intent(INTENT_ACTION_USB_GRANT), 0);
                                mUsbManager.requestPermission(device, usbPermissionIntent);
                            } else {
                                setTouchPort(touchPort);
                            }
                        }
                        if (!isChecked) {
                            Log.d(TAG, "pen serial port release");
                            ToastUtils.showShort("pen serial port release");
                            setTouchPort(null);
//                            otherPort = null;
                            isPenConnected = false;
                            mHandler.sendEmptyMessage(MSG_RESET_SERIAL_INFO);
                        }
                    }
                });
                if (!viewHolder.serialPortItem.isChecked()) {
                    viewHolder.serialPortItem.setChecked(true);
                } else {
                    if (TextUtils.equals(penConnect.getText(), getResources().getString(R.string.pen_connect))) {
                        ToastUtils.showShort("please disconnect pen serial port");
                    } else {
                        viewHolder.serialPortItem.setChecked(false);
                    }
                }
                break;
            default:
                viewHolder.serialPortItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        otherPort = mPortList.get(position);
                        final UsbDevice device = mPortList.get(position).getDriver().getDevice();
                        if (!mUsbManager.hasPermission(device)) {
                            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(SerialPortActivity.this, 0, new Intent(INTENT_ACTION_USB_GRANT), 0);
                            mUsbManager.requestPermission(device, usbPermissionIntent);
                        } else {
                            setOtherPort(otherPort);
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

                break;
            default:
                break;
        }


    }

    private void refreshList() {
        ThreadUtils.executeBySingle(new ThreadUtils.SimpleTask<List<UsbSerialPort>>() {
            @Override
            public List<UsbSerialPort> doInBackground() throws Throwable {
                UsbManager mUsbManager = (UsbManager) App.getContext().getSystemService(Context.USB_SERVICE);
                final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

                final List<UsbSerialPort> result = new ArrayList<>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    result.addAll(ports);
                }
                return result;
            }

            @Override
            public void onSuccess(List<UsbSerialPort> result) {
                mAdapter.setUsbDevices(result);
            }
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {

        switch (msg.what) {
            case MSG_RESET_SERIAL_INFO:
                if (null == touchPort || null == otherPort) {
                    Log.d(TAG, "have not select any serial port, ready to refresh list");
                    refreshList();
                } else {
                    if (isPenConnected) {

                    }
                }
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
                    ToastUtils.showShort("permission grant");
                } else {
                    Log.d(TAG, "permission denied");
                    ToastUtils.showShort("permission denied");
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
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(INTENT_ACTION_USB_GRANT);
        mUsbDeviceReceiver = new UsbDeviceReceiver();
        registerReceiver(mUsbDeviceReceiver, filter);
    }

    private void unRegisterReceiver() {
        if (null != mUsbDeviceReceiver)
            unregisterReceiver(mUsbDeviceReceiver);
        mUsbDeviceReceiver = null;
    }

    private void findPenPort(){

    }

}
