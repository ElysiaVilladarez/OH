package com.oohana.database;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by elysi on 5/11/2017.
 */

public class ServerGeofence extends RealmObject {
    @PrimaryKey
    private int geof_id;
    private String geof_name;
    private double geof_lat;
    private double geof_long;
    private float geof_rad; //must be in meters

    public int getGeof_id() {
        return geof_id;
    }

    public void setGeof_id(int geof_id) {
        this.geof_id = geof_id;
    }

    public String getGeof_name() {
        return geof_name;
    }

    public void setGeof_name(String geof_name) {
        this.geof_name = geof_name;
    }

    public double getGeof_lat() {
        return geof_lat;
    }

    public void setGeof_lat(double geof_lat) {
        this.geof_lat = geof_lat;
    }

    public double getGeof_long() {
        return geof_long;
    }

    public void setGeof_long(double geof_long) {
        this.geof_long = geof_long;
    }

    public float getGeof_rad() {
        return geof_rad;
    }

    public void setGeof_rad(float geof_rad) {
        this.geof_rad = geof_rad;
    }
}
