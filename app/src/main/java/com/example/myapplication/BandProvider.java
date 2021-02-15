package com.example.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.partron.wearable.band.sdk.core.BandResultCode;
import com.partron.wearable.band.sdk.core.BandUUID;
import com.partron.wearable.band.sdk.core.ConnectionState;
import com.partron.wearable.band.sdk.core.PWB_ClientManager;
import com.partron.wearable.band.sdk.core.UserProfile;
import com.partron.wearable.band.sdk.core.interfaces.BandConnectStateCallback;
import com.partron.wearable.band.sdk.core.interfaces.BandScanCallback;
import com.partron.wearable.band.sdk.core.interfaces.BandSettings;
import com.partron.wearable.band.sdk.core.interfaces.BandUrbanListener;
import com.partron.wearable.band.sdk.core.interfaces.BandUrbanMeasureListener;
import com.partron.wearable.band.sdk.core.interfaces.OnCompleteListener;
import com.partron.wearable.band.sdk.core.interfaces.PWB_Client;
import com.partron.wearable.band.sdk.core.item.ConditionInfoItem;
import com.partron.wearable.band.sdk.core.item.UrbanInfoItem;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 밴드 검색, 연결, 심박수 측정 등.
 */
public class BandProvider {

    String TAG = getClass().getName();
    //밴드 연결을 위해 필요한 정보
    private UserProfile profile;
    //밴드 연결 client
    private PWB_Client mClient;
    //밴드 및 블루투스 연결 리스너
    private BandListener bandListener;
    //밴드 정보 객체
    private Band band;

    private Context mContext;



    //사용자에 의해 발생한 요청 종류(success코드가 연결요청, 연결 해제 모두 발생하므로 이를 구분하기위한 값)
    public static final int REQ_CONNECT = 1;
    public static final int REQ_DISCONNECT = 2;
    private int requestCode = 999;

    private boolean isCheckingHeartbeat = false;




    //블루투스 장비 스캔시 callback
    public BandScanCallback mBandScanCallback = new BandScanCallback(){

        @Override
        public void onBandScanCallback(int state, BluetoothDevice bluetoothDevice, int rssi) {
            Log.d(TAG, "into band scan callback " + state + "," + rssi);

            //블루투스 비활성화
            if (state == BandResultCode.BLUETOOTH_DISABLE){
                bandListener.disabledBluetooth();
            }else if (state == BandResultCode.BAND_SCAN_TIMEOUT){  //밴드 스캔 종료
                bandListener.findTimeout();
            }else{
                if (bluetoothDevice == null) {
                    return;
                }
                Log.d(TAG, "band scan callback : " + bluetoothDevice.getName());
                if (bluetoothDevice.getName() != null){ //장비 찾음.
                    bandListener.findDevice(bluetoothDevice);
                }

            }

        }
    };

    //밴드 연결 callback
    public BandConnectStateCallback mBandConnCallback = new BandConnectStateCallback(){
        String TAG = getClass().getName();
        @Override
        public void onBandConnectState(int state, ConnectionState connectionState) {
            Log.d(TAG, state + "");

            switch (state){
                case BandResultCode.SUCCESS : //밴드 연결 성공, 밴드 연결 해제 성공

                    //연결성공
                    if(connectionState.equals(ConnectionState.CONNECTED)){
                        Log.d(TAG, "success connect");
                        bandListener.connectedBand();
                        NotificationUtil.notification(NotificationUtil.ID_HEARTBEATCHECK, "HI-U", "심박수 체크 중 입니다.", mContext);
                    }else if(connectionState.equals(ConnectionState.DISCONNECTED)){

                        //요청된 disconnect이면 정상 처리
                        if (getRequestCode() == REQ_DISCONNECT){
                            Log.d(TAG, "success disconnect");
                            bandListener.disconnectedBand();
                        }else{ //핸드폰과 멀리 떨어지는 등의 disconnect
                            Log.d(TAG, "force disconnect..notification!");
                            NotificationUtil.notification(NotificationUtil.ID_HEARTBEATCHECK, "HI-U", "심박수 체크 준비 중 입니다.", mContext);
                            NotificationUtil.notification(NotificationUtil.ID_ALERT, "HI-U", "밴드와의 연결이 해제 되었습니다. 다시 연결해 주세요.", mContext);
                        }

                    }
                    break;
                case BandResultCode.BAND_CONNECTION_FAIL : //밴드 연결 실패
                    setBand(null);
                    bandListener.connectFail();
                    break;
                case BandResultCode.BAND_DISCONNECD : //밴드 연결해제
                    Log.d(TAG, "disconnected");
                    bandListener.disconnectedBand();
                    break;
                case BandResultCode.BLUETOOTH_DISABLE: //블루투스 비활성화
                    Log.d(TAG, "bluetooth is disabled");
                    break;
                default :
                    Log.d(TAG, "state is " + state);
                    break;


            }



        }
    };

    private BandUrbanListener mBandUrbanListener = new BandUrbanListener() {

        @Override
        public void urbanPedoInfo(UrbanInfoItem urbanInfoItem) {

        }

        @Override
        public void urbanInfo(UrbanInfoItem urbanInfoItem) {
            //
        }

        @Override
        public void urbanPPGInfo(int hrm, int stress) {
            Log.d(TAG, "(CB)HRM : " + hrm);
        }

        @Override
        public void urbanConditionInfo(ConditionInfoItem item) {

        }

        @Override
        public void urbanBatteryInfo(int batt) {

        }
    };

    public BandProvider(Context context, BandListener bandListener) {
        this.mContext = context;
        UserProfile item = new UserProfile();
        item.setAge(31);
        item.setHeight(170);
        item.setWeight(70);
        item.setGender(0);
        //밴드 설정 등록
        PWB_ClientManager clientManager = PWB_ClientManager.getInstance();
        this.mClient = clientManager.create(context, item, BandUUID.PWB_250);
        this.mClient.getUrbanMode().registerBandUrbanListener(mBandUrbanListener);
        this.mClient.registerBandScanCallback(mBandScanCallback);
        this.mClient.registerBandConnectStateCallback(mBandConnCallback);
        
        //main activity와 데이터 처리용 리스너(내부에 핸들러)
        this.bandListener = bandListener;
    }

    public BandProvider(Context context) {
        this.mContext = context;
        UserProfile item = new UserProfile();
        item.setAge(31);
        item.setHeight(170);
        item.setWeight(70);
        item.setGender(0);
        //밴드 설정 등록
        PWB_ClientManager clientManager = PWB_ClientManager.getInstance();
        this.mClient = clientManager.create(context, item, BandUUID.PWB_250);
        this.mClient.getUrbanMode().registerBandUrbanListener(mBandUrbanListener);
        this.mClient.registerBandScanCallback(mBandScanCallback);
        this.mClient.registerBandConnectStateCallback(mBandConnCallback);

        //main activity와 데이터 처리용 리스너(내부에 핸들러)
        this.bandListener = new HandleBandListener();
    }

    /**
     * 밴드 검색
     */
    public void scanBand() {
        if (mClient != null) {
            mClient.bandScan().start(1000 * 10); //Callback
        }
    }

    /**
     * 밴드 검색 중지
     */
    public void stopScanBand() {
        if (mClient != null){
            mClient.bandScan().stop();
        }
    }

    public void urbanModeHeartBeat(int interval){
        if (mClient != null) {
            //example
            //int interval; // 15 minute, 30 minute, 60 minute , 0 = OFF
            mClient.getBandSettings().setUrbanPPGInterval(interval,
                    new OnCompleteListener() {
                        @Override
                        public void onResult(int result, Object object) {
                            //Handle BandResultCode, Oject : null
                            Log.d("OnCompleteListener1", "PPG : " + result);
                            if(result == BandResultCode.SUCCESS){
                                Log.d("OnCompleteListener2", "PPG : " + result);
                            }else{
                            }
                        }
                    });
        }
    }

    /**
     * 밴드 연결
     * @param address
     */
    public void connectBand(String address){
        if (mClient != null && BluetoothUtil.enableBluetooth()){
            Log.d(TAG, "band connecting");
            mClient.bandConnect(address);
            requestCode = REQ_CONNECT;
        }
    }

    /**
     * 밴드 연결 해제
     * requestCode 에 REQ_DISCONNECT 를 할당하여 사용자 요청에의한 연결해제임을 명시한다.
     */
    public void disconnectBand(){
        if (mClient != null){
            Log.d(TAG, "band disconnecting");
            mClient.bandDisconnect();
            requestCode = REQ_DISCONNECT;
        }
    }

    /**
     * 연결상태 체크
     * @return
     */
    public boolean isConnected(){
        if (mClient != null){
            return mClient.isBandConnected();
        }else{
            return false;
        }
    }

    /**
     * measuremode 심박수 측정
     * 측정이 끝날 때 마다 밴드에서 진동 발생
     */
    public void checkHeartBeat(){
        if (mClient != null && !isCheckingHeartbeat) {
            //BandSettings settings = mClient.getBandSettings();
//            mClient.getBandSettings().setCustomContent(5, false, true, new byte[]{}, new OnCompleteListener() {
//                @Override
//                public void onResult(int i, Object o) {
//
//                }
//            });
            isCheckingHeartbeat = true;
            mClient.getMeasureMode().ppgMeasure(new BandUrbanMeasureListener() {
                String TAG = getClass().getName();
                @Override
                public void onPPGResult(int result, int status, int hrm, int stress) {
                    if (status == 0x00) {
                        Log.d(TAG, "측정 완료 : " + hrm);
                        //얻어온 심박을 로컬에 저장한다.
                        HeartbeatManager.saveHeartbeat(mContext, hrm);
                        isCheckingHeartbeat = false;
                    } else if (status == 0x01) {
                        Log.d(TAG, "측정 중");
                    } else if (status == 0xff) {
                        Log.d(TAG, "측정 시작");
                    }
                }

                @Override
                public void onAlitudeResult(int i, int i1) {

                }

                @Override
                public void onBPResult(int i, int i1, int i2, int i3) {

                }
            });


        }
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public PWB_Client getmClient() {
        return mClient;
    }

    public void setmClient(PWB_Client mClient) {
        this.mClient = mClient;
    }

    public BandListener getBandListener() {
        return bandListener;
    }

    public void setBandListener(BandListener bandListener) {
        this.bandListener = bandListener;
    }

    public Band getBand() {
        return band;
    }

    public void setBand(Band band) {
        this.band = band;
    }

    public void setBand(String name, String address) {
        this.band = new Band(name, address);
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }
}
