package com.example.seniordesignproject;

/*
ECE 4800 - Senior Project
David Carlson and Bryce Martin

Inspiration for the design and coding of this android app originate from:
https://app.pluralsight.com/library/courses/android-fundamentals-fragments/table-of-contents
                -- Sriyank Siddhartha
                For his teaching about Fragments and UI management

 https://app.pluralsight.com/library/courses/android-studio-connected-app-building-first/table-of-contents
                -- Simone Alessandria
                For his teaching how to do Async internet/server fetches
 */

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.Iterator;

public class MainPage extends Fragment {
    private static final String TAG = "S.D.A.MainPage";  // Used for debug output

    // Alloc/prep for utilizaton of these views/objects
    public RecyclerView recyclerView;
    public RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    // Get a reference to the main activity (for 'global' data)
    MainActivity mainActivity;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_layout, container, false);
        mainActivity = (MainActivity) getActivity();

        // Once the MainActivity instantiates this fragment, we will pre-set some attributes to some items
        ((ToggleButton) view.findViewById(R.id.sysEnabledToggle)).setEnabled(false);
        ((EditText) view.findViewById(R.id.sysTimeEdit)).setEnabled(false);

        // Inform our Recycler view where it will put/show our data
        recyclerView = (RecyclerView) view.findViewById(R.id.waterQueRecycle);
        recyclerView.setHasFixedSize(true);  // Optional
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        return view;
    }

    public class MyAdapterQue extends RecyclerView.Adapter<MyAdapterQue.ViewHolder> {
        // local variables
        private LayoutInflater mInflater;
        JSONObject jsonObject;

        // data is passed into the constructor
        MyAdapterQue(JSONObject data) {
            this.mInflater = LayoutInflater.from(getContext());
            this.jsonObject = data;
        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.waterque_recycler_rows, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // We pass in our JSONObject, then iterate through it -- for ever item in side it, the
            // recycler view will create sufficient rows to hold our items
            // Ever time this 'onBindViewHolder' is called, the position value in incremented by 1
            JSONObject jsonObjectSubValues;

            Iterator<String> obs = jsonObject.keys();

            String jsonResult = obs.next();  // get our first Xbee KEY out of the dictionary
            String zone = "Unset";


            // Move our JSONObject iterator  position  number of times
            for (int i = 0; i < position; i++) jsonResult = obs.next();  // get the next KEY

            try {
                jsonObjectSubValues = jsonObject.getJSONObject(jsonResult);
                zone = jsonObjectSubValues.getString("Sector");

                // Set the values of the TextViews
                holder.xbeeSector.setText("Zone: " + zone);
            }
            catch (Exception e){
                e.printStackTrace();
                holder.xbeeSector.setText("Nothing In Queue at this time");
            }
        }

        // total number of rows
        @Override
        public int getItemCount() {
            return  jsonObject.length();  // How many keys are in our XbeeJSONObject
        }

        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder/*implements View.OnClickListener*/ {
            TextView xbeeSector;

            ViewHolder(View itemView) {
                super(itemView);
                xbeeSector = itemView.findViewById(R.id.sectorWater);
            }
        }
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Log.d(TAG, "onAttach()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        // 1st, we determine what state the APP is in, and get access to its data
        mainActivity = (MainActivity) getActivity();

        if (mainActivity.HAVEDATA) {
            try {
                // We have data, lets get displaying it!

                // An 'Adapter'
                mAdapter = new MyAdapterQue(mainActivity.WateringQue);
                recyclerView.setAdapter(mAdapter);

                // Local variables for temp storage and calculations
                double avTemp = 0.0;
                int avMoist = 0;
                int avSun = 0;
                double avHum = 0.0;
                double avWind = 0.0;
                int count = 0;
                Iterator<String> keys = mainActivity.SensorStats.keys();
                while (keys.hasNext()) {
                    String key = keys.next();

                    avTemp += Double.parseDouble((((JSONObject) (mainActivity.SensorStats.get(key))).get("Temperature").toString()));
                    avMoist += Integer.parseInt((((JSONObject) (mainActivity.SensorStats.get(key))).get("Moisture").toString()));
                    avSun += Integer.parseInt((((JSONObject) (mainActivity.SensorStats.get(key))).get("Sunlight").toString()));
                    avHum += Double.parseDouble((((JSONObject) (mainActivity.SensorStats.get(key))).get("Humidity").toString()));
                    avWind += Double.parseDouble((((JSONObject) (mainActivity.SensorStats.get(key))).get("Wind").toString()));
                    count++;
                }
                avTemp /= count;
                avMoist /= count;
                avSun /= count;
                avHum /= count;
                avWind /= count;

//                boolean sysEnabled = Boolean.parseBoolean(mainActivity.PiResponses[0]);
                boolean sysEnabled = Boolean.parseBoolean(mainActivity.SystemState.getString("State"));


                // Enable user interface items
                ((ToggleButton) view.findViewById(R.id.sysEnabledToggle)).setEnabled(true);
                ((EditText) view.findViewById(R.id.sysTimeEdit)).setEnabled(true);

                // Set display values
                ((ToggleButton) view.findViewById(R.id.sysEnabledToggle)).setChecked(sysEnabled);
                ((EditText) view.findViewById(R.id.sysTimeEdit)).setText(mainActivity.DateTime.getString("TimeStamp"));
                ((TextView) view.findViewById(R.id.moisture)).setText(String.format("%d", avMoist));
                ((TextView) view.findViewById(R.id.tempurature)).setText(String.format("%.2f", avTemp)+" \u00B0C");
                ((TextView) view.findViewById(R.id.wind)).setText(String.format("%.2f", avWind)+" m/s");
                ((TextView) view.findViewById(R.id.sun)).setText(String.format("%d", avSun));
                ((TextView) view.findViewById(R.id.humid)).setText(String.format("%.2f", avHum)+" RH");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Callable function to update the data displayed by this fragment
    public void updateValues() {

        mainActivity = (MainActivity) getActivity();
        if (mainActivity.manager.findFragmentByTag("MainPage") != null && mainActivity.manager.findFragmentByTag("MainPage").isVisible()) onResume();

    }
}
