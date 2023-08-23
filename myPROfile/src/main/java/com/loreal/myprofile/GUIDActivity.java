package com.loreal.myprofile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loreal.myprofile.common.LorealMain;

public class GUIDActivity extends Activity implements View.OnClickListener {

    LorealMain lorealMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guid);
        lorealMain = LorealMain.getInstance(GUIDActivity.this);
        registerReceiver(lorealMain.networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        final Button btnValidateGUID = (Button) findViewById(R.id.btnValidateGUID);
        btnValidateGUID.setOnClickListener(this);
        final EditText editTextCode = (EditText) findViewById(R.id.editTextCode);
        editTextCode.requestFocus();
        editTextCode.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    initialize();
                    return true;
                }
                return false;
            }
        });

    }

    protected void initialize(){
        final EditText w_editText_GUID = (EditText) findViewById(R.id.editTextCode);
        String w_app_code_str = w_editText_GUID.getText().toString();
        if (w_app_code_str.length() == 8) {
            lorealMain.setGUID(w_app_code_str);
            startActivity(new Intent(getApplicationContext(), WebViewActivity.class));
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnValidateGUID :
                initialize();
                break;
            case R.id.textViewGetCode :
                AlertDialog.Builder popin = new AlertDialog.Builder(this, R.style.Theme_MaterialComponents_Light_Dialog);
                popin.setTitle(getString(R.string.security_code_help_title));
                popin.setMessage(getString(R.string.security_code_help_mess));
                popin.setCancelable(true);
                popin.setNeutralButton(R.string.txt_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                popin.show();
                break;
            default:
                break;


        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(lorealMain.networkStateReceiver);
    }
}
