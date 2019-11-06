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


// This is 90% a copy and amalgamation of StackOverflow and Google Developer documentation
// Still learning how this works.


public class SensorsPage extends Fragment {
    private static final String TAG = "S.D.A.SensorsPage";  // Used for debug output

    // We allocate memory for the RecyclerView -- Scrolling list
    public RecyclerView recyclerView;
    public RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    // Local variables/references
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

        // Here we snag a reference to the mainactivity (access to data/globals)
        mainActivity = (MainActivity) getActivity();

        // Inform our Recycler view where it will put/show our data
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);  // Optional
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // An 'Adapter' (we tell the OS/View what data will be displayed by the recycler view
        mAdapter = new MyAdapter(mainActivity.SensorStats);
        recyclerView.setAdapter(mAdapter);

        return view;
    }



    // Here we define how our data will be displayed by our RecyclerView->Adapter
    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        // Local variables
        private LayoutInflater mInflater;
        JSONObject jsonObject;

        // data is passed into the constructor
        MyAdapter(JSONObject data) {
            this.mInflater = LayoutInflater.from(getContext());
            this.jsonObject = data;
        }

        // inflates the row layout defined by xml file, when needed
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sensors_recycler_rows, parent, false);
            return new ViewHolder(view);
        }

        // binds the data to the TextView in each row
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            // We pass in our JSONObject, then iterate through it -- for every item in side it, the
            // recycler view will create sufficient rows to hold our items
            // Ever time this 'onBindViewHolder' is called, the position value in incremented by 1
            // The RecyclerView/Adapter/OS Makes these calls based on the USER scrolling
            // the data shown (and when the data doesn't fit on the screen)
            JSONObject jsonObjectSubValues;

            Iterator<String> obs = jsonObject.keys();

            // ViewHolder Local Variables/References
            String jsonResult = obs.next();  // get our first Xbee KEY out of the dictionary
            String zone = "Unset";
            String health = "Unset";
            String moisture = "Unset";
            String battery = "Unset";
            String sunlight = "Unset";
            String checkin = "Unset";

            // Move our JSONObject iterator  position  number of times
            for (int i = 0; i < position; i++) jsonResult = obs.next();  // get the next KEY

            try {
                // Lets try displaying some data!
                jsonObjectSubValues = jsonObject.getJSONObject(jsonResult);
                zone = jsonObjectSubValues.getString("Sector");
                health = jsonObjectSubValues.getString("Health");
                moisture = jsonObjectSubValues.getString("Moisture");
                battery = jsonObjectSubValues.getString("Battery");
                sunlight = jsonObjectSubValues.getString("Sunlight");
                checkin =  jsonObjectSubValues.getString("Month") + "/" + jsonObjectSubValues.getString("Day") + "/" + jsonObjectSubValues.getString("Year") + " at " + jsonObjectSubValues.getString("Hour") + ":" + jsonObjectSubValues.getString("Minute");
                Log.d(TAG, "onBindView(  " + jsonResult + ": Zone=" + jsonObjectSubValues.get("Sector"));


                // Set the values of the TextViews with our data
                holder.xbeeSector.setText("Zone: " + zone);
                holder.xbeeChild0.setText("Health Status: "+ health);
                holder.xbeeChild1.setText("Moisture: " + moisture);
                holder.xbeeChild2.setText("Battery: " + battery);
                holder.xbeeChild3.setText("Sunlight: " + sunlight);
                holder.xbeeChild4.setText("LastSeen: " + checkin);

            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        // total number of rows
        @Override
        public int getItemCount() {
            // from onCreateView -> mAdapter = new MyAdapter(mainActivity.SensorStats);
            // This returns the size/length of our SensorStats object (which is utilized by the
            // onBindView->position argument/value.
            return  jsonObject.length();  // How many keys are in our XbeeJSONObject
        }


        // Here we define/declare what visual elements are going to be used for this view
        // stores and recycles views as they are scrolled off screen
        public class ViewHolder extends RecyclerView.ViewHolder/*implements View.OnClickListener*/ {
            TextView xbeeSector;
            TextView xbeeChild0;
            TextView xbeeChild1;
            TextView xbeeChild2;
            TextView xbeeChild3;
            TextView xbeeChild4;

            ViewHolder(View itemView) {
                super(itemView);
                xbeeSector = itemView.findViewById(R.id.xbeeZone);
                xbeeChild0 = itemView.findViewById(R.id.xbeeKeyValue0);
                xbeeChild1 = itemView.findViewById(R.id.xbeeKeyValue1);
                xbeeChild2 = itemView.findViewById(R.id.xbeeKeyValue2);
                xbeeChild3 = itemView.findViewById(R.id.xbeeKeyValue3);
                xbeeChild4 = itemView.findViewById(R.id.xbeeKeyValue4);
            }
        }
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

        if (mainActivity.HAVEDATA) {
            try {
                // If we have data, then we can initialize the recyclerView!
                mAdapter = new MyAdapter(mainActivity.SensorStats);
                recyclerView.setAdapter(mAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Callable function to update the data displayed by this fragment
    public void updateValues() {
        mainActivity = (MainActivity) getActivity();
        if (mainActivity.manager.findFragmentByTag("SensorPage") != null && mainActivity.manager.findFragmentByTag("SensorPage").isVisible()) onResume();
    }


}
