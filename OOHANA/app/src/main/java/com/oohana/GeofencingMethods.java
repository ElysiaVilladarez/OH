package com.oohana;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by elysi on 5/19/2017.
 */

public class GeofencingMethods {
    private GeofencingRequest.Builder geofencingRequestBuilder;
    private PendingIntent geofencePendingIntent;
    private Context c;
    private GoogleApiClient googleApiClient;
    private SharedPreferences prefs;
    public static Location location;


    public GeofencingMethods(Context c, SharedPreferences prefs) {
        this.c = c;
        this.prefs = prefs;
        geofencingRequestBuilder = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL);
    }

    public Geofence createGeofence(String name, double lat, double lng, float radius) {
        System.out.println("Creating geofences");
        if(radius <= 0) radius = Constants.GEOFENCE_RADIUS_DEFAULT_VALUE;
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
        System.out.println("Adding geofencing request");
        prefs.edit().putInt(Constants.GEOFENCE_NUM, prefs.getInt(Constants.GEOFENCE_NUM, 0)+1).commit();
        geofencingRequestBuilder.addGeofence(g);
    }

    public PendingIntent createPendingIntent() {
        System.out.println("Creating pending intent");
        if (geofencePendingIntent != null) return geofencePendingIntent;

        Intent intent = new Intent(c, GeofenceTriggeredService.class);
        geofencePendingIntent = PendingIntent.getService(c, Constants.PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

//        Intent intent = new Intent(Constants.ACTION_GEOFENCE_RECEIVED);
//        geofencePendingIntent = PendingIntent.getBroadcast(c, Constants.PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return geofencePendingIntent;
    }

    public void addToGeofencingApi(){

        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("addToGeofencingAPI");

        }
//        if(Home.geofenceNum.getText().toString().equals("0")){
//            new AlertDialog.Builder(c)
//                    .setTitle("No Geofence Data Found")
//                    .setMessage("No geofence list found. An error occurred in getting the data from the server.")
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                        }
//                    });
//            return;
//        }
        LocationServices.GeofencingApi.addGeofences(
                googleApiClient, geofencingRequestBuilder.build(), createPendingIntent())
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            System.out.println("Successfully set up geofences");
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
                        } else{
                            Toast.makeText(
                                    c,
                                    "Error status code: "+ status.getStatusCode(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        //Home.geofenceNum.setText(Integer.toString(prefs.getInt(Constants.GEOFENCE_NUM, 0)));
                        prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();

                    }
                });
    }

    //Building googleAPI client
    public synchronized void buildGoogleApiClient() {
        System.out.println("BUILD GOOGLE API");
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(c)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            System.out.println("GOOGLE API CLIENT CONNECTED");
                            addToGeofencingApi();
                            updateLocation();

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

    //Location updates
    public void updateLocation(){
        LocationParams.Builder builder = new LocationParams.Builder()
                .setAccuracy(LocationAccuracy.HIGH)
                .setDistance(0)
                .setInterval(Constants.LOCATION_TRACKING_INTERVAL);
        SmartLocation.with(c).location(new LocationGooglePlayServicesWithFallbackProvider(c))
                .continuous()
                .config(builder.build())
                .continuous()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        System.out.println("LOCATION UPDATED");

                        GeofencingMethods.this.location = location;

                        if(Home.latText !=null && Home.lngText !=null && Home.timestamp!=null) {
                            Home.latText.setText(Double.toString(location.getLatitude()));
                            Home.lngText.setText(Double.toString(location.getLongitude()));
                            Home.timestamp.setText(Constants.f.format(Calendar.getInstance().getTime()));
                        }

                        getNearest(location);
                    }
                });
    }


    //Harversin formula
    public static final double R = 6372.8; // In kilometers

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c * 1000;
    }

    //apply harversine to all
    public void getNearest(Location location){ // this should be 95 just in case
        if(location!=null) {
            Realm realm = Realm.getDefaultInstance();
            RealmResults<ServerGeofence> sgList = realm.where(ServerGeofence.class).findAll();
            realm.beginTransaction();
            for (ServerGeofence a : sgList) {
                a.setNearnesstToCurrLoc(haversine(location.getLatitude(), location.getLongitude(), a.getGeof_lat(), a.getGeof_long()));
            }
            realm.commitTransaction();
        } else{
            updateLocation();
        }
    }
}
