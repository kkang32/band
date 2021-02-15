package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    String TAG = getClass().getName();
    private BandProvider bandProvider;
    private ListView mListView;
    private LeDeviceListAdapter mLeDeviceListAdapter;


    private Button buttonSearchBand;
    private Button buttonStartService;
    private Button buttonStopService;
    private Button buttonCheckHeartBeat;

    private Button buttonConnectRegistered;
    private Button buttonDisconnectRegistered;

    private Band registeredBand;
    private Context mContext;

    private BandService mService;
    //bound 상태를 저장할 변수
    private boolean mBound;

    private boolean nowInit = false;


    private final Handler mHandler = new Handler(Looper.getMainLooper()){
        String TAG = getClass().getName();
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case HandleBandListener.FIND_BLUETOOTH_DEVICE: //블루투스 검색에서 장비를 찾음
                    BluetoothDevice device = (BluetoothDevice)msg.obj;
                    Log.d(TAG, "find device : " + device.getName());

                    mLeDeviceListAdapter.addDevice(device); //리스트view에 add
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    break;

                case HandleBandListener.BAND_SCAN_TIMEOUT: //블루투스 탐색 종료
                    Log.d(TAG, "completed find device");
                    break;

                case HandleBandListener.BLUETOOTH_DISABLE: //블루투스 비활성화됨(서비스, mclient에서 보냄)
                    Log.d(TAG, "disabledBluetooth");
                    initWidget();
                    //bluetooth 재연결 유도
                    if (checkPermission()){
                        return;
                    }
                    break;

                case HandleBandListener.CONNECTED: 
                    Log.d(TAG, " success connect");
                    Toast.makeText(mContext, "연결되었습니다.(" + bandProvider.getBand().toString() + ")", Toast.LENGTH_SHORT).show();
                    BandManager.registerBand(mContext, bandProvider.getBand()); //밴드 등록함
                    initWidget(); //위젯 초기화
                    startBandService(); //서비스 시작
                    break;
                case HandleBandListener.DISCONNECTED: //사용자 명령에 의해 발생함
                    Log.d(TAG, " success disconnect");
                    Toast.makeText(mContext, "연결이 해제 되었습니다.", Toast.LENGTH_SHORT).show();
                    initWidget(); //위젯 초기화
                    break;

                case HandleBandListener.BAND_CONNECTION_FAIL:
                    Toast.makeText(mContext, TAG, Toast.LENGTH_SHORT).show();
                    //BandManager.unregisterBand(mContext); //사용자가 밴드 바로 연결을 시도 했으나 밴드 연결하지 못함. 등록된 밴드 삭제
                    break;
                case HandleBandListener.BAND_DISCONNECD: //이벤트, 서비스에서 발생함. 예를들어 멀리 떨어지는 경우 발생함
                    Log.d(TAG, " disconnected!!!! ");
                    initWidget(); //위젯 초기화
                    break;
                case HandleBandListener.BAND_RECONNECTING: //서비스에서 재연결시 발생
                    Log.d(TAG, " reconnected ");
                    initWidget();
                    break;
            }
        }
    };

    //서비스 binding을 위한 connection
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) { //서비스에 연결 되면
            BandService.MyBinder binder = (BandService.MyBinder) service;
            mService = binder.getService();
            mBound = true;
            if(mService.getBandProvider() != null){ //서비스에 생성된 bandProvider가 있으면 가져와서 activity에서 재사용 한다.
                Log.d(TAG, "bandProvider 를 서비스에서 받아옴");
                bandProvider = mService.getBandProvider();
                if (!bandProvider.getBandListener().available()){ //서비스에서 직접 생성한 provider라면 listener에 연결된 handler가 없다. 별도로 생성해 준다.
                    bandProvider.setBandListener(new HandleBandListener(mHandler));
                }
            }else{
                bandProvider = new BandProvider(mContext, new HandleBandListener(mHandler));
                mService.setBandProvider(bandProvider);
            }

            //서비스에서 activity의 화면을 제어하기위해 handler전달
            mService.setmHandler(mHandler);

            //serviceConnection은 resume에서만 수행되므로 블루투스 활성화를 완료 했음을 service에 알린다.
            if (mService.isSendRequestBluetoothConnect()){
                mService.setSendRequestBluetoothConnect(false);
            }

            initWidget();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 예기치 않은 종료
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mContext = this;
        //등록된 밴드가 있으면 가져온다.
        registeredBand = BandManager.getRegisteredBand(this);

        buttonSearchBand = (Button) findViewById(R.id.button0);
        buttonStartService = (Button) findViewById(R.id.button1);
        buttonStopService = (Button) findViewById(R.id.button2);
        buttonCheckHeartBeat = (Button) findViewById(R.id.button3);

        buttonConnectRegistered = (Button) findViewById(R.id.button_connect_registered);
        buttonDisconnectRegistered = (Button) findViewById(R.id.button_disconnect_registered);
        mListView = (ListView) findViewById(R.id.listView);




        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            String TAG = getClass().getName();

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { //검색된 블루투스 장비를 클릭하면
                Log.d(TAG, "band connecting");
                bandProvider.stopScanBand(); //밴드 검색을 중지하고
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position); //선택된 장비 정보를 얻어와
                bandProvider.connectBand(device.getAddress()); //밴드 연결을 수행한다.
                //연결완료시점에 밴드 정보를 알 수 없어서 미리 셋팅한다.
                //연결완료시 해당 밴드를 register한다.
                //연결실패시 unregister한다.
                bandProvider.setBand(device.getName(), device.getAddress());
            }
        });



        buttonSearchBand.setOnClickListener(new View.OnClickListener() {
            String TAG = getClass().getName();

            @Override
            public void onClick(View v) {
                // 서비스 시작하기
                Log.d(TAG, "액티비티-장비 검색");
                bandProvider.scanBand();
            }
        });

        buttonStartService.setOnClickListener(new View.OnClickListener() {
            String TAG = getClass().getName();
            public void onClick(View v) {
                // 서비스 시작하기
                Log.d(TAG, "액티비티-서비스 시작버튼클릭");
                startBandService();
            }
        });

        buttonStopService.setOnClickListener(new View.OnClickListener() {
            String TAG = getClass().getName();
            public void onClick(View v) {
                // 서비스 종료하기
                Log.d(TAG, "액티비티-서비스 종료버튼클릭");
                Intent intent = new Intent(
                        getApplicationContext(),
                        BandService.class);
                stopService(intent);
            }
        });
        buttonCheckHeartBeat.setOnClickListener(new View.OnClickListener() {
            String TAG = getClass().getName();
            public void onClick(View v) {
                // 서비스 종료하기
                Log.d(TAG, "액티비티-심박수 체크");

                bandProvider.checkHeartBeat();
            }
        });


        buttonConnectRegistered.setOnClickListener(new View.OnClickListener(){
            String TAG = getClass().getName();
            @Override
            public void onClick(View v) {
                //bandProvider.stopScanBand();
                if (registeredBand != null){
                    bandProvider.setBand(registeredBand);
                    bandProvider.connectBand(registeredBand.address);
                }else{
                    Toast.makeText(mContext, "등록된 Band가 없습니다. 밴드를 검색하여 연결해 주세요.", Toast.LENGTH_LONG).show();
                }

            }
        });

        buttonDisconnectRegistered.setOnClickListener(new View.OnClickListener(){
            String TAG = getClass().getName();
            @Override
            public void onClick(View v) {
                //bandProvider.stopScanBand();
                if (bandProvider.isConnected()) {
                    bandProvider.disconnectBand();
                }


            }
        });

        if (checkPermission()){
            return;
        }


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothUtil.ACTION_ENABLE_BT) {
            // 사용자가 블루투스 활성화 승인했을때
            if (resultCode == RESULT_OK) {
               Log.d(TAG, "\nDevice can use");
               initWidget();
               NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
               notificationManager.cancel(22);

            }
            // 사용자가 블루투스 활성화 취소했을때
            else {
                Log.d(TAG, "\nDevice cannot use");
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onresume","resume!!");
        Intent intent = new Intent(this, BandService.class);
        if (mService != null){
            if (mService.isSendRequestBluetoothConnect()){
                mService.setSendRequestBluetoothConnect(false);
            }
        }

        bindService(intent, mConnection, BIND_AUTO_CREATE);


        //bandProvider는 항상 service에서 생성한 것을 사용한다.
        //initWidget();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound){
            unbindService(mConnection);
            //mBound = false;
        }
    }

    public boolean checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission, permission2, permission3;
            permission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
            permission2 = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permission == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 0x03);//0x03 is REQUEST_BLUETOOTH_ADMIN
                return false;
            } else {
                return true;
            }
        }
        return true;
    }


    private void initWidget(){

        boolean enabled = BluetoothUtil.canUseBluetooth(this);
        buttonSearchBand.setEnabled(enabled);
        buttonStartService.setEnabled(enabled);
        buttonStopService.setEnabled(enabled);
        buttonCheckHeartBeat.setEnabled(enabled);
        buttonConnectRegistered.setEnabled(enabled);
        mListView.setEnabled(enabled);
        buttonDisconnectRegistered.setEnabled(enabled);

        //등록된 밴드가 없으면 "밴드 바로 연결" disabled
        if (registeredBand == null){
            buttonConnectRegistered.setEnabled(false);
        }

        //밴드가 연결중이면 "밴드 바로 연결" disabled
        if (bandProvider != null){
            if (bandProvider.isConnected()){
                buttonConnectRegistered.setEnabled(false);
                buttonDisconnectRegistered.setEnabled(true);
            }else{
                buttonConnectRegistered.setEnabled(true);
                buttonDisconnectRegistered.setEnabled(false);
            }
        }



    }


    private void startBandService(){

        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                BandService.class); // 이동할 컴포넌트


        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent);
        }else {
            startService(intent); // 서비스 시작
        }

        mService.setBandProvider(bandProvider);

    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            final String deviceAddress = device.getAddress();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            }

            if (deviceAddress != null && deviceAddress.length() > 0) {
                viewHolder.deviceAddress.setText(deviceAddress);
            }
            return view;
        }
    }




}