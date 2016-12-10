package tk.pseudonymous.slumberhub.graphics;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tk.pseudonymous.slumberhub.MainActivity;
import tk.pseudonymous.slumberhub.dataparse.AxisCleaner;
import tk.pseudonymous.slumberhub.dataparse.CommonResponse;
import tk.pseudonymous.slumberhub.dataparse.NightData;
import tk.pseudonymous.slumberhub.dataparse.ValuePair;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;

/**
 * Created by David Smerkous on 10/8/16.
 *
 */

public class Chart extends LineChart {

    private ArrayList<LineDataSet> chartLines = new ArrayList<>();
    private ArrayList<ValuePair> pairData = new ArrayList<>();
    private HashMap<String, Integer[]> ColorReps = new HashMap<>();

    public Description desc = new Description();

    //Line configurations
    private float lineWidth = 2f,
            circleRadius = 5f;

    private boolean drawCircles = true,
            drawCirlcleHole = true;

    private int scalePoints = 100;

    public Chart(Context context) {
        super(context);
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Chart(Context context, AttributeSet attrs, int defStyle) {
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
        LogData("SETTING NEW BACKGROUND COLOR: " + String.valueOf(newColor));
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

        this.setBackgroundColor(Color.parseColor(MainActivity.bgColor));

        this.setBorderColor(Color.WHITE);
        this.setNoDataTextColor(Color.WHITE);
        this.getDescription().setTextColor(Color.WHITE);
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

        float id = 0;

        for(ValuePair pair : pairs) {
            Object rawValue = pair.getValue();
            Float yPoint = (Float) rawValue;
            float timeSecs = CommonResponse.dateEpoch(pair.getTimeStamp());
            //DateTime dateTime = pair.getTimeStamp();


            if(timeSecs <= id) continue;
            id = timeSecs;
            Entry toAdd = new Entry(id, yPoint); //new Entry(timeSecs, yPoint);
            entries.add(toAdd);

            //Update reorganized labels since they've been reversed
            labels.add(pair.getTimeStamp().toString());
            //pair = pairs.get((int)id);
            //pair.setIdV((int)id);
            //pairs.set((int)id, pair);
            //id += 1;
        }

        LineDataSet lineSet = new LineDataSet(entries, label);
        lineSet.setValueTextColor(Color.WHITE);
        lineSet.setDrawCircles(false);
        lineSet.setDrawCircleHole(false);
        Integer vals[] = ColorReps.get(label);
        if(vals != null) lineSet = setLineConfig(lineSet, label, vals[0], vals[1]);
        else LogData("Line Values are null!");

        chartLines.add(lineSet);
    }

    public void LoadByCommonNetwork(String stream, final String label) {
        //LoadByCommonNetwork(stream, label, false, null);
    }

    public void setTouchable(boolean touchable) {
        this.setTouchEnabled(touchable);
    }

    /*public void LoadByCommonNetwork(String stream, final String label, final boolean invalidate,
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
    }*/

    public LineDataSet setLineConfig(LineDataSet dataSet, String label, int color, int text_color) {
        dataSet.setLineWidth(lineWidth);
        //dataSet.setCircleRadius(circleRadius);
        //dataSet.setDrawCircles(drawCircles);
        //dataSet.setDrawCircleHole(drawCirlcleHole);
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
        lineData.notifyDataChanged();
        lineData.setDrawValues(true);
        try {
            this.setData(lineData);
        } catch (IndexOutOfBoundsException id) {
            LogData("TOO MANY INDEX'S!");
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
        final Chart tempClass = Chart.this;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempClass.invalidate();
            }
        });
    }
}
