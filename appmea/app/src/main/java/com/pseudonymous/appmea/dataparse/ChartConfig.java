package com.pseudonymous.appmea.dataparse;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.pseudonymous.appmea.MainActivity;
import com.pseudonymous.appmea.network.CommonNetwork;
import com.pseudonymous.appmea.network.CommonResponse;
import com.pseudonymous.appmea.network.ResponseListener;
import com.pseudonymous.appmea.network.ValuePair;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by David Smerkous on 10/8/16.
 *
 */

public class ChartConfig extends LineChart {

    private static final String chartLocation = "chart_config.xml";
    private ArrayList<LineDataSet> chartLines = new ArrayList<>();
    private ArrayList<ValuePair> pairData = new ArrayList<>();
    private HashMap<String, Integer[]> ColorReps = new HashMap<>();

    //Line configurations
    private float lineWidth = 2f,
                    circleRadius = 5f;

    private boolean drawCircles = true,
                    drawCirlcleHole = true;

    private int scalePoints = 100;

    XMLParser parser;

    public ChartConfig(Context context) {
        super(context);
    }

    public ChartConfig(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChartConfig(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setBackgroundColorC(String args) {
        String colors[] = args.split(",");
        int rawColors[] = new int[4];

        int curInd = 0;
        for(String color : colors) {
            rawColors[curInd] = Integer.parseInt(color);
            curInd += 1;
        }

        int newColor = Color.argb(rawColors[0], rawColors[1], rawColors[2], rawColors[3]);
        MainActivity.LogData("SETTING NEW BACKGROUND COLOR: " + String.valueOf(newColor));
        this.setBackgroundColor(newColor);
    }

    public void setGridSets(boolean toset) {
        this.getAxisLeft().setDrawGridLines(toset);
        this.getAxisRight().setDrawGridLines(toset);
        this.getXAxis().setDrawGridLines(toset);

        this.getAxisLeft().setDrawAxisLine(toset);
        this.getAxisRight().setDrawAxisLine(toset);
        this.getXAxis().setDrawAxisLine(toset);
    }

    public void setAxisDraw(boolean axisDraw) {
    }

    public void setLightColors() {
        this.getAxisLeft().setDrawZeroLine(true);
        this.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        this.getAxis(YAxis.AxisDependency.LEFT).setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        this.getAxisRight().setDrawLabels(false);
        this.getAxisLeft().setDrawZeroLine(true);
        this.setBackgroundColor(Color.BLACK);
        this.setDrawBorders(false);

        this.setBorderColor(Color.WHITE);
        this.setNoDataTextColor(Color.WHITE);
        this.setDescriptionColor(Color.WHITE);
        this.setGridBackgroundColor(Color.WHITE);
        this.getAxisLeft().setTextColor(Color.WHITE);
        this.getAxisRight().setTextColor(Color.WHITE);
        this.getXAxis().setTextColor(Color.WHITE);
    }

    private Object convertArgument(Class clazz, String value) {
        if(boolean.class == clazz) return Boolean.parseBoolean(value);
        if(float.class == clazz) return Float.parseFloat(value);
        if(int.class == clazz) return Integer.parseInt(value);
        return value;
    }

    private Class findObject(String value) {
        if(value.equals("true") || value.equals("false")) return boolean.class;
        if(value.matches("^[0-9.]+$") && value.contains(".")) return float.class;
        if(value.matches("^[0-9.]+$") && !value.contains(".")) return int.class;
        return String.class;
    }

    public void resetData() {
        this.chartLines = new ArrayList<>(); //Reset all lines on new request
    }

    public void addLineSet(LineDataSet lineData) {
        chartLines.add(lineData);
    }

    public void setLabelDetails(String label, int color, int text_color) {
        ColorReps.put(label, new Integer[] {color, text_color});
    }

    public void setLineByPairs(ArrayList<ValuePair> pairs, String label) {
        List<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        pairData = pairs;

        int id = 0;

        for(ValuePair pair : pairs) {
            Object rawValue = pair.getValue();
            Float yPoint = ((Double) rawValue).floatValue();
            Entry toAdd = new Entry(id, yPoint);
            entries.add(toAdd);

            //Update reorganized labels since they've been reversed
            labels.add(pair.getTimeStamp().toString());
            pair = pairs.get(id);
            pair.setIdV(id);
            pairs.set(id, pair);

            id += 1;
        }

        LineDataSet lineSet = new LineDataSet(entries, label);
        Integer vals[] = ColorReps.get(label);
        if(vals != null) lineSet = setLineConfig(lineSet, label, vals[0], vals[1]);
        else MainActivity.LogData("Line Values are null!", true);

        chartLines.add(lineSet);
    }

    public void LoadByCommonNetwork(String stream, final String label) {
        LoadByCommonNetwork(stream, label, false, null);
    }

    public void setTouchable(boolean touchable) {
        this.setTouchEnabled(touchable);
    }

    public void LoadByCommonNetwork(String stream, final String label, final boolean invalidate,
                                    @Nullable final Activity activity) {
        ResponseListener responseListener = new ResponseListener() {
            @Override
            public void on_complete(CommonResponse req) {
                ArrayList<ValuePair> pairs = req.getPairs();
                Collections.reverse(pairs); //Reverse the order from oldest to newest
                setLineByPairs(pairs, label);

                if(invalidate) {
                    compileLinesInGraph();

                    //Call method to update the activity graph
                    //This actually runs on the ui method
                    if(activity != null) updateGraph(activity);
                }
            }

            @Override
            public void on_fail(CommonResponse req) {

            }
        };
        CommonNetwork.getValues(stream, responseListener);
    }

    public LineDataSet setLineConfig(LineDataSet dataSet, String label, int color, int text_color) {
        dataSet.setLineWidth(lineWidth);
        dataSet.setCircleRadius(circleRadius);
        dataSet.setDrawCircles(drawCircles);
        dataSet.setDrawCircleHole(drawCirlcleHole);
        dataSet.setValueTextColor(text_color);
        dataSet.setLabel(label);
        dataSet.setColor(color);

        return dataSet;
    }

    public void compileLinesInGraph() {
        LineDataSet setsAdd[] = new LineDataSet[chartLines.size()];
        int ind = 0;
        for(LineDataSet addSet : chartLines) {
            setsAdd[ind] = addSet;
            ind += 1;
        }

        LineData lineData = new LineData(setsAdd);
        lineData.calcMinMax();
        lineData.notifyDataChanged();
        lineData.setDrawValues(true);
        try {
            this.setData(lineData);
        } catch (IndexOutOfBoundsException id) {
            MainActivity.LogData("TOO MANY INDEX'S!", true);
        }
    }

    public void cleanXAxis() {
        AxisCleaner cleaner = new AxisCleaner();
        cleaner.setAxis(this.pairData);
        cleaner.setScale(scalePoints);
        cleaner.fixForAxis();


        XAxis xAxis = this.getXAxis();
        xAxis.setValueFormatter(cleaner);
    }

    public void updateGraph(Activity activity) {
        final ChartConfig tempClass = ChartConfig.this;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempClass.invalidate();
            }
        });
    }

    public void setFromXMLSettings(Context context) {
        this.setScaleEnabled(true);
        this.setSaveEnabled(true);

        try {
            parser = new XMLParser(context, chartLocation);
        } catch (IOException err) {
            err.printStackTrace();
        }

        ArrayList<ConfigurationType> configs = parser.LoadConfig();

        MainActivity.LogData("CHART CONFIG COUNT: " + String.valueOf(configs.size()));
        for(ConfigurationType configType : configs) {
            MainActivity.LogData("ATTEMPTING: " + configType.name);

            Method configMethod;
            Class[] params = new Class[1];
            try {
                params[0] = findObject(configType.first_arg);
                MainActivity.LogData("PARAM ZERO: " + params[0]);
                configMethod = this.getClass().getMethod(configType.method_name, params);
            } catch (NoSuchMethodException | SecurityException e) {
                MainActivity.LogData("NO SUCH METHOD AS: " + configType.method_name, true);
                e.printStackTrace();
                continue; //Don't continue below
            }

            try {
                if(params[0] != null) {
                    MainActivity.LogData("CONFIG TYPE: " + params[0]);
                    configMethod.invoke(this, convertArgument(params[0], configType.first_arg));
                } else {
                    MainActivity.LogData("Invoking method with no arguments, must be a mistake");
                    configMethod.invoke(this);
                }
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                MainActivity.LogData("FAILED CONVERTING ARGUMENT OR RUNNING METHOD: " +
                        configType.method_name);
                e.printStackTrace();
            }
        }
    }
}
