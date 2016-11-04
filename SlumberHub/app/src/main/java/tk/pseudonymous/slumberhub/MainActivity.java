package tk.pseudonymous.slumberhub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("WrongConstant")
public class MainActivity extends Activity {

    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    final static boolean logData = true;

    boolean currentFocus, isPaused;
    Handler collapseNotificationHandler;

    static volatile boolean responseThread = true;

    static String appName = "Slumber";

    Thread readThread;
    Button refreshWifi;

    ParcelFileDescriptor parcelFileDescriptor;
    UsbManager usbManager;
    UsbAccessory usbAccessory;
    FileInputStream inputS;
    FileOutputStream outputS;

    AlertDialog dialogPassword, dialogConnect;

    ListView listView;

    final static short lengthSize = 28;
    long timeoutMillis = 10000;
    boolean isConnected = false;

    public void writeString(final String towrite) {
        Thread toWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte buffer[] = towrite.getBytes();
                byte initbuff[] = new byte[lengthSize];
                int sizeBuffer = buffer.length;

                String sendSize = String.valueOf(sizeBuffer);

                if(sendSize.length() > 5) {
                    int newSize = Integer.valueOf(sendSize.substring(0, 5));
                    buffer = towrite.substring(0, newSize).getBytes();
                } else {
                    initbuff = sendSize.getBytes();
                }


                if(outputS == null || inputS == null) {
                    LogData("Failed sending data! NULL OBJECT");
                    return;

                }

                try {
                    outputS.write(initbuff);
                } catch (IOException e) {
                    //mMessage = "Failed to send initial size";
                    e.printStackTrace();
                }

                try {
                    outputS.write(buffer);
                    //mMessage = "Sent: " + Arrays.toString(buffer);

                    String msg = new String(buffer);
                    //mMessage2 += msg + System.getProperty("line.separator");
                } catch (IOException e) {
                    //mMessage = "Failed to send buffer";
                    e.printStackTrace();
                }

                //mText.post(mUpdateUI);
            }
        });
        toWriteThread.setDaemon(true);
        toWriteThread.setName("Write accessory thread");
        toWriteThread.start();
    }

    public String readString() {
        byte bufferinit[] = new byte[lengthSize];
        int readSize = 0;
        boolean passed = true;

        String toRet = null;

        try {
            //noinspection ResultOfMethodCallIgnored
            inputS.read(bufferinit);
            String recv = new String(bufferinit).replaceAll("[^0-9]|[�]", "");
            Log.d("APP", recv);
            readSize = Integer.valueOf(recv);
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            //mMessage += "Read size error";
            passed = false;
        }

        if (passed) {
            byte[] buffer = new byte[readSize];
            try {
                //mMessage = ">>> ";
                //noinspection ResultOfMethodCallIgnored
                inputS.read(buffer);
                toRet = new String(buffer).replaceAll("[�]", "");
                //mMessage += toRet;
            } catch (IOException e) {
                e.printStackTrace();
                //mMessage += "Read error";
            }

            //mMessage += System.getProperty("line.separator");
            //mText.post(mUpdateUI);
        }

        return toRet;
    }


    /** Called when the user clicks the Decrease or Increase button */
    public void sendMessage(View view) {

        char direction = 0;

        JSONObject ab = new JSONObject();
        try {
            ab.put("exec", "babe");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        writeString(ab.toString());

    }

    private void getWifiList(JSONObject data) {
        LogData("GOT GET WIFI");
        ArrayList<WifiView> apList;
        try {
            apList = WifiView.getAps(getApplicationContext(), data.getJSONArray("APs"));
        } catch (JSONException err) {
            LogData("Failed getting aps");
            err.printStackTrace();
            return;
        }

        final ArrayList<WifiView> newApList = new ArrayList<>();

        for(WifiView wifiView : apList) {
            boolean isThere = false;
            for(WifiView wifiViewNew : newApList) {
                if(wifiViewNew.ssid.equals(wifiView.ssid) &&
                        wifiView.secure == wifiViewNew.secure) {
                    isThere = true;
                }
            }

            if(!isThere) {
                newApList.add(wifiView);
                Log.d("Adding new AP", wifiView.ssid);
            }
        }

        final WifiAdapter wifiAdapter = new WifiAdapter(getApplicationContext(), newApList);
        MainActivity.this.runOnUiThread(new Runnable() {

            private void showConnecting(JSONObject connection, String ssid) {
                writeString(connection.toString());

                final ProgressDialog ringProgressDialog =
                        ProgressDialog.show(
                                MainActivity.this,
                                ssid,
                                "Connecting to " + ssid + "...", true);
                ringProgressDialog.setCancelable(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long start =
                                    SystemClock.currentThreadTimeMillis();

                            while ((SystemClock.currentThreadTimeMillis()
                                    - start < timeoutMillis)
                                    && !isConnected) {
                                Thread.sleep(20);
                            }
                            if(!isConnected) {
                                Toast.makeText(MainActivity.this,
                                        "Timed out connecting",
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception ignored) {}
                        ringProgressDialog.dismiss();
                    }
                }).start();
            }

            @Override
            public void run() {
                listView.setAdapter(wifiAdapter);
                listView.setVisibility(View.VISIBLE);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        isConnected = false;

                        final JSONObject connection = new JSONObject();
                        final WifiView toConnect = newApList.get(position);

                        try {
                            connection.put("exec", "connectWifi");

                            final JSONObject ssidPass = new JSONObject();
                            ssidPass.put("ssid", toConnect.ssid);

                            LogData("Connection is secure building");
                            AlertDialog.Builder builder =
                                    new AlertDialog.Builder(
                                            MainActivity.this);
                            LinearLayout linearL = new LinearLayout(MainActivity.this);

                            final TextView securityType = new TextView(MainActivity.this);
                            securityType.setText(R.string.connect_security);

                            final TextView securityMain = new TextView(MainActivity.this);
                            toConnect.securityType += "\n";
                            securityMain.setText(toConnect.securityType);
                            securityMain.setTextColor(Color.CYAN);
                            securityMain.setAllCaps(true);
                            securityMain.setTypeface(null, Typeface.BOLD);

                            final TextView passwordType = new TextView(MainActivity.this);
                            passwordType.setText(R.string.connect_password);

                            final EditText input = new EditText(MainActivity.this);
                            input.setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD);

                            final CheckBox showView = new CheckBox(MainActivity.this);
                            showView.setText(R.string.check_box_connect);
                            showView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    boolean checkedBox = ((CheckBox) v).isChecked();
                                    if(checkedBox) {
                                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                                    } else {
                                        input.setInputType(InputType.TYPE_CLASS_TEXT |
                                                InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                    }
                                }
                            });

                            linearL.setOrientation(LinearLayout.VERTICAL);
                            linearL.setGravity(Gravity.START);
                            linearL.setVisibility(View.VISIBLE);

                            linearL.addView(securityType);
                            linearL.addView(securityMain);
                            linearL.addView(passwordType);

                            if(!toConnect.secure){
                                String connecterString = "Are you sure you want to connect to "
                                        + toConnect.ssid + "?";
                                passwordType.setText(connecterString);
                            } else {
                                linearL.addView(input);
                                linearL.addView(showView);
                            }

                            builder.setView(linearL);
                            builder.setTitle(toConnect.ssid)
                                    .setPositiveButton("Connect",
                                            new DialogInterface.OnClickListener
                                                    () {
                                                @Override
                                                public void
                                                onClick(DialogInterface dialog, int id)
                                                {
                                                    try {
                                                        if(toConnect.secure) {
                                                            ssidPass.put("password",
                                                                    input.getText());
                                                        } else {
                                                            ssidPass.put("password", "");
                                                        }
                                                        connection.put("data",
                                                                ssidPass);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    showConnecting(connection, toConnect.ssid);
                                                }
                                            })
                                    .setNegativeButton("Cancel",
                                            new DialogInterface.OnClickListener
                                                    () {
                                                @Override
                                                public void
                                                onClick(DialogInterface dialog, int id)
                                                {
                                                    dialogPassword.dismiss();
                                                }
                                            });
                            dialogPassword = builder.create();
                            dialogPassword.show();
                        } catch (JSONException ignored) {
                            MainActivity.LogData("Failed compiling json");
                        }
                    }
                });
                listView.invalidate();
            }
        });
    }

    final Runnable onRecieve = new Runnable() {
        @Override
        public void run() {
            while(responseThread) {
                String received = readString();
                JSONObject result;

                try {
                    result = new JSONObject(received);
                } catch (JSONException error) {
                    LogData("Failed parsing json");
                    continue;
                }

                LogData("GOT JSON: " + result.toString());

                String responseType;
                JSONObject data;

                try {
                    responseType = result.getString("response");
                    data = result.getJSONObject("data");
                } catch (JSONException err) {
                    LogData("Malformed res" +
                            "ponse type");
                    err.printStackTrace();
                    continue;
                }

                try {
                    if(responseType.equals("getWifi")) {
                        getWifiList(data);
                    } else if(responseType.equals("connectWifi")) {
                        int connectionType = data.getInt("connected");


                        Looper.prepare();
                        if(connectionType == 1) {
                            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed to connect", Toast.LENGTH_LONG).show();

                        }

                        isConnected = true;

                    } else {
                        LogData("Didn't find a proper response for: " + responseType);
                    }
                } catch (Throwable ignored) {
                    LogData("FAILED PARSING JSON");
                    ignored.printStackTrace();
                }

                LogData("Finished reading from accessory");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        makeFullscreen();

        appName = getResources().getString(R.string.app_name);

        Intent intent = getIntent();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        refreshWifi = (Button) findViewById(R.id.refresh);

        refreshWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject towrite = new JSONObject();

                try {
                    towrite.put("exec", "getWifi");
                } catch (JSONException ignored) {}
                writeString(towrite.toString());
                LogData("PUSHED THIS TO JSON");
            }
        });

        listView = (ListView) findViewById(R.id.wifiView);

        JSONObject object = new JSONObject();

        try {
            object.put("APs", new JSONArray("[{ ssid: 'Smerkous',\n" +
                    "    mac: '62:F1:89:78:CF:19',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -50,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:A2:42',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -64,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DD:42',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -65.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:A2:22',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -66.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:88:04:C2',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -67.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DD:62',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -70,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DD:22',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -70.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:88:04:E2',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -72.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DC:A2',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -74,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:88:04:A2',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -75,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DC:E2',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -76.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:81:72:E2',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -80,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:81:75:A2',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -80.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:87:62',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -84,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:CE:62',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -86.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:A2:41',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -64,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DD:41',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -65.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DD:61',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -69,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DD:21',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -71.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:88:04:E1',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -72.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DC:A1',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -74,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:88:04:A1',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -75,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DC:E1',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -76.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:81:72:E1',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -80,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:87:61',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -83,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:CE:61',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -85.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:A2:62',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -70.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:86:DC:C2',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -70.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:87:22',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -85,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:81:72:A2',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -85.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:A2:61',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -67.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:86:DC:C1',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -70.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:81:72:A1',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -85,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:81:75:C2',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -75.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:1A:A2',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -82.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:88:04:C1',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -65.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:81:75:C1',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -75.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:1A:A1',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -83,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:81:72:C2',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -77.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:1A:C2',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -81.5,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:81:72:C1',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -77.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:81:75:A1',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -80.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:1A:C1',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -81.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:1A:E1',\n" +
                    "    channel: 2462,\n" +
                    "    signal_level: -81.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:CE:21',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -85.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:A2:21',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -72,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:87:42',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -80,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'n0W1r3s',\n" +
                    "    mac: '50:60:28:87:1A:42',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -85,\n" +
                    "    security: 'WPA2' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:87:41',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -80.5,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:1A:41',\n" +
                    "    channel: 2437,\n" +
                    "    signal_level: -85,\n" +
                    "    security: '' },\n" +
                    "  { ssid: 'PISDguest',\n" +
                    "    mac: '50:60:28:87:87:21',\n" +
                    "    channel: 2412,\n" +
                    "    signal_level: -84,\n" +
                    "    security: '' }]"));
        } catch (JSONException ignored) {
            LogData("Failed parsing");
            ignored.printStackTrace();
        }

        getWifiList(object);

        /*SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                // set item width
                openItem.setWidth(40);
                // set item title
                openItem.setTitle("Open");
                // set item title fontsize
                openItem.setTitleSize(18);
                // set item title font color
                openItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(openItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(40);
                // set a icon
                deleteItem.setIcon(android.R.drawable.ic_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };*/

        //listView.setMenuCreator(creator);
        /*
        listView.setCloseInterpolator(new BounceInterpolator());
        listView.setOpenInterpolator(new BounceInterpolator());
        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        // open
                        break;
                    case 1:
                        // delete
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });*/


        if (usbAccessory == null) {
            //mText.append("Not started by the accessory directly" +
            //        System.getProperty("line.separator"));
            return;
        }

        parcelFileDescriptor= usbManager.openAccessory(usbAccessory);
        if (parcelFileDescriptor != null) {
            FileDescriptor fd = parcelFileDescriptor.getFileDescriptor();
            inputS = new FileInputStream(fd);
            outputS= new FileOutputStream(fd);
        } else {
            LogData("Failed getting file descriptor");
        }

        readThread = new Thread(onRecieve);
        readThread.setDaemon(true);
        readThread.setName("Usb python read thread");
        readThread.start();
    }

    public static void LogData(String tolog) {
        if(logData) Log.d(appName, tolog);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        currentFocus = hasFocus;

        if (!hasFocus) {
            collapseNow();
        }
    }

    public void collapseNow() {
        if (collapseNotificationHandler == null) {
            collapseNotificationHandler = new Handler();
        }
        if (!currentFocus && !isPaused) {
            collapseNotificationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Object statusBarService = getSystemService("statusbar");
                    Class<?> statusBarManager = null;
                    try {
                        statusBarManager = Class.forName("android.app.StatusBarManager");
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    Method collapseStatusBar = null;
                    if(statusBarManager != null) {
                        try {
                            collapseStatusBar = statusBarManager.getMethod("collapsePanels");
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                    if(collapseStatusBar != null) {
                        collapseStatusBar.setAccessible(true);
                        try {
                            collapseStatusBar.invoke(statusBarService);
                        } catch (IllegalArgumentException | IllegalAccessException |
                                InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!currentFocus && !isPaused) {
                        collapseNotificationHandler.postDelayed(this, 0L);
                    }
                }
            }, 0L);
        }
    }

    private void makeFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(flags);
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            decorView.setSystemUiVisibility(flags);
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
    }
}
