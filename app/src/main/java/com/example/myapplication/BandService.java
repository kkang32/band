package com.example.myapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



public class BandService extends Service {
    String TAG = getClass().getName();
    boolean bRunning = false;
    private IBinder mBinder = new MyBinder();
    private BandProvider bandProvider;
    private ServiceBroadcastReceiver mReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // 서비스에서 가장 먼저 호출됨(최초에 한번만)
        Log.d(TAG, "서비스의 onCreate");
        startForegroundService();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction("android.intent.action.BOOT_COMPLETE");

        if(mReceiver == null) {
            mReceiver = new ServiceBroadcastReceiver();
            this.registerReceiver(mReceiver, filter);
        }else{
            this.unregisterReceiver(mReceiver);
        }


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 서비스가 호출될 때마다 실행
        Log.d(TAG, "서비스의 onStartCommand");
        if (!bRunning) {
            bRunning = true;
            createBandObserver();

            new Thread(new Runnable() {
                public void run() {

                    while(bRunning){
                        Log.d(TAG, "LOOP command" + System.currentTimeMillis());
                        if(bandProvider != null){
                            if (bandProvider.isConnected()){
                                bandProvider.checkHeartBeat();
                            }
                        }

//                        String json = "{\"heartbeat\": \"24\"}";
//                        HttpManager.post("http://172.30.1.29:8080/testpost", json, new HttpManagerCallback() {
//                            @Override
//                            public void responseBody(String body) {
//                                Log.d(TAG, body);
//                            }
//
//                            @Override
//                            public void onError(String error) {
//                                Log.d(TAG, "http error : " + error);
//                            }
//                        });
                        try {
                            Thread.sleep(1000 * 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void createBandObserver(){
        new Thread(new Runnable() {
            public void run() {

                while(bRunning){
                    synchronized (bandProvider){
                        if (bandProvider == null){
                            createBandProvider();
                        }

                        if (!bandProvider.isConnected()){
                            connectBand();
                        }
                    }


                    try {
                        Thread.sleep(1000 * 5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 서비스가 종료될 때 실행
        Log.d(TAG, "서비스의 onDestroy");
        bRunning = false;
    }


    private void createBandProvider(){
        setBandProvider(new BandProvider(getApplicationContext()));
    }

    public void connectBand(){
        Band registeredBand = BandManager.getRegisteredBand(getApplicationContext());
        if (this.bandProvider != null){
            this.bandProvider.setBand(registeredBand);
            this.bandProvider.connectBand(registeredBand.getAddress());
        }

    }
    void startForegroundService() {
        startForeground(NotificationUtil.ID_HEARTBEATCHECK,  NotificationUtil.builder("HI-U", "심박수 체크 준비 중 입니다.", this).build());
    }

    public class MyBinder extends Binder {
        public BandService getService(){
            return BandService.this;
        }
    }

    public BandProvider getBandProvider() {
        return bandProvider;
    }

    public void setBandProvider(BandProvider bandProvider) {
        this.bandProvider = bandProvider;

    }
}
