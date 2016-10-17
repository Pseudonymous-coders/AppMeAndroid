package com.pseudonymous.appmea.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.utils.ColorTemplate;
import com.pseudonymous.appmea.MainActivity;
import com.pseudonymous.appmea.R;
import com.pseudonymous.appmea.dataparse.ChartConfig;
import com.pseudonymous.appmea.graphics.TypeData;
import com.pseudonymous.appmea.graphics.ViewListAdapter;
import com.pseudonymous.appmea.network.CommonNetwork;
import com.rey.material.widget.LinearLayout;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import lumenghz.com.pullrefresh.PullToRefreshView;

public class DataFragment extends Fragment {

    private static int CURRENT_TAB = 0;

    private ArrayList<Pair<String, Pair<Integer, Pair<DateTime, DateTime>>>>
            TabSelection = new ArrayList<>();

    private Pair<DateTime, DateTime> currentRange;

    private static ListView totalListView;
    private static PullToRefreshView pullRefresh;
    private static ArrayList<ChartConfig> charts = new ArrayList<>(); //Set data globally
    private static boolean isOneDay = true, firstRefresh = true;
    private static final int REFRESH_DELAY = 500, REFRESH_AMOUNT = 120000;
    private static Timer timeTask = new Timer();


    public DataFragment() {
        //Use this to setup custom tab systems

        currentRange = new Pair<>(DateTime.now(), DateTime.now()); //Set default just in case
        //Of bad load

        TabSelection.add(new Pair<>("Last Night", new Pair<>(R.drawable.circle_nav, currentRange)));
        TabSelection.add(new Pair<>("Week View", new Pair<>(R.drawable.circle_nav, currentRange)));
        TabSelection.add(new Pair<>("Monthly View", new Pair<>(R.drawable.circle_nav, currentRange)));

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Currently waiting for inflation to happen

        try {
            JodaTimeAndroid.init(getContext()); //Start Joda time
        } catch (Throwable ignored) {} //Main activity might have already started it

    }

    //Called when we want to update the graph data
    //Usually called when the TabSelected Listener is activated
    private void updateLayout(int tabPosition) {
        CURRENT_TAB = tabPosition;
        isOneDay = (tabPosition == 0); //Set to is current day if it's the first tab

        if(isOneDay) {
            if(charts.isEmpty()) {
                String toSet = "No Data Available For This Day";

                TextView noData = new TextView(getContext());
                noData.setText(toSet);
                noData.setGravity(Gravity.CENTER);
                noData.setTextColor(Color.RED);
                noData.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Looper.prepare();
                        } catch (Throwable ignored) {}

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                                builder.setTitle("No Data");
                                builder.setMessage("There isn't any data for this day")
                                        .setCancelable(false)
                                        .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });
                    }
                });

                totalListView.addView(noData);
            } else {
                ChartConfig lastNight = charts.get(0);

                lastNight.resetData(); //Set everything back to 0


                lastNight.setLabelDetails("Random", ColorTemplate.rgb("#80CBC4"),
                        ColorTemplate.rgb("#80DEEA"));
                lastNight.setLabelDetails("Random2", ColorTemplate.rgb("#E57373"),
                        ColorTemplate.rgb("#E57373"));

                //Default method to pull from stream and display to chart value name
                //This method is completely asynchronous
                //The last loading by common must be true to make sure it invalidates
                //The graph and actually fixes the axis, including the activity to run
                //The update thread on
                lastNight.LoadByCommonNetwork("graph_test_2", "Random2");
                lastNight.LoadByCommonNetwork(CommonNetwork.test_stream, "Random", true,
                        getActivity());


                ArrayList<Pair<TypeData, Object>> composition = new ArrayList<>();

                composition.add(new Pair<>(TypeData.titleInit(), (Object) "Sensor Report"));
                composition.add(new Pair<>(TypeData.chartInit(), (Object) lastNight));
                composition.add(new Pair<>(TypeData.titleInit(), (Object) "Recommendations"));


                Object arr[] = new Object[] {
                        "<html><body><h1><font color = \"FFFFFF\">I recommend</font></h1></body></html>",
                        "<html><head><style>html *\n" +
                                "{\n" +
                                "   font-size: 1em !important;\n" +
                                "   color: #FFF !important;\n" +
                                "   font-family: Arial !important;\n" +
                                "}</style></head><body><p>You should probably just go to hell</p>" +
                                "<ol>\n" +
                                "   <li>YAAY</li>\n" +
                                "   <li>BAY</li>\n" +
                                "</ol>\n" +
                                "<a href=\"http://google.com\">Google Link</a>" +
                                "</body></html>"
                };

                composition.add(new Pair<>(TypeData.textInit(), (Object) arr));

                MainActivity.LogData("SIZE: " + composition.size());

                ViewListAdapter viewListAdapter = new ViewListAdapter(getActivity(),
                        getActivity(), composition);

                totalListView.setAdapter(viewListAdapter);

                //viewListAdapter.showAllViews(); //Make sure everything is visible

                //ViewListAdapter.setListViewHeightBasedOnChildren(totalListView);

                totalListView.invalidate(); //Update interface to make sure it appears
            }
        }

    }

    //Only show the single graph and data sets about that day
    private void populateOneDay() {
        ChartConfig singleDayGraph = new ChartConfig(getActivity()); //Create new chart

        //@TODO - MAKE THE XML LOADING GLOBAL SO THAT THE LOAD TIME IS FASTER
        singleDayGraph.setFromXMLSettings(getContext()); //Load current graph settings
        singleDayGraph.setLightColors();

        charts.add(0, singleDayGraph);
    }

    //Add widget boxes to the list
    private void populateLayout() {
        MainActivity.LogData("Populating data fragment");


        //@TODO - ADD MORE DAY SETTINGS AND CUSTOM TIME RANGES IN POPULATION OF LAYOUT
        if(isOneDay) {
            populateOneDay();
        }
    }

    private void connectLayout(final LinearLayout layout) {
        totalListView = (ListView) layout.findViewById(R.id.totalList);
        pullRefresh = (PullToRefreshView) layout.findViewById(R.id.pull_refresh);

        pullRefresh.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pullRefresh.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullRefresh.setRefreshing(true);
                        populateLayout();
                        updateLayout(CURRENT_TAB);
                        pullRefresh.setRefreshing(false);
                    }
                }, REFRESH_DELAY);
            }
        });

        startRefresh();

        isOneDay = true;
        populateLayout(); //Add all current items to the list
        updateLayout(0); //Always going to be the first tab
    }

    public void startRefresh() {
        TimerTask refreshLayout = new TimerTask() {
            @Override
            public void run() {
                if(firstRefresh) { firstRefresh = false; return; }

                try {
                    Looper.prepare();
                } catch (Throwable ignored) {
                    MainActivity.LogData("Failed pulling looper", true);
                }

                if(pullRefresh != null) {
                    //@TODO - DECIDE: WHILE REFRESHING SHOULD THE LITTLE TOP POPUP OCCUR
                    //Currently the part which does is commented out

                    /*getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pullRefresh.setRefreshing(true);
                        }
                    });*/

                    /*try {
                        Thread.sleep(REFRESH_DELAY); //Make sure it's visible to the user
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/

                    Activity tempRun = getActivity();

                    if(tempRun == null) {
                        MainActivity.LogData("Activity to update is null!", true);
                        return;
                    }

                    tempRun.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateLayout();
                            updateLayout(CURRENT_TAB);
                            //pullRefresh.setRefreshing(false);
                        }
                    });
                }
            }
        };

        timeTask = new Timer();
        timeTask.scheduleAtFixedRate(refreshLayout, 0, REFRESH_AMOUNT);
    }

    public void stopRefresh() {
        if(timeTask != null)
            timeTask.cancel();
        timeTask = null;
    }


    private void setupTabs(LinearLayout layout) {
        TabLayout tabLayout = (TabLayout) layout.findViewById(R.id.tab_layout);

        for(Pair<String, Pair<Integer, Pair<DateTime, DateTime>>> key : TabSelection) {
            TabLayout.Tab tabAdd = tabLayout.newTab();
            tabAdd.setText(key.first);

            //Load tab data from hashmap
            Pair<Integer, Pair<DateTime, DateTime>> TabData = key.second;
            //tabAdd.setIcon(TabData.first);
            currentRange = TabData.second;
            tabLayout.addTab(tabAdd);
        }

        //Set the tab spacing to full
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //On tab selection update the current graph
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                MainActivity.LogData("Current Tab: " + String.valueOf(tab.getPosition()));
                updateLayout(tab.getPosition());
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {/* No need for unset */}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                MainActivity.LogData("Refreshing tab: " + String.valueOf(tab.getPosition()));
                //No need to refresh the tab data
                //updateLayout(tab.getPosition());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout rl = (LinearLayout) inflater.inflate(R.layout.fragment_data,
                container, false);



        setupTabs(rl); //Parse the tabs and update the bar
        connectLayout(rl); //On inflation connect view items to the objects
        return rl;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        startRefresh();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopRefresh();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
