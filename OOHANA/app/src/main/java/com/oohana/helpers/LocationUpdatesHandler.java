package com.oohana.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

/**
 * Created by elysi on 5/15/2017.
 */

public class LocationUpdatesHandler {

    private Context c;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    private boolean checkPermisson() {
        return (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public LocationUpdatesHandler(Context c, GoogleApiClient googleApiClient) {
        this.c = c;
        this.googleApiClient = googleApiClient;
    }

    public void getLastKnownLocation() {
        if (checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                System.out.println("Last Location - LONG=" + lastLocation.getLongitude() + "\nLAT=" +
                        lastLocation.getLatitude() + "\nTimeStamp=" + Calendar.getInstance().getTime().toString());
                startLocationUpdates();
            } else {
                startLocationUpdates();
            }
        } else {
            //askPermission();
        }
    }


    private void startLocationUpdates() {

        if (checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }
//             if (getLocationMode(c) == 0) {
//                new AlertDialog.Builder(this).setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                }).setCancelable(true).setTitle("Use Google location services").setMessage("Please turn on Google Location services in Settings").create().show();
//
//            } else if (mode == 3) {
//                LocationRequest locationRequest = LocationRequest.create()
//                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                        .setInterval(Constants.UPDATE_INTERVAL)
//                        .setFastestInterval(Constants.FASTEST_INTERVAL);
//                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
//                        locationRequest, new com.google.android.gms.location.LocationListener() {
//                    @Override
//                    public void onLocationChanged(Location location) {
//                        System.out.println("CHECK: Location Changes");
//                        lastLocation = location;
//                        System.out.println("Last Location - LONG=" + lastLocation.getLongitude() + "\nLAT=" +
//                                lastLocation.getLatitude() + "\nTimeStamp=" + Calendar.getInstance().getTime().toString());
//                    }
//                });
//            } else {
            System.out.println("CHECK: requesting updates");

            LocationManager locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.UPDATE_INTERVAL,
                    10, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    System.out.println("CHECK: Location Changes");
                    lastLocation = location;
                    System.out.println("Last Location - LONG=" + lastLocation.getLongitude() + "\nLAT=" +
                            lastLocation.getLatitude() + "\nTimeStamp=" + Calendar.getInstance().getTime().toString());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
//            }
        } else {

            //askPermission();
        }
    }
    private boolean isLocationModeAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= 19 && getLocationMode(context) != Settings.Secure.LOCATION_MODE_OFF) {
            return true;
        }
        else return false;
    }

    public boolean isLocationServciesAvailable(Context context) {
        if (Build.VERSION.SDK_INT < 19) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        }
        else return false;
    }

    public int getLocationMode(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
