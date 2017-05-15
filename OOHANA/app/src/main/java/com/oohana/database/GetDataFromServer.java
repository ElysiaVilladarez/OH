package com.oohana.database;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.oohana.helpers.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;

/**
 * Created by elysi on 5/15/2017.
 */

public class GetDataFromServer {

    public static void getData(Context c){
        final Realm realm = Realm.getDefaultInstance();
        final SharedPreferences prefs = c.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getInt(Constants.INSTALLATION_NUM, -1) == 2 || realm.where(ServerGeofence.class).count() <= 0) {
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
                                        }
                                    });


                                    prefs.edit().putInt(Constants.GEOFENCE_NUM, 0).commit();

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
}
