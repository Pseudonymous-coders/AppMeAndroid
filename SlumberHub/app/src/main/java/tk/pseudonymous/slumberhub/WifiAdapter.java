package tk.pseudonymous.slumberhub;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by David Smerkous on 11/3/16.
 * Project: SlumberHub
 */

class WifiView {
    String ssid;
    private int rssi = 0;
    Drawable imageDisplay = null;
    boolean secure = false, connectedTo = false;
    String securityType = "None";
    private final static String secureTypes = ""; //No security

    WifiView(String ssid, int rssi, Drawable imageDisplay, boolean secure) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.imageDisplay = imageDisplay;
        this.secure = secure;
    }

    public static ArrayList<WifiView> getAps(Context context, JSONArray allAp) {
        ArrayList<WifiView> wifiAps = new ArrayList<>();

        for(int ind = 0; ind < allAp.length(); ind++) {
            try {
                JSONObject apParse = (JSONObject) allAp.get(ind);
                wifiAps.add(new WifiView(context, apParse));
            } catch (JSONException ignored) {
                Log.d(MainActivity.appName, "Failed loading json");
            }
        }
        return wifiAps;
    }

    private Drawable getDrawable(Context context, int id) {
        return ContextCompat.getDrawable(context, id);
    }

    private WifiView(Context context, JSONObject toparse) {
        try {
            this.ssid = toparse.getString("ssid");
        } catch (JSONException ignored) {}

        try {
            this.rssi = 2 * (toparse.getInt("signal_level") + 100); //Turn rssi to quality percentage
        } catch (JSONException ignored) {}

        try {
            String secureType = toparse.getString("security");
            this.secure = !(secureType == null || secureType.equals(secureTypes));
            this.securityType = this.secure ? secureType : "None";
        } catch (JSONException ignored) {}

        try {
            if(this.rssi <= 35) {
                if(this.secure) {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_low_locked);
                } else {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_low_unlocked);
                }
            } else if(this.rssi > 35 && this.rssi <= 55) {
                if(this.secure) {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_low_medium_locked);
                } else {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_low_medium_unlocked);
                }
            } else if(this.rssi > 55 && this.rssi <= 80) {
                if(this.secure) {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_medium_locked);
                } else {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_medium_unlocked);
                }
            } else {
                if(this.secure) {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_high_locked);
                } else {
                    imageDisplay = getDrawable(context, R.drawable.ic_wifi_high_unlocked);
                }
            }
        } catch (Throwable ignored) {}
    }
}


public class WifiAdapter extends ArrayAdapter<WifiView> {
    public WifiAdapter(Context context, ArrayList<WifiView> aps) {
        super(context, 0, aps);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        WifiView ap = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_data_wifi_view
                    , parent, false);
        } else {
            MainActivity.LogData("The listview is null");
        }

        if(ap == null) {
            Log.d(MainActivity.appName, "Failed loading wifi ap view");
            return convertView;
        }

        TextView ssidName = (TextView) convertView.findViewById(R.id.ssid_name);
        ImageView rssiImage = (ImageView) convertView.findViewById(R.id.rssi_image);

        ssidName.setText(ap.ssid);
        rssiImage.setImageDrawable(ap.imageDisplay);

        if(ap.connectedTo) {
            MainActivity.LogData("Already connected to " + ap.ssid);
            convertView.setBackgroundResource(R.color.highlightWifi);
        }

        return convertView;
    }
}