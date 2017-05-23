package com.oohana;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by elysi on 5/19/2017.
 */

public class GeofenceTriggeredService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTriggeredService(String name) {
        super(name);
    }
    public GeofenceTriggeredService() {
        super(GeofenceTriggeredService.class.getSimpleName());
    }



    @Override
    protected void onHandleIntent(Intent intent) {

        new NotificationMaker(getApplicationContext()).geofenceEvent(intent);

    }
}
