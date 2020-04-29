package com.jacky.uitest.adapter;

import android.hardware.usb.UsbDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.jacky.uitest.App;
import com.jacky.uitest.R;

import java.util.ArrayList;
import java.util.List;

public class SerialPortAdapter extends BaseAdapter {

    public List<UsbSerialPort> getUsbSerialPortList() {
        return usbSerialPortList;
    }

    private List<UsbSerialPort> usbSerialPortList = new ArrayList<>();

    public SerialPortAdapter() {
        super();
    }

    public void setUsbDevices(List<UsbSerialPort> ports) {
        this.usbSerialPortList.clear();
        this.usbSerialPortList.addAll(ports);
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return usbSerialPortList != null ? usbSerialPortList.size() : 0;
    }

    @Override
    public UsbSerialPort getItem(int position) {
        return usbSerialPortList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        View view = null;
        if (null == convertView) {
            view = LayoutInflater.from(App.getContext()).inflate(R.layout.item_port, null);
            viewHolder = new ViewHolder();
            viewHolder.serialPortItem = view.findViewById(R.id.serial_port_item);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        final UsbSerialPort port = usbSerialPortList.get(position);
        if (null != port) {
            final UsbSerialDriver driver = port.getDriver();
            if (null != driver) {
                final UsbDevice device = driver.getDevice();
                if (null != device) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("串口 ").append(position).append(" ").append(device.getDeviceName()).append("\t").append(" ").append("的PID为：")
                            .append(Integer.toHexString(device.getProductId())).append("\t").append("的VID为：")
                            .append(Integer.toHexString(device.getVendorId()));
                    viewHolder.serialPortItem.setText(builder.toString());
                }
            }
        }

        return view;
    }

    public static class ViewHolder {
        public CheckBox serialPortItem;
    }

}
