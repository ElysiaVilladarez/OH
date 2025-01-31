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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
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
                                realm.beginTransaction();
                                realm.delete(ServerGeofence.class);
                                realm.commitTransaction();
                                final JSONArray geofences = response.getJSONArray("geofence");
                                for (int i = 0; i < geofences.length(); i++) {
                                    JSONObject g = geofences.getJSONObject(i);
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

                                System.out.println("Successfully gotten data from server. Count: " +
                                        realm.where(ServerGeofence.class).count());
                                Home.geofenceNum.setText(Long.toString(realm.where(ServerGeofence.class).count()));
                                realm.close();
                                startGeofencing();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            realm.close();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        realm.close();
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
        ArrayList<GeofenceLogs> geofenceLogs = new ArrayList<>();
        for(TriggeredGeofence l: logs){
            geofenceLogs.add(new GeofenceLogs(l.getGeof_id(), l.getStatus(), l.getTimestamp()));
        }
        realm.close();
        for (final GeofenceLogs t : geofenceLogs) {
            params.put("geof_id", Integer.toString(t.getGeof_id()));
            params.put("status", translateStatus(t.getStatus()));
            params.put("timestamp", Constants.f.format(t.getTimestamp()));

            JsonObjectRequest syncRequest = new JsonObjectRequest(Request.Method.POST, Constants.syncLink, new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response

                            Realm r = Realm.getDefaultInstance();
                            try {
                                System.out.println(response.getString("result"));
                                r.beginTransaction();
                                r.where(TriggeredGeofence.class).equalTo("geof_id", t.getGeof_id()).findFirst().deleteFromRealm();
                                r.commitTransaction();
                                System.out.println("Successfully syced data");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } finally {
                                r.close();
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
        else if (i == 2) return "EXITING";
        else return "NORMAL";
    }

    public void startGeofencing() {
        //Set up geofence
        System.out.println("Start geofencing");
        SharedPreferences prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        final GeofencingMethods gM = new GeofencingMethods(c, prefs);

        SmartLocation.with(c).location(new LocationGooglePlayServicesWithFallbackProvider(c))
                .oneFix()
                .start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        System.out.println("Smart location");
                        gM.getNearest(location);

                        final Realm realm = Realm.getDefaultInstance();
                        RealmResults<ServerGeofence> geofenceList = realm.where(ServerGeofence.class)
                                .findAllSorted("nearnesstToCurrLoc", Sort.ASCENDING);
                        List<ServerGeofence> list;
                        if(geofenceList.size() > 97){
                            list = geofenceList.subList(0, 97);
                        }else{
                            list = geofenceList;
                        }
                        System.out.println("Geofence list Count: " + list.size());
                        for (ServerGeofence g : list) {
                            Geofence geofence = gM.createGeofence(g.getGeof_name(), g.getGeof_lat(), g.getGeof_long(), g.getGeof_rad() * 1000);
                            gM.addToGeofencingRequest(geofence);
                        }
                        //Build googleApiClient and connect to service
                        gM.buildGoogleApiClient();
                        realm.close();
                    }
                });

    }

    public void getData2() {
        JsonObjectRequest geofenceList = new JsonObjectRequest
                (Request.Method.GET, Constants.geofenceListLink, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        final Realm realm = Realm.getDefaultInstance();
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
                                    realm.close();
                                    startGeofencing();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }finally {
                            if(!realm.isClosed()) realm.close();
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
