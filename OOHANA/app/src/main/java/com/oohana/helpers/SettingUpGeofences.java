package com.oohana.helpers;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.oohana.GeofenceTransitionReceiver;
import com.oohana.database.ServerGeofence;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by elysi on 5/15/2017.
 */

public class SettingUpGeofences {

    private Context c;
    private SharedPreferences prefs;
    private GoogleApiClient googleApiClient;

    public SettingUpGeofences(Context c, GoogleApiClient googleApiClient) {
        this.c = c;
        this.googleApiClient = googleApiClient;
        prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private boolean checkPermisson() {
        return (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    public void startGeofencing() {
        if (checkPermisson()) {
            Realm realm = Realm.getDefaultInstance();
            RealmResults<ServerGeofence> geofences = realm.where(ServerGeofence.class).findAll();
            ArrayList<Geofence> mGeofenceList = new ArrayList<>();
            for (ServerGeofence g : geofences) {
                mGeofenceList.add(createGeofence(g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad() * 1000, g.getGeof_name()));
            }
            addGeofence(createGeofenceRequest(mGeofenceList));
        } else {
            // askPermission();
        }
    }

    private Geofence createGeofence(double lat, double lng, float radius, String id) {
        System.out.println("CHECK: creatingGeofence=" + id);
        prefs.edit().putInt(Constants.GEOFENCE_NUM, prefs.getInt(Constants.GEOFENCE_NUM,0)+1).commit();
        System.out.println("CHECK: creatingGeofence_num=" + prefs.getInt(Constants.GEOFENCE_NUM, 0));
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_DWELL
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(Constants.LOITERING_DELAY) //10 minutes dwelling
                .build();
    }

    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        System.out.println("CHECK: creatingRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL
                        | GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build();
    }

    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {
        System.out.println("CHECK: creatingRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofences)
                .build();
    }

    private PendingIntent geoFencePendingIntent;

    private PendingIntent createGeofencePendingIntent() {
        if (geoFencePendingIntent != null) return geoFencePendingIntent;

        Intent intent = new Intent(c, GeofenceTransitionReceiver.class);
        intent.putExtra(Constants.IS_NOTIFICATION, true);
        geoFencePendingIntent = PendingIntent.getBroadcast(c, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoFencePendingIntent;
    }

    private void addGeofence(GeofencingRequest request) {

        System.out.println("CHECK: Entered adding geofence");
        if (checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }

            System.out.println("CHECK: adding geofence request");
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient, request, createGeofencePendingIntent()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {

                    System.out.println("CHECK: Status=" + status.getStatus());
                    if (status.isSuccess()) {
                        Toast.makeText(
                                c,
                                "Geofences Added",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else if (status.getStatusCode() == 1000) {
                        // Get the status code for the error and log it using a user-friendly message.
                        prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();
                        Toast.makeText(
                                c,
                                "Please turn on Google Location services in Settings",
                                Toast.LENGTH_LONG
                        ).show();

                        System.out.println("CHECK: Status code 100");
                    }
                }
            });
        }
    }
}
