package com.pseudonymous.appmea.network;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by David Smerkous on 9/28/16.
 *
 */

public class CommonNetwork {
    public static String device_id = "4a9225497188974913e3473724c0f6d4";
    public static String m2x_key = "144284b35d059ca80b5bcde4c1895f35";
    public static String test_stream = "graph_test";

    public static void init() {
        M2X.init(device_id, m2x_key);
    }

    private static DateTime getDateFromISO(String ISOString) {
        //DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC();
        try {
            return parser.parseDateTime(ISOString);
        } catch (Throwable e) {
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                return new DateTime(df.parse(ISOString));
            } catch (Throwable ignored) {
                Log.d("ERROR", "Failed parsing string into date");
                e.printStackTrace();
            }
        }
        return null;
    }


    public static void setMetaData(final String name, final JSONObject newDatam,
                                   final ResponseListener resp) {
        Thread metaupdate = new Thread(new Runnable() {
            @Override
            public void run() {
                Request req = M2X.setMetaValue(name, newDatam);
                CommonResponse c_resp = c_response(req);
                resp.on_complete(c_resp);
            }
        });
        metaupdate.setDaemon(true);
        metaupdate.setName("M2X profile update");
        metaupdate.start();
    }

    public static void getMetaData(final ResponseListener resp) {
        Thread metaupdate = new Thread(new Runnable() {
            @Override
            public void run() {
                Request req = M2X.getMetaValues();
                CommonResponse c_resp = c_response(req);
                resp.on_complete(c_resp);
            }
        });
        metaupdate.setDaemon(true);
        metaupdate.setName("M2X profile getter");
        metaupdate.start();
    }

    public static void setValue(final String key, final String set, final ResponseListener resp) {
        Thread sender = new Thread(new Runnable() {
            @Override
            public void run() {
                Request req = M2X.setDataValue(key, set);
                CommonResponse c_resp = c_response(req);
                resp.on_complete(c_resp);
            }
        });
        sender.setDaemon(true);
        sender.setName("M2X send value");
        sender.start();
    }

    private static CommonResponse c_response(Request req) {
        CommonResponse c_resp = new CommonResponse();
        c_resp.return_type = req.type_return;
        if(req.json_obj.has("value")) try {
            ValuePair pair = new ValuePair();
            pair.setIdV(0);
            try {
                pair.setTimeStamp(getDateFromISO(String.valueOf(req.json_obj.get("timestamp"))));
            } catch (JSONException a) {
                pair.setTimeStamp(null);
            }
            pair.setValue(req.json_obj.get("value"));
            c_resp.value_response = pair;
        } catch (JSONException e) {
            e.printStackTrace();
        } else if(req.json_obj.has("values")) try {
            JSONArray multi_val = req.json_obj.getJSONArray("values");

            ArrayList<ValuePair> arr_l = new ArrayList<>();

            for(int ind = 0; ind < multi_val.length(); ind++) {
                JSONObject current = multi_val.getJSONObject(ind);

                ValuePair pair = new ValuePair();
                pair.setIdV(ind);
                try {
                    pair.setTimeStamp(getDateFromISO(String.valueOf(current.get("timestamp"))));
                } catch (JSONException a) {
                    pair.setTimeStamp(null);
                }
                pair.setValue(current.get("value"));

                arr_l.add(pair);
            }
            c_resp.value_multi_responses = arr_l;
        } catch (JSONException e) {
            Log.d("ERROR", "Error turning JSON values into objects");
            e.printStackTrace();
        } else c_resp.value_response = null;
        c_resp.parent = req.json_obj;
        c_resp.return_code = req.code_req;
        return c_resp;
    }

    public static void getValue(final String key, final ResponseListener resp) {
        Thread getter = new Thread(new Runnable() {
            @Override
            public void run() {
                Request req = M2X.getDataValue(key);
                CommonResponse c_resp = c_response(req);
                resp.on_complete(c_resp);
            }
        });
        getter.setDaemon(true);
        getter.setName("M2X get value");
        getter.start();
    }

    public static void getValues(final String key, final ResponseListener resp) {
        Thread getter = new Thread(new Runnable() {
            @Override
            public void run() {
                Request req = M2X.getDataValues(key);
                CommonResponse c_resp = c_response(req);
                resp.on_complete(c_resp);
            }
        });
        getter.setDaemon(true);
        getter.setName("M2X get value");
        getter.start();
    }

    public static void test() {
        ResponseListener resp = new ResponseListener() {

            @Override
            public void on_complete(CommonResponse req) {
                int a = 0;
                for(ValuePair pairs : req.getPairs()) {
                    Log.d(a + " RESPONSE", pairs.toString() + "  ");
                    //Log.d(a + " CODE", String.valueOf(req.return_code) + "  ");
                    //Log.d(a + " TYPE", String.valueOf(
                    //        req.return_type) + "  ");
                    a += 1;
                }
            }

            @Override
            public void on_fail(CommonResponse req) {

            }
        };
        getValues(test_stream, resp);
    }
}
