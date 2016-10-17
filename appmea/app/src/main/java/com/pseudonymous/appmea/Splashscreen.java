package com.pseudonymous.appmea;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pseudonymous.appmea.network.CommonNetwork;

import net.danlew.android.joda.JodaTimeAndroid;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * Created by David Smerkous on 10/8/16.
 */

public class Splashscreen extends Activity {

    private static final long timeWait = 2900;
    private static final int intervalTime = 50;
    private static final String testSite = "https://m2x.att.com";

    public static volatile boolean continueSplash = true;

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
    }

    Thread splashTread;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);
        UnitTestThread();
        AnimationThread();
    }

    public boolean isConnectedTointernet() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworks = connectivityManager.getActiveNetworkInfo();
        return activeNetworks != null && activeNetworks.isConnected();
    }

    public static boolean pingSite(String site) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(site).openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        return (responseCode > 190 && responseCode < 210);
    }

    public void ErrorDialog(final String title, final String text) {
        try {
            Looper.prepare();
        } catch (Throwable ignored) {}

        continueSplash = false;

        Splashscreen.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(Splashscreen.this);

                builder.setTitle(title);
                builder.setMessage(text).setCancelable(false)
                        .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                continueSplash = true;
                                dialogInterface.cancel();
                            }
                        })
                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                continueSplash = true;
                                System.exit(1);
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void UnitTestThread() {
        Thread assertTest = new Thread() {
            @SuppressLint("Assert")
            @Override
            public void run() {

                boolean internetConnection;

                //Check to see if device is connected to any network
                try {
                    internetConnection = isConnectedTointernet();
                    if(!internetConnection) throw new Exception("NO INTERNET");
                    MainActivity.LogData("Connected to a network!");
                } catch (Throwable err) {
                    internetConnection = false;
                    MainActivity.LogData("NO INTERNET CONNECTION", true);
                }

                if(!internetConnection) ErrorDialog("No Network",
                        "Your phone isn't connected to any network");

                while(!continueSplash) {
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break; //Stop loop
                    }
                }

                //Check to see if we have access to current servers
                try {
                    internetConnection = pingSite(testSite);
                    if(!internetConnection) throw new Exception("NO SERVICE");
                    MainActivity.LogData("Found ATT service");
                } catch (Throwable err) {
                    internetConnection = false;
                    MainActivity.LogData("FAILED TO GET A RESPONSE FROM ATT", true);
                }

                if(!internetConnection) ErrorDialog("Server Error",
                        "We've tried testing the server and got a bad response");

                while(!continueSplash) {
                    try {
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break; //Stop loop
                    }
                }

                //Start the network interface communication to main servers
                //Currently set to M2X, find the key in CommonNetwork.java
                try {
                    CommonNetwork.init();
                    internetConnection = true;
                } catch(Throwable ignored) {
                    internetConnection = false;
                    MainActivity.LogData("FAILED SETTING UP M2X", true);
                }

                if(!internetConnection) {
                    ErrorDialog("Data Error",
                            "Failed loading user details from the server! Won't continue!");

                    while(!continueSplash) {
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break; //Stop loop
                        }
                    }

                    return;
                }

                try {

                } catch (Throwable ignored) {
                    MainActivity.LogData("FAILED LOADING USER PROFILE DETAILS", true);
                }

                //Start the Joda Time class interface
                //This must be ran before any other Joda reference
                try {
                    JodaTimeAndroid.init(Splashscreen.this);
                } catch(Throwable ignored) {
                    MainActivity.LogData("FAILED TO SET JODA TIME", true);
                }
            }
        };

        assertTest.setDaemon(true);
        assertTest.setName("System check and tests");
        assertTest.start();
    }

    private void AnimationThread() {
        //Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        //anim.reset();
        final LinearLayout l=(LinearLayout) findViewById(R.id.lin_splash);
        l.clearAnimation();
        //l.startAnimation(anim);

        //anim = AnimationUtils.loadAnimation(this, R.anim.translate);
        //anim.reset();
        final ImageView iv = (ImageView) findViewById(R.id.splash);
        iv.clearAnimation();
        //iv.startAnimation(anim);

        //final Animation finalAnim = anim;
        splashTread = new Thread() {
            @Override
            public void run() {
                try {
                    //Having sleep intervals allow Intterupt Exception to occur
                    long start = System.currentTimeMillis();
                    while((System.currentTimeMillis() - start) < timeWait) {
                        while(!continueSplash) sleep(50); //Check to see if dialog has poped up
                        sleep(intervalTime);
                    }

                    //Prevent the screen from flashing black for a second
                    /*Splashscreen.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            l.setBackgroundColor(Color.WHITE); //Reset the view back to white
                            l.setAlpha(0); //Double check

                            //Remove the image from the activity from being visible
                            iv.invalidate();
                            iv.clearColorFilter();
                            iv.setImageAlpha(0);
                            iv.setBackgroundColor(Color.TRANSPARENT);

                            //Reset the animation to the beginning and make sure the background is
                            //Transparent so it doesn't flash black for a second
                            //finalAnim.setBackgroundColor(Color.TRANSPARENT);
                            //finalAnim.reset();
                        }
                    });*/

                    Intent intent = new Intent(Splashscreen.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent); //Starts MainActivity
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (InterruptedException ignored) {
                } finally {
                    Splashscreen.this.finish();
                }

            }
        };
        splashTread.start();
    }

}