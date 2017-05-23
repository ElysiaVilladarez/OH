package com.oohana;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by elysi on 5/19/2017.
 */

public class GeofencingMethods {
    private GeofencingRequest.Builder geofencingRequestBuilder;
    private PendingIntent geofencePendingIntent;
    private Context c;
    private GoogleApiClient googleApiClient;
    private SharedPreferences prefs;

    public GeofencingMethods(Context c, SharedPreferences prefs) {
        this.c = c;
        this.prefs = prefs;
        geofencingRequestBuilder = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
    }

    public Geofence createGeofence(String name, double lat, double lng, float radius) {
        return new Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_DWELL
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(Constants.LOITERING_DELAY) //10 minutes dwelling
                .build();
    }

    public void addToGeofencingRequest(Geofence g) {
        Home.geofenceNum.setText(Integer.toString(Integer.parseInt(Home.geofenceNum.getText().toString())+1));
        prefs.edit().putInt(Constants.GEOFENCE_NUM, prefs.getInt(Constants.GEOFENCE_NUM, 0)+1).commit();
        geofencingRequestBuilder.addGeofence(g);
    }

    public PendingIntent createPendingIntent() {
        if (geofencePendingIntent != null) return geofencePendingIntent;

     //   Intent intent = new Intent(c, GeofenceTriggeredService.class);
     //   geofencePendingIntent = PendingIntent.getService(c, Constants.PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        Intent intent = new Intent(Constants.ACTION_GEOFENCE_RECEIVED);
        geofencePendingIntent = PendingIntent.getBroadcast(c, Constants.PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;
    }

    public void addToGeofencingApi(){
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        if(Home.geofenceNum.getText().equals(0)){
            new AlertDialog.Builder(c)
                    .setTitle("No Geofence Data Found")
                    .setMessage("No geofence list found. An error occurred in getting the data from the server.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                googleApiClient, geofencingRequestBuilder.build(), createPendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Toast.makeText(
                                    c,
                                    "Geofences added",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else if (status.getStatusCode() == 1000) {
                            Toast.makeText(
                                    c,
                                    "Please turn on Google Location services in Settings. Switch to \"High Accuracy\" mode",
                                    Toast.LENGTH_LONG
                            ).show();


                            prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();
                            Home.geofenceNum.setText("0");
                        }


                    }
                });
    }
    //Building googleAPI client
    public synchronized void buildGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(c)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            System.out.println("GOOGLE API CLIENT CONNECTED");
                            addToGeofencingApi();
                            //Go to service when triggered

                        }
                        @Override
                        public void onConnectionSuspended(int i) {
                            if(googleApiClient!=null){
                                googleApiClient.connect();
                            }
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Toast.makeText(c, "Failed to connect to GoogleApiClient. Please restart application.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addApi(LocationServices.API).build();

        }
        if(!googleApiClient.isConnected() && !googleApiClient.isConnecting()){
            googleApiClient.connect();
        }
    }


}
