package com.loreal.myprofile;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.loreal.myprofile.common.LorealMain;

public class SplashActivity extends Activity {

    LorealMain lorealMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        lorealMain = LorealMain.getInstance(SplashActivity.this);
        registerReceiver(lorealMain.networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (lorealMain.isNetworkAvailable()) {
            startActivity(lorealMain.getGUID().equals("") ? new Intent(getApplicationContext(), GUIDActivity.class) : new Intent(getApplicationContext(), WebViewActivity.class));
            finish();
        }
        else {
            startActivity(new Intent(SplashActivity.this, NoConnActivity.class));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(lorealMain.networkStateReceiver);
    }

}