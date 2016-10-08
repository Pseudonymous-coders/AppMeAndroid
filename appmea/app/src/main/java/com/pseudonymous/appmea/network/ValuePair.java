package com.pseudonymous.appmea.network;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by David Smerkous on 9/28/16.
 *
 */

public class ValuePair {
    public ValuePair(int id_p) {
        this.id = id_p;
    }

    public ValuePair() {
        this.id = 0;
    }

    private DateTime value_stamp;
    private Object value = null;
    private int id = 0;

    public DateTime getTimeStamp() {
        return this.value_stamp;
    }

    public Object getValue() {
        return this.value;
    }

    public int getIdV() {
        return this.id;
    }

    public void setTimeStamp(DateTime timestamp) {
        this.value_stamp = timestamp;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setIdV(int id) {
        this.id = id;
    }

    public String toString() {
        String curVal;
        String curDate;
        try {
            curVal = String.valueOf(this.getValue());
            curDate = this.getTimeStamp().toString();
        } catch (Throwable ignored) {
            return "BAD PAIR";
        }
        return "VALUE: " + curVal + " TIME: " + curDate;
    }
}
