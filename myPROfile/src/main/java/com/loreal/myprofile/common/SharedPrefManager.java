package com.loreal.myprofile.common;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class SharedPrefManager {

    private static SharedPrefManager mInstance;

    public static final String MY_APP_PREFS = "MySharedAttrs";

    private SharedPreferences settings;

    private SharedPrefManager(Context context) {
        //settings = context.getSharedPreferences(MY_APP_PREFS, context.MODE_PRIVATE);
        MasterKey masterKey = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        settings = EncryptedSharedPreferences.create(
                context,
                MY_APP_PREFS,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
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