package com.pseudonymous.appmea.dataparse;

import android.util.Log;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.pseudonymous.appmea.MainActivity;
import com.pseudonymous.appmea.network.ValuePair;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by David Smerkous on 10/7/16.
 *
 */

public class AxisCleaner implements AxisValueFormatter{

    private ArrayList<ValuePair> values = null;
    private HashMap<Float, String> axisMap = new HashMap<>();
    private int valueCount = 0;
    private boolean setFirst = true, setLast = true;
    private int scalingPoints = 100;
    private DateTime firstDate, lastDate;

    public void setScale(int points) {
        this.scalingPoints = points;
    }

    private boolean deleteMaster(ValuePair previousPoint, ValuePair currentPoint) {
        float previous = (Float) previousPoint.getValue();
        float current = (Float) currentPoint.getValue();
        return false;
    }

    public void setAxis(ArrayList<ValuePair> pairs) {
        this.values = pairs;
        this.valueCount = this.values.size();

        if(this.values != null && this.valueCount > 1) {
            this.firstDate = pairs.get(0).getTimeStamp();
            this.lastDate = pairs.get(this.valueCount - 1).getTimeStamp();

            if(this.firstDate != null && this.lastDate != null) {
                /*if(this.valueCount < this.scalingPoints) {
                    int vectorDisplacement = this.scalingPoints - this.valueCount;
                }*/
                long totalVals = 0;

                DateTime lastTime = this.firstDate;

                for(ValuePair pairTest : this.values) {
                    long toComp = pairTest.getTimeStamp().getMillis() - lastTime.getMillis();
                    Log.d("UPDATE_DISTANCE", String.valueOf(toComp));
                    lastTime = pairTest.getTimeStamp();
                    totalVals += toComp;//(pairTest.getTimeStamp().getMillis() - toComp);
                }

                long averageDistance = totalVals / this.valueCount;

                Log.d("DISTANCE", "AVERAGE " + String.valueOf(averageDistance));

            } else Log.d("Error", "Couldn't set dates");

        } else Log.d("Error", "Data was blank");
    }

    public void fixForAxis() {
        if(this.values == null) return;

        boolean isFirst = true;
        int valueCounter = 0;

        for(ValuePair pair : this.values) {
            DateTime curTime = pair.getTimeStamp();
            String toDisplay;

            if(isFirst && setFirst) {
                toDisplay = curTime.toString();
                isFirst = false;
            } else if(setLast && valueCounter == (valueCount - 1)) {
                toDisplay = curTime.toString();
            } else {
                toDisplay = "9";
            }
            axisMap.put(((Integer)pair.getIdV()).floatValue(), toDisplay);
            valueCounter += 1;
        }
    }

    @Override
    public String getFormattedValue(float x_axis, AxisBase axis) {
        if(axisMap == null || valueCount == 0) return "";
        String toRet = this.axisMap.get(x_axis);
        if(toRet == null) {
            //MainActivity.LogData("Value at " + String.valueOf(x_axis) + " is blank");
            return "";
        }
        return toRet;
    }

    @Override
    public int getDecimalDigits() {
        return 0;
    }
}
