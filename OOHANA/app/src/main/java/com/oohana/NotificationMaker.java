package com.oohana;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;

/**
 * Created by elysi on 5/20/2017.
 */

public class NotificationMaker {
    /**
     * Created by elysi on 5/15/2017.
     */
    private Context c;
    private int s;

    public NotificationMaker(Context c){
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

          //  sendNotification(geofenceTransitionDetails);

        }
    }

    private String getGeofenceTransitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences) {
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesList.add(geofence.getRequestId());
        }
        String status = null;
        s = 0;
        if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            status = "Entering ";
            s = 0;
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            status = "Dwelling ";
            s = 1;
        } else if (geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            status = "Exiting ";
            s = 2;
        }
        Realm realm = Realm.getDefaultInstance();
        if(realm.where(TriggeredGeofence.class).count()<1000) {
            for (final Geofence g : triggeringGeofences) {
                realm.beginTransaction();
                TriggeredGeofence tg = new TriggeredGeofence();
                tg.setGeof_id(realm.where(ServerGeofence.class).equalTo("geof_name", g.getRequestId()).findFirst().getGeof_id());
                tg.setStatus(s);
                tg.setTimestamp(Calendar.getInstance().getTime());
                realm.insert(tg);
                realm.commitTransaction();
                System.out.println("LOG COUNT:" + realm.where(TriggeredGeofence.class).count());
            }
        } else{
            Toast.makeText(c,
                    "Please connect to the internet to sync with server. Logs will no longer be recorded.",
                    Toast.LENGTH_LONG).show();
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

    private Notification createNotification(String msg, PendingIntent notificationPendingIntent) {

        System.out.println("Creating notification");
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c);
        notificationBuilder
                .setSmallIcon(com.google.android.gms.R.drawable.common_full_open_on_phone)
                .setColor(ContextCompat.getColor(c, R.color.colorPrimary))
                .setContentTitle(msg)
                .setContentText("Geofence Spotted")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true);
        return notificationBuilder.build();


    }

    private void sendNotification(String msg) {

        System.out.println("Sending notification");
        Intent notificationIntent = new Intent(c, Home.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(c);
        stackBuilder.addParentStack(Home.class);
        stackBuilder.addNextIntent(notificationIntent);

        int id = (int) System.currentTimeMillis();
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, createNotification(msg, notificationPendingIntent));
    }


}
