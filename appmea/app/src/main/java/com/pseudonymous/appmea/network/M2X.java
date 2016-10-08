package com.pseudonymous.appmea.network;

import android.util.Log;
import android.util.SparseArray;

import com.github.kevinsawicki.http.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by David Smerkous on 9/28/16.
 *
 */

class Request {
    boolean fail = false;
    int code_req = 0;
    long cont_length = 0;
    String body_resp = null;
    String type_return = "OK";
    JSONObject json_obj;
}

public class M2X {
    private static String base_url = "https://api-m2x.att.com/v2";
    private static String device_base = "/devices/";
    private static String stream_base = "/streams";
    private static String device_id = "";
    public static String m2x_key = "";
    private static Map<Integer, String> ret_types = new HashMap<>();

    private static String[][] header_map = new String[][] {
            {"X-M2X-KEY", ""},
            {"Content-Type", "application/json"},
            {"Accept", "application/json"}
    };

    private static String getISOtimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());

        //return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(new Date());
    }

    public static void init(String device_uri, String m2x_key_l) {
        //Set the response types and return codes
        ret_types.put(200, "Okay");
        ret_types.put(201, "Created");
        ret_types.put(202, "Accepted");
        ret_types.put(204, "No Content");
        ret_types.put(400, "Bad request");
        ret_types.put(401, "Unauthorized");
        ret_types.put(403, "Forbidden");
        ret_types.put(404, "Not Found");
        ret_types.put(415, "Method Not Allowed");
        ret_types.put(422, "Unsupported Media Type");
        ret_types.put(401, "Unprocessable Entity");
        ret_types.put(429, "Too Many Requests");
        for(int ind = 500; ind < 505; ind++) ret_types.put(ind, "Server error");

        //Set the header m2x authentication key
        m2x_key = m2x_key_l;
        header_map[0][1] = m2x_key;
        device_id = device_uri;
    }

    private static String stream_build(String stream) {
        return base_url + device_base + device_id +
                stream_base + (stream.isEmpty() ? "": ("/" + stream));
    }

    public static Request getDataValue(String stream) {
        String url = stream_build(stream);
        return get_request(url);
    }

    public static Request getDataValues(String stream) {
        String url = stream_build(stream) + "/values.json";
        return get_request(url);
    }

    public static Request setDataValue(String stream, String toset) {
        String url = stream_build(stream) + "/value";
        Request request = new Request();
        try {
            JSONObject tosend = new JSONObject();
            tosend.put("timestamp", getISOtimestamp());
            tosend.put("value", toset);
            request = put_request(url, tosend.toString());
        } catch (JSONException e) {
            Log.d("ERROR", "Failed to send");
        }
        return request;
    }

    private static HttpRequest set_heads(HttpRequest req) {
        for(String[] map : header_map) req.header(map[0], map[1]);
        return req;
    }


    private static Request post_request(String url, String body) {
        return p_request(true, url, body);
    }

    private static Request put_request(String url, String body) {
        return p_request(false, url, body);
    }

    //Use to create a new entity on m2x
    //True = post
    //False = put
    private static Request p_request(boolean type, String url, String body) {
        HttpRequest post;
        if(type) post = HttpRequest.post(url);
        else post = HttpRequest.put(url);
        post = set_heads(post.followRedirects(true));
        post.send(body);
        Request request = new Request();
        if(post.badRequest()) {
            request.fail = true;
            return request;
        }
        request.code_req = post.code();
        request.type_return = ret_types.get(request.code_req);

        return request;
    }

    //Used to get data from http/m2x
    private static Request get_request(String url) {
        HttpRequest get = HttpRequest.get(url).followRedirects(true);
        get = set_heads(get);
        Request request = new Request();
        if(get.badRequest()) {
            request.fail = true;
            return request;
        }
        request.body_resp = get.body();
        try {
            request.json_obj = new JSONObject(request.body_resp);
        } catch (JSONException ignored) {}
        request.code_req = get.code();
        request.cont_length = get.contentLength();
        request.type_return = ret_types.get(request.code_req);

        return request;
    }
}
