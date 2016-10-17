package com.pseudonymous.appmea.graphics;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.util.Pair;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.pseudonymous.appmea.MainActivity;
import com.pseudonymous.appmea.R;
import com.pseudonymous.appmea.dataparse.ChartConfig;
import com.rey.material.widget.FrameLayout;
import com.rey.material.widget.ListView;
import com.rey.material.widget.TextView;

import java.util.ArrayList;

/**
 * Created by David Smerkous on 10/15/16.
 *
 */

@SuppressWarnings("deprecation")
public class ViewListAdapter extends ArrayAdapter<Pair<TypeData, Object>> {

    private ArrayList<Pair<TypeData, Object>> items;
    private Context context;
    private Activity activity;
    private LayoutInflater vi;

    public ViewListAdapter(Context context, Activity activity, ArrayList<Pair<TypeData, Object>> views) {
        super(context, 0, views);
        this.context = context;
        this.activity = activity;
        this.items = views;
        vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void addItem(final Pair<TypeData, Object> item) {
        items.add(item);
        notifyDataSetChanged();
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = listView.getPaddingTop() + listView.getPaddingBottom();
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            if (listItem instanceof ViewGroup) {
                listItem.setLayoutParams(new AppBarLayout.LayoutParams(
                        AppBarLayout.LayoutParams.WRAP_CONTENT,
                        AppBarLayout.LayoutParams.WRAP_CONTENT));
            }
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Pair<TypeData, Object> getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private ArrayList<View> getAllViews() {
        int amount = this.getCount();

        ArrayList<View> toRet = new ArrayList<>();

        for(int ind = 0; ind < amount; ind++) {
            View currentView = getView(ind, null, new ViewGroup(context) {
                @Override
                protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
                    //Place holder
                    //For the view selection
                }});

            //No need to check for null since annotation was provided
            toRet.add(currentView);
        }

        return toRet;
    }

    private void hideAllViews() {
        ArrayList<View> allViews = this.getAllViews();

        for(View currentView : allViews) {
            try {
                currentView.setVisibility(View.GONE);
                currentView.setMinimumHeight(0);
                currentView.setPadding(0, 0, 0, 0);
                currentView.setFocusable(false);
                currentView.setClickable(false);
            } catch (Throwable err) {
                err.printStackTrace();
                MainActivity.LogData("FAILED HIDING VIEW", true);
            }
        }
    }

    public void showAllViews() {
        ArrayList<View> allViews = this.getAllViews();

        for(View currentView : allViews) {
            try {
                currentView.setVisibility(View.VISIBLE);
                currentView.setMinimumHeight(10);
                currentView.setPadding(0, 10, 0, 10);
                currentView.setFocusable(true);
                currentView.setClickable(true);
            } catch (Throwable err) {
                err.printStackTrace();
                MainActivity.LogData("FAILED SHOWING VIEW", true);
            }
        }
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View cv = convertView;

        Pair<TypeData, Object> typeSet = getItem(position);

        if(typeSet == null) {
            MainActivity.LogData("FAILED TO DETERMINE TYPESET ON: " + position, true);
            return cv;
        }

        final TypeData ct = typeSet.first;
        Object fv = typeSet.second;

        MainActivity.LogData("TYPE " + ct.getType() + "\nPos: " + position);

        if(fv != null) {
            if(ct.isTitleType()) {
                MainActivity.LogData("ADDING SECTION TITLE TO LIST");
                cv = vi.inflate(R.layout.activity_data_fragment_section_header, null);

                cv.setOnClickListener(null);
                cv.setOnLongClickListener(null);
                cv.setLongClickable(false);
                cv.setClickable(false);

                final TextView sectionTitle = (TextView) cv.findViewById(R.id.title_list_text);
                sectionTitle.setText((String) fv);
            } else if(ct.isTextType()) {
                MainActivity.LogData("ADDING TEXT DESCRIPTION TO LIST");
                cv = vi.inflate(R.layout.activity_data_fragment_section_text, null);

                cv.setClickable(true);
                cv.getRootView().setFocusableInTouchMode(true);
                cv.getRootView().requestFocus();

                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity.LogData("Text was clicked on");
                    }
                });

                //final TextView descriptionTitle = (TextView) cv.findViewById(R.id.text_title_desc);
                //final TextView descroptionBody = (TextView) cv.findViewById(R.id.text_main_desc);

                final WebView descriptionTitle = (WebView) cv.findViewById(R.id.text_title_desc);
                final WebView descriptionBody = (WebView) cv.findViewById(R.id.text_main_desc);

                descriptionTitle.setFocusable(false);
                descriptionTitle.setClickable(false);

                descriptionBody.setFocusable(false);
                descriptionBody.setClickable(false);


                Object stuffAdd[] = (Object[]) fv;

                descriptionTitle.loadData((String) stuffAdd[0], "text/html", null);
                descriptionBody.loadData((String) stuffAdd[1], "text/html", null);

                descriptionTitle.getSettings();
                descriptionTitle.setBackgroundColor(Color.TRANSPARENT);

                descriptionBody.getSettings();
                descriptionBody.setBackgroundColor(Color.TRANSPARENT);

                //descriptionTitle.setText(Html.fromHtml((String) stuffAdd[0]));
                //descroptionBody.setText(Html.fromHtml((String) stuffAdd[1]));
            } else if(ct.isChartType()) {

                final ChartConfig chart = (ChartConfig) fv;

                MainActivity.LogData("ADDING CHART TO LIST");
                cv = vi.inflate(R.layout.activity_data_fragment_section_chart, null);

                final LinearLayout ll = (LinearLayout) cv.findViewById(R.id.chart_container_section);

                try {
                    ll.removeAllViewsInLayout();
                    ll.removeView(chart);
                } catch (Throwable ignored) {
                    MainActivity.LogData("FAILED REMOVING CHART", true);
                }


                //Remove chart if it already exists
                if(chart.getParent() != null) ((ViewManager) chart.getParent()).removeView(chart);

                MainActivity.LogData("Child added");
                chart.setTouchable(false);
                chart.setFocusable(false);
                chart.setPadding(0,0,0,0);
                chart.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
                chart.setNoDataText("HEY BABE");
                ll.addView(chart); //Add the chart
                chart.setVisibility(View.VISIBLE);

                chart.invalidate(); //Update UI

                final View updateView = cv;
                final Activity activityT = this.activity;
                cv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity.LogData("Chart widget clicked on");

                        view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.FILL_PARENT,
                                AbsListView.LayoutParams.FILL_PARENT));

                        ViewParent viewParent = view.getParent();

                        if(viewParent != null) {
                            ViewGroup viewGroup = (ViewGroup) viewParent;


                            //Reason for doing two parents since we have the
                            //Pull to refresh view we have as our parent
                            View current = (View) viewGroup.getParent().getParent();

                            if(current == null) {
                                MainActivity.LogData("FAILED POPPING UP WIDGET, VIEW IS NULL",
                                        true);
                                return;
                            }

                            current.setLayoutParams(new FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.FILL_PARENT,
                                    FrameLayout.LayoutParams.FILL_PARENT));

                            final LinearLayout linearLayout = new LinearLayout(activityT);
                            linearLayout.setGravity(Gravity.CENTER);
                            linearLayout.setPadding(1, 1, 1, 1);
                            linearLayout.setVerticalScrollBarEnabled(false);
                            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.FILL_PARENT));


                            ViewManager parentView = (ViewManager) chart.getParent();

                            if(parentView != null)
                                parentView.removeView(chart);

                            linearLayout.addView(chart);

                            linearLayout.getRootView().setFocusableInTouchMode(true);
                            linearLayout.getRootView().requestFocus();

                            //Save current view to revert to later
                            //This is kind of like a temporary cache
                            //To save the total view and reload on the back button
                            final ViewGroup viewGroups = (ViewGroup) ((ViewGroup) activityT
                                    .findViewById(android.R.id.content)).getChildAt(0);

                            linearLayout.getRootView().setOnKeyListener(new View.OnKeyListener() {
                                @Override
                                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                                    if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                                            && i == KeyEvent.KEYCODE_BACK
                                            && keyEvent.getRepeatCount() == 0) {
                                        MainActivity.LogData("Back key pressed");
                                        linearLayout.startAnimation(AnimationUtils.loadAnimation(activityT,
                                                R.anim.zoom_out));
                                        viewGroups.startAnimation(AnimationUtils.loadAnimation(activityT,
                                                R.anim.zoom_in));
                                        activityT.setContentView(viewGroups); //Set original view

                                        chart.setClickable(false);
                                        chart.setTouchable(false);

                                        viewGroups.invalidate();
                                        return true;
                                    }
                                    return false;
                                }
                            });


                            chart.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.FILL_PARENT,
                                    LinearLayout.LayoutParams.FILL_PARENT));

                            chart.invalidate();

                            linearLayout.startAnimation(AnimationUtils.loadAnimation(activityT,
                                    R.anim.zoom_in));
                            viewGroups.startAnimation(AnimationUtils.loadAnimation(activityT,
                                    R.anim.zoom_out));

                            chart.setClickable(true);
                            chart.setFocusable(true);
                            chart.setTouchable(true);
                            chart.setVisibility(View.VISIBLE);

                            activityT.setContentView(linearLayout);
                        }

                        chart.invalidate();
                        updateView.invalidate();
                        view.invalidate();
                    }
                });

              //  ChartConfig chartConfig = (ChartConfig)
                        //cv.findViewById(R.id.chart_data_section);

                //chartConfig.setPadding(3, 5, 3, 5);
                //((ChartConfig) fv)
            }
        } else {
            MainActivity.LogData("FAILED LOADING ITEM: " + String.valueOf(position) + " To list",
                    true);
        }

        return cv;
    }
}