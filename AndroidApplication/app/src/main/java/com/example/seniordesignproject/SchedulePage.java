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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

public class SchedulePage extends Fragment {
    private static final String TAG = "S.D.A.SchedulePage";  // Used for debug output

    MainActivity mainActivity;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.schedule_layout, container, false);
        mainActivity = (MainActivity) getActivity();
        return view;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Log.d(TAG, "onAttach()");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        mainActivity = (MainActivity) getActivity();

        if (mainActivity.HAVEDATA) {
            try{
                // Thresholds
                int dryTrig = mainActivity.Thresholds.getInt("Dry");
                int windTrig = mainActivity.Thresholds.getInt("Wind max");
                int humidStop = mainActivity.Thresholds.getInt("Humidity max");
                int tempStop = mainActivity.Thresholds.getInt("Temperature min");
                int waterDur = mainActivity.Thresholds.getInt("Water Duration");
                int timeStart = mainActivity.Thresholds.getInt("Prohibited time start");
                int timeStop = mainActivity.Thresholds.getInt("Prohibited time end");

                // TimerTriggering
                Log.d(TAG, "onResume: " + timeStop);
                boolean sundayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Sunday"))).get(0).toString());
                boolean mondayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Monday"))).get(0).toString());
                boolean tuesdayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Tuesday"))).get(0).toString());
                boolean wednesdayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Wednesday"))).get(0).toString());
                boolean thursdayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Thursday"))).get(0).toString());
                boolean fridayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Friday"))).get(0).toString());
                boolean saturdayEnable = Boolean.parseBoolean(((JSONArray) (mainActivity.TimerTriggering.get("Saturday"))).get(0).toString());

                int sundayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Sunday"))).get(1).toString());
                int mondayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Monday"))).get(1).toString());
                int tuesdayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Tuesday"))).get(1).toString());
                int wednesdayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Wednesday"))).get(1).toString());
                int thursdayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Thursday"))).get(1).toString());
                int fridayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Friday"))).get(1).toString());
                int saturdayStart = Integer.parseInt(((JSONArray) (mainActivity.TimerTriggering.get("Saturday"))).get(1).toString());

                // Set Display Values
                // set trigger thresholds
                ((EditText) view.findViewById(R.id.dryTrgEdit)).setText(String.format("%d", dryTrig));
                ((EditText) view.findViewById(R.id.windTrgEdit)).setText(String.format("%d", windTrig));
                ((EditText) view.findViewById(R.id.humidTrgEdit)).setText(String.format("%d", humidStop));
                ((EditText) view.findViewById(R.id.tempTrgEdit)).setText(String.format("%d", tempStop));
                ((EditText) view.findViewById(R.id.waterDurEdit)).setText(String.format("%d", waterDur));
                ((EditText) view.findViewById(R.id.prohibStartEdit)).setText(String.format("%d", timeStart));
                ((EditText) view.findViewById(R.id.prohibEndEdit)).setText(String.format("%d", timeStop));
                // set schedule times
                ((EditText) view.findViewById(R.id.waterSchedSunEdit)).setText(String.format("%d", sundayStart));
                ((EditText) view.findViewById(R.id.waterSchedMonEdit)).setText(String.format("%d", mondayStart));
                ((EditText) view.findViewById(R.id.waterSchedTueEdit)).setText(String.format("%d", tuesdayStart));
                ((EditText) view.findViewById(R.id.waterSchedWedEdit)).setText(String.format("%d", wednesdayStart));
                ((EditText) view.findViewById(R.id.waterSchedThuEdit)).setText(String.format("%d", thursdayStart));
                ((EditText) view.findViewById(R.id.waterSchedFriEdit)).setText(String.format("%d", fridayStart));
                ((EditText) view.findViewById(R.id.waterSchedSatEdit)).setText(String.format("%d", saturdayStart));

                ((ToggleButton) view.findViewById(R.id.waterSchedSundayToggle)).setChecked(sundayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedMondayToggle)).setChecked(mondayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedTuesdayToggle)).setChecked(tuesdayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedWednesdayToggle)).setChecked(wednesdayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedThursdayToggle)).setChecked(thursdayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedFridayToggle)).setChecked(fridayEnable);
                ((ToggleButton) view.findViewById(R.id.waterSchedSaturdayToggle)).setChecked(saturdayEnable);



            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateValues() {
        mainActivity = (MainActivity) getActivity();
        if (mainActivity.manager.findFragmentByTag("SchedulePage") != null && mainActivity.manager.findFragmentByTag("SchedulePage").isVisible()) onResume();
    }

    // TODO Create check boxes and text input areas for future data POSTing to Pi
}
