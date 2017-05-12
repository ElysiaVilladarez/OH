package com.oohana;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.oohana.database.ServerGeofence;
import com.oohana.helpers.Constants;
import com.oohana.oohana.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

public class HomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    private GoogleApiClient googleApiClient;
    protected ArrayList<Geofence> mGeofenceList;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private final int UPDATE_INTERVAL = 60000;
    private final int FASTEST_INTERVAL = 30000;
    private final int REQ_PERMISSION = 100;
    private Realm realm;
    private TextView lastLocationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Realm.init(getApplicationContext());
        realm = Realm.getDefaultInstance();
        lastLocationText = (TextView) findViewById(R.id.lastLocationText);
        mGeofenceList = new ArrayList<>();

        if(getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE).getInt(Constants.INSTALLATION_NUM, -1) == 2) {
            JsonObjectRequest geofenceList = new JsonObjectRequest
                    (Request.Method.GET, Constants.geofenceListLink, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String success = response.getString("result");
                                if (success.equals("success")) {
                                    final JSONArray geofences = response.getJSONArray("geofence");
                                    realm.executeTransaction(new Realm.Transaction() {
                                        @Override
                                        public void execute(Realm realm) {
                                            realm.createOrUpdateAllFromJson(ServerGeofence.class, geofences);

                                            System.out.println("CHECK: GEOF COUNT=" + realm.where(ServerGeofence.class).count());

                                            //create googleAPI client instance
                                            if (googleApiClient == null) {
                                                googleApiClient = new GoogleApiClient.Builder(HomeActivity.this)
                                                        .addConnectionCallbacks(HomeActivity.this)
                                                        .addOnConnectionFailedListener(HomeActivity.this)
                                                        .addApi(LocationServices.API)
                                                        .build();
                                                googleApiClient.connect();
                                            }



                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    });

            RequestQueSingleton.getInstance(getApplicationContext()).getRequestQueue().add(geofenceList);
        }

        Switch geofenceSwitch = (Switch) findViewById(R.id.geofenceSwitch);
        geofenceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                   // getLastKnownLocation();

                }
            }
        });


    }

    public void startGeofencing(){
        if(checkPermisson()) {
            RealmResults<ServerGeofence> geofences = realm.where(ServerGeofence.class).findAll();
            for (ServerGeofence g : geofences) {
                mGeofenceList.add(createGeofence(g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad() * 1000, g.getGeof_name()));
            }

            addGeofence(createGeofenceRequest(mGeofenceList));
        } else{
            askPermission();
        }
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        System.out.println("CHECK: Google API connected");
        getLastKnownLocation();
        startGeofencing();

    }

    private void getLastKnownLocation() {
        if (checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
               lastLocationText.setText("Last Location - LONG=" + lastLocation.getLongitude() + "\nLAT=" +
                       lastLocation.getLatitude() + "\nTimeStamp=" + Calendar.getInstance().getTime().toString());
                startLocationUpdates();
            } else {
                startLocationUpdates();
            }
        } else askPermission();
    }

    private void startLocationUpdates() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        if (checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Failed to connect to GoogleApiClient. Please restart application.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //disconnect though this might be remove to continuously get location
        if (googleApiClient!=null) {
            if (googleApiClient.isConnecting() || googleApiClient.isConnected()) {
                googleApiClient.disconnect();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient!=null) {
            if (!googleApiClient.isConnecting() || !googleApiClient.isConnected()) {
                googleApiClient.connect();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("CHECK: Location Changes");
        lastLocation = location;
        lastLocationText.setText("Last Location - LONG=" + lastLocation.getLongitude() + "\nLAT=" +
                lastLocation.getLatitude() + "\nTimeStamp=" + Calendar.getInstance().getTime().toString());
    }

    private boolean checkPermisson() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastKnownLocation();
                    startGeofencing();
                } else {
                    permissionDenied();
                }
        }
    }

    private void permissionDenied() {
        //what happens when permission is denied?
        // exit app
    }

    //Creating geofence

    private Geofence createGeofence(double lat, double lng, float radius, String id) {

        System.out.println("CHECK: creatingGeofence=" + id);
        return new Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_DWELL
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(600000) //10 minutes dwelling
                .build();
    }

    private GeofencingRequest createGeofenceRequest(Geofence geofence) {

        System.out.println("CHECK: creatingRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    private GeofencingRequest createGeofenceRequest(List<Geofence> geofences) {

        System.out.println("CHECK: creatingRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();
    }

    //Creating the service to take care of event triggers
    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;

    private PendingIntent createGeofencePendingIntent() {
        if (geoFencePendingIntent != null) return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionService.class);
        geoFencePendingIntent = PendingIntent.getService(this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoFencePendingIntent;
    }

    private void addGeofence(GeofencingRequest request) {

        System.out.println("CHECK: Entered adding geofence");
        if(checkPermisson()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }

            System.out.println("CHECK: adding geofence request");
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient, request, createGeofencePendingIntent()
            ).setResultCallback(this);
        }
    }
    @Override
    public void onResult(Status status) {

        System.out.println("CHECK: Status=" + status.getStatus());
        if (status.isSuccess()) {
            Toast.makeText(
                    this,
                    "Geofences Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else if (status.getStatusCode()==1000){
            // Get the status code for the error and log it using a user-friendly message.
            new AlertDialog.Builder(this).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setCancelable(true).setTitle("Use Google location services").setMessage("Please turn on Google Location services in Settings. Enable High Accuracy Mode.").create().show();
        }
    }

//    private void startGeofence(double lat, double lng, float radius, String id){
//        System.out.println("CHECK: Start Geofence=" + id);
//        Geofence geofence = createGeofence(lat, lng, radius ,id);
//        GeofencingRequest geofencingRequest = createGeofenceRequest(geofence);
//        addGeofence(geofencingRequest);
//    }


}
