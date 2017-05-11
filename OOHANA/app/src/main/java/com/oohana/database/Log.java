package com.oohana.database;

import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by elysi on 5/11/2017.
 */

public class Log extends RealmObject {
    private int geof_id;
    private int status; // 0 - Entering, 1 - Dwelling, 2 - Exiting
    private Date timestamp;

    public int getGeof_id() {
        return geof_id;
    }

    public void setGeof_id(int geof_id) {
        this.geof_id = geof_id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
