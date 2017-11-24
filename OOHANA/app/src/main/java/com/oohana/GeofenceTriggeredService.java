package com.oohana;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.Geofence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by elysi on 5/19/2017.
 */

public class GeofenceTriggeredService extends IntentService {

    private Realm realm;
    private SharedPreferences prefs;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTriggeredService(String name) {
        super(name);
    }
    public GeofenceTriggeredService(){
        super("GeofenceTriggeredService");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        Realm.init(this);
        DataMethods dm  = new DataMethods(this);
        if (Constants.ACTION_SYNC.equals(intent.getAction())) {
            if(isInternetAvailable(this)) {
                System.out.println("Internet is available!");
                dm.getData2();
                dm.syncData();
            } else{
                System.out.println("Internet is not available.");
            }
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent myIntent = new Intent(this, GeofenceTriggeredService.class);
            myIntent.setAction(Constants.ACTION_SYNC);
            PendingIntent pendingIntent = PendingIntent.getService(this,
                    0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);

            }
        } else if (Constants.ACTION_OUTSIDE_SYNC.equals(intent.getAction())) {
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
                Toast.makeText(this,
                        "Please connect to the internet to sync with server. Logs will no longer be recorded.",
                        Toast.LENGTH_LONG).show();
            }
            realm.close();
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent myIntent = new Intent(this, GeofenceTriggeredService.class);
            myIntent.setAction(Constants.ACTION_OUTSIDE_SYNC);
            PendingIntent pendingIntent = PendingIntent.getService(this,
                    Constants.OUTSIDE_INTENT_ID, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                System.out.println("setting outside sync . . .");
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent);
            } else{
                System.out.println("setting outside sync . . .");
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent);

            }
        }
        new NotificationMaker(this).geofenceEvent(intent);
    }

    public boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());

    }
}
