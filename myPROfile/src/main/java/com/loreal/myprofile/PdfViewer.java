package com.loreal.myprofile;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;


import com.github.barteksc.pdfviewer.PDFView;
import com.loreal.myprofile.common.LorealMain;

public class PdfViewer extends Activity {
    PDFView pdfView;
    String name;
    byte[] decodedString;
    LorealMain lorealMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        lorealMain = LorealMain.getInstance(PdfViewer.this);
        name = getIntent().getStringExtra("name");
        decodedString = getIntent().getByteArrayExtra("decodedString");

        FloatingActionButton saveBtn = findViewById(R.id.fABSavePDF);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lorealMain.isPermissionGranted(PdfViewer.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) writeFile();
                else lorealMain.requestPermission(PdfViewer.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
            }
        });
        pdfView = ((PDFView) findViewById(R.id.pdfView));
        pdfView.fromBytes(decodedString).load();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length >= 1 && grantResults[0]== PackageManager.PERMISSION_GRANTED && permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE) writeFile();
    }

    private void writeFile(){
        lorealMain.writePDF(name, decodedString);
        /*Intent intent = new Intent();
        intent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(intent);*/
    }

}
