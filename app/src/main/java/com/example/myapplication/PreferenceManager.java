package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    String TAG = getClass().getName();
    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("hiu_sharedPreference", Context.MODE_PRIVATE);
    }

    public static void setString(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void remove(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.commit();
    }

    public static String getString(Context context, String key, String defaultValue) {
        SharedPreferences prefs = getPreferences(context);
        String value = prefs.getString(key, defaultValue);
        return value;
    }
}
