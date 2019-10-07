package com.example.seniordesignproject;
/*
ECE 4800 - Senior Project
David Carlson and Bryce Martin

Inspiration for the design and coding of this android app originate from:
https://app.pluralsight.com/library/courses/android-fundamentals-fragments/table-of-contents
                -- Sriyank Siddhartha
                For his teaching about Fragments and UI management

 https://app.pluralsight.com/
                      library/courses/android-studio-connected-app-building-first/table-of-contents
                -- Simone Alessandria
                For his teaching how to do Async internet/server fetches

 https://developer.android.com/guide/topics/ui/layout/recyclerview
 https://stackoverflow.com/questions/26621060/display-a-recyclerview-in-fragment
 https://stackoverflow.com/questions/40584424/simple-android-recyclerview-example
 */
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.Iterator;


// TODO This page is a work in progress...
// This is 90% a copy and amalgamation of StackOverflow and Google Developer documentation
// Still learning how this works.


public class SensorsPage extends Fragment {
    private static final String TAG = "S.D.A.SensorsPage";  // Used for debug output

    // We allocate memory for the RecyclerView -- Scrolling list
    public RecyclerView recyclerView;
    public RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    MainActivity mainActivity;
    private View view;

    // When a Fragment is added to the FragmentManager and a transaction for this fragment is called
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        // This object represents content and data currently viewed on screen
        view = inflater.inflate(R.layout.sensors_layout, container, false);

        mainActivity = (MainActivity) getActivity();

        // Inform our Recycler view where it will put/show our data
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);  // Optional
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // An 'Adapter'
        mAdapter = new MyAdapter(mainActivity.XbeeSensors);
        recyclerView.setAdapter(mAdapter);

        return view;
    }




    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private LayoutInflater mInflater;
//        private ItemClickListener mClickListener;

        // data is passed into the constructor
        JSONObject jsonObject;
        MyAdapter(JSONObject data) {
            this.mInflater = LayoutInflater.from(getContext());
            this.jsonObject = data;
//            this.mData3 = data2;
        }

        // inflates the row layout from xml when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sensors_recycler_rows, parent, false);
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
            String moisture = "Unset";
            String battery = "Unset";
            String sunlight = "Unset";

            // Move our JSONObject iterator  position  number of times
            for (int i = 0; i < position; i++) jsonResult = obs.next();  // get the next KEY

            try {
                jsonObjectSubValues = jsonObject.getJSONObject(jsonResult);
                zone = jsonObjectSubValues.getString("Sector");
                moisture = jsonObjectSubValues.getString("Moisture");
                battery = jsonObjectSubValues.getString("Battery");
                sunlight = jsonObjectSubValues.getString("Sunlight");
                Log.d(TAG, "onBindView(  " + jsonResult + ": Zone=" + jsonObjectSubValues.get("Sector"));


                // Set the values of the TextViews
                holder.xbeeSector.setText("Zone: " + zone);
                holder.xbeeChild.setText("Moisture: " + moisture);
                holder.xbeeChild1.setText("Battery: " + battery);
                holder.xbeeChild2.setText("Sunlight: " + sunlight);
                holder.xbeeChild3.setText("MACAddr: " + jsonResult);

            }
            catch (Exception e){
                e.printStackTrace();
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
            TextView xbeeChild;
            TextView xbeeChild1;
            TextView xbeeChild2;
            TextView xbeeChild3;

            ViewHolder(View itemView) {
                super(itemView);
                xbeeSector = itemView.findViewById(R.id.xbeeZone);
                xbeeChild = itemView.findViewById(R.id.xbeeKeyValue1);
                xbeeChild1 = itemView.findViewById(R.id.xbeeKeyValue2);
                xbeeChild2 = itemView.findViewById(R.id.xbeeKeyValue3);
                xbeeChild3 = itemView.findViewById(R.id.xbeeKeyValue4);
//                itemView.setOnClickListener(this);
            }
//            @Override
//            public void onClick(View view) {
//                if (mClickListener != null) {
//                    mClickListener.onItemClick(view, getAdapterPosition());
//                }
//            }
        }

        // convenience method for getting data at click position
//        String getItem(int id) {
//            return mData.get(id);
//        }

        // allows clicks events to be caught
//        void setClickListener(ItemClickListener itemClickListener) {
//            this.mClickListener = itemClickListener;
//        }

        // parent activity will implement this method to respond to click events
//        public interface ItemClickListener {
//            void onItemClick(View view, int position);
//        }
    }


    // When a Fragment is created, then added to a contentmanager/switcher, this method is called
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        Log.d(TAG, "onAttach()");
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "onResume");

        mainActivity = (MainActivity) getActivity();

        if (mainActivity.HaveData) {
            try {
//                recyclerView.getAdapter().notifyDataSetChanged();
                mAdapter = new MyAdapter(mainActivity.XbeeSensors);
                recyclerView.setAdapter(mAdapter);
//                ((TextView) view.findViewById(R.id.sysEnabled)).setText((mainActivity.PiResponses[0].equals("false")) ? "Disabled" : "Enabled");
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

        if (mainActivity.manager.findFragmentByTag("SensorPage") != null && mainActivity.manager.findFragmentByTag("SensorPage").isVisible()) onResume();
    }


}
