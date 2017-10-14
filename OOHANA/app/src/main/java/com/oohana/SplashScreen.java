package com.oohana;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.lang.ref.WeakReference;

import io.realm.Realm;

public class SplashScreen extends AppCompatActivity {
    private static class StartMainActivityRunnable implements Runnable {
        private WeakReference mActivity;
        private View view;

        /**
         * Duration of wait
         **/


        private StartMainActivityRunnable(Activity activity, View view) {
            mActivity = new WeakReference(activity);
            this.view = view;
        }

        @Override
        public void run() {
            // 3. Check that the reference is valid and execute the code
            if (mActivity.get() != null) {

                Activity activity = (Activity) mActivity.get();
                activity.startActivity(new Intent(activity, Home.class));

            }
        }
    }

    private Handler mHandler = new Handler();
    private View view;
    private AlertDialog alert;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Realm.init(getApplicationContext());

        view = findViewById(R.id.activity_splash_screen);

    }

    private void checkLocPermission(){
        if(checkPermisson()) {
            if(checkPermission2()) {
                checkProvider();
            } else{
                askPermission2();
            }
        } else askPermission();
    }
    private void checkProvider(){
        LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            System.out.println("PROVIDER ENABLED");
            if(alert != null)
                alert.dismiss();

            mHandler.postDelayed(new StartMainActivityRunnable(this, view), Constants.LOGO_DISPLAY_LENGTH);
        } else {
            buildAlertMessageNoGps();
        }
    }

    //Override life cycles
    @Override
    public void onResume(){
        super.onResume();
        checkLocPermission();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);

        mHandler = null;

    }
    @Override
    protected void onStop() {
        super.onStop();

        if(alert != null)
            alert.dismiss();
    }

    //Permission Methods
    private boolean checkPermisson() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQ_PERMISSION);
    }

    private boolean checkPermission2(){
//        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
//        return (permissionCheck == PackageManager.PERMISSION_GRANTED);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE);
        return (permissionCheck == PackageManager.PERMISSION_GRANTED);

    }

    private void askPermission2(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, Constants.REQUEST_READ_PHONE_STATE);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQ_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("PERMISSION GRANTED-1");
                    if(checkPermission2()) {
                        checkProvider();
                    }else askPermission2();
                } else {
                    permissionDenied();
                }
            case Constants.REQUEST_READ_PHONE_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("PERMISSION GRANTED-2");
                    checkProvider();
                } else {
                    permissionDenied();
                }
        }
    }

    public void permissionDenied(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please allow OOHANA to permissions to proceed.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent i = new Intent();
                        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        i.setData(uri);
                        startActivity(i);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    // ProviderEnabled method
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, You should enable GPS: High Accuracy to work OOHANA.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        alert = builder.create();
        alert.show();
    }
}