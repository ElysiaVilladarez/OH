package com.oohana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by elysi on 5/20/2017.
 */

public class GeofenceReceiver extends BroadcastReceiver {
    private Context c;
    private SharedPreferences prefs;
   // Intent broadcastIntent = new Intent();

    Realm realm = Realm.getDefaultInstance();


    @Override
    public void onReceive(Context context, Intent intent) {
        this.c = context;
        prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                context.registerReceiver(this, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

            }
        }

        if ("android.location.PROVIDERS_CHANGED".equals(intent.getAction())) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (realm.where(ServerGeofence.class).count() <= 0) {
                    getData();
                } else {
                    startGeofencing();
                }
            } else {
                // go to settings and do it!!
            }
        }

        if(Constants.ACTION_GEOFENCE_RECEIVED.equals(intent.getAction())) {
            Toast.makeText(context, "Geofence Spotted", Toast.LENGTH_LONG).show();
            new NotificationMaker(context).geofenceEvent(intent);
        }
//        broadcastIntent
//                .setAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION)
//                .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
//                .putExtra(GeofenceUtils.EXTRA_GEOFENCE_ID, geofenceIds)
//                .putExtra(GeofenceUtils.EXTRA_GEOFENCE_TRANSITION_TYPE,
//                        transitionType);
//
//        LocalBroadcastManager.getInstance(c).sendBroadcast(broadcastIntent);

        // getData();
    }

    private void getData() {
        JsonObjectRequest geofenceList = new JsonObjectRequest
                (Request.Method.GET, Constants.geofenceListLink, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String success = response.getString("result");
                            if (success.equals("success")) {
                                final JSONArray geofences = response.getJSONArray("geofence");
                                for (int i = 0; i < geofences.length(); i++) {
                                    JSONObject g = geofences.getJSONObject(i);
                                    if (realm.where(ServerGeofence.class).equalTo("geof_id", g.getInt("geof_id")).count() <= 0) {
                                        realm.beginTransaction();
                                        ServerGeofence sg = new ServerGeofence();
                                        sg.setGeof_id(g.getInt("geof_id"));
                                        sg.setGeof_name(g.getString("geof_name"));
                                        sg.setGeof_lat(g.getDouble("geof_lat"));
                                        sg.setGeof_long(g.getDouble("geof_long"));
                                        sg.setGeof_rad((float) g.getDouble("geof_rad"));
                                        realm.insert(sg);
                                        realm.commitTransaction();
                                    }

                                }

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

        RequestQueSingleton.getInstance(c).getRequestQueue().add(geofenceList);
    }


    private void syncData() {
        final TelephonyManager tm = (TelephonyManager) c.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(c.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("deviceID", deviceId);
        Realm realm = Realm.getDefaultInstance();
        RealmResults<TriggeredGeofence> logs = realm.where(TriggeredGeofence.class).findAll();
        for (TriggeredGeofence t : logs) {
            params.put("geof_id", Integer.toString(t.getGeof_id()));
            params.put("status", translateStatus(t.getStatus()));
            params.put("timestamp", t.getTimestamp().toString());

            JsonObjectRequest syncRequest = new JsonObjectRequest(Request.Method.POST, Constants.syncLink, new JSONObject(params),
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // response
                    try {
                        Log.d("Response", response.getString("result"));
                        // if success, delete the log from db
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // error
                    Log.d("Error.Response", error.getMessage());
                }
            });
            RequestQueSingleton.getInstance(c).getRequestQueue().add(syncRequest);
        }
    }

    public String translateStatus(int i) {
        if (i == 0) return "ENTERING";
        else if (i == 1) return "DWELLING";
        else return "EXITING";
    }

    private void startGeofencing() {
        //Set up geofence
        GeofencingMethods gM = new GeofencingMethods(c, prefs);
        RealmResults<ServerGeofence> geofenceList = realm.where(ServerGeofence.class).findAll();
        for (ServerGeofence g : geofenceList) {
            Geofence geofence = gM.createGeofence(g.getGeof_name(), g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad() * 1000);
            gM.addToGeofencingRequest(geofence);
        }

        //Build googleApiClient and connect to service
        gM.buildGoogleApiClient();
    }
}
