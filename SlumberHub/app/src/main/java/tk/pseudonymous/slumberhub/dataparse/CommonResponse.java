package tk.pseudonymous.slumberhub.dataparse;

import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

public class CommonResponse {
    String
            valueName = "sleepScore",
            chartName = valueName;
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


    public static DateTime getDate(long POSIXlong) {
        Long longTime = POSIXlong * 1000L;
        return new DateTime(longTime);
    }

    public static String setDate(DateTime posixDate) {
        return String.valueOf(posixDate.getMillis() / 1000L);
    }

    public static long dateEpoch(DateTime posixDate) {
        return posixDate.getMillis() / 1000L;
    }
}