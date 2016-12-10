package tk.pseudonymous.slumberhub.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lumenghz.com.pullrefresh.PullToRefreshView;
import tk.pseudonymous.slumberhub.MainActivity;
import tk.pseudonymous.slumberhub.R;
import tk.pseudonymous.slumberhub.graphics.Chart;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;
import static tk.pseudonymous.slumberhub.MainActivity.countSaid;

/**
 * Created by David Smerkous on 11/29/16.
 * Project: SlumberHub
 */

public class LastNightFragment extends Fragment {
    Activity activity;
    public static Chart nightChart;

    static ProgressDialog loadingDialog;
    //static Context lastNightContext;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;

    public static String urlPull = "http://72.180.45.88:6767/user_data/uuidtest/all/0/99999999999999999";

    Button refreshButton;

    /*public static void showDialog() {
        loadingDialog = ProgressDialog.show(lastNightContext, "Loading Last Night",
                "Loading Last Nights Data", true);
    }

    public static void hideDialog() {
        loadingDialog.dismiss();
    }*/


    public static void requestData() {
        try {
            JSONObject toWrite = new JSONObject();
            toWrite.put("exec", "updateNight");
            MainActivity.writeString(toWrite.toString());
        } catch (Throwable ignored) {}
    }

    public static JSONArray fromServer() throws JSONException {

        String response = "[]";
        try {
            response = HttpRequest.get(urlPull).body();
        } catch (Throwable errorpull) {
            LogData("Error pulling from server: " + errorpull.toString());
        }
            // try {
       //     LogData("Got response (SHORT): " + response.substring(0, 100));
       // } catch (Throwable ignored) {
            LogData("Got response: " + response);
       // }
        return new JSONArray(response);
    }


    public static void pullAndUpdateView(final Activity activity) {
        Thread updatingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long counter = 0L;

                while (counter++ <= 3000L && LastNightFragment.nightChart == null) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LastNightFragment.nightChart.resetData();

                JSONArray arr = null;

                try {
                    arr = fromServer();
                } catch (JSONException ignored) {
                    LogData("FAILED PULLING LAST NIGHT FROM SERVER");
                }

                try {
                    LogData("Parsing json");
                    MainActivity.lastNight.parseJson(arr);
                } catch(JSONException err) {
                    LogData("Failed parsing json: " + err.toString());
                }

                LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getAccelLabel(),
                        Color.parseColor("#33691E"), Color.parseColor("#33691E"));

                LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getTempLabel(),
                        Color.parseColor("#673AB7"), Color.parseColor("#673AB7"));

                LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getHumidityLabel(),
                        Color.parseColor("#607D8B"), Color.parseColor("#607D8B"));

                LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getBatteryLabel(),
                        Color.parseColor("#CDDC39"), Color.parseColor("#CDDC39"));

                LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getAccelScore(),
                        MainActivity.lastNight.getAccelLabel());

                LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getTempScore(),
                        MainActivity.lastNight.getTempLabel());

                LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getHumidityScore(),
                        MainActivity.lastNight.getHumidityLabel());

                LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getBatteryScore(),
                        MainActivity.lastNight.getBatteryLabel());

                //Update the graph on the activity
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            LastNightFragment.nightChart.compileLinesInGraph();
                            LastNightFragment.nightChart.updateGraph(activity);
                        } catch (Throwable ignored) {
                            LogData("FAILED LOADING GRAPH");
                        }
                    }
                });
            }
        });
        updatingThread.setDaemon(true);
        updatingThread.setName("PullDataBaseData");
        updatingThread.start();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout rl = (RelativeLayout) inflater.inflate(
                R.layout.last_night_fragment, container, false);

        activity = getActivity();

        //lastNightContext = activity.getApplicationContext();


        JSONArray arr = null;
        try {
            arr = new JSONArray("[{\"type\":\"accels\",\"time\":1480607066,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607068,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607071,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607073,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607078,\"data\":{\"x\":0,\"y\":7,\"z\":0}},{\"type\":\"accels\",\"time\":1480607080,\"data\":{\"x\":0,\"y\":26,\"z\":0}},{\"type\":\"accels\",\"time\":1480607082,\"data\":{\"x\":13,\"y\":9,\"z\":47}},{\"type\":\"accels\",\"time\":1480607084,\"data\":{\"x\":90,\"y\":42,\"z\":57}},{\"type\":\"accels\",\"time\":1480607087,\"data\":{\"x\":9,\"y\":11,\"z\":37}},{\"type\":\"accels\",\"time\":1480607089,\"data\":{\"x\":43,\"y\":37,\"z\":17}},{\"type\":\"accels\",\"time\":1480607091,\"data\":{\"x\":9,\"y\":2,\"z\":1}},{\"type\":\"accels\",\"time\":1480607094,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607096,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607098,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607100,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607103,\"data\":{\"x\":0,\"y\":0,\"z\":0}},{\"type\":\"accels\",\"time\":1480607105,\"data\":{\"x\":47,\"y\":29,\"z\":15}},{\"type\":\"accels\",\"time\":1480607107,\"data\":{\"x\":90,\"y\":35,\"z\":6}},{\"type\":\"accels\",\"time\":1480607114,\"data\":{\"x\":10,\"y\":12,\"z\":48}},{\"type\":\"accels\",\"time\":1480607116,\"data\":{\"x\":0,\"y\":0,\"z\":20}},{\"type\":\"accels\",\"time\":1480607119,\"data\":{\"x\":0,\"y\":3,\"z\":0}},{\"type\":\"tnh\",\"time\":1480607043,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607047,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607048,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607050,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607052,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607055,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607057,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607059,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607062,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607064,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607066,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607068,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607071,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607073,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607078,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607080,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607082,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607084,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607087,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607089,\"data\":{\"temp\":78,\"hum\":29}},{\"type\":\"tnh\",\"time\":1480607091,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607094,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607096,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607098,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607100,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607103,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607105,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607107,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607114,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607116,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"tnh\",\"time\":1480607119,\"data\":{\"temp\":78,\"hum\":30}},{\"type\":\"vbatt\",\"time\":1480607044,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607046,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607048,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607050,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607052,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607055,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607057,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607059,\"data\":{\"vbatt\":4}},{\"type\":\"vbatt\",\"time\":1480607062,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607064,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607066,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607068,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607071,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607073,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607078,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607080,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607082,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607084,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607087,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607089,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607091,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607093,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607096,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607098,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607100,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607103,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607105,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607107,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607114,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607116,\"data\":{\"vbatt\":434}},{\"type\":\"vbatt\",\"time\":1480607119,\"data\":{\"vbatt\":434}}]");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nightChart = (Chart) rl.findViewById(R.id.chart);

        progressBar = (ProgressBar) rl.findViewById(R.id.progress_update);
        refreshButton = (Button) rl.findViewById(R.id.refresh_graph);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogData("Refresh pushed and updating graph");
                pullAndUpdateView(activity);
            }
        });

        progressBar.setProgress(0);

        nightChart.setLightColors();
        nightChart.setPinchZoom(true);
        nightChart.setGridSets(false);
        nightChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                //Nothing
            }

            @Override
            public void onNothingSelected() {
                //Nothing
            }
        });
        nightChart.setDrawGridBackground(false);

        nightChart.setNoDataText("Loading data...");

        nightChart.desc.setText("Slumber hub results");
        nightChart.desc.setTextColor(MainActivity.textColor);
        nightChart.setDescription(nightChart.desc);
        nightChart.setTouchable(true);
        nightChart.resetData();


        IAxisValueFormatter IAxisFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if(value == 0.0f) return "Min";
                else if(value == 100.0f) return "Max";
                else return "";
            }
        };


        YAxis yAxis = nightChart.getAxisLeft();
        yAxis.setDrawZeroLine(false);
        yAxis.setDrawZeroLine(false);
        yAxis.setLabelCount(100, false);
        yAxis.setValueFormatter(IAxisFormatter);

        //pullAndUpdateView(activity); //Update view

        try {
            LogData("Parsing json");
            MainActivity.lastNight.parseJson(arr);
        } catch(JSONException err) {
            LogData("Failed parsing json: " + err.toString());
        }

        LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getAccelLabel(),
                Color.parseColor("#33691E"), Color.parseColor("#33691E"));

        LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getTempLabel(),
                Color.parseColor("#673AB7"), Color.parseColor("#673AB7"));

        LastNightFragment.nightChart.setLabelDetails(MainActivity.lastNight.getHumidityLabel(),
                Color.parseColor("#607D8B"), Color.parseColor("#607D8B"));

        LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getAccelScore(),
                MainActivity.lastNight.getAccelLabel());

        LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getTempScore(),
                MainActivity.lastNight.getTempLabel());

        LastNightFragment.nightChart.setLineByPairs(MainActivity.lastNight.getHumidityScore(),
                MainActivity.lastNight.getHumidityLabel());

        Legend legend = nightChart.getLegend();
        legend.setTextColor(MainActivity.textColor);

        //setBgColor(rl);

        rl.setBackgroundColor(Color.parseColor(bgColor));

        return rl;
    }

}
