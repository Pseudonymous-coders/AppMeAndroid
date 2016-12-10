package tk.pseudonymous.slumberhub;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import tk.pseudonymous.slumberhub.accessory.InternalWireless;
import tk.pseudonymous.slumberhub.accessory.PackagedProcessor;
import tk.pseudonymous.slumberhub.accessory.SpeechRecognition;
import tk.pseudonymous.slumberhub.dataparse.NightData;
import tk.pseudonymous.slumberhub.dataparse.ValuePair;
import tk.pseudonymous.slumberhub.fragments.HomeFragment;
import tk.pseudonymous.slumberhub.fragments.LastNightFragment;
import tk.pseudonymous.slumberhub.fragments.LiveFragment;
import tk.pseudonymous.slumberhub.fragments.SettingsFragment;

@SuppressWarnings("WrongConstant")
public class MainActivity extends FragmentActivity implements TextToSpeech.OnInitListener {

    static final int PAGES = 4;

    private ViewPager vPager;
    private PagerAdapter pAdapter;


    final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    final static boolean logData = true;

    boolean currentFocus, isPaused;
    Handler collapseNotificationHandler;

    public static MainActivity wakeContext;

    static volatile boolean responseThread = true, looperStarted = true;

    static String appName = "Slumber";

    Thread readThread;

    static ParcelFileDescriptor parcelFileDescriptor;
    static UsbManager usbManager;
    static UsbAccessory usbAccessory;
    static FileInputStream inputS;
    static FileOutputStream outputS;

    static AlertDialog dialogPassword, dialogConnect;

    final static short lengthSize = 28;
    static long timeoutMillis = 10000;
    static boolean isConnected = false;


    public static PowerManager.WakeLock fullWakeLock, partialWakeLock;


    static DisplayMetrics metrics = null;

    public static PackagedProcessor packagedProcessor = new PackagedProcessor();


    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    public static AudioManager amanager;

    public static TextToSpeech tts;

    public static String
            bgColor = "#212121",
            barColor = "#33691e",
            rimColor = "#a5d6a7",
            barTextColor = "#FFFFFFFF",
            barUnitColor = "#FFCFD8DC",
            tryColor = "#FF33691E";

    public static int totalProcessing = 1, currentProcess = 0, progressMax = 100;

    int sleepScore = 0, accelX = 0 , accelY = 0, accelZ = 0,
            accelScore = 0, lightScore = 0, temp = 0, humidity = 0;

    float voltageBattery = 0;


    public static final int CHECK_TTS_DATA = 15;

    ArrayList<ValuePair> tempNightData = new ArrayList<>();

    public static int textColor = Color.WHITE;
    public static NightData lastNight;

    public static int mBindFlag;
    public static Messenger mServiceMessenger, mTTSServiceMessenger;


    public static BluetoothAdapter btAdapter;
    public static Set<BluetoothDevice> pairedDevices;
    public static BluetoothHeadset btHeadset;

    public static void writeString(final String towrite) {
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
                    e.printStackTrace();
                }

                try {
                    outputS.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        toWriteThread.setDaemon(true);
        toWriteThread.setName("WriteAccessoryThread");
        toWriteThread.start();
    }

    public static String readString() {
        byte bufferinit[] = new byte[lengthSize];
        int readSize = 0, curPacket = 0;
        boolean passed = true, is_large = false;

        String toRet = null;

        //while(true) {

            try {
                //noinspection ResultOfMethodCallIgnored
                int counter = 0;
                while (inputS == null && counter < 100) {
                    Thread.sleep(1000);
                    counter++;
                }
                if (inputS == null) {
                    LogData("The readString was null");
                    return "";
                }

                inputS.read(bufferinit);
                String recv = new String(bufferinit).replaceAll("[^0-9]|[�]", "");
                readSize = Integer.valueOf(recv);

                LogData("Got size: " + String.valueOf(readSize));

            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                passed = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (passed) {
                byte[] buffer = new byte[readSize];
                try {
                    try {
                        if (inputS == null) return null;

                        inputS.read(buffer);
                    } catch (NullPointerException readfail) {
                        LogData("Failed to read");
                    }
                    toRet = new String(buffer).replaceAll("[�]", "");
                    toRet = toRet.replaceAll("[^a-zA-Z0-9 ,.\\\\\\?\\$\\^()\\{\\}*-_+=@!~`<>\\[\\]|:;\"']", "");

                    /*if(toRet.contains("response") && toRet.contains("nightUpdate")) {
                        is_large = true;
                    }*/

                    Log.d("APP", toRet);

                    //LogData("FIRST: " + tempStr.substring(0, 10));
                    //LogData("LAST: " + tempStr.substring(tempStr.length() - 15, tempStr.length() - 1));
                    //LogData("DOES IT HAVE IT??: " + String.valueOf(tempStr.contains("]}}")));

                    /*if(is_large && tempStr.contains("]}}")) {
                        is_large = false;
                        break;
                    } else if(!is_large) break;*/

                } catch (IOException e) {
                    e.printStackTrace();
                }

                //mMessage += System.getProperty("line.separator");
                //mText.post(mUpdateUI);
            }

        //}

        return toRet;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogData("Got binding to speech service (CONNECTED)");

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = SpeechRecognition.MSG_RECOGNIZER_START_LISTENING;

            //Start listening
            try {
                mServiceMessenger.send(msg);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogData("Binding to speech service lost (DISCONNECTED)");
            mServiceMessenger = null;
        }

    };

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

    public static void getWifiList(Context context, final Activity activity, JSONObject data) {
        LogData("GOT GET WIFI");
        ArrayList<WifiView> apList;
        String connnectedSsid = null;
        boolean checkSsid = false;

        try {
            apList = WifiView.getAps(context, data.getJSONArray("APs"));
            connnectedSsid = data.getString("connected");
        } catch (JSONException err) {
            LogData("Failed getting aps");
            err.printStackTrace();
            return;
        }

        if(connnectedSsid != null && connnectedSsid.length() > 0) checkSsid = true;

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
                if(checkSsid) {
                    if(connnectedSsid.equals(wifiView.ssid)) wifiView.connectedTo = true;
                }

                newApList.add(wifiView);
                Log.d("Adding new AP", wifiView.ssid);
            }
        }

        final WifiAdapter wifiAdapter = new WifiAdapter(context, newApList);
        activity.runOnUiThread(new Runnable() {

            private void showConnecting(JSONObject connection, String ssid) {
                writeString(connection.toString());

                try {
                    String password = null;

                    try {
                        password = connection.getJSONObject("data").getString("password");
                    } catch (JSONException ignored) {
                        LogData("Failed pulling password from json");
                    }

                    if(password != null) {
                        if (password.length() == 0 || password.isEmpty()) password = null;
                    }
                    InternalWireless.connect(ssid, password);
                } catch (Throwable err) {
                    LogData("Failed to connect to WiFi via internal device ERROR: " +
                            err.toString());
                }

                final ProgressDialog ringProgressDialog =
                        ProgressDialog.show(
                                activity,
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
                                Toast.makeText(activity,
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
                SettingsFragment.listView.setAdapter(wifiAdapter);
                SettingsFragment.listView.setVisibility(View.VISIBLE);
                SettingsFragment.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
                                            activity);
                            LinearLayout linearL = new LinearLayout(activity);

                            final TextView securityType = new TextView(activity);
                            securityType.setText(R.string.connect_security);

                            final TextView securityMain = new TextView(activity);
                            toConnect.securityType += "\n";
                            securityMain.setText(toConnect.securityType);
                            securityMain.setTextColor(Color.CYAN);
                            securityMain.setAllCaps(true);
                            securityMain.setTypeface(null, Typeface.BOLD);

                            final TextView passwordType = new TextView(activity);
                            passwordType.setText(R.string.connect_password);

                            final EditText input = new EditText(activity);
                            input.setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD);

                            final CheckBox showView = new CheckBox(activity);
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
                                String connecterString = "Connect to " + toConnect.ssid;
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
                SettingsFragment.listView.invalidate();
            }
        });
    }

    public static int countSaid = 0;

    final Runnable onRecieve = new Runnable() {
        @Override
        public void run() {
            while(responseThread) {
                String received = readString();

                if(received == null && countSaid < 4) {
                    LogData("Failed reading from script");
                    countSaid++;
                    continue;
                } else countSaid = 0;

                JSONObject result = new JSONObject();

                try {
                    result = new JSONObject(received);
                } catch (JSONException error) {
                    LogData("Failed parsing json " + error.toString());

                    try {
                        result.put("response", "failed");
                        result.put("data", new JSONObject());
                    } catch (JSONException ignored) {

                    }
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
                    switch (responseType) {
                        case "getWifi":
                            getWifiList(getApplicationContext(), MainActivity.this, data);
                            break;
                        case "connectWifi":
                            int connectionType = data.getInt("connected");

                            if (looperStarted) {
                                Looper.prepare();
                                looperStarted = false;
                            }

                            if (connectionType == 1) {
                                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to connect", Toast.LENGTH_LONG).show();

                            }

                            isConnected = true;

                            break;
                        case "liveUpdate":
                            sleepScore = data.getInt("sleepScore");
                            //accelX = data.getInt("accel");
                            temp = data.getInt("temp");
                            humidity = data.getInt("hum");
                            voltageBattery = ((((float)data.getInt("vbatt")
                                    / 100.0f) - 3.4f) * (100.0f / (4.27f-3.4f)));

                            //lightScore = data.getInt("light");

                            updateBars(); //Update all the bars
                            break;
                        case "nightLen":
                            /*try {
                                totalProcessing = data.getInt("len");

                                LogData("Processing a total of: " + totalProcessing);
                            } catch (JSONException ignored) {
                                LogData("FAILED SETTING TOTAL RECIEVING");
                            }
                            currentProcess = 0;
                            tempNightData = new ArrayList<>();*/
                            break;
                        case "nightEnd":
                            /*LastNightFragment.nightChart.resetData();

                            LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getAccelLabel(),
                                    Color.CYAN, Color.CYAN);

                            LastNightFragment.nightChart.setLineByPairs(tempNightData,
                                    lastNight.getAccelLabel());

                            LastNightFragment.nightChart.compileLinesInGraph();
                            LastNightFragment.nightChart.updateGraph(MainActivity.this);*/
                            //LogData("Finished rendering got night update!");

                            break;
                        case "nightQuery":
                            /*lastNight.parseSingle(data);
                            tempNightData.add(lastNight.getSingleAccel());
                            currentProcess++;
                            try {
                                double progNotRound = ((double) progressMax) *
                                        (((double) currentProcess) / ((double) totalProcessing));

                                if (LastNightFragment.progressBar != null)
                                    LastNightFragment.progressBar.setProgress((int) progNotRound);
                            } catch(RuntimeException ignored) {}*/
                            //LogData("Finished getting night update");
                            break;
                        default:
                            LogData("Didn't find a proper response for: " + responseType);
                            break;
                    }
                } catch (Throwable ignored) {
                    LogData("FAILED PARSING JSON");
                    ignored.printStackTrace();
                }

                //LogData("Finished reading from accessory");
            }
        }
    };

    public void updateBars() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(HomeFragment.sleepBar != null)
                    HomeFragment.sleepBar.setValueAnimated((float) sleepScore);

                /*if(LiveFragment.accels != null) {
                    LiveFragment.setAccelData(accelX, accelY, accelZ);
                    LiveFragment.accels.invalidate();
                }*/

                if(LiveFragment.tempBar != null)
                    LiveFragment.tempBar.setValueAnimated((float) temp);

                if(LiveFragment.humidityBar != null)
                    LiveFragment.humidityBar.setValueAnimated((float) humidity);

                if(LiveFragment.smartScore != null)
                    LiveFragment.smartScore.setValueAnimated((float) sleepScore);

                LiveFragment.updateBatteryLevel(getApplicationContext(), (int) voltageBattery);
            }
        });
    }

    @Override
    public void onInit(int status) {
        LogData("Initialized TextToSpeech Engine");
        setSoundOff();
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.UK);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                LogData("Language not available");
            } else {
                LogData("Saying stuff");
                deviceSpeak("Hello user I'm Slumber!", null);
                //setSoundOff();
            }
        } else {
            LogData("Couldn't initialize the TextToSpeech Engine");
        }
    }

    /*
    public void expandButton(final RelativeLayout expanding) {

        RelativeLayout others[] = new RelativeLayout[] {
                top_left,
                top_right,
                bottom_left,
                bottom_right
        };

        for(int ind  = 0; ind < others.length; ind++) {
            RelativeLayout curButton = others[ind];

            if(curButton == null) {
                MainActivity.LogData("INDEX: " + ind + " FAILED LOADING BUTTON");
                continue;
            }

            //It's the same button
            if(expanding.getId()
                    ==
                    curButton.getId()) {
                final RelativeLayout tempOthers[] = new RelativeLayout[others.length - 1];
                int addOther = 0;

                for(int addInd = 0; addInd < others.length; addInd++) {
                    if(addInd != ind) tempOthers[addOther] = others[addInd];
                }
                others = tempOthers;
            }
        }

        final int
                finalHeight = metrics.heightPixels,
                finalWidth = metrics.widthPixels + 100,
                startHeight = expanding.getLayoutParams().height,
                startWidth = expanding.getLayoutParams().width;

        final float
                scaleTransformH = (1 / (float) finalHeight) * (float) startHeight,
                scaleTransformW = (1 / (float) finalWidth) * (float) startWidth;


        //Set all the buttons to visible
        for(RelativeLayout button : others) {
            button.setVisibility(View.VISIBLE);
            button.setAlpha(1);
            button.setClickable(false);
        }

        expanding.setVisibility(View.VISIBLE);
        expanding.setAlpha(1);

        //Expand the button to a full screen
        final RelativeLayout[] finalOthers = others;
        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                float calcHeight = (scaleTransformH + interpolatedTime);
                float calcWidth = (scaleTransformW + interpolatedTime);

                //Expand the view to fill the screen
                expanding.getLayoutParams().height = (int) ((calcHeight < 1) ? (finalHeight * calcHeight): finalHeight);
                expanding.getLayoutParams().width = (int) ((calcWidth < 1) ? (finalWidth * calcWidth): finalWidth);
                expanding.setAlpha(1.0f - interpolatedTime);

                expanding.requestLayout();
                expanding.invalidate();

                //Make sure the other buttons stay the same
                for(RelativeLayout button : finalOthers) {
                    button.getLayoutParams().height = startHeight;
                    button.getLayoutParams().width = startWidth;

                    button.setAlpha(1.0f - interpolatedTime);

                    button.requestLayout();
                    button.invalidate();
                }

            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        anim.setDuration(2500);
        expanding.startAnimation(anim);
    }*/

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment retFrag = new HomeFragment();

            switch (position) {
                default:
                    break;
                case PageEnum.LASTNIGHT:
                    retFrag = new LastNightFragment();
                    break;
                case PageEnum.HOME:
                    break;
                case PageEnum.LIVE:
                    retFrag = new LiveFragment();
                    break;
                case PageEnum.SETTINGS:
                    retFrag = new SettingsFragment();
                    break;
            }

            return retFrag;
        }

        @Override
        public int getCount() {
            return PAGES;
        }
    }

    @Override
    public void onBackPressed() {
        if(vPager.getCurrentItem() != 0) {
            vPager.setCurrentItem(vPager.getCurrentItem() - 1);
        }
    }

    public static void setSoundOff() {
        if(amanager == null) {
            LogData("Failed turning sound off!");
            return;
        }

        int audioFlags[] = {
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC
        };

        int toSet = AudioManager.ADJUST_MUTE;

        for(int flag : audioFlags) {
            amanager.setStreamVolume(flag, toSet, 0);
        }

        /*
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        amanager.setStreamMute(AudioManager.STREAM_RING, true);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);*/
    }

    public static void setSoundOn() {
        if(amanager == null) {
            LogData("Failed turning sound on!");
            return;
        }

        int audioFlags[] = {
                AudioManager.STREAM_NOTIFICATION,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC
        };

        int toSet = AudioManager.ADJUST_UNMUTE;

        for(int flag : audioFlags) {
            amanager.setStreamVolume(flag, toSet, 0);
            amanager.setStreamVolume(flag, amanager.getStreamMaxVolume(flag),
                    0);
        }

        /*
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
        amanager.setStreamMute(AudioManager.STREAM_ALARM, false);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        amanager.setStreamMute(AudioManager.STREAM_RING, false);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CHECK_TTS_DATA: {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    LogData("TTS available");
                    tts = new TextToSpeech(this, this);
                    LogData("Attempted to start service");
                }
                else {
                    LogData("TTS NOT available");
                    Intent promptInstall = new Intent();
                    promptInstall.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(promptInstall);
                }
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        makeFullscreen();


        Intent checkIntent = new Intent();
        checkIntent.setAction( TextToSpeech.Engine.ACTION_CHECK_TTS_DATA );
        startActivityForResult(checkIntent, CHECK_TTS_DATA);


        /*Intent ttsService = new Intent(getApplicationContext(), TextToSpeechService.class);
        ttsService.setAction("tk.pseudonymous.slumberhub.accessory.TextToSpeechService");
        getApplicationContext().startService(ttsService);
        mBindFlag = Context.BIND_ABOVE_CLIENT;*/

        amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setSoundOff();
        //tts.speak("What's up ladies", TextToSpeech.QUEUE_FLUSH, null);

        appName = getResources().getString(R.string.app_name);

        lastNight = new NightData();

        metrics = getApplicationContext().getResources().getDisplayMetrics();

        wakeContext = MainActivity.this;

        Intent intent = getIntent();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        vPager = (ViewPager) findViewById(R.id.pager);
        vPager.setOffscreenPageLimit(10); //Always keep the pages loaded
        pAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        vPager.setAdapter(pAdapter);
        vPager.setCurrentItem(1, false); //Set to home page (Don't scroll to it)

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "WakeLock");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PWakeLock");

        SetupBluetooth(); //Starting blueooth

        String[] permissionChecks = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BATTERY_STATS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_CONFIGURATION,
                Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.BIND_VOICE_INTERACTION,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED
        };

        boolean needRequesting = false;

        for(String toCheck : permissionChecks) {
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), toCheck);
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                needRequesting = true;
                break;
            }
        }

        if(needRequesting) {
            ActivityCompat.requestPermissions(this, permissionChecks,
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        if(ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            LogData("Got permission for speech starting service");
            Intent recognitionService = new Intent(getApplicationContext(), SpeechRecognition.class);
            recognitionService.setAction("tk.pseudonymous.slumberhub.accessory.SpeechRecognition");
            getApplicationContext().startService(recognitionService);
            mBindFlag = Context.BIND_ABOVE_CLIENT; //Bind with client*.
            setSoundOff();

            LogData("Started service");
        }




        //sleepBar.setValueAnimated(0);

        /*
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
        */

        //getWifiList(object);

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
            Toast.makeText(MainActivity.this, "Please wait until smart score change",
                    Toast.LENGTH_LONG).show();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                readThread = new Thread(onRecieve);
                readThread.setDaemon(true);
                readThread.setName("UsbPythonReadThread");
                readThread.start();
            }
        }).start();
    }

    public static void wakeUpDevice() {
        if(wakeContext == null) {
            LogData("Failed waking up device NULL exception!");
        }

        try {
            wakeContext.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        } catch (Throwable err) {
            LogData("Failed setting wake of device");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, SpeechRecognition.class), mServiceConnection, mBindFlag);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        try {
            if (fullWakeLock != null) {
                fullWakeLock.acquire();
                fullWakeLock.release();
            }

            if (partialWakeLock != null) {
                partialWakeLock.acquire();
                partialWakeLock.release();
            }

        } catch (Throwable err) {
            LogData("Error releasing wakeLock ERROR: " + err.toString());
        }

        if (mServiceMessenger != null)
        {
            unbindService(mServiceConnection);
            mServiceMessenger = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LogData("PERMISSIONS GRANTED!");
            }
        }
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


    public static void startProxyB() {
        LogData("Starting bluetooth proxy");
        if(btAdapter != null) {
            if (btAdapter.isEnabled()) {
                for (BluetoothDevice tryDevice : pairedDevices) {
                    if (btHeadset == null || tryDevice == null) {
                        LogData("Failed getting bluetooth headset");
                        continue;
                    } else {
                        LogData("Found bluetooth device: " + tryDevice.getName());
                    }
                    try {
                        if (btHeadset.startVoiceRecognition(tryDevice)) {
                            LogData("Started the voice " +
                                    "proxy via the bluetooth device");
                            break;
                        }
                    } catch (Throwable ignored) {
                        LogData("FATAL STARTING BLUETOOTH HEADSET!");
                    }
                }
            }
        } else LogData("Proxy bluetooth stream is null");
    }

    public static void stopProxyB() {
        if(btAdapter != null && btHeadset != null) {
            for (BluetoothDevice tryDevice : pairedDevices) {
                if (btHeadset == null || tryDevice == null) {
                    LogData("Failed getting bluetooth headset");
                    continue;
                } else {
                    LogData("Found bluetooth device: " + tryDevice.getName());
                }
                try {
                    if (btHeadset.stopVoiceRecognition(tryDevice)) {
                        LogData("STOPPED the voice " +
                                "proxy via the bluetooth device");
                        break;
                    }
                } catch (Throwable ignored) {
                    LogData("FATAL STOPPING BLUETOOTH HEADSET!");
                }
            }
        }
    }

    private void SetupBluetooth()
    {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        pairedDevices = btAdapter.getBondedDevices();

        BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    LogData("Got bluetooth headset");
                    btHeadset = (BluetoothHeadset) proxy;
                    //stopProxyB();
                }
            }

            public void onServiceDisconnected(int profile)  {
                if (profile == BluetoothProfile.HEADSET) {
                    LogData("Bluetooth headset disconnected");
                    //stopProxyB();
                    btHeadset = null;
                }
            }
        };
        btAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
    }

    public static void deviceSpeak(final String toSpeak, final Messenger toSend) {
        LogData("SPEAKING: " + toSpeak + " MESSENGER " + toSend);

        if(toSpeak == null) {
            LogData("Attempting to start listening again");
            //mServiceMessenger = new Messenger(service);

            if(toSend != null) {
                setSoundOff();
                Message msg = new Message();
                msg.what = SpeechRecognition.MSG_RECOGNIZER_START_LISTENING;

                try {
                    toSend.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        try {

            if (tts != null) {
                if (tts.isSpeaking()) {
                    LogData("Already speaking attempting to stop!");
                    tts.stop();
                }

                //setSoundOn();

                LogData("Attempting to speak!!!");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                LogData("Starting toSpeak");
                                setSoundOn();
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                LogData("Finished speaking... turning off sound");
                                setSoundOff();

                                if (toSend != null) {
                                    setSoundOff();
                                    Message msg = new Message();
                                    msg.what = SpeechRecognition.MSG_RECOGNIZER_START_LISTENING;

                                    try {
                                        toSend.send(msg);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onError(String utteranceId) {}
                        });

                        HashMap<String, String> params = new HashMap<>();
                        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "speechId");
                        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, params);

                    }
                }).start();
            } else {
                LogData("CANNOT SPEAK: " + toSpeak + " NULL DATA");
            }

        } catch (Throwable ignored) {
            LogData("NOT ABLE TO DEVICESPEAK");
            if(toSend != null) {
                setSoundOff();
                Message msg = new Message();
                msg.what = SpeechRecognition.MSG_RECOGNIZER_START_LISTENING;

                try {
                    toSend.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Throwable ignored) {}
    }

    private class PageEnum {
        public static final int LASTNIGHT = 0, HOME = 1, LIVE = 2, SETTINGS = 3;
    }
}
