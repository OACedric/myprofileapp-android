    package com.loreal.myprofile;

    import android.Manifest;
    import android.app.Activity;
    import android.app.AlertDialog;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.content.pm.PackageManager;
    import android.database.Cursor;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.net.ConnectivityManager;
    import android.net.Uri;
    import android.net.http.SslError;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Message;
    import android.provider.MediaStore;
    import android.provider.OpenableColumns;
    import android.support.constraint.ConstraintLayout;
    import android.support.v4.content.FileProvider;
    import android.text.Html;
    import android.util.Base64;
    import android.util.Log;
    import android.view.View;
    import android.view.ViewGroup;
    import android.view.inputmethod.InputMethodManager;
    import android.webkit.ConsoleMessage;
    import android.webkit.JavascriptInterface;
    import android.webkit.SslErrorHandler;
    import android.webkit.WebChromeClient;
    import android.webkit.WebSettings;
    import android.webkit.WebView;
    import android.webkit.WebViewClient;
    import android.widget.ProgressBar;
    import android.widget.RelativeLayout;
    import android.widget.Toast;
    import com.loreal.myprofile.common.LorealMain;
    import com.loreal.myprofile.common.LorealWebViewOnTouchListener;
    import com.theartofdev.edmodo.cropper.CropImage;
    import com.theartofdev.edmodo.cropper.CropImageView;
    import org.json.JSONObject;
    import java.io.ByteArrayOutputStream;
    import java.io.File;
    import java.io.InputStream;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.UUID;

    public class WebViewActivity extends Activity {

        public LorealWebview myWebView;
        public ConstraintLayout reloadLayout;
        public ProgressBar reloadProgress;
        protected LorealMain lorealMain;
        protected final int FILE_SELECT = 2;
        protected final int CAMERA_CAPTURE_FOR_FILE = 3;
        protected Map<String, String> internalB64Data = new HashMap<String, String>();
        protected File _tmpFile;
        protected Uri _tmpUri;
        protected String _tmpB64File = "";
        protected String _tmpB64FileName = "";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) myWebView.restoreState(savedInstanceState);
            else {
                setContentView(R.layout.activity_web_view);
                lorealMain = LorealMain.getInstance(WebViewActivity.this);
                registerReceiver(lorealMain.networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                lorealMain.requestAllPermission(this);
                myWebView = (LorealWebview) findViewById(R.id.webView);
                reloadLayout = (ConstraintLayout) findViewById(R.id.reloadLayout);
                reloadProgress = (ProgressBar) findViewById(R.id.reloadProgress);

                WebView.setWebContentsDebuggingEnabled(true);

                myWebView.getSettings().setJavaScriptEnabled(true);
                myWebView.getSettings().setAllowFileAccess(true);
                myWebView.getSettings().setBuiltInZoomControls(true);
                myWebView.getSettings().setDomStorageEnabled(true);
                //myWebView.getSettings().setAppCacheEnabled(true);
                myWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                //myWebView.getSettings().setAppCachePath(getApplicationContext().getFilesDir().getAbsolutePath() + "/cache");
                myWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
                myWebView.getSettings().setDatabaseEnabled(true);
                myWebView.getSettings().setLoadWithOverviewMode(true);
                myWebView.getSettings().setUseWideViewPort(true);
                myWebView.getSettings().setBuiltInZoomControls(true);
                myWebView.getSettings().setDisplayZoomControls(false);
                myWebView.getSettings().setSupportZoom(true);

                myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                myWebView.getSettings().setSupportMultipleWindows(true);

                myWebView.getSettings().setDefaultTextEncodingName("utf-8");
                //Inject WebAppInterface methods into Web page by having Interface name 'webViewInterface'
                myWebView.addJavascriptInterface(new WebAppInterface(this, myWebView), "webViewInterface");
                myWebView.setWebViewClient(new LorealWebViewClient()); //SSL tolerant
                myWebView.setWebChromeClient(new LorealWebChromeClient());
                myWebView.setOnTouchListener(new LorealWebViewOnTouchListener(this));
                myWebView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        return true;
                    }
                });
                myWebView.clearCache(true);
                myWebView.loadUrl(LorealMain.appUrl);
                showWaiting(false);
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            unregisterReceiver(lorealMain.networkStateReceiver);
        }

        @Override
        protected void onStart() {
            super.onStart();
        }

        @Override
        protected void onSaveInstanceState(Bundle outState ) {
            super.onSaveInstanceState(outState);
            myWebView.saveState(outState);
        }

        @Override
        protected void onRestoreInstanceState(Bundle savedInstanceState) {
            super.onRestoreInstanceState(savedInstanceState);
            myWebView.restoreState(savedInstanceState);
        }

        @Override
        public void onBackPressed() {
            myWebView.loadUrl("javascript:fwkAppInterface.backHandler();");
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            //Log.d("CED", "onActivityResult " + requestCode);
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_OK) {
                switch (requestCode) {
                    case FILE_SELECT:
                        performReadFile(data.getData());
                        break;
                    case CAMERA_CAPTURE_FOR_FILE:
                        performReadFile(_tmpUri);
                        removeTmpFile();
                        break;
                    case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                        readCropImage(CropImage.getActivityResult(data).getUri());
                        break;
                }
            }
            else {
                    //Log.d("CED", "onActivityResult Error " + resultCode);
                    if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) Log.d("CED", CropImage.getActivityResult(data).getError().toString());
                }

            }

        protected void removeTmpFile(){
            if (_tmpFile != null) {
                _tmpFile.delete();
                _tmpFile = null;
                _tmpUri = null;
            }
        }

        protected void performReadFile(Uri p_uri){
            try {
                String mimeType = getContentResolver().getType(p_uri);
                Cursor returnCursor = getContentResolver().query(p_uri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                String filename = returnCursor.getString(nameIndex);
                String size = Long.toString(returnCursor.getLong(sizeIndex));
                final String base64String = Base64.encodeToString(LorealMain.getBytes(this, p_uri), Base64.NO_WRAP);
                final String id = UUID.randomUUID().toString();
                internalB64Data.put(id, base64String);
                myWebView.loadUrl("javascript:fwkAppInterface.getFileBack(\""+id+"\",\""+filename+"\",\""+size+"\",\""+mimeType+"\");");
            }
            catch (Exception ex){
                Log.d("CED", ex.getMessage());
            }
        }

        protected void readCropImage(Uri p_uri){
            try {
                String mimeType = getContentResolver().getType(p_uri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                InputStream in = getApplicationContext().getContentResolver().openInputStream(p_uri);
                Bitmap imageBitmap = BitmapFactory.decodeStream(in, null, new BitmapFactory.Options());
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap,lorealMain.cropWidth, lorealMain.cropHeight, false);
                imageBitmap.recycle();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                resizedBitmap.recycle();
                byte[] bytes = stream.toByteArray();
                final String imgBase64String = Base64.encodeToString(bytes, Base64.NO_WRAP);
                final String id = UUID.randomUUID().toString();
                internalB64Data.put(id, imgBase64String);
                myWebView.loadUrl("javascript:fwkAppInterface.getPhotoBack(\""+id+"\");");
            }
            catch (Exception ex){
                Log.d("CED", ex.getMessage());
            }
        }

        public void showWaiting(boolean p_bool){
            final boolean visible = p_bool;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final ProgressBar waitProgressBar = (ProgressBar) findViewById(R.id.waitProgress);
                    waitProgressBar.setVisibility((visible)? View.VISIBLE : View.GONE);
                }
            });
        }

        public void exitApp(){
            showWaiting(true);
            myWebView.post(new Runnable() {
                public void run() {
                    myWebView.loadUrl(LorealMain.appLogoutUrl);
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            WebViewActivity.this.showWaiting(false);
                            WebViewActivity.this.finishAffinity();
                        }
                    }, 400);
                }
            });
        }

        protected void writeTmpB64File() {
            try {
                lorealMain.writeFile(_tmpB64FileName, Base64.decode(_tmpB64File, Base64.DEFAULT));
            }
            catch (Exception ex){
                showWaiting(false);
                Toast.makeText(this, getString(R.string.err_download), Toast.LENGTH_SHORT).show();
                ex.printStackTrace();
            }
            _tmpB64FileName = "";
            _tmpB64File = "";
            showWaiting(false);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (grantResults.length >= 1 && permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                if (grantResults[0]== PackageManager.PERMISSION_GRANTED) writeTmpB64File();
                _tmpB64FileName = "";
                _tmpB64File = "";
            }
        }

        private class LorealWebViewClient extends WebViewClient {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                if (error.getUrl().equals(LorealMain.appUrl+'/')) handler.proceed();
                else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this);
                    builder.setMessage(R.string.err_ssl);
                    builder.setPositiveButton(R.string.txt_proceed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.proceed();// Ignore SSL certificate errors
                        }
                    });
                    builder.setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            handler.cancel();
                        }
                    });
                    builder.show();
                }
            }

            /*protected boolean isProfileURL(String p_host) {
                return p_host.equals("profile.loreal.com") || p_host.equals("profile.loreal.wans") || p_host.equals("myprofile.loreal.wans");
            }*/
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //Log.d("CED", "shouldOverrideUrlLoading --> " + url);
                /*if (isProfileURL(Uri.parse(url).getHost())) {
                  String[] parts = url.split("DanaInfo=");
                  if (parts.length < 2) return false;
                  else {
                      parts = parts[1].split(",");
                      if (isProfileURL(parts[0])) return false;
                  }
                }
                // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;*/
                super.shouldOverrideUrlLoading(view,url);
                return false;
            }
        }//WebViewClient

        private class LorealWebChromeClient extends WebChromeClient {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage){
                Log.d("CED", "JS console --> " + consoleMessage.message());
                return true  ;
            }

            @Override
            public boolean onCreateWindow (WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg){
                //Log.d("CED", "onCreateWindow--> " + isDialog);
                Toast.makeText(WebViewActivity.this, getString(R.string.err_winopen), Toast.LENGTH_SHORT).show();
                super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                return false;
            }
        }//WebChromeClient

        /**************************** JAVASCRIPT INTERFACE **************************** */
        public class WebAppInterface {
            Context mContext;
            WebView webView;

            WebAppInterface(Context c, WebView w) {
                mContext = c;
                webView = w;
            }

            @JavascriptInterface
            public String testInterface(String p_str) {
                Toast.makeText(mContext, p_str, Toast.LENGTH_SHORT).show();
                return "STR FROM ANDROID";
            }

            @JavascriptInterface
            public String getAppToken() {
                return lorealMain.appToken_create().toString();
            }

            @JavascriptInterface
            public String appGUID_get() {
                return lorealMain.getGUID();
            }

            @JavascriptInterface
            public void appGUID_invalidate(){
                lorealMain.invalidateGUID();
                startActivity(new Intent(getApplicationContext(), SplashActivity.class));
            }

            @JavascriptInterface
            public String appUUID_get() {
                return lorealMain.getUUID();
            }

            @JavascriptInterface
            public String getAppVersion() {
                return lorealMain.getVERSION();
            }

            @JavascriptInterface
            public String getData(String p_key) {
                return lorealMain.getData(p_key);
            }

            @JavascriptInterface
            public void setData(String p_key, String p_value) {
                lorealMain.setData(p_key,p_value);
            }

            @JavascriptInterface
            public void closeApplication(){
                exitApp();
            }

            @JavascriptInterface
            public String getInternalB64Data(String p_filename){
                return internalB64Data.remove(p_filename);
            }

            @JavascriptInterface
            public void showPDF(String p_name, String b64data){
                //Log.d("CED", "showPDF " + p_name);
                Intent intentPDF  = new Intent(this.mContext, PdfViewer.class);
                intentPDF.putExtra("name", p_name);
                intentPDF.putExtra("decodedString", Base64.decode(b64data, Base64.DEFAULT));
                this.mContext.startActivity(intentPDF);
            }

            @JavascriptInterface
            public void mailTo(String data){
                JSONObject mailData = null;
                String dest = "";
                String cc = "";
                String subject = "";
                String content = "";
                try {
                    mailData = new JSONObject(data);
                    dest = mailData.getString("dest");
                    cc = mailData.getString("cc");
                    subject = mailData.getString("subject");
                    content = mailData.getString("content");
                } catch (Exception ex){
                    Log.d("CED", ex.getMessage());
                }
               // MailTo mailTo = MailTo.parse(url);
                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/html");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, dest);
                emailIntent.putExtra(Intent.EXTRA_CC, cc);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(content));
                startActivity(emailIntent);
                //startActivity(Intent.createChooser(emailIntent, getString(R.string.txt_sendEmail)));

            }

            @JavascriptInterface
            public void hideKeyboard() {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }

            @JavascriptInterface
            public void showKeyboard() {
                //Activity activity = ActivityWebView.this; // .getSystemService & getCurrentFocus;
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
            }

            @JavascriptInterface
            public void openLinkInBrowser(String p_url){
                final Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(p_url));
                startActivity(i);
            }

            @JavascriptInterface
            public void getFile() {
                getFile(getString(R.string.txt_getFile));
            }

            @JavascriptInterface
            public void getFile(String p_title) {
                if (lorealMain.isPermissionGranted(WebViewActivity.this, Manifest.permission.CAMERA)) {
                    final CharSequence[] items = { getString(R.string.txt_take_picture), getString(R.string.txt_open_files)};
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.Theme_MaterialComponents_Light_Dialog);
                    builder.setTitle(R.string.txt_getFile);
                    builder.setIcon(android.R.drawable.ic_menu_upload);
                    builder.setNegativeButton (R.string.txt_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            dialog.dismiss();
                        }
                    });
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (item == 0) {
                                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (captureIntent.resolveActivity(getPackageManager()) != null) {
                                    _tmpFile = new File(mContext.getExternalCacheDir(), String.valueOf(System.currentTimeMillis()) + ".jpg");
                                    _tmpUri = FileProvider.getUriForFile(mContext, getApplicationContext().getPackageName()+".fileProvider", _tmpFile);
                                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, _tmpUri);
                                    startActivityForResult(captureIntent, CAMERA_CAPTURE_FOR_FILE);
                                }
                            }
                            else {
                                Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                                fileIntent.setType("*/*");
                                startActivityForResult(fileIntent, FILE_SELECT);
                            }
                        }
                    });
                    builder.show();
                }
                else {
                    Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    fileIntent.setType("*/*");
                    startActivityForResult(fileIntent, FILE_SELECT);
                }
            }

            @JavascriptInterface
            public void downloadFile(String p_name, String p_base64Data) {
                _tmpB64FileName = p_name;
                _tmpB64File = p_base64Data;
                if (lorealMain.isPermissionGranted(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) writeTmpB64File();
                else lorealMain.requestPermission(WebViewActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
            }

            @JavascriptInterface
            public void getPhoto(String cropWidth, String cropHeight) {
                lorealMain.cropWidth = Integer.parseInt(cropWidth);
                lorealMain.cropHeight = Integer.parseInt(cropHeight);
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setFixAspectRatio(true)
                        .setAspectRatio(lorealMain.cropWidth , lorealMain.cropHeight)
                        .setMinCropResultSize(lorealMain.cropWidth , lorealMain.cropHeight)
                        .start(WebViewActivity.this);
            }

            @JavascriptInterface
            public void showWaiting(String p_bool){
                WebViewActivity.this.showWaiting(Boolean.valueOf(p_bool));
            }

            @JavascriptInterface
            public void promptB4Exit(){
                new AlertDialog.Builder(WebViewActivity.this)
                    .setMessage(getString(R.string.confirmExit))
                    .setPositiveButton(getString(R.string.txt_exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exitApp();
                        }
                    })
                    .setNegativeButton(getString(R.string.txt_cancel), null)
                    .show();
            }

        }//WebAppInterface

    }//Activity