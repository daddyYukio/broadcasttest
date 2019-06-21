package com.daddysoffice.sample.broadcasttest;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionConfirmActivity extends Activity {

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()){
                startApplication();
            }
        }
        else {
            startApplication();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkPermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { android.Manifest.permission.CAMERA },
                    PERMISSIONS_REQUEST_CAMERA);

            return false;
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermission()){
                    startApplication();
                }
            }
            else {
                finish();
            }
        }
    }

    private void startApplication(){
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
