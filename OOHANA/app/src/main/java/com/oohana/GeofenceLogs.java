package com.oohana;

import java.util.Date;

/**
 * Created by SaperiumDev on 11/24/2017.
 */

public class GeofenceLogs {private int geof_id;
    private int status; // 0 - Entering, 1 - Dwelling, 2 - Exiting
    private Date timestamp;

    public GeofenceLogs(int geof_id, int status, Date timestamp) {
        this.geof_id = geof_id;
        this.status = status;
        this.timestamp = timestamp;
    }

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
