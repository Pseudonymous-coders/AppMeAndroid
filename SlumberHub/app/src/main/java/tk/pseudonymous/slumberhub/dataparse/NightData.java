package tk.pseudonymous.slumberhub.dataparse;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.FactoryConfigurationError;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;

/**
 * Created by David Smerkous on 11/30/16.
 * Project: SlumberHub
 */

public class NightData {
    private CommonResponse sleepScore, accelScore, soundScore, lightScore, tempScore,
            humidityScore, batteryScore;

    private final static String
            SLEEP = "Sleep Score",
            ACCEL = "Accelerometer",
            SOUND = "Ambient Noise",
            LIGHT = "Ambient Light",
            TEMP = "Temperature (F)",
            HUMIDITY = "Humidity",
            BATTERY = "Battery (V)";



    public NightData() {
        sleepScore = new CommonResponse();
        sleepScore.chartName = SLEEP;
        sleepScore.valueName = "sleepScore";

        accelScore = new CommonResponse();
        accelScore.chartName = ACCEL;
        accelScore.valueName = "accel";

        soundScore = new CommonResponse();
        soundScore.chartName = SOUND;
        soundScore.valueName = "soundScore";

        lightScore = new CommonResponse();
        lightScore.chartName = LIGHT;
        lightScore.valueName = "lightScore";

        tempScore = new CommonResponse();
        tempScore.chartName = TEMP;
        tempScore.valueName = "tnh";

        humidityScore = new CommonResponse();
        humidityScore.chartName = HUMIDITY;
        humidityScore.valueName = "hum";

        batteryScore = new CommonResponse();
        batteryScore.chartName = BATTERY;
        batteryScore.valueName = "vbatt";
    }

    private void fixScaling() {
        ArrayList<CommonResponse> pairData = new ArrayList<>();
        pairData.add(sleepScore);
        pairData.add(accelScore);
        pairData.add(tempScore);
        pairData.add(humidityScore);
        pairData.add(batteryScore);

        int largestSize = 0;
        int largestInd = -1;

        int index = 0;
        for(CommonResponse commonResponse : pairData) {
            if(commonResponse.value_multi_responses.size() > largestSize) {
                largestInd = index;
            }
            index++;
        }

        if(largestInd == -1) {
            LogData("NO DATA FOUND NOT CONTINUING");
            return;
        }



    }

    public void parseSingle(JSONObject toParse) throws JSONException {
        ValuePair pair = new ValuePair();

        float accelValue = 0.0f;

        try {
            accelValue = (float) toParse.getInt("accel");
        } catch (JSONException ignored) {
            LogData("Failed getting nightData parse");
        }

        pair.setValue(accelValue);
        pair.setTimeStamp(CommonResponse.getDate(toParse.getLong("time")));

        accelScore.value_response = pair;
    }


    public void parseJson(JSONArray toParse) throws JSONException {
        long sizeArr;
        try {
            sizeArr = toParse.length();
        } catch (Throwable ignored) {
            return;
        }

        sleepScore.value_multi_responses = new ArrayList<>();
        accelScore.value_multi_responses = new ArrayList<>();
        tempScore.value_multi_responses = new ArrayList<>();
        humidityScore.value_multi_responses = new ArrayList<>();
        batteryScore.value_multi_responses = new ArrayList<>();


        for(long ind = 0; ind < sizeArr; ind++) {
            JSONObject curObj = toParse.getJSONObject((int)ind);
            JSONObject dataObj = curObj.getJSONObject("data");


            String type = curObj.getString("type");

            DateTime timeCapture = CommonResponse.getDate(curObj.getLong("time"));

            ValuePair pairAdding = new ValuePair();
            pairAdding.setTimeStamp(timeCapture);

            if(type.compareTo(sleepScore.valueName) == 0) {

                float sleepScoreF = 0.0f;
                try {
                    dataObj.getInt("score");
                } catch (JSONException ignored) {
                    LogData("Failed getting score");
                }

                pairAdding.setValue(sleepScoreF);
                sleepScore.value_multi_responses.add(pairAdding);
            } else if(type.compareTo(accelScore.valueName) == 0) {
                float accel = 0.0f;

                try {
                    accel = (float) dataObj.getInt("accel");
                } catch (JSONException ignored) {
                    LogData("Failed getting x");
                }

                //if(accel > 1.0f) {
                    pairAdding.setValue(accel);
                    accelScore.value_multi_responses.add(pairAdding);
                //}
            } else if(type.compareTo(tempScore.valueName) == 0) {
                float temp = 0.0f;
                float hum = 0.0f;

                try {
                    temp = (float) dataObj.getInt("temp");
                } catch (JSONException ignored) {
                    LogData("Failed getting temp");
                }

                try {
                    hum = (float) dataObj.getInt("hum");
                } catch (JSONException ignored) {
                    LogData("Failed getting humidity");
                }
                pairAdding.setValue(temp);
                ValuePair humPair = new ValuePair();
                humPair.setTimeStamp(timeCapture);
                humPair.setValue(hum);
                tempScore.value_multi_responses.add(pairAdding);
                humidityScore.value_multi_responses.add(humPair);

            } else if(type.compareTo(batteryScore.valueName) == 0) {
                float battery = 0.0f;

                try {
                    battery = ((((float)dataObj.getInt("vbatt") / 100.0f) - 3.4f) * (100.0f / (4.27f-3.4f)));
                } catch (JSONException ignored) {
                    LogData("Failed getting battery voltage");
                }
                if(battery > 3.0f) {
                    pairAdding.setValue(battery);
                    batteryScore.value_multi_responses.add(pairAdding);
                }
            } else {
                LogData("Failed finding type: " + type + " As an argument!");
            }
        }

        Collections.sort(sleepScore.value_multi_responses);
        Collections.sort(accelScore.value_multi_responses);
        Collections.sort(tempScore.value_multi_responses);
        Collections.sort(humidityScore.value_multi_responses);
        Collections.sort(batteryScore.value_multi_responses);
    }

    public ArrayList<ValuePair> getSleepScore() {
        return sleepScore.getPairs();
    }

    public ArrayList<ValuePair> getAccelScore() {
        return accelScore.getPairs();
    }

    public String getAccelLabel() {
        return accelScore.chartName;
    }

    public ArrayList<ValuePair> getTempScore() {
        return tempScore.getPairs();
    }

    public String getTempLabel() {
        return tempScore.chartName;
    }

    public ArrayList<ValuePair> getHumidityScore() {
        return humidityScore.getPairs();
    }

    public String getHumidityLabel() {
        return humidityScore.chartName;
    }

    public ArrayList<ValuePair> getBatteryScore() {
        return batteryScore.getPairs();
    }

    public ValuePair getSingleAccel() {
        return accelScore.value_response;
    }

    public String getBatteryLabel() {
        return batteryScore.chartName;
    }

}
