package com.pseudonymous.appmea.network;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David Smerkous on 9/28/16.
 *
 */

public class CommonResponse {
    int return_code = 0;
    String return_type = "OK";
    ValuePair value_response = null;
    ArrayList<ValuePair> value_multi_responses = new ArrayList<>();
    JSONObject parent = null;

    public ValuePair getPair() {
        if(value_response == null) {
            if(value_multi_responses.size() > 0)
                return value_multi_responses.get(0);
            else
                return null;
        }
        return value_response;
    }

    public ArrayList<ValuePair> getPairs() {
        return value_multi_responses;
    }
}
