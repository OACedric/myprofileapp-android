package com.loreal.myprofile.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static SharedPrefManager mInstance;

    public static final String MY_APP_PREFS = "MySharedAttrs";

    private SharedPreferences settings;

    private SharedPrefManager(Context context) {
        settings = context.getSharedPreferences(MY_APP_PREFS, context.MODE_PRIVATE);
    }

    public static SharedPrefManager getInstance(Context context){
        if (mInstance == null)
            mInstance = new SharedPrefManager(context);
        return  mInstance;
    }

    public String LoadFromPref(String keyName) {
        return  settings.getString(keyName,"");

    }

    public void StoreToPref(String keyName , String value) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(keyName,value);
        editor.commit();
    }

    public void DeleteSingleEntryFromPref(String keyName) {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(keyName);
        editor.commit();
    }

    public void DeleteAllEntriesFromPref() {
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.commit();
    }

}