package com.pseudonymous.appmea.graphics;

/**
 * Created by David Smerkous on 10/15/16.
 *
 */

public class TypeData {

    private short currentType = 0;

    public final static short
            CHARTTYPE = 0,
            TITLETYPE = 1,
            TEXTTYPE = 2;


    public TypeData() {
        currentType = CHARTTYPE;
    }

    public TypeData(short startType) {
        currentType = startType;
    }

    public static TypeData chartInit() {
        return new TypeData(CHARTTYPE);
    }

    public static TypeData titleInit() {
        return new TypeData(TITLETYPE);
    }

    public static TypeData textInit() {
        return new TypeData(TEXTTYPE);
    }

    public boolean isChartType() {
        return currentType == CHARTTYPE;
    }

    public boolean isTitleType() {
        return currentType == TITLETYPE;
    }

    public boolean isTextType() {
        return currentType == TEXTTYPE;
    }

    public void setChartType() {
        currentType = CHARTTYPE;
    }

    public void setTitleType() {
        currentType = TITLETYPE;
    }

    public void setTextType() {
        currentType = TEXTTYPE;
    }

    public void setType(short toSet) {
        currentType = toSet;
    }

    public short getType() {
        return currentType;
    }
}
