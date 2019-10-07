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
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;

public class MainPage extends Fragment {
    private static final String TAG = "S.D.A.MainPage";  // Used for debug output

    MainActivity mainActivity;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.main_layout, container, false);
        mainActivity = (MainActivity) getActivity();
        return view;
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        Log.d(TAG, "onAttach()");
        //if (mainActivity.HaveData) updateValues();
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

        mainActivity = (MainActivity) getActivity();

        if (mainActivity.HaveData) {
            try {
                ((TextView) view.findViewById(R.id.sysEnabled)).setText((mainActivity.PiResponses[0].equals("false")) ? "Disabled" : "Enabled");
//            ((TextView) view.findViewById(R.id.sysEnabled)).setText((mainActivity.PiResponses[0].equals("false")) ? "Disabled" : "Enabled");
//            ((TextView) mainActivity.findViewById(R.id.moisture)).setText(((JSONArray) mainActivity.Thresholds.get("Moisture")).getInt(0));
//            ((TextView) mainActivity.findViewById(R.id.tempurature)).setText(((JSONArray) mainActivity.Thresholds.get("Temperature")).getInt(0));
//            ((TextView) mainActivity.findViewById(R.id.wind)).setText(((JSONArray) mainActivity.Thresholds.get("Wind")).getInt(0));
//            ((TextView) mainActivity.findViewById(R.id.rain)).setText(((JSONArray) mainActivity.Thresholds.get("Rain")).getInt(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateValues() {

        mainActivity = (MainActivity) getActivity();
        if (mainActivity.manager.findFragmentByTag("MainPage") != null && mainActivity.manager.findFragmentByTag("MainPage").isVisible()) onResume();

    }

    // TODO Create a toggle/switch for posting if the system is Enabled or not

}
