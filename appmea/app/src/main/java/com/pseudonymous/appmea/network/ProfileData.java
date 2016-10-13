package com.pseudonymous.appmea.network;

import com.pseudonymous.appmea.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Smerkous on 10/12/16.
 *
 */

public class ProfileData {
    private String deviceName = "AppMeA device";
    private String firstName = "Demo";
    private String lastName = "User";
    private boolean sex = false; //false = male, true = female
    private short age = 16;
    private String profileImgUrl = "https://media.licdn.com/mpr/mpr/shrinknp_200_200/" +
            "AAEAAQAAAAAAAAltAAAAJGJkMTFkZjY3LTEwMjktNDk4Yy04Zjg5LWJkZDlhZThkMzQ1NQ.jpg";

    private String bgImgUrl = "http://mwhd.altervista.org/wp_upload/wallpapers/material/" +
            "Rainbow_Material_Dark-Qwen_Lee.png";

    public void setDeviceName(String toset) {
        this.deviceName = toset;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setFirstName(String toset) {
        this.firstName = toset;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setLastName(String toset) {
        this.lastName = toset;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setAge(short newAge) {
        this.age = newAge;
    }

    public short getAge() {
        return this.age;
    }

    public void setProfileImg(String url) {
        this.profileImgUrl = url;
    }

    public String getProfileImg() {
        return this.profileImgUrl;
    }

    public void setBgImg(String url) {
        this.bgImgUrl = url;
    }

    public String getBgImg() {
        return this.bgImgUrl;
    }

    public void setSex(boolean toSet) {
        this.sex = toSet;
    }

    public boolean getSex() {
        return this.sex;
    }

    public void pullDetails(final ResponseListener completed) {
        final ProfileData sendUpdates = this;
        ResponseListener detailsUpdate = new ResponseListener() {
            @Override
            public void on_complete(CommonResponse req) {
                MainActivity.LogData("GOT USER DETAIL REQUEST");
                JSONObject toResp = req.parent;

                CommonResponse c_resp = new CommonResponse();

                try {
                    sendUpdates.setFirstName(toResp.getString("firstName"));
                    sendUpdates.setLastName(toResp.getString("lastName"));
                    sendUpdates.setAge((short) toResp.getInt("age"));
                    sendUpdates.setProfileImg(toResp.getString("profileImg"));
                    sendUpdates.setBgImg(toResp.getString("bgImg"));
                    sendUpdates.setSex(toResp.getBoolean("sex"));

                    ValuePair obj_compile = new ValuePair();
                    obj_compile.setIdV(0);
                    obj_compile.setValue(sendUpdates);

                    c_resp.value_response = obj_compile;
                    completed.on_complete(c_resp);

                } catch (JSONException ignored) {
                    completed.on_fail(c_resp);
                    MainActivity.LogData("FAILED PARSING PROFILE DETAILS", true);
                }
            }

            @Override
            public void on_fail(CommonResponse req) {

            }
        };

        CommonNetwork.getMetaData(detailsUpdate);
    }

    public void updateDetails() {
        ResponseListener detailsUpdate = new ResponseListener() {
            @Override
            public void on_complete(CommonResponse req) {
                MainActivity.LogData("UPDATED USER DETAILS SUCCESSFULLY!");
            }

            @Override
            public void on_fail(CommonResponse req) {

            }
        };

        try {
            JSONObject compiled = new JSONObject();
            compiled.put("firstName", this.getFirstName());
            compiled.put("lastName", this.getLastName());
            compiled.put("age", this.getAge());
            compiled.put("profileImg", this.getProfileImg());
            compiled.put("bgImg", this.getBgImg());
            compiled.put("sex", this.getSex());

            CommonNetwork.setMetaData(this.getDeviceName(), compiled, detailsUpdate);
        } catch (JSONException error) {
            MainActivity.LogData("FAILED UPDATING USER DETAILS");
        }
    }

}
