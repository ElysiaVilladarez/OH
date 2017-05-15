package com.oohana;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
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

import io.realm.Realm;
import io.realm.RealmResults;

//import com.google.android.gms.location.LocationListener;

public class HomeActivity extends AppCompatActivity {

    private GoogleApiClient googleApiClient;
    protected ArrayList<Geofence> mGeofenceList;
    private Location lastLocation;
    private LocationRequest locationRequest;
    private final int UPDATE_INTERVAL = 60000;
    private final int FASTEST_INTERVAL = 30000;
    private final int REQ_PERMISSION = 100;
    private final int LOITERING_DELAY = 60000;// should be 10 mins
    private Realm realm;
    private TextView lastLocationText;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Realm.init(getApplicationContext());
        realm = realm.getDefaultInstance();
        Switch geofenceSwitch = (Switch) findViewById(R.id.geofenceSwitch);
        geofenceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()

                                                  {
                                                      @Override
                                                      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                          if (isChecked) {
                                                              // getLastKnownLocation();

                                                          }
                                                      }
                                                  }

        );
        if(checkPermisson()){
            reCheckGPS();
        } else{
            askPermission();
        }
    }
    private void reCheckGPS(){
        LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            registerBrodcast();
        } else{
            buildAlertMessageNoGps();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        System.out.println("CHECK: onResume Geofence_Count=" +getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getInt(Constants.GEOFENCE_NUM,0));
        if(getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
                .getInt(Constants.GEOFENCE_NUM,0) < realm.where(ServerGeofence.class).count()) {
            reCheckGPS();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, You should enable GPS to work Geo-fence.")
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
        final AlertDialog alert = builder.create();
        alert.show();
    }
    private void registerBrodcast(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(this, GeofenceTransitionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if(Build.VERSION.SDK_INT < 23){
            if(Build.VERSION.SDK_INT >= 19){
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, 1000, pendingIntent);
            }
            else{
                alarmManager.set(AlarmManager.RTC_WAKEUP, 1000, pendingIntent);
            }
        }
        else{
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 1000, pendingIntent);
        }

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
                   registerBrodcast();
                } else {
                    permissionDenied();
                }
        }
    }

    private void permissionDenied() {
        //what happens when permission is denied?
        // exit app
    }


}
