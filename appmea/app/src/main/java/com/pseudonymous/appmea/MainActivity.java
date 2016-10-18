package com.pseudonymous.appmea;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.Theme;
import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pseudonymous.appmea.fragment.DataFragment;
import com.pseudonymous.appmea.fragment.DeviceFragment;
import com.pseudonymous.appmea.fragment.HomeFragment;
import com.pseudonymous.appmea.fragment.NotificationsFragment;
import com.pseudonymous.appmea.fragment.SettingsFragment;
import com.pseudonymous.appmea.graphics.CircleTransform;
import com.pseudonymous.appmea.network.CommonResponse;
import com.pseudonymous.appmea.network.ProfileData;
import com.pseudonymous.appmea.network.ResponseListener;

import net.danlew.android.joda.JodaTimeAndroid;

public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener,
        DataFragment.OnFragmentInteractionListener,
        DeviceFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        NotificationsFragment.OnFragmentInteractionListener {

    /*
        VIEW LOADING
     */


    public static String appName, snackText;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private ImageView imageHeader, imageProfile;
    private TextView nameHeader, nameWebsite;
    private FloatingActionButton fab;
    private Toolbar toolbar;

    /*
        USER DATA AND PROFILE
     */


    //Global simple account settings
    //User name (AKA first + " " + last)
    //User site (Don't know why we have this currently)
    public static volatile String
            userName = "Slumber User",
            userSite = "http://pseudonymous.tk";

    //Background image to load (Maybe already cached on device) for the drawer layout
    //Both images are loaded with Glide
    private static String urlNavHeaderBg =
            "https://0.s3.envato.com/files/120334242/Preview%20image%20set/" +
                    "blue-grey-light-blue.png";

    //Profile picture (We auto round the image)
    //@See CircleTransform.java
    private static String urlProfileImg =
            "https://media.licdn.com/mpr/mpr/shrinknp_200_200/" +
                    "AAEAAQAAAAAAAAltAAAAJGJkMTFkZjY3LTEwMjktNDk4Yy04Zjg5LWJkZDlhZThkMzQ1NQ.jpg";


    public static ProfileData pfData; //Profile settings object
    //Includes the auto loading of the data

    /*
        NAVIGATION AND LAYOUT
     */


    //Start the navigation at home
    //Then add a circular padding to the notification icon in the drawer layout
    public static int
            navigationIndex = navEnum.HOME,
            numberPadding = 2;

    //Tags used to attach the fragments (No name display just the tag)
    private static final String
            TAG_HOME = "home",
            TAG_DATA = "data",
            TAG_DEVICE = "device",
            TAG_NOTIFICATIONS = "notifications",
            TAG_SETTINGS = "settings";

    private static String CURRENT_TAG = TAG_HOME; //Current tag to set at load time
    private String[] activityT; //Titles of the activities
    private static final boolean loadOnBack = true; //If back is pressed load the home fragment
    private Handler mHandle; //Message handling on main thread



    /**
     * The main function of the activity which will set the content of the activity and initially
     * Blank view. This will also load any preset data such as the main thread handlers.
     *
     * @param savedInstanceState Previously saved data from the activity with Parcelable
     * @see AppCompatActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Load basic string resources before anything else
        final Resources resources = getResources();
        appName = resources.getString(R.string.app_name); //Load app name into global
        snackText = resources.getString(R.string.action_snack_text);

        //Set the current view to main activity
        setContentView(R.layout.activity_main);

        //UI thread handler for the refresh
        mHandle = new Handler();

        //Custom toolbar and selection drop down options
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        //Add toolbar to the main view
        setSupportActionBar(toolbar);

        //Set toolbar settings such as the app name
        toolbar.setTitle(appName);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setCollapsible(true);
        toolbar.setTitleMarginBottom(16);
        toolbar.invalidate();

        //Set the main fragment loading drawer
        drawer = (DrawerLayout) findViewById(R.id.activity_main);

        navigationView = (NavigationView) findViewById(R.id.nav_view);

        //Little button in the bottom right for voice activation
        fab = (FloatingActionButton) findViewById(R.id.fab);

        //Navigation view header
        View navigationHeader = navigationView.getHeaderView(0);
        nameHeader = (TextView) navigationHeader.findViewById(R.id.name);
        nameWebsite = (TextView) navigationHeader.findViewById(R.id.website);
        imageHeader = (ImageView) navigationHeader.findViewById(R.id.img_header_bg);
        imageProfile = (ImageView) navigationHeader.findViewById(R.id.img_profile);

        //Set the global activity string list for the titles
        activityT = getResources().getStringArray(R.array.nav_item_activity_titles);

        //Animate the movement up
        //animateCenter();

        //Register default android bootstrap icon sets
        TypefaceProvider.registerDefaultIconSets();

        //Start the Joda Time class interface
        //This must be ran before any other Joda reference
        JodaTimeAndroid.init(this);

        //Load the custom created chart extends LineGraph in dataparse
        //chart = (ChartConfig) findViewById(R.id.chart);
        //chart.setFromXMLSettings(getApplicationContext()); //Load settings from chart_config.xml


        pfData = new ProfileData(); //New profile data object currently one user

        fab.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onClick(View view) {
                Snackbar snackbar = Snackbar.make(view, snackText, Snackbar.LENGTH_LONG);
                snackbar.setAction("Just say something and we will attempt to listen", null);

                //Modify the color of the snackbar to match our dark theme
                View viewSnack = snackbar.getView();

                viewSnack.setBackgroundColor(resources.getColor(R.color.colorPrimary));

                TextView textView = (TextView) viewSnack.
                        findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(resources.getColor(R.color.pseudo_text_color));

                snackbar.show();
            }
        });

        //Load the navigation bar header (THEY SHOULD BE CONNECTED TO THE INTERNET)
        //Set to demo user during initialization
        loadNavBar();


        // initializing navigation menu
        setUpNavigationView();

        if (savedInstanceState == null) {
            navigationIndex = 0;
            CURRENT_TAG = TAG_HOME;
            loadHomeFragment();
        }

        /*final Button button = (Button) findViewById(R.id.on_clicks);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //CommonNetwork.test();
                ResponseListener resp = new ResponseListener() {

                    @Override
                    public void on_complete(final CommonResponse req) {
                        ArrayList<ValuePair> valuePairs = req.getPairs();
                        Collections.reverse(valuePairs);

                        chart.setLabelDetails("sleep", ColorTemplate.MATERIAL_COLORS[0], ColorTemplate.rgb("#000000"));
                        chart.setLineByPairs(valuePairs, "sleep");
                        chart.compileLinesInGraph();
                        //chart.cleanXAxis();
                        chart.updateGraph(MainActivity.this);

                        /*MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                List<Entry> entries = new ArrayList<>();
                                ArrayList<String> labels = new ArrayList<>();


                                ArrayList<ValuePair> pairRev = req.getPairs();
                                Collections.reverse(pairRev);
                                //for(int ind = (req.getPairs().size() - 1); ind > 0; ind --)
                                //    pairRev.add(req.getPairs().get(ind));

                                int a = 0;
                                for(ValuePair pairs : pairRev) {
                                    entries.add(new Entry(a, Float.valueOf(pairs.getValue().toString())));
                                    labels.add(pairs.getTimeStamp().toString());
                                    pairs = pairRev.get(a);
                                    pairs.setIdV(a);
                                    pairRev.set(a, pairs);
                                    Log.d(a + " RESPONSE", pairs.toString() + "  ");
                                    a += 1;
                                }

                                //Collections.reverse(entries); //Turn from oldest to newest

                                LineDataSet dataSet = new LineDataSet(entries, "Value");
                                dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
                                dataSet.setLineWidth(2f);
                                dataSet.setCircleRadius(5f);
                                dataSet.setDrawCircles(true);
                                dataSet.setDrawCircleHole(true);
                                dataSet.setValueTextColor(2);
                                dataSet.setLabel("Restlessness during the night");


                                ArrayList<LineDataSet> dataSets = new ArrayList<>();
                                dataSets.add(dataSet); // add the datasets

                                LineData lineData = new LineData(dataSet);
                                lineData.calcMinMax();
                                chart.setData(lineData);

                                AxisValueFormatter formatter = new AxisValueFormatter() {

                                    @Override
                                    public String getFormattedValue(float value, AxisBase axis) {
                                        return req.getPairs().get((int) value).getTimeStamp().toString();
                                    }

                                    // we don't draw numbers, so no decimal digits needed
                                    @Override
                                    public int getDecimalDigits() {  return 0; }
                                };

                                AxisCleaner axisFormat = new AxisCleaner();

                                axisFormat.setAxis(pairRev);
                                axisFormat.setScale(100);
                                axisFormat.fixForAxis();

                                XAxis xAxis = chart.getXAxis();
                                xAxis.setGranularity(1f);
                                xAxis.setValueFormatter(axisFormat);
                                //chart.setDescriptionTextSize(24);
                                //chart.animateX(1500);
                                chart.invalidate(); // refresh
                            }
                        });
                    }

                    @Override
                    public void on_fail(CommonResponse req) {

                    }
                };
                CommonNetwork.getValues(CommonNetwork.test_stream, resp);
            }
        });*/
    }


    /**
     * Globally log data with the same app tag so when using regex on the android monitor
     * It's easier to parse by the appname and debugging vs changing each one individually
     *
     * @param toLog A string to log the new data with
     */
    public static void LogData(String toLog) {
        LogData(toLog, false);
    }

    /**
     * Globally log data with the same app tag so when using regex on the android monitor
     * It's easier to parse by the appname and debugging vs changing each one individually
     *
     * @param toLog A string to log the new data with
     * @param error A boolean to indicate if the logged data is an error or not
     */
    public static void LogData(String toLog, boolean error) {
        Log.d(appName, (error) ? "ERROR: " + toLog : toLog);
        //Add ERROR string to beginning if error flag set
    }

    /**
     * Load asynchronously the user profile data from the server and update the global user
     * Details. The current Value pair method is hacky and should be replaced in the final product
     * For faster loading time and not as many crashes when the server responds with null values
     * but with a still 200 okay response
     */
    private void loadUserProfile() {

        //Create an asynchronous response listener
        ResponseListener respListen = new ResponseListener() {
            @Override
            public void on_complete(CommonResponse req) {
                //Using the common network we created for M2X
                //We can easily turn metadata into a commonResponse object
                //With a single ValuePair with a JSON for all the user details
                //Then parse it into the proper settings for ProfileData
                //Essentially the steps behind this weird looking method
                pfData = (ProfileData) req.getPair().getValue();

                //@TODO - MAKE THIS MULTI USER AND NOT AS SLOW


                //@TODO - DAVID PLEASE FINISH CREATING A PROFILE SETTING SO
                //@TODO - THAT THIS CAN PROPERLY UPDATE THE IMAGE AND CHECK LOGIN
                userName = pfData.getFirstName() + " " + pfData.getLastName();
                urlProfileImg = pfData.getProfileImg();
                urlNavHeaderBg = pfData.getBgImg();
            }

            @Override
            public void on_fail(CommonResponse req) {
                //When the asynchronous request fails to load
            }
        };

        pfData.pullDetails(respListen);
    }


    /**
     * Method to load the profile data into the drawer layout whenever a new request is updated
     * This will initially load {onCreate()} to show the demo user so it's not blank on load
     * This should be changed in the final product
     */
    private void loadNavBar() {

        //Set the header text
        nameHeader.setText(userName);
        nameWebsite.setText(userSite);


        //Load the global account images into the navigation drawer
        Glide.with(this).load(urlNavHeaderBg).crossFade().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageHeader);

        Glide.with(this).load(urlProfileImg).crossFade().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.gray_circle).thumbnail(0.5f)
                .bitmapTransform(new CircleTransform(this)) //Circle crop all images
                .into(imageProfile);

        //setNotificationDot(3, true, 1);

        //navigationView.getMenu().getItem(3).setActionView(R.layout.menu_dot);
    }


    /**
     * This method is for the drawer layout icon to display new notifications or updates
     * To any drawer layout menu item that has a proper index, on fail nothing will be able
     * To render
     *
     * @param index The menu item index (From top to bottom)
     * @param dotEnabled Boolean to set alpha of dot to 100% or not
     * @param amountNotifications The number to display in the menu bubble
     * @see DrawerLayout
     */
    public void setNotificationDot(int index, boolean dotEnabled, int amountNotifications) {
        //Get action menu and set dot as view

        try {
            navigationView.getMenu().getItem(index).setActionView(R.layout.menu_dot);
        } catch (Throwable ignored) {
            MainActivity.LogData("INDEX NOT AVAILABLE");
            return;
        }

        //Locate the view of the dot and set the alpha channel based on if the device is enabled
        LinearLayout dotMenu = (LinearLayout) navigationView.getMenu().getItem(index)
                .getActionView().findViewById(R.id.view_notification);
        dotMenu.setAlpha((dotEnabled) ? 1f : 0f);
        dotMenu.setGravity(Gravity.CENTER);
        //Set dot enabled based on how many notifications there are
        if(amountNotifications > 0) {
            //Add text to the dot to signify the amount of notifications
            TextView amount = new TextView(this);
            amount.setGravity(Gravity.CENTER);
            amount.setTextColor(Color.WHITE);
            amount.setIncludeFontPadding(true);
            amount.setPadding(numberPadding, numberPadding, numberPadding, numberPadding);

            //If more than 9 notification just add a +
            if(amountNotifications > 9) amount.setText("9+");
            else amount.setText(String.valueOf(amountNotifications));

            //Show the little circle with notification amounts
            dotMenu.addView(amount);
        }

        //Rendering check
        dotMenu.invalidate();
    }

    private void loadHomeFragment() {
        // selecting appropriate nav menu item
        selectNavMenu();

        // set toolbar title
        setToolbarTitle();

        // if user select the current navigation menu again, don't do anything
        // just close the navigation drawer
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_TAG) != null) {
            drawer.closeDrawers();
            toggleFab();
            return;
        }

        //Running this on the UI thread might slow down rendering
        //Adding a cross fade effect makes if feel faster but load the same time
        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                // update the main content by replacing fragments
                Fragment fragment = getHomeFragment();
                FragmentTransaction fragmentTransaction =
                        getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.frame, fragment, CURRENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        };

        // If mPendingRunnable is not null, then add to the message queue
        if(mHandle != null) {
            mHandle.post(mPendingRunnable);
            MainActivity.LogData("RUNNING NEW ACTIVITY");
        }
        else MainActivity.LogData("FAILED POSTING NEW ACTIVITY", true);

        // show or hide the fab button
        toggleFab();

        //Closing drawer on item click
        drawer.closeDrawers();

        // refresh toolbar menu
        invalidateOptionsMenu();
    }

    private Fragment getHomeFragment() {
        MainActivity.LogData("LOADING FRAGMENT: " + String.valueOf(navigationIndex));

        switch (navigationIndex) {
            default:
                return new HomeFragment(); //Default home fragment
            case navEnum.DATA:
                return new DataFragment(); //Show the sleep reports
            case navEnum.DEVICE:
                return new DeviceFragment(); //Show hub details
            case navEnum.NOTIFICATIONS:
                return new NotificationsFragment(); //Show all notifications
            case navEnum.SETTINGS:
                return new SettingsFragment(); // Load the settings fragment
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setToolbarTitle() {
        String toSet = activityT[navigationIndex];
        if(toSet == null) toSet = "Error";
        try {
            getSupportActionBar().setTitle(toSet);
        } catch (NullPointerException ignored) {}
    }

    private void selectNavMenu() {
        navigationView.getMenu().getItem(navigationIndex).setChecked(true);
    }

    private void setUpNavigationView() {
        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            // This method will trigger on item Click of navigation menu
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                //Check to see which item was being clicked and perform appropriate action
                switch (menuItem.getItemId()) {
                    //Replacing the main content with ContentFragment Which is our Inbox View;
                    case R.id.nav_home:
                        navigationIndex = navEnum.HOME;
                        CURRENT_TAG = TAG_HOME;
                        break;
                    case R.id.nav_data:
                        navigationIndex = navEnum.DATA;
                        CURRENT_TAG = TAG_DATA;
                        break;
                    case R.id.nav_device:
                        navigationIndex = navEnum.DEVICE;
                        CURRENT_TAG = TAG_DEVICE;
                        break;
                    case R.id.nav_notifications:
                        navigationIndex = navEnum.NOTIFICATIONS;
                        CURRENT_TAG = TAG_NOTIFICATIONS;
                        break;
                    case R.id.nav_settings:
                        navigationIndex = navEnum.SETTINGS;
                        CURRENT_TAG = TAG_SETTINGS;
                        break;
                    case R.id.nav_about_us:
                        drawer.closeDrawers();
                        startActivity(new Intent(MainActivity.this, AboutActivity.class));
                        overridePendingTransition(android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right);
                        return true;
                    case R.id.nav_privacy_policy:
                        // launch new intent instead of loading fragment
                        //startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
                        drawer.closeDrawers();
                        return true;
                    default:
                        navigationIndex = navEnum.HOME;
                }

                //Checking if the item is in checked state or not, if not make it in checked state
                if (menuItem.isChecked()) {
                    menuItem.setChecked(false);
                } else {
                    menuItem.setChecked(true);
                }
                menuItem.setChecked(true);

                loadHomeFragment();

                return true;
            }
        });


        //Custom actions to handle when the drawer changes states
        ActionBarDrawerToggle actionBarDrawerToggle =
                new ActionBarDrawerToggle(this, drawer, toolbar,
                        R.string.openDrawer, R.string.closeDrawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        //Abstract listener for toggle state
        drawer.addDrawerListener(actionBarDrawerToggle);

        //Icon update period
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers();
            return;
        }

        // This code loads home fragment when back key is pressed
        // when user is in other fragment than home
        if (loadOnBack) {
            // checking if user is on other navigation menu
            // rather than home
            if (navigationIndex != navEnum.HOME) {
                navigationIndex = navEnum.HOME;
                CURRENT_TAG = TAG_HOME;
                loadHomeFragment();
                return;
            }
        }

        super.onBackPressed();
    }


    public void animateCenter() {
        RelativeLayout entireView = (RelativeLayout) findViewById(R.id.activity_main);

        Animation moveUp = AnimationUtils.loadAnimation(this, R.anim.view_flow);
        moveUp.reset();

        Animation moveDown = AnimationUtils.loadAnimation(this, R.anim.toolbar_down);
        moveDown.reset();

        entireView.clearAnimation();
        entireView.startAnimation(moveUp);

        toolbar.invalidate();
        if(!toolbar.isActivated()) try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        toolbar.clearAnimation();
        toolbar.startAnimation(moveDown);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        // show menu only when home fragment is selected
        if (navigationIndex == navEnum.HOME) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }

        // when fragment is notifications, load the menu created for notifications
        if (navigationIndex == navEnum.NOTIFICATIONS) {
            getMenuInflater().inflate(R.menu.notifications, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        //settingsItem.setIcon(R.drawable.card);
        return super.onPrepareOptionsMenu(menu);
    }

    private void startSettings() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_m:
                startSettings();
                return true;
            case R.id.logout_m:
                Log.d("LOGOUT", "LOGGED OUT");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Display fab if on the home screen
    private void toggleFab() { if (navigationIndex == navEnum.HOME) fab.show(); else fab.hide(); }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

class navEnum {
    static final int
            HOME = 0,
            DATA = 1,
            DEVICE = 2,
            NOTIFICATIONS = 3,
            SETTINGS = 4;
}
