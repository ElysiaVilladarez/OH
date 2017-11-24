package com.oohana;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        DataMethods dm  = new DataMethods(this);

        realm = Realm.getDefaultInstance();
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);

        //get views
        latText = (TextView) findViewById(R.id.latText);
        lngText = (TextView) findViewById(R.id.lngText);
        timestamp = (TextView) findViewById(R.id.dateUpdatedText);
        geofenceNum = (TextView) findViewById(R.id.geofenceNum);
        //logsList = (ListView) findViewById(R.id.logList);


        System.out.println("SUCCESS");
//        Get geofence from server
        if (realm.where(ServerGeofence.class).count() <= 0) {
            realm.close();
            dm.getData();
        } else {
            realm.close();
            dm.startGeofencing();
        }



        realm = Realm.getDefaultInstance();
        geofenceNum.setText(Long.toString(realm.where(ServerGeofence.class).count()));
        int logCount = (int) realm.where(TriggeredGeofence.class).count();
        System.out.println("LOG COUNT: " + logCount);
        realm.close();
        dm.syncData();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(this, GeofenceTriggeredService.class);
        myIntent.setAction(Constants.ACTION_SYNC);
        PendingIntent pendingIntent = PendingIntent.getService(this,
                0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            System.out.println("setting alarm . . .");
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);
        } else{
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.syncingTime, pendingIntent);

        }


        AlarmManager alarmManager2 = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent2 = new Intent(this, GeofenceTriggeredService.class);
        myIntent2.setAction(Constants.ACTION_OUTSIDE_SYNC);
        PendingIntent pendingIntent2 = PendingIntent.getService(this,
                Constants.OUTSIDE_INTENT_ID, myIntent2, PendingIntent.FLAG_CANCEL_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            System.out.println("setting outside sync . . .");
            alarmManager2.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent2);
        } else{
            System.out.println("setting outside sync . . .");
            alarmManager2.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+Constants.logOutsideTime, pendingIntent2);

        }
    }


}
