package com.oohana.helpers;

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
import com.oohana.HomeActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by elysi on 5/15/2017.
 */

public class NotificationsHandler {

    private Context c;

    public NotificationsHandler(Context c){
        this.c = c;
    }

    public void geofenceEvent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) {
            Toast.makeText(c, "ERROR: " + getErrorString(geofencingEvent.getErrorCode()), Toast.LENGTH_LONG).show();
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

    private String getErrorString(int errorCode) {
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

    private Notification createNotification(String msg, PendingIntent notificationPendingIntent){

        System.out.println("CHECK: creating notification");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
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

    private void sendNotification(String msg){

        System.out.println("CHECK: Sending notification");
        Intent notificationIntent = new Intent(c, HomeActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        int id = (int)System.currentTimeMillis();
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, createNotification(msg, notificationPendingIntent));
    }



}
