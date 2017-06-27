package com.oohana;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
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

        new NotificationMaker(this).geofenceEvent(intent);
    }
}
