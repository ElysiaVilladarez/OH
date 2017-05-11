package com.oohana;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.oohana.asynctasks.CheckingStart;
import com.oohana.helpers.Constants;
import com.oohana.oohana.R;

import java.lang.ref.WeakReference;

import io.realm.Realm;

public class SplashScreen extends AppCompatActivity {
    private static class StartMainActivityRunnable implements Runnable {
        private WeakReference mActivity;
        private View view;

        /**
         * Duration of wait
         **/


        private StartMainActivityRunnable(Activity activity, View view) {
            mActivity = new WeakReference(activity);
            this.view = view;
        }

        @Override
        public void run() {
            // 3. Check that the reference is valid and execute the code
            if (mActivity.get() != null) {

                Activity activity = (Activity) mActivity.get();
                new CheckingStart(activity).execute();


            }
        }
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        Realm.init(getApplicationContext());

        View view = findViewById(R.id.activity_splash_screen);
        mHandler.postDelayed(new StartMainActivityRunnable(this, view), Constants.LOGO_DISPLAY_LENGTH);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);

        mHandler = null;
    }

}


