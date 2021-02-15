package com.example.myapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;



public class BandService extends Service {
    String TAG = getClass().getName();
    boolean bRunning = false;
    private IBinder mBinder = new MyBinder();
    private BandProvider bandProvider;
    private Handler mHandler;
    //사용자가 블루투스를 활성화 하기전까지 메시지가 계속 발생하는 것을 방지하기위한 flag
    private boolean sendRequestBluetoothConnect = false;
    //private ServiceBroadcastReceiver mReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        // 서비스에서 가장 먼저 호출됨(최초에 한번만)
        Log.d(TAG, "서비스의 onCreate");
        //무한루프 제어용 변수
        bRunning = true;

        createBandObserver();
        startForegroundService();

//        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//        filter.addAction("android.intent.action.BOOT_COMPLETE");
//        if(mReceiver == null) {
//            mReceiver = new ServiceBroadcastReceiver();
//            this.registerReceiver(mReceiver, filter);
//        }else{
//            this.unregisterReceiver(mReceiver);
//        }


    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 서비스가 호출될 때마다 실행
        Log.d(TAG, "서비스의 onStartCommand");



        createHeartbeatChecker();



        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 블루투스, bandProvider, bandConnection을 체크하여 연결을 유도하거나 연결을 시도한다.
     */
    private void createBandObserver(){
        new Thread(new Runnable() {
            public void run() {
                while(bRunning){

                    //블루투스가 비활성화 되었다
                    if (!BluetoothUtil.enableBluetooth()){
                        Log.d("band observer", "bluetooth is unavailable");
                        if (!sendRequestBluetoothConnect){
                            Log.d("band observer", "send request bluetooth enable");
                            sendStatusToActivity(HandleBandListener.BLUETOOTH_DISABLE, null);
                            sendRequestBluetoothConnect = true;
                        }

                    }


                    if (bandProvider == null){
                        createBandProvider();
                        Log.d("band observer", "check band connection - create band provider");
                    }

                    //밴드의 연결이 끊겼다.
                    if (!bandProvider.isConnected()){
                        if (!sendRequestBluetoothConnect) {//블루투스가 연결이 되지 않아 아직 연결이 완료되기 전이다.
                            sendStatusToActivity(HandleBandListener.BAND_DISCONNECD, null);
                            Log.d("band observer", "check band connection - band disconnected");
                            connectBand();
                        }
                    }



                    try {
                        Thread.sleep(1000 * 3);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    private void createHeartbeatChecker(){
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
        if (this.bandProvider != null && registeredBand != null){
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

    private void sendStatusToActivity(int status, Object obj){
        if (this.mHandler != null){
            Message msg = this.mHandler.obtainMessage();
            msg.what = status;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public boolean isSendRequestBluetoothConnect() {
        return sendRequestBluetoothConnect;
    }

    public void setSendRequestBluetoothConnect(boolean sendRequestBluetoothConnect) {
        this.sendRequestBluetoothConnect = sendRequestBluetoothConnect;
    }
}
