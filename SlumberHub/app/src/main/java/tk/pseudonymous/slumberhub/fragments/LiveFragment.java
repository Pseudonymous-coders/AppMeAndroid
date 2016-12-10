package tk.pseudonymous.slumberhub.fragments;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.animation.EasingFunction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;

import at.grabner.circleprogress.CircleProgressView;
import tk.pseudonymous.slumberhub.MainActivity;
import tk.pseudonymous.slumberhub.R;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;

/**
 * Created by David Smerkous on 11/29/16.
 * Project: SlumberHub
 */

public class LiveFragment extends Fragment{

    Activity activity;

    public static CircleProgressView tempBar, humidityBar, smartScore;
    @SuppressLint("StaticFieldLeak")
    //public static TextView smartScore;
    public static BarChart accels;

    public static ImageView batteryStatus;

    public static String
            xAxisVals[] = {"X", "Y", "Z"},
            yAxisVals[] = {"None", "Max"};

    public static String colors[] = {"#d50000", "#1565c0", "#33691e"};

    private static void setDrawable(Context context, int id) {
        batteryStatus.setImageDrawable(ContextCompat.getDrawable(context, id));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout rl = (RelativeLayout) inflater.inflate(
                R.layout.live_data_fragment, container, false);

        tempBar = (CircleProgressView) rl.findViewById(R.id.temp_bar);
        humidityBar = (CircleProgressView) rl.findViewById(R.id.humidity_bar);
        smartScore = (CircleProgressView) rl.findViewById(R.id.smart_live_score);

        //smartScore = (TextView) rl.findViewById(R.id.smart_score_value);
        //accels = (BarChart) rl.findViewById(R.id.accel_bars);

        tempBar.setUnit("F");
        humidityBar.setUnit("RH");
        smartScore.setUnitVisible(false);

        tempBar.setUnitScale(0.5f);

        tempBar.setAutoTextSize(true);
        tempBar.setTextColorAuto(true);
        humidityBar.setAutoTextSize(true);
        humidityBar.setTextColorAuto(true);

        smartScore.setAutoTextSize(true);

        tempBar.setValueAnimated(0);
        humidityBar.setValueAnimated(0);
        smartScore.setValueAnimated(0);

        batteryStatus = (ImageView) rl.findViewById(R.id.battery_icon);

        /*accels.setClickable(false);
        accels.setTouchEnabled(false);
        accels.setDrawValueAboveBar(true);
        accels.setBackgroundColor(Color.TRANSPARENT);
        accels.setDrawGridBackground(false);
        accels.setDrawBorders(false);
        accels.getDescription().setEnabled(false);
        accels.setPinchZoom(false);



        final IAxisValueFormatter xAxisFormat = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xAxisVals[(int) value - 1];
            }
        };

        final IAxisValueFormatter yAxisFormat = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return (value == 0.0f) ? yAxisVals[0] : (value == 100.0f) ? yAxisVals[1] : null;
            }
        };

        XAxis xAxis = accels.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTypeface(Typeface.DEFAULT);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(MainActivity.textColor);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextSize(11f);
        xAxis.setValueFormatter(xAxisFormat);

        YAxis yAxis = accels.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setDrawZeroLine(false);
        yAxis.setLabelCount(2, true);
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(100f);
        yAxis.setTextColor(MainActivity.textColor);
        yAxis.setGridColor(Color.TRANSPARENT);
        yAxis.setDrawAxisLine(false);
        yAxis.setTextSize(11f);
        yAxis.setEnabled(false);
        yAxis.setZeroLineColor(Color.TRANSPARENT);
        yAxis.setValueFormatter(yAxisFormat);


        YAxis rightSide = accels.getAxisRight();
        rightSide.setDrawZeroLine(false);
        rightSide.setDrawGridLines(false);
        rightSide.setDrawAxisLine(false);
        rightSide.setDrawTopYLabelEntry(false);
        rightSide.setDrawLabels(false);
        rightSide.setEnabled(false);
        rightSide.setTextColor(MainActivity.textColor);
        accels.getAxisRight().setDrawZeroLine(false);
        accels.getAxisRight().setDrawGridLines(false);

        accels.getLegend().setTextColor(MainActivity.textColor);
        accels.getLegend().setEnabled(false);

        accels.setNoDataText("Loading Data..");
        accels.setNoDataTextColor(MainActivity.textColor);*/

        activity = getActivity();

        //setBgColor(rl);

        rl.setBackgroundColor(Color.parseColor(bgColor));

        return rl;
    }

    public static void updateBatteryLevel(Context context, int level) {
        if(batteryStatus == null) {
            LogData("Couldn't update the battery level");
            return;
        }

        if(level < 5) {
            setDrawable(context, R.drawable.ic_battery_critical);
        } else if(level >= 5 && level < 30) {
            setDrawable(context, R.drawable.ic_battery_low);
        } else if(level >= 30 && level < 40) {
            setDrawable(context, R.drawable.ic_battery_medium);
        } else if(level >= 40 && level < 60) {
            setDrawable(context, R.drawable.ic_battery_middle);
        } else if(level >= 60 && level < 70) {
            setDrawable(context, R.drawable.ic_battery_high_middle);
        } else if(level >= 70 && level < 85) {
            setDrawable(context, R.drawable.ic_battery_high);
        } else if(level >= 85 && level < 90) {
            setDrawable(context, R.drawable.ic_battery_really_high);
        } else {
            setDrawable(context, R.drawable.ic_battery_full);
        }
        batteryStatus.invalidate();
    }

    /*public static void setAccelData(float x, float y, float z) {
        ArrayList<BarEntry> values = new ArrayList<>();
        values.add(new BarEntry(1, x));
        values.add(new BarEntry(2, y));
        values.add(new BarEntry(3, z));

        BarDataSet set = new BarDataSet(values, " ");
        set.setColors(Color.parseColor(colors[0]), Color.parseColor(colors[1]),
                Color.parseColor(colors[2]));

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);

        BarData data = new BarData(dataSets);
        data.setValueTextColor(MainActivity.textColor);
        data.setDrawValues(false);
        data.setBarWidth(0.9f);

        accels.setData(data);
    }*/

}
