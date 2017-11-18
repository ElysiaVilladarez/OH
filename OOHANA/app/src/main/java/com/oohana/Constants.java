package com.oohana;

import java.text.SimpleDateFormat;

/**
 * Created by elysi on 5/18/2017.
 */

public class Constants {

    public final static int LOGO_DISPLAY_LENGTH = 1500;
    public final static int REQ_PERMISSION = 100;
    public final static int REQUEST_READ_PHONE_STATE = 101;

    public final static int LOITERING_DELAY = 600000; //should be 10 mins

    public final static int PENDING_INTENT_ID = 210;
    public final static int OUTSIDE_INTENT_ID = 220;

    public final static String geofenceListLink = "http://oohana.technotrekinc.com/get_geofence_list.php";
    public final static String syncLink = "http://oohana.technotrekinc.com/insert_to_server.php";

    public final static String PREFS_NAME = "MY_PREFS";
    public final static String GEOFENCE_NUM = "GEOF_NUM";

    public final static String ACTION_GEOFENCE_RECEIVED = "com.oohana.ACTION_GEOFENCE_RECEIVED";
    public final static String ACTION_SYNC = "com.oohana.ACTION_SYNC";
    public final static String ACTION_OUTSIDE_SYNC = "com.oohana.OUTSIDE_SYNC";

    public final static int syncingTime = 1000 * 60 * 1; //should be 5 mins
    public final static int logOutsideTime = 1000 * 60 * 5; //should be 20 mins
    public final static int GEOFENCE_RADIUS_DEFAULT_VALUE = 5;
    public final static int LOCATION_TRACKING_INTERVAL = 60000; //1 min

    public final static SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

}
