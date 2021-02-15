
package com.example.myapplication;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HeartbeatManager {
    public static final String TAG = HeartbeatManager.class.getName();
    public static final String PREF_KEY = "lgl_heartbeat";
    public static final String PREF_KEY_LAST_UPLOADED = "lgl_heartbeat_lastuploaded";
    public static final int HEARTBEAT_CHECK_INTERVAL_MIN = 5;

    /**
     * 로컬스토리지에 데이터 저장
     * @param context
     * @param beat
     */
    public static void saveHeartbeat(Context context, int beat){
        Date today = new Date(System.currentTimeMillis());
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss");
        String curTimestamp = format.format(today);

        String allHeartbeat = getAllHeartbeat(context);
        PreferenceManager.setString(context, PREF_KEY, allHeartbeat + "," + curTimestamp + ":" + beat);

        Log.d(TAG, PreferenceManager.getString(context, PREF_KEY, ""));
    }

    /**
     * 로컬스토리지에 저장된 전체 데이터
     * @param context
     * @return
     */
    public static String getAllHeartbeat(Context context){
        return PreferenceManager.getString(context, PREF_KEY, "");
    }

    /**
     * 서버에 전송된 데이터중 제일 마지막 데이터 저장
     * @param context
     * @param lastUploadedData 서버에 전송된 데이터
     */
    public static void setLastUploadedHeartbeat(Context context, String lastUploadedData){
        String[] uploadedData = lastUploadedData.split(",");
        PreferenceManager.setString(context, PREF_KEY_LAST_UPLOADED, uploadedData[uploadedData.length-1]);
    }

    /**
     * 서버에 전송된 데이터중 제일 마지막 데이터 
     * @param context
     * @return e.g.) 20200309112303:67
     */
    public static String getLastUploadedData(Context context){
        return PreferenceManager.getString(context, PREF_KEY_LAST_UPLOADED, "");
    }

    /**
     * 서버에 전송할 데이터
     * @param context
     * @return e.g.)20200309112303:67,20200309122303:68,20200309132303:67......
     */
    public static String getUploadData(Context context){
        //저장된 전체 데이터를 가져온다.
        String wholeHeartbeat = getAllHeartbeat(context);
        //지난 업로드에서 제일 마지막 데이터를 가져온다.
        String lastUploaded = getLastUploadedData(context);
        //문자열에서 검색해서 마지막 데이터 이후의 데이터만 취한다.
        String lastData = wholeHeartbeat.substring(wholeHeartbeat.indexOf(lastUploaded));

        //서버전송 주기별로 카운트된 횟수로 데이터를 가져오기위해 계산
        int sendCount = 60 / HEARTBEAT_CHECK_INTERVAL_MIN;

        //데이터를 잘라서
        String[] arrSendData = lastData.split(",");
        String toData = arrSendData[sendCount-1];
        String sendData = StringUtils.left(lastData, lastData.indexOf(toData));

        return sendData;
    }

}
