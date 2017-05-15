package com.oohana;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.oohana.database.GetDataFromServer;
import com.oohana.database.ServerGeofence;
import com.oohana.helpers.Constants;
import com.oohana.helpers.LocationUpdatesHandler;
import com.oohana.helpers.NotificationsHandler;
import com.oohana.helpers.SettingUpGeofences;

import java.util.ArrayList;

import io.realm.Realm;

/**
 * Created by elysi on 5/13/2017.
 */

public class GeofenceTransitionReceiver extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private Context c;
    private boolean rebootedOrToggled;
    private GoogleApiClient googleApiClient;
    protected ArrayList<Geofence> mGeofenceList;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private Realm realm;
    private LocationManager locationManager;
    private SharedPreferences prefs;


    private boolean checkPermisson() {
        return (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        c = context;

        System.out.println("CHECK: Receiver triggered");
        prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        rebootedOrToggled = false;

        locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                context.registerReceiver(this, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                rebootedOrToggled = true;
                prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();
            } else {

            }
        }

        if ("android.location.PROVIDERS_CHANGED".equals(intent.getAction())) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //add google api client again, add geofences
                rebootedOrToggled = true;
                prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();
            } else {
                // go to settings and do it!!
            }
        }

        if(intent.getBooleanExtra(Constants.IS_NOTIFICATION, false)){
            new NotificationsHandler(c).geofenceEvent(intent);

        } else{
            realm = Realm.getDefaultInstance();
            mGeofenceList = new ArrayList<>();
            GetDataFromServer.getData(c);

            System.out.println("CHECK: Geofence_Num="+prefs.getInt(Constants.GEOFENCE_NUM,0));
            if(prefs.getInt(Constants.GEOFENCE_NUM,0) < realm.where(ServerGeofence.class).count()){
                buildGoogleApiClient();
            }
        }


    }

    //Building googleAPI client
    protected synchronized void buildGoogleApiClient() {
        System.out.println("CHECK: Building GoogleAPI . . . ");
        if(googleApiClient==null) {
            googleApiClient = new GoogleApiClient.Builder(c)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();

        }
        if(!googleApiClient.isConnected() && !googleApiClient.isConnecting()){
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("CHECK: Google API connected");
        LocationUpdatesHandler luh = new LocationUpdatesHandler(c, googleApiClient);
        System.out.println("CHECK: mode=" + luh.getLocationMode(c));
        if (c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.INSTALLATION_NUM, -1) == 2
                || rebootedOrToggled) {
            System.out.println("CHECK: First install. Setting up . . .");
            luh.getLastKnownLocation();
            SettingUpGeofences sug = new SettingUpGeofences(c, googleApiClient);
            sug.startGeofencing();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

        System.out.println("CHECK: Connection suspended="+ i);
        if(googleApiClient!=null)
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(c, "Failed to connect to GoogleApiClient. Please restart application.", Toast.LENGTH_LONG).show();
    }

}
