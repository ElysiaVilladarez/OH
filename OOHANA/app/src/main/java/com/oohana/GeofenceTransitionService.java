package com.oohana;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by elysi on 5/10/2017.
 */

public class GeofenceTransitionService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionService(String name) {
        super(name);
    }
    public GeofenceTransitionService() {
        super(GeofenceTransitionService.class.getSimpleName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        System.out.println("CHECK: Service activated");
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Toast.makeText(getApplicationContext(), "ERROR: " + getErrorString(geofencingEvent.getErrorCode()), Toast.LENGTH_LONG).show();
            return;
        }

        int geoFenceTransition = geofencingEvent.getGeofenceTransition();

        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geoFenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
                || geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geoFenceTransition, triggeringGeofences);

            sendNotification(geofenceTransitionDetails);
        }
    }

    private String getGeofenceTransitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences){
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for(Geofence geofence: triggeringGeofences){
            triggeringGeofencesList.add(geofence.getRequestId());
        }
        String status = null;
        if(geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
            status = "Entering ";
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
            status = "Dwelling ";
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
            status = "Exiting ";
        }

        return status + TextUtils.join(", ", triggeringGeofencesList);
    }

    private void sendNotification(String msg){

        System.out.println("CHECK: Sending notification");
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        int id = (int)System.currentTimeMillis();
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, createNotification(msg, notificationPendingIntent));
    }

    private Notification createNotification(String msg, PendingIntent notificationPendingIntent){

        System.out.println("CHECK: creating notification");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setSmallIcon(com.google.android.gms.R.drawable.common_full_open_on_phone)
                .setColor(Color.RED)
                .setContentTitle(msg)
                .setContentText("geofence Spotted")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true);
        return notificationBuilder.build();


    }

    private static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }

}
