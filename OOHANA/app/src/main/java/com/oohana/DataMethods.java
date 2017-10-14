package com.oohana;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.internal.Util;

/**
 * Created by elysi on 6/5/2017.
 */

public class DataMethods {
    private Context c;

    public DataMethods(Context c){
        this.c = c;
    }

    public void getData() {
        final Realm realm = Realm.getDefaultInstance();
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

                                System.out.println("Successfully gotten data from server. Count: " +
                                        realm.where(ServerGeofence.class).count());
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

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }
    public void syncData() {
//        final TelephonyManager tm = (TelephonyManager) c.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
//        final String tmDevice, tmSerial, androidId;
//        tmDevice = "" + tm.getDeviceId();
//        tmSerial = "" + tm.getSimSerialNumber();
//        androidId = "" + android.provider.Settings.Secure.getString(c.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
//
//        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
//        String deviceId = deviceUuid.toString();

//        WifiManager wifiManager = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String deviceId = "";
        String p1 = Utils.getMACAddress("wlan0");
        String p2 = Utils.getMACAddress("eth0");
        if(p1.equals("02:00:00:00:00:00") || p1.isEmpty()) deviceId = p2;
        else deviceId = p1;
        System.out.println("MAC Address: "+ deviceId);

        Map<String, String> params = new HashMap<String, String>();
        params.put("deviceID", deviceId);
        final Realm realm = Realm.getDefaultInstance();
        RealmResults<TriggeredGeofence> logs = realm.where(TriggeredGeofence.class).findAll();
        for (final TriggeredGeofence t : logs) {
            params.put("geof_id", Integer.toString(t.getGeof_id()));
            params.put("status", translateStatus(t.getStatus()));
            params.put("timestamp", Constants.f.format(t.getTimestamp()));

            JsonObjectRequest syncRequest = new JsonObjectRequest(Request.Method.POST, Constants.syncLink, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            try {
                                System.out.println(response.getString("result"));
                                realm.beginTransaction();
                                t.deleteFromRealm();
                                realm.commitTransaction();
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

        System.out.println("Successfully syced data. ServerGeofence Count: " +
                realm.where(ServerGeofence.class).count() + " Log Count: " + realm.where(TriggeredGeofence.class).count());

    }

    public String translateStatus(int i) {
        if (i == 0) return "ENTERING";
        else if (i == 1) return "DWELLING";
        else if (i==1) return "EXITING";
        else return "OUTSIDE";
    }

    public void startGeofencing() {
        //Set up geofence
        System.out.println("Start geofencing");
        final Realm realm = Realm.getDefaultInstance();
        SharedPreferences prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        final GeofencingMethods gM = new GeofencingMethods(c, prefs);
        SmartLocation.with(c).location(new LocationGooglePlayServicesWithFallbackProvider(c))
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        gM.getNearest(location);
                        RealmResults<ServerGeofence> geofenceList = realm.where(ServerGeofence.class)
                                .findAllSorted("nearnesstToCurrLoc", Sort.ASCENDING);
                        if(geofenceList.size() > 97){
                            geofenceList.subList(0, 97);
                        }

                        System.out.println("Geofence list Count: " + geofenceList.size());
                        for (ServerGeofence g : geofenceList) {
                            Geofence geofence = gM.createGeofence(g.getGeof_name(), g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad() * 1000);
                            gM.addToGeofencingRequest(geofence);
                        }
                        //Build googleApiClient and connect to service
                        gM.buildGoogleApiClient();
                    }
                });

    }

    public void getData2() {
        final Realm realm = Realm.getDefaultInstance();
        JsonObjectRequest geofenceList = new JsonObjectRequest
                (Request.Method.GET, Constants.geofenceListLink, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String success = response.getString("result");
                            boolean isNew = false;
                            if (success.equals("success")) {
                                final JSONArray geofences = response.getJSONArray("geofence");
                                for (int i = 0; i < geofences.length(); i++) {
                                    JSONObject g = geofences.getJSONObject(i);
                                    if (realm.where(ServerGeofence.class).equalTo("geof_id", g.getInt("geof_id")).count() <= 0) {
                                        isNew = true;
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


                                if(isNew){
                                    startGeofencing();
                                }

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
}
