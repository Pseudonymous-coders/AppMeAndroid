package tk.pseudonymous.slumberhub.accessory;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;

/**
 * Created by David Smerkous on 12/4/16.
 * Project: SlumberHub
 */

public class InternalWireless {
    private static WifiConfiguration wifiConfiguration;
    private static WifiManager wifiManager;


    public static void init(Context context) {
        wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public static void connect(String ssid, String password) {
        ssid = "\"" + ssid + "\"";
        if(password != null) password = "\"" + password + "\"";

        wifiConfiguration.SSID = ssid;
        if(password != null) wifiConfiguration.preSharedKey = password;

        final String finalPassword = password;
        final String finalSsid = ssid;
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogData("CONNECTING TO WIFI NETWORK: " + finalSsid);
                LogData("NETWORK PASSWORD: " + ((finalPassword == null) ? "NONE" :
                                                            finalPassword));
                int netId = wifiManager.addNetwork(wifiConfiguration);
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();

                LogData("CONNECTED TO " + finalSsid);
            }
        }).start();
    }

}
