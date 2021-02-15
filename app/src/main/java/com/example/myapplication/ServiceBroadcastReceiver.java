package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class ServiceBroadcastReceiver extends BroadcastReceiver {


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BandService.MyBinder binder = (BandService.MyBinder) service;
            BandService mService = binder.getService();
            mService.connectBand();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 예기치 않은 종료
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            Log.d("BootReceiver", "부팅완료. 서비스 시작");
            Intent tempService = new Intent(
                    context,
                    BandService.class); // 이동할 컴포넌트

            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(tempService);
            }
            else {
                context.startService(tempService); // 서비스 시작
            }
        }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){ //블루투스 상태 변경 감시
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == BluetoothAdapter.STATE_OFF) {
                Log.d("BroadcastActions", "Bluetooth is off");
                NotificationUtil.notification(NotificationUtil.ID_ALERT, "HI-U", "블루투스가 비활성화 되어있습니다. 여기를 눌러 블루투스를 활성화 하세요.", context);
            }else{
                Intent tempService = new Intent(
                        context,
                        BandService.class); // 이동할 컴포넌트
                context.bindService(tempService, mConnection, Context.BIND_AUTO_CREATE);
            }
        }
            
    }
}
