package com.example.myapplication;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

public interface BandListener {

    /**
     * 블루투스 장비를 찾았을때
     * @param device
     */
    public void findDevice(BluetoothDevice device);

    /**
     * 장비 검색중 타임아웃 발생. 탐색 종료
     */
    public void findTimeout();

    /**
     * 블루투스가 비활성화됨
     */
    public void disabledBluetooth();

    /**
     * 밴드 연결됨
     */
    public void connectedBand();

    /**
     * 밴드 연결 끊김
     */
    public void disconnectedBand();

    /**
     * 밴드 연결 실패
     */
    public void connectFail();

    public boolean available();
}
