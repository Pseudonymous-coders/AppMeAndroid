package com.pseudonymous.appmea;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.devpaul.bluetoothutillib.abstracts.BaseBluetoothActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.pseudonymous.appmea.bluetooth.BlueManager;
import com.pseudonymous.appmea.network.CommonNetwork;
import com.pseudonymous.appmea.network.CommonResponse;
import com.pseudonymous.appmea.network.ResponseListener;
import com.pseudonymous.appmea.network.ValuePair;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.os.SystemClock.currentThreadTimeMillis;

public class MainActivity extends AppCompatActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CommonNetwork.init();
        final LineChart chart = (LineChart) findViewById(R.id.chart);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setDescription("Appmea Chart");
        chart.setDrawGridBackground(false);
        chart.setSaveEnabled(true);

        JodaTimeAndroid.init(this);
        final Button button = (Button) findViewById(R.id.on_clicks);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //CommonNetwork.test();
                ResponseListener resp = new ResponseListener() {

                    @Override
                    public void on_complete(final CommonResponse req) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                List<Entry> entries = new ArrayList<>();
                                ArrayList<String> labels = new ArrayList<>();

                                int a = 0;
                                for(ValuePair pairs : req.getPairs()) {
                                    entries.add(new Entry(pairs.getIdV(), Float.valueOf(pairs.getValue().toString())));
                                    labels.add(pairs.getTimeStamp().toString());
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

                                XAxis xAxis = chart.getXAxis();
                                xAxis.setGranularity(1f);
                                xAxis.setValueFormatter(formatter);

                                chart.setDescriptionTextSize(24);
                                chart.animateX(1500);
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
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
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
}
