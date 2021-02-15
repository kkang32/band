package com.example.myapplication;

import android.content.Context;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

/**
 * 밴드를 등록하고 삭제한다.
 */
public class BandManager {

    String TAG = getClass().getName();
    public static final String PREF_KEY_NAME = "lgl_band_name";
    public static final String PREF_KEY_ADDRESS = "lgl_band_address";
    public static Band getRegisteredBand(Context context){
        String bandName = PreferenceManager.getString(context, PREF_KEY_NAME, "");
        String bandAddress = PreferenceManager.getString(context, PREF_KEY_ADDRESS, "");

        if(StringUtils.isEmpty(bandAddress)){
            return null;
        }else{

            Band band = new Band(bandName, bandAddress);
            return band;
        }
    }

    public static void registerBand(Context context, Band band){
        registerBand(context, band.getName(), band.getAddress());
    }

    public static void registerBand(Context context, String name, String address){
        PreferenceManager.setString(context, PREF_KEY_NAME, name);
        PreferenceManager.setString(context, PREF_KEY_ADDRESS, address);
    }

    public static void unregisterBand(Context context){
        PreferenceManager.remove(context, PREF_KEY_NAME);
        PreferenceManager.remove(context, PREF_KEY_ADDRESS );
    }



}
