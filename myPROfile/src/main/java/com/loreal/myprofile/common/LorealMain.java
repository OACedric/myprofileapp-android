package com.loreal.myprofile.common;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.loreal.myprofile.NetworkStateReceiver;
import com.loreal.myprofile.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class LorealMain {

    private static LorealMain instance ;

    protected  Context context;
    protected String VERSION    = "1.0";
    protected String CONN       = "COM" ;
    protected String BUNDLETYPE = "A";//A --> M bypass guid ctrl
    protected String DEVICE     = "A";
    protected String DEVICETYPE = "S";
    protected String DEVICENAME = Build.MANUFACTURER + ' ' + Build.PRODUCT + " (" + Build.MODEL + ')';
    protected MimeTypeMap mime = MimeTypeMap.getSingleton();
    //public static final String appUrl = "https://dgrhq38.loreal.wans/profile/hrislogin.html?FL=X";
    public static final String appUrl = "https://profile.loreal.com";
    public static final String appLogoutUrl = "https://profile.loreal.com/dana-na/auth/logout.cgi";
    //public static final String appUrl = "https://myprofile.loreal.com";

    public int cropWidth = 200;
    public int cropHeight = 200;

    public final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    private LorealMain(Context context) {
        this.context = context;
    }

    public static LorealMain getInstance(Context context){
        context = context;
        if (instance == null) { instance = new LorealMain(context);}
        return instance;
    }

    public String getVERSION() { return this.VERSION; }

    public JSONObject appToken_create(){
        JSONObject w_token = new JSONObject();
        try {
            w_token.put("guid", this.getGUID());
            w_token.put("uuid", this.getUUID());
            w_token.put("conn", this.CONN);
            w_token.put("bundleType", this.BUNDLETYPE);
            w_token.put("device", this.DEVICE);
            w_token.put("version",this.VERSION);
            w_token.put("deviceType", this.DEVICETYPE);
            w_token.put("deviceName", this.DEVICENAME);
        }
        catch (JSONException e){ e.printStackTrace(); }
        return w_token;
    }

    public String getGUID() {
        return SharedPrefManager.getInstance(this.context).LoadFromPref("GUID");
    }

    public void setGUID(String GUID) {
        if(0!= GUID.length()) SharedPrefManager.getInstance(this.context).StoreToPref("GUID",GUID);
    }

    public void invalidateGUID(){
        SharedPrefManager.getInstance(this.context).StoreToPref("GUID","");
    }

    public String getUUID() {
        if ( SharedPrefManager.getInstance(this.context).LoadFromPref("UUID") == "" ) setUUID();
        return SharedPrefManager.getInstance(this.context).LoadFromPref("UUID");
    }

    private void setUUID() {
        String UUID = "";
        synchronized (this) {
            final String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            // Android ID sinon UUID random - https://en.proft.me/2017/06/13/how-get-unique-id-identify-android-devices/
            try {
                if (!"9774d56d682e549c".equals(androidId)) {
                    UUID = java.util.UUID.nameUUIDFromBytes(androidId.getBytes("utf8")).toString();
                }
                else {
                    if (ContextCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        final String deviceId = ((TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE )).getDeviceId();
                        UUID = deviceId.equals("") ? java.util.UUID.randomUUID().toString() : java.util.UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")).toString();
                    }
                    else {
                        UUID = java.util.UUID.nameUUIDFromBytes(androidId.getBytes("utf8")).toString();
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            SharedPrefManager.getInstance(this.context).StoreToPref("UUID",UUID);
        }
    }

    public void setData(String p_key, String p_value){
        SharedPrefManager.getInstance(this.context).StoreToPref(p_key,p_value);
    }

    public String getData(String p_key){
        return SharedPrefManager.getInstance(this.context).LoadFromPref(p_key);
    }

    public void writePDF(String p_name, byte[] fileContent){
        writeFile(p_name,"pdf", fileContent);
    }

    private void writeFile(String p_name, String p_ext, byte[] p_fileContent) {
        String ext = p_name.substring(p_name.lastIndexOf(".") + 1);
        if (p_ext != null && !ext.equalsIgnoreCase(p_ext)) p_name = p_name + "." + p_ext;
        writeFile(p_name, p_fileContent);
    }

    public void writeFile(String p_name, byte[] p_fileContent){
        if (isExternalStorageWritable()){
            try {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()+"/PROfile";
                File myDir = new File(path);
                if (!myDir.exists()) myDir.mkdirs();
                File file = new File(myDir, p_name);
                String ext = p_name.substring(p_name.lastIndexOf(".") + 1);
                //Log.d("CED", "WriteFile " + p_name + " ## " + mime.getMimeTypeFromExtension(ext));
                OutputStream output = new FileOutputStream(file);
                output.write(p_fileContent);
                output.flush();
                output.close();
                DownloadManager dlm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                dlm.addCompletedDownload(file.getName(),file.getName(), true, mime.getMimeTypeFromExtension(ext), file.getAbsolutePath(), file.length(),true);
                Toast.makeText(context, context.getString(R.string.txt_downloaded), Toast.LENGTH_SHORT).show();
            }
            catch (IOException e) {
                Toast.makeText(context, context.getString(R.string.err_download), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        catch (Exception ex) {
            return true;
        }
    }

    public void requestAllPermission(Activity p_activity){
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        Boolean lackRights = false;
        for (int i=0; i<permissions.length; i++){
            lackRights = (p_activity.checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED);
            if (lackRights) ActivityCompat.requestPermissions(p_activity, permissions, 1);
        }
    }

    public void requestPermission(Activity p_activity, String[] p_permissions, int p_requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(p_activity, p_permissions, p_requestCode);
        }
    }

    public boolean isPermissionGranted(Activity p_activity, String p_permission) {
        if (Build.VERSION.SDK_INT >= 23) return (p_activity.checkSelfPermission(p_permission) == PackageManager.PERMISSION_GRANTED);
        else return true; //permission is automatically granted on sdk<23 upon installation
    }

    private boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /*public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }*/

    /**
     * get bytes array from Uri.
     * @param context current context.
     * @param uri uri fo the file to read.
     * @return a bytes array.
     * @throws IOException
     */
    public static byte[] getBytes(Context context, Uri uri) throws IOException {
        InputStream iStream = context.getContentResolver().openInputStream(uri);
        try {
            return getBytes(iStream);
        } finally {
            try {
                iStream.close();
            } catch (IOException ignored) { /* do nothing */ }
        }
    }

    /**
     * get bytes from input stream.
     * @param inputStream inputStream.
     * @return byte array read from the inputStream.
     * @throws IOException
     */
    private static byte[] getBytes(InputStream inputStream) throws IOException {

        byte[] bytesResult = null;
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        try {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        } finally {
            try{ byteBuffer.close(); } catch (IOException ignored){ /* do nothing */ }
        }
        return bytesResult;
    }

}


