package com.oohana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmResults;

public class Home extends AppCompatActivity {
    private Realm realm;
    public static TextView latText, lngText, timestamp, geofenceNum;
    private SharedPreferences prefs;
    private ListView logsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Realm.init(getApplicationContext());

        realm = Realm.getDefaultInstance();
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        //get views
//        latText = (TextView) findViewById(R.id.latText);
//        lngText = (TextView) findViewById(R.id.lngText);
//        timestamp = (TextView) findViewById(R.id.dateUpdatedText);
        geofenceNum = (TextView) findViewById(R.id.geofenceNum);
//        logsList = (ListView) findViewById(R.id.logList);
//        log



        //Get geofence from server
        if (realm.where(ServerGeofence.class).count() <= 0) {
            getData();
        } else {
            startGeofencing();
        }

//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        Intent myIntent = new Intent(this, GeofenceReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
//                0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 30*60*1000,
//                        AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);


    }

    private void getData(){
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
                                            System.out.println("GOT DATA FROM SERVER=" + realm.where(ServerGeofence.class).count());
                                        }
                                    });

                                    startGeofencing();
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

    private void startGeofencing(){
        //Set up geofence
        GeofencingMethods gM = new GeofencingMethods(getApplicationContext(), prefs);
        RealmResults<ServerGeofence> geofenceList = realm.where(ServerGeofence.class).findAll();
        for (ServerGeofence g : geofenceList) {
            Geofence geofence = gM.createGeofence(g.getGeof_name(), g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad()*1000);
            gM.addToGeofencingRequest(geofence);
        }

        //Build googleApiClient and connect to service
        gM.buildGoogleApiClient();
    }
}
