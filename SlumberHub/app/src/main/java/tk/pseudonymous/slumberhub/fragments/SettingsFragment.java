package tk.pseudonymous.slumberhub.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import tk.pseudonymous.slumberhub.MainActivity;
import tk.pseudonymous.slumberhub.R;
import tk.pseudonymous.slumberhub.accessory.InternalWireless;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;

/**
 * Created by David Smerkous on 11/29/16.
 * Project: SlumberHub
 */

public class SettingsFragment extends Fragment{

    Activity activity;
    public static ListView listView;
    Button refreshWifi, rebootSlumber, lightOff, lightOn;

    public static void getWifiUpdate() {
        JSONObject towrite = new JSONObject();

        try {
            towrite.put("exec", "getWifi");
        } catch (JSONException ignored) {}
        MainActivity.writeString(towrite.toString());
        LogData("PUSHED THIS TO JSON");
    }

    public static void setLightState(boolean state) {
        JSONObject towrite = new JSONObject();

        try {
            towrite.put("exec", (state) ? "lightOn" : "lightOff");
        } catch (JSONException ignored) {
            LogData("Failed setting light state");
        }
        MainActivity.writeString(towrite.toString());
        LogData("Pushed light state of: " + state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout rl = (LinearLayout) inflater.inflate(
                R.layout.settings_fragment, container, false);

        activity = getActivity();

        //setBgColor(rl);

        InternalWireless.init(activity.getApplicationContext());

        rl.setBackgroundColor(Color.parseColor(bgColor));


        refreshWifi = (Button) rl.findViewById(R.id.refresh);

        getWifiUpdate();

        refreshWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            getWifiUpdate();
            }
        });


        rebootSlumber = (Button) rl.findViewById(R.id.reboot_button);

        rebootSlumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject towrite = new JSONObject();

                try {
                    towrite.put("exec", "reboot");
                } catch (JSONException ignored) {}
                MainActivity.writeString(towrite.toString());

                System.exit(0);
            }
        });

        lightOff = (Button) rl.findViewById(R.id.light_off_button);
        lightOn = (Button) rl.findViewById(R.id.light_on_button);

        lightOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLightState(false);
            }
        });

        lightOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLightState(true);
            }
        });

        listView = (ListView) rl.findViewById(R.id.wifiView);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(
                        activity.getApplicationContext());
                // set item background
                openItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                // set item width
                openItem.setWidth(40);
                // set item title
                openItem.setTitle("Open");
                // set item title fontsize
                openItem.setTitleSize(18);
                // set item title font color
                openItem.setTitleColor(Color.WHITE);
                // add to menu
                menu.addMenuItem(openItem);

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        activity.getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(40);
                // set a icon
                deleteItem.setIcon(android.R.drawable.ic_delete);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };

        /*listView.setMenuCreator(creator);

        listView.setCloseInterpolator(new BounceInterpolator());
        listView.setOpenInterpolator(new BounceInterpolator());
        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        // open
                        break;
                    case 1:
                        // delete
                        break;
                }
                // false : close the menu; true : not close the menu
                return false;
            }
        });*/

        return rl;
    }
}
