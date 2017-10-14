package com.oohana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
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

import java.util.Calendar;
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



    @Override
    public void onReceive(Context context, Intent intent) {
        this.c = context;
        prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        DataMethods dm  = new DataMethods(c);

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                context.registerReceiver(this, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

            }
        }

        if ("android.location.PROVIDERS_CHANGED".equals(intent.getAction())) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                Realm realm = Realm.getDefaultInstance();
                if (realm.where(ServerGeofence.class).count() <= 0) {
                    realm.close();
                    dm.getData();
                } else {
                    realm.close();
                    dm.startGeofencing();
                }
            } else {
                // go to settings and do it!!
            }
        }

//        if(Constants.ACTION_GEOFENCE_RECEIVED.equals(intent.getAction())) {
//            new NotificationMaker(context).geofenceEvent(intent);
//        }
//        broadcastIntent
//                .setAction(GeofenceUtils.ACTION_GEOFENCE_TRANSITION)
//                .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
//                .putExtra(GeofenceUtils.EXTRA_GEOFENCE_ID, geofenceIds)
//                .putExtra(GeofenceUtils.EXTRA_GEOFENCE_TRANSITION_TYPE,
//                        transitionType);
//
//        LocalBroadcastManager.getInstance(c).sendBroadcast(broadcastIntent);

        // getData();
        if (Constants.ACTION_SYNC.equals(intent.getAction())) {
            //Toast.makeText(c, "Syncing with Server . . .", Toast.LENGTH_LONG).show();
            dm.getData2();
            dm.syncData();
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent myIntent = new Intent(context, GeofenceReceiver.class);
            myIntent.setAction(Constants.ACTION_SYNC);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);

            }
        }

        //sync outside
        if (Constants.ACTION_OUTSIDE_SYNC.equals(intent.getAction())) {
            System.out.println("syncing outside . . .");

            Realm realm = Realm.getDefaultInstance();
            //log outside
            if(realm.where(TriggeredGeofence.class).count()<1000) {
                    realm.beginTransaction();
                    TriggeredGeofence tg = new TriggeredGeofence();
                    tg.setGeof_id(-1);
                    tg.setStatus(100);
                    tg.setTimestamp(Calendar.getInstance().getTime());
                    realm.insert(tg);
                    realm.commitTransaction();
                    System.out.println("LOG COUNT:" + realm.where(TriggeredGeofence.class).count());
            } else{
                Toast.makeText(c,
                        "Please connect to the internet to sync with server. Logs will no longer be recorded.",
                        Toast.LENGTH_LONG).show();
            }
            realm.close();
            AlarmManager alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent myIntent = new Intent(c, GeofenceReceiver.class);
            myIntent.setAction(Constants.ACTION_OUTSIDE_SYNC);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(c,
                    Constants.OUTSIDE_INTENT_ID, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                System.out.println("setting outside sync . . .");
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent);
            } else{
                System.out.println("setting outside sync . . .");
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent);

            }
        }

    }




}
