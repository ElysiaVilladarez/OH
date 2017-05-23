package com.oohana;

/**
 * Created by elysi on 5/18/2017.
 */

public class Constants {

    public final static int LOGO_DISPLAY_LENGTH = 1500;
    public final static int REQ_PERMISSION = 100;

    public final static int LOITERING_DELAY = 60000; //should be 10 mins but for the sake of testing, 1 min

    public final static int PENDING_INTENT_ID = 210;

    public final static String geofenceListLink = "http://oohana.technotrekinc.com/get_geofence_list.php";
    public final static String syncLink = "";

    public final static String PREFS_NAME = "MY_PREFS";
    public final static String GEOFENCE_NUM = "GEOF_NUM";

    public final static String ACTION_GEOFENCE_RECEIVED = "com.oohana.ACTION_GEOFENCE_RECEIVED";
}
