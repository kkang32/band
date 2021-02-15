package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothUtil {
    static String TAG = BluetoothUtil.class.getName();
    public static final int ACTION_ENABLE_BT = 101;

    /**
     * 블루투스 상태 체크하여 연결 유도
     * @param activity
     * @return
     */
    public static boolean canUseBluetooth(Activity activity) {
        // 블루투스 어댑터를 구한다

        if (!availableBluetooth()){
            return false;
        }

        if (enableBluetooth()){
            return true;
        }

        Log.d(TAG, "need to request enable bluetooth");
        // 사용자에게 블루투스 활성화를 요청한다
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, ACTION_ENABLE_BT);
        return false;
    }

    /**
     * 블루투스가 사용가능한 장비인지 판단
     * @return
     */
    public static boolean availableBluetooth(){
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        // 블루투스 어댑터가 null 이면 블루투스 장비가 존재하지 않는다.
        if( ba == null ) {
            Log.d(TAG, "cannot use the bluetooth");
            return false;
        }else{
            return true;
        }
    }

    /**
     * 블루투스가 활성화 되어있는지 판단
     * @return
     */
    public static boolean enableBluetooth(){
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        // 블루투스 활성화 상태라면 함수 탈출
        if(ba != null && ba.isEnabled() ) {
            Log.d(TAG, "can use the bluetooth");
            return true;
        }else{
            return false;
        }
    }
}
