package com.example.myapplication;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import com.partron.wearable.band.sdk.core.BandResultCode;

public class HandleBandListener extends BandResultCode implements BandListener  {
    String TAG = getClass().getName();
    private Handler mHandler;

    /*
     public static final int SUCCESS = 0;
    public static final int FAIL = 1;
    public static final int REQUEST_TIMEOUT_ERROR = 2;
    public static final int BLUETOOTH_DISABLE = 3;
    public static final int BAND_CONNECTION_FAIL = 4;
    public static final int BAND_SCAN_TIMEOUT = 5;
    public static final int BAND_DISCONNECD = 6;
    public static final int BAND_SYNCING = 7;
    public static final int BAND_NO_SYNC_DATA = 8;
    public static final int BAND_CHARGING = 9;
    public static final int BAND_FIRMWARE_UPDATING = 16;
    public static final int BAND_RECONNECTING = 17;
    public static final int BAND_NO_SEARCH = 255;
     */


    public static final int FIND_BLUETOOTH_DEVICE = 18;
    public static final int CONNECTED = 19;
    public static final int DISCONNECTED = 20;

    public HandleBandListener(){
    }

    public HandleBandListener(Handler handler){
        this.mHandler = handler;
    }

    @Override
    public void findDevice(BluetoothDevice device) {
        if (this.mHandler != null){
            Message message = mHandler.obtainMessage();
            message.what = FIND_BLUETOOTH_DEVICE;
            message.obj = device;

            mHandler.sendMessage(message);
        }


    }

    @Override
    public void findTimeout() {
        if (this.mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = BAND_SCAN_TIMEOUT;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void disabledBluetooth() {
        if (this.mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = BLUETOOTH_DISABLE;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void connectedBand() {
        if (this.mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = CONNECTED;
            mHandler.sendMessage(message);
        }

    }

    @Override
    public void disconnectedBand() {
        if (this.mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = DISCONNECTED;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void connectFail() {
        if (this.mHandler != null) {
            Message message = mHandler.obtainMessage();
            message.what = BAND_CONNECTION_FAIL;
            mHandler.sendMessage(message);
        }
    }

    @Override
    public boolean available() {
        return this.mHandler != null;
    }

}
