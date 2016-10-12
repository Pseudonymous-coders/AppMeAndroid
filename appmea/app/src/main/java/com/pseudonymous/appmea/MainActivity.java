package com.pseudonymous.appmea;

import android.content.Intent;
import android.graphics.Color;
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

import com.beardedhen.androidbootstrap.TypefaceProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pseudonymous.appmea.dataparse.ChartConfig;
import com.pseudonymous.appmea.fragment.HomeFragment;
import com.pseudonymous.appmea.fragment.MoviesFragment;
import com.pseudonymous.appmea.fragment.NotificationsFragment;
import com.pseudonymous.appmea.fragment.DataFragment;
import com.pseudonymous.appmea.fragment.SettingsFragment;
import com.pseudonymous.appmea.graphics.CircleTransform;

import net.danlew.android.joda.JodaTimeAndroid;

public class MainActivity extends AppCompatActivity {

    public static final String appName = "Appmea";
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private View navigationHeader;
    private ImageView imageHeader, imageProfile;
    private TextView nameHeader, nameWebsite;
    private FloatingActionButton fab;

    ChartConfig chart;
    Toolbar toolbar;

    private static final String urlNavHeaderBg =
            "http://mwhd.altervista.org/wp_upload/wallpapers/material/" +
                    "Rainbow_Material_Dark-Qwen_Lee.png";

    private static final String urlProfileImg =
            "https://media.licdn.com/mpr/mpr/shrinknp_200_200/" +
                    "AAEAAQAAAAAAAAltAAAAJGJkMTFkZjY3LTEwMjktNDk4Yy04Zjg5LWJkZDlhZThkMzQ1NQ.jpg";

    public static int
            navigationIndex = 0,
            numberPadding = 2;

    public static volatile String
            userName = "Demo User",
            userSite = "http://pseudonymous.tk";


    // tags used to attach the fragments
    private static final String TAG_HOME = "home";
    private static final String TAG_PHOTOS = "photos";
    private static final String TAG_MOVIES = "movies";
    private static final String TAG_NOTIFICATIONS = "notifications";
    private static final String TAG_SETTINGS = "settings";
    private static String CURRENT_TAG = TAG_HOME;

    private String[] activityT; //Titles of the activities

    private boolean loadOnBack = true;
    private Handler mHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        toolbar.setTitle(appName);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        toolbar.setCollapsible(true);
        toolbar.setTitleMarginBottom(16);
        toolbar.invalidate();

        drawer = (DrawerLayout) findViewById(R.id.activity_main);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        //Navigation view header
        navigationHeader = navigationView.getHeaderView(0);
        nameHeader = (TextView) navigationHeader.findViewById(R.id.name);
        nameWebsite = (TextView) navigationHeader.findViewById(R.id.website);
        imageHeader = (ImageView) navigationHeader.findViewById(R.id.img_header_bg);
        imageProfile = (ImageView) navigationHeader.findViewById(R.id.img_profile);


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


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "HAHAHA ACTION SNACK", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        //Load the navigation bar header (THEY SHOULD BE CONNECTED TO THE INTERNET)
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

    public static void LogData(String toLog) {
        LogData(toLog, false);
    }

    public static void LogData(String toLog, boolean error) {
        Log.d(appName, (error) ? "ERROR: " + toLog : toLog);
    }


    private void loadNavBar() {
        nameHeader.setText(userName);
        nameWebsite.setText(userSite);

        Glide.with(this).load(urlNavHeaderBg).crossFade().diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageHeader);

        Glide.with(this).load(urlProfileImg).crossFade().diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.gray_circle).thumbnail(0.5f)
                .bitmapTransform(new CircleTransform(this)) //Circle crop all images
                .into(imageProfile);

        //setNotificationDot(3, true, 1);

        //navigationView.getMenu().getItem(3).setActionView(R.layout.menu_dot);
    }

    public void setNotificationDot(int index, boolean dotEnabled, int amountNotifications) {
        navigationView.getMenu().getItem(index).setActionView(R.layout.menu_dot);

        LinearLayout dotMenu = (LinearLayout) navigationView.getMenu().getItem(index)
                .getActionView().findViewById(R.id.view_notification);
        dotMenu.setAlpha((dotEnabled) ? 1f : 0f);
        dotMenu.setGravity(Gravity.CENTER);
        //Set dot enabled based on how many notifications there are
        if(amountNotifications > 0) {
            TextView amount = new TextView(this);
            amount.setGravity(Gravity.CENTER);
            amount.setTextColor(Color.WHITE);
            amount.setIncludeFontPadding(true);
            amount.setPadding(numberPadding, numberPadding, numberPadding, numberPadding);
            if(amountNotifications > 9)
                amount.setText("9+");
            else amount.setText(String.valueOf(amountNotifications));
            dotMenu.addView(amount);
        }

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
        if(mPendingRunnable != null && mHandle != null)
            mHandle.post(mPendingRunnable);

        // show or hide the fab button
        toggleFab();

        //Closing drawer on item click
        drawer.closeDrawers();

        // refresh toolbar menu
        invalidateOptionsMenu();
    }

    private Fragment getHomeFragment() {
        switch (navigationIndex) {
            default:
                return new HomeFragment(); //Default home fragment
            case 1:
                return new DataFragment(); //Show the sleep reports
            case 2:
                // movies fragment
                return new MoviesFragment();
            case 3:
                // notifications fragment
                return new NotificationsFragment();
            case 4:
                // settings fragment
                return new SettingsFragment();
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
                        CURRENT_TAG = TAG_PHOTOS;
                        break;
                    case R.id.nav_movies:
                        navigationIndex = 2;
                        CURRENT_TAG = TAG_MOVIES;
                        break;
                    case R.id.nav_notifications:
                        navigationIndex = 3;
                        CURRENT_TAG = TAG_NOTIFICATIONS;
                        break;
                    case R.id.nav_settings:
                        navigationIndex = 4;
                        CURRENT_TAG = TAG_SETTINGS;
                        break;
                    case R.id.nav_about_us:
                        // launch new intent instead of loading fragment
                        //startActivity(new Intent(MainActivity.this, AboutUsActivity.class));
                        drawer.closeDrawers();
                        return true;
                    case R.id.nav_privacy_policy:
                        // launch new intent instead of loading fragment
                        //startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
                        drawer.closeDrawers();
                        return true;
                    default:
                        navigationIndex = 0;
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
            if (navigationIndex != 0) {
                navigationIndex = 0;
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
        if (navigationIndex == 0) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
        }

        // when fragment is notifications, load the menu created for notifications
        if (navigationIndex == 3) {
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
        Intent intent = new Intent(this, SettingsActivity.class);
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
}

class navEnum {
    static final int
            HOME = 0,
            DATA = 1;
}
