package com.loreal.myprofile;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.loreal.myprofile.common.LorealMain;

public class NetworkStateReceiver extends BroadcastReceiver {

    LorealMain lorealMain;

    @Override
    public void onReceive(Context context, Intent intent) {
        lorealMain = LorealMain.getInstance(context);
        if (!lorealMain.isNetworkAvailable()) context.startActivity(new Intent(context, NoConnActivity.class));
    }

}
