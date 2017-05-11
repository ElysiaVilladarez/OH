package com.oohana.asynctasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.compat.BuildConfig;
import android.view.Window;
import android.view.WindowManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.oohana.HomeActivity;
import com.oohana.RequestQueSingleton;
import com.oohana.helpers.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by elysi on 5/11/2017.
 */

public class CheckingStart extends AsyncTask<Void, Void, Integer> {
    private Activity act;
//    private static ProgressDialog progressDialog;


    public CheckingStart(Activity act) {
        this.act = act;
    }

    @Override
    protected void onPreExecute() {
//        progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
//        progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        progressDialog.setTitle("Getting geofences from server");
//        progressDialog.setMessage("Please make sure you have a stable internet connection");
//        progressDialog.setCancelable(true);
//        progressDialog.setCanceledOnTouchOutside(true);
//        progressDialog.show();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        final int currentVersionCode = BuildConfig.VERSION_CODE;
        SharedPreferences prefs = act.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        // Get current version code
        if (prefs == null || prefs.getInt(Constants.PREF_VERSION_CODE_KEY, Constants.DOESNT_EXIST) == Constants.DOESNT_EXIST) {
            // TODO This is a new install (or the user cleared the shared preferences)
            prefs.edit().putInt(Constants.PREF_VERSION_CODE_KEY, currentVersionCode).apply();
            //get all geofences from server

            return 2;

        } else if (currentVersionCode == prefs.getInt(Constants.PREF_VERSION_CODE_KEY, Constants.DOESNT_EXIST)) {
            //Normal run
            return -1;

        } else if (currentVersionCode > prefs.getInt(Constants.PREF_VERSION_CODE_KEY, Constants.DOESNT_EXIST)) {
            prefs.edit().putInt(Constants.PREF_VERSION_CODE_KEY, currentVersionCode).apply();
            return 1;

            // TODO This is an upgrade

        } else return -1;


    }

    @Override
    public void onPostExecute(Integer i) {
//        if (progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }

        SharedPreferences prefs = act.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(Constants.INSTALLATION_NUM, i).commit();
        Intent mainIntent = new Intent(act, HomeActivity.class);
        act.startActivity(mainIntent);
        act.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        act.finish();
    }
}