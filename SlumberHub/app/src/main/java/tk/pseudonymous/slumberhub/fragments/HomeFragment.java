package tk.pseudonymous.slumberhub.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.media.midi.MidiDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import at.grabner.circleprogress.AnimationState;
import at.grabner.circleprogress.AnimationStateChangedListener;
import at.grabner.circleprogress.CircleProgressView;
import lumenghz.com.pullrefresh.PullToRefreshView;
import tk.pseudonymous.slumberhub.MainActivity;
import tk.pseudonymous.slumberhub.R;
import tk.pseudonymous.slumberhub.accessory.SpeechRecognition;

import static tk.pseudonymous.slumberhub.MainActivity.LogData;
import static tk.pseudonymous.slumberhub.MainActivity.barColor;
import static tk.pseudonymous.slumberhub.MainActivity.barTextColor;
import static tk.pseudonymous.slumberhub.MainActivity.barUnitColor;
import static tk.pseudonymous.slumberhub.MainActivity.bgColor;
import static tk.pseudonymous.slumberhub.MainActivity.rimColor;
import static tk.pseudonymous.slumberhub.fragments.LastNightFragment.requestData;

/**
 * Created by David Smerkous on 11/29/16.
 * Project: SlumberHub
 */

public class HomeFragment extends Fragment {

    Activity activity;
    LinearLayout layout;
    public static CircleProgressView sleepBar;
    public static boolean started = false;



    /*public void setBgColor(Activity activity) {
        RelativeLayout relativeLayout = (RelativeLayout) activity.findViewById(R.id.main_relative);
        if(relativeLayout != null)
        relativeLayout.setBackgroundColor(Color.parseColor(bgColor));
        else LogData("Failed setting bg color");
    }*/

    public void initializeCenterBar() {
        sleepBar.calcTextColor();
        sleepBar.setAutoTextSize(true);
        sleepBar.setTextColorAuto(true);
        sleepBar.setBarColor(Color.parseColor(barColor));
        sleepBar.setTextColor(Color.parseColor(barTextColor));
        sleepBar.setUnitColor(Color.parseColor(barUnitColor));
        sleepBar.setUnitVisible(false);
        sleepBar.setClickable(false);
        sleepBar.setRimColor(Color.parseColor(rimColor));
        sleepBar.setValueAnimated(90);
        sleepBar.setLongClickable(false);
        sleepBar.setSeekModeEnabled(false);
        sleepBar.invalidate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout rl = (RelativeLayout) inflater.inflate(
                R.layout.home_fragment, container, false);

        SpeechRecognition.changeActivity = rl;
        SpeechRecognition.runActivity = getActivity();

        activity = getActivity();

        //setBgColor(rl);

        rl.setBackgroundColor(Color.parseColor(bgColor));

        sleepBar = (CircleProgressView) rl.findViewById(R.id.sleep_progress);

        initializeCenterBar();

        sleepBar.setClickable(false);

        if(!started) {
            sleepBar.setValue(0);
            sleepBar.setValueAnimated(100.0f);
            sleepBar.setOnAnimationStateChangedListener(new AnimationStateChangedListener() {
                @Override
                public void onAnimationStateChanged(AnimationState _animationState) {
                    if (sleepBar.getCurrentValue() > 90f) {
                        sleepBar.setOnAnimationStateChangedListener(null);
                        sleepBar.setValueAnimated(0.0f);
                    }
                }
            });
            started = true;
        }

        return rl;
    }
}
