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

 https://stackoverflow.com/
                    questions/12069669/how-can-you-pass-multiple-primitive-parameters-to-asynctask

https://www.youtube.com/watch?v=bNpWGI_hGGg
                    For showing an implementation for TabLayout
 */

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URL;


// Main Activity embodies the entire application.
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "S.D.A.MainActivity";  // Used for debug output

    // We allocate memory for each of our FragmentViews
    // (well, inform... the compiler what data objects will later be used)
    SensorsPage sensors;
    SchedulePage schedule;
    MainPage mainPage;
    FragmentManager manager;
    FragmentTransaction transaction;
    public TabLayout tabLayout;

    // Regex string for DateTime entry validation
    String patternSource = "(Sun|Mon|Tue|Wed|Thu|Fri|Sat) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)  \\d{1,2} \\d{1,2}:\\d{1,2}:\\d{1,2} \\d{4}";

    // Here are two static addresses to the RPi
    private String[] URL = {"http://192.168.1.104:8008",
            "http://seniorproject.figureix.com:8008",
            "http://10.0.2.2:8008"
    };
    // And the available file paths on the Pi
    private String[] PATHS = {"/SystemEnabled/",
            "/TimerTriggering/",
            "/Thresholds/",
            "/SensorStats/",
            "/DateTime/",
            "/WateringQue/"
    };


    // Our Async Task will save variable into this array
    // [0] <- TimerControl/State
    // [1] <- TimerControl/TimerTriggering
    // [2] <- TimerControl/Thresholds
    // [3] <- Xbee3/Dump
    public String[] PiResponses = {"", "", "", "", "", ""};
    public JSONObject SystemState;
    public JSONObject TimerTriggering;
    public JSONObject Thresholds;
    public JSONObject SensorStats;
    public String DateTime;
    public JSONObject WateringQue;


    // Global-like variable: If this is true, we have connected to, and pulled data from the RPi
    public boolean HAVEDATA = false;


    // When the Android system allocates time for the APP to run, this function
    // is called once the app is loaded and ready for execution.
    // Think of this as  main()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize the fragments and transaction manager.
        // We are essentially creating groups of elements that 'start and die' at the
        // command of our Fragment-->Transaction manager.
        // We have to create all our fragments, else any data elements
        // linked to a non-instantiated fragment will essentially
        // be 'null referenced' (The data object/container won't exist)

        // 1st Instantiate/Fill our Fragment Objects previously declared.
        sensors = new SensorsPage();
        schedule = new SchedulePage();
        mainPage = new MainPage();

        // 2nd Place our fragments in the Fragment Manager (AndroidOS entity)
        manager = getSupportFragmentManager();
        transaction = manager.beginTransaction();
        transaction.add(R.id.frameWindow, mainPage, "MainPage");
        transaction.show(mainPage);
        transaction.commit();  // Think of this as "Update the GUI'


        // Here we associate the TAB GUI to the 'under lying code' here
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.getTabAt(1).select();

        // https://stackoverflow.com/questions/33646586/tablayout-without-using-viewpager
        // Here we create a listener object that catches TAB Button input
        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {

                    // When a user selects a TAB on the screen
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        // So long as we HAVE DATA we can switch views.
                        if(!HAVEDATA) tabLayout.getTabAt(1).select();
                        if(tabLayout.getSelectedTabPosition() == 0) SensorsButton(tabLayout.getRootView());
                        if(tabLayout.getSelectedTabPosition() == 1) MainButton(tabLayout.getRootView());
                        if(tabLayout.getSelectedTabPosition() == 2) TriggersButton(tabLayout.getRootView());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {}

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {}
                }
        );
    }



    // This function is linked to a GUI Button: When pressed this runs
    public void MainButton(View view) {
        if (!HAVEDATA) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        // We should now have data from the RPi
        // If our triggers view hasn't been created: Create it. Move view to front/visible
        transaction = manager.beginTransaction();

        // Does view/fragment exist?
        if (manager.findFragmentByTag("MainPage") == null) {
            transaction.add(R.id.frameWindow, mainPage, "MainPage");
        }
        // Is it covered by another view?
        if (manager.findFragmentByTag("SchedulePage") != null && manager.findFragmentByTag("SchedulePage").isVisible()){
            transaction.hide(schedule);
        }
        if (manager.findFragmentByTag("SensorPage") != null && manager.findFragmentByTag("SensorPage").isVisible()) {
            transaction.hide(sensors);
        }

        // Place view/fragment to front, and show it
        transaction.replace(R.id.frameWindow, mainPage, "MainPage");
        transaction.show(mainPage);
        transaction.commit();
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void SensorsButton(View view) {
        if (!HAVEDATA) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        // We should now have data from the RPi
        // If our triggers view hasn't been created: Create it. Move view to front/visible
        transaction = manager.beginTransaction();

        // Does view/fragment exist?
        if (manager.findFragmentByTag("SensorPage") == null) {
            transaction.add(R.id.frameWindow, sensors, "SensorPage");
        }
        // Is it covered by another view?
        if (manager.findFragmentByTag("SchedulePage") != null && manager.findFragmentByTag("SchedulePage").isVisible()){
            transaction.hide(schedule);
        }
        if (manager.findFragmentByTag("MainPage") != null && manager.findFragmentByTag("MainPage").isVisible()) {
            transaction.hide(mainPage);
        }

        // Place view/fragment to front, and show it
        transaction.replace(R.id.frameWindow, sensors, "SensorPage");
        transaction.show(sensors);
        transaction.commit();
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void TriggersButton(View view) {
        if (!HAVEDATA) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        // We should now have data from the RPi
        // If our triggers view hasn't been created: Create it. Move view to front/visible
        transaction = manager.beginTransaction();

        // Does view/fragment exist?
        if (manager.findFragmentByTag("SchedulePage") == null) {
            transaction.add(R.id.frameWindow, schedule, "SchedulePage");
        }
        // Is it covered by another view?
        if (manager.findFragmentByTag("SensorPage") != null && manager.findFragmentByTag("SensorPage").isVisible()){
            transaction.hide(sensors);
        }
        if (manager.findFragmentByTag("MainPage") != null && manager.findFragmentByTag("MainPage").isVisible()) {
            transaction.hide(mainPage);
        }

        // Place view/fragment to front, and show it
        transaction.replace(R.id.frameWindow, schedule, "SchedulePage");
        transaction.show(schedule);
        transaction.commit();
    }



    // PiQuery is our 'custom/tailored' task to fetch a web page
    public class PiQuery extends AsyncTask<URL, Void, String> {
        // This class defines a Thread process.
        // It takes a URL, then sends it to our ServerConnect function (which runs on the thread)

        // Local Variable
        int saveIndex;

        // Everytime we call this task, we will be passing a different URL/Path, which
        // will return separate data
        // So, we will be passing an index to the AsyncTask,
        // to allow it to save information into an array correctly
        PiQuery(int index) {
            this.saveIndex = index;
        }

        // Provide visual feedback that the app is running/working via loading circle
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Before the ServerConnect begins, we will create a loading bar for the User to see.
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        // Do in background: We are attempting to contact the RPi server
        // and get the data from it -- We don't know how long it could take.
        @Override
        protected String doInBackground(URL... urls) { // this is a Library/API specific name/function
            URL searchURL = urls[0];
            String result = null;

            // Try connecting to the PiServer using the passed in URL, and return the payload from it.
            try {
                result = ServerConnect.getJson(searchURL);
            } catch (Exception e) {
                String msg = e.getMessage().toString();
                Log.d(TAG, "ASYNC Exception: " + msg);
            }
            Log.d(TAG, "ASYNC URLResult: " + result);
            return result;
        }

        // On thread/task finish, return the message body as text -- Save it, and show it to the User
        @Override
        protected void onPostExecute(String result) {
            TextView connection = (TextView) findViewById(R.id.connectStatus);
            progressBar.setVisibility(ProgressBar.INVISIBLE);

            // If a server response is bad/empty, then 'break fast'
            if (result == null | result.equals("null")) {
                HAVEDATA = false;
                connection.setText("Error");
                return;
            }

            // There is a risk that one of the 1st three URL requests fail, but this still works...
            // For each AsyncTask that runs, save its return value into our array
            PiResponses[saveIndex] = result;

            // If each AsyncTask has completed, except the last one, then the
            // last one will do data processing
            if (PiResponses[5] != "") {
                connection.setText("Success");
                // Allow fragments to fetch new data
                HAVEDATA = true;

                // Save our responses into data-objects that we can use
                try {
                    SystemState = new JSONObject("{'State': "+ PiResponses[0] + "}");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    TimerTriggering = new JSONObject(PiResponses[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    Thresholds = new JSONObject(PiResponses[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    SensorStats = new JSONObject(PiResponses[3]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    // A bit redundant, Saving a String to a String
                    PiResponses[4] = PiResponses[4].replace("\"", "");
                    DateTime = PiResponses[4];
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (PiResponses[5].isEmpty()) Log.d(TAG, "PiResponse[5] is EMPTY");
                    WateringQue = new JSONObject(PiResponses[5]);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                // Update Values in Fragments (Notify of new data)
                if (manager.findFragmentByTag("MainPage") != null) mainPage.updateValues();
                if (manager.findFragmentByTag("SensorPage") != null) sensors.updateValues();
                if (manager.findFragmentByTag("SchedulePage") != null) schedule.updateValues();
            }
        }
    }

    // PiQuery is our 'custom/tailored' task to fetch a web page
    public class PiPush extends AsyncTask<URL, Void, String> {
        // This class defines a Thread process.
        // It takes a URL, then sends it to our ServerConnect function (which runs on the thread)

        // Local Variable
        String payload;

        // Stackoverflow passing parameters -- via constructor
        // We will be transmitting the passed in String to the RPi
        PiPush(String payload) {this.payload = payload;}

        // Provide visual feedback that the app is running/working via loading circle
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Before the ServerConnect begins, we will create a loading bar for the User to see.
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        // Do in background: We are attempting to contact the RPi server
        // and get the data from it -- We don't know how long it could take.
        @Override
        protected String doInBackground(URL... urls) { // this is a Library/API specific name/function
            URL searchURL = urls[0];
            String result = null;

            // Try connecting to the PiServer using the passed in URL, and return the payload from it.
            try {
                ServerConnect.postJson(searchURL, payload);
                result = "Pass";
            } catch (Exception e) {
                result = "Fail";
                Log.d(TAG, "ASYNC Exception: " + e.getMessage());
            }
            Log.d(TAG, "ASYNC URLResult: " + result);
            return result;
        }

        // On thread/task finish, return the message body as text -- Save it, and show it to the User
        @Override
        protected void onPostExecute(String result) {
//            TextView connection = (TextView) findViewById(R.id.connectStatus);
            progressBar.setVisibility(ProgressBar.INVISIBLE);

            // If a server response is bad/empty, then 'break fast'
            if (result == null || result.equals("Fail")) {
                ToastMessage("Error sending data", "Long");
                Log.d(TAG, "onPostPush: ERROR" + payload);
                return;
            }
            ToastMessage("Sending data Success", "Short");
        }
    }


    // This Function Attempts to initiate the ServerConnect function, to get data off the RPi
//    public void UpdateData(){
    public void UpdateButtonFetch(View view)   {
        // Here is the start of our code: We want to try accessing the RPi webserver
        // We use the URL java utility to build a URL, then we send it over
        // to a thread to run --> AsyncTask (PiQuery)

        // #TODO Scan network for server, then use it/that
        // https://developer.android.com/reference/java/net/DatagramSocket
        // https://developer.android.com/reference/java/net/DatagramPacket
        // https://developer.android.com/reference/java/net/DatagramSocketImpl



        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Check if we Have an IP Address
        String CurrentGatewayAddress = getGatewayAddress();
        if (CurrentGatewayAddress.equals("0.0.0.0")) {
            Log.d("UpdateData", "No Gateway!");
            ToastMessage("Not connected to Network", "Long");
            ((TextView) findViewById(R.id.connectStatus)).setText("Error");
            return;
        }

        //https://stackoverflow.com/questions/3905358/how-to-ping-external-ip-from-java-android
        // This code allows us to run an Android OS program -- In this case, PING
        Runtime runtime = Runtime.getRuntime();
        try {
            int retVal = 1;

//            if (CurrentGatewayAddress.equals("192.168.1.1")) retVal = 0;
//            if (CurrentGatewayAddress.equals("192.168.232.1")) {
//                progressBar.setVisibility(ProgressBar.VISIBLE);
//                Process pingAddr = runtime.exec("/system/bin/ping -c 1 192.168.1.104");
//                retVal = pingAddr.waitFor();  //retVal = 1 on fail, 0 on success
//                progressBar.setVisibility(ProgressBar.INVISIBLE);
//                Log.d(TAG, "UpdateData: Ping result=" + retVal);
//                if (retVal == 1) retVal = 2;
//            }
            retVal = 2;

            for (int i = 0; i < PATHS.length; i++) {
                try {
                    // Request data from the RPi server, using the LocalLAN IP Address
                    URL webUrl = ServerConnect.buildURL(URL[retVal] + PATHS[i]);
                    new PiQuery(i).execute(webUrl);
                } catch (Exception e) {
                    Log.d("UpdateData", "We have Try Error");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // We have a button to fetch information from the RPi, now we need to post data/changes to it.
    public void UpdateButtonPush(View view) {
        if (!HAVEDATA) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        boolean thresholdsChanged = false;
        boolean timerchanged = false;

        Log.d(TAG, "UpdatePush_______");
        // Check if anything has changed


        ////////////////////////////////////////////////////////// SYSTEM CONTROL ///////////////////////////////////////////////////////////////////////
        // System Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String sysToggle = ((ToggleButton) mainPage.getView().findViewById(R.id.sysEnabledToggle)).getText().toString();
            String sysToggleFromFetch = (Boolean.parseBoolean(SystemState.getString("State"))) ? "Enabled" : "Disabled";
            if (!(sysToggle.equals(sysToggleFromFetch))) {
                URL webUrl = ServerConnect.buildURL(URL[2] + PATHS[0]); // SystemEnabled
                SystemState.put("State", sysToggle.equals("Enabled") ? "True" : "False");
                new PiPush(SystemState.toString()).execute(webUrl);
            }
        } catch (Exception e) {e.printStackTrace();}
        // System Time
        try {
            // if the SystemTime DOES NOT match the fetched data, Prep for PiPush
            String datetimeInput = ((EditText) mainPage.getView().findViewById(R.id.sysTimeEdit)).getText().toString();
            if (datetimeInput.matches(patternSource)) {
                if (!datetimeInput.equals(DateTime)) {
//                    datetimeInput = "{'TimeStamp': '" + datetimeInput + "'}";
                    JSONObject payload = new JSONObject("{'TimeStamp': '" + datetimeInput + "'}");
                    URL webUrl = ServerConnect.buildURL(URL[2] + PATHS[4]); // SystemTime Update
                    new PiPush(payload.toString()).execute(webUrl);
                }
            }
            else {ToastMessage("DateTime invalid.", "Long");}
        } catch (Exception e) {e.printStackTrace();}

        ///////////////////////////////////////////////////////// THRESHOLDS ///////////////////////////////////////////////////////////////////
        // DryTrigger
        try {
            String key = "Dry";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.dryTrgEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 4096 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("Dry Value: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // WindTrigger
        try {
            String key = "Wind max";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.windTrgEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 32 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("Wind Value: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // HumidTrig
        try {
            String key = "Humidity max";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.humidTrgEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 100 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("Humid Value: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Temp Min
        try {
            String key = "Temperature min";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.tempTrgEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 32 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("Temp Value: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // WaterDur
        try {
            String key = "Water Duration";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterDurEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 30 && currVal >= 1) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("Water Duration: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // ProhibStart
        try {
            String key = "Prohibited time start";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.prohibStartEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 2359 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("From Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // ProhibEnd
        try {
            String key = "Prohibited time end";
            Log.d(TAG, "UpdatePush: " + Thresholds.getInt(key));
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.prohibEndEdit)).getText().toString());
            if (currVal != Thresholds.getInt(key)) {
                if (currVal <= 2359 && currVal >= 0) {
                    Thresholds.put(key, currVal);
                    thresholdsChanged = true;
                } else ToastMessage("To Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}


        /////////////////////////////////////////////////////// TIMER TRIGGERING //////////////////////////////////////////////////////////////////
        // Sunday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Sunday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedSundayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Sunday Time
        try {
            String key = "Sunday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedSunEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Monday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Monday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedMondayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Monday Time
        try {
            String key = "Monday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedMonEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Tuesday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Tuesday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedTuesdayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Tuesday Time
        try {
            String key = "Tuesday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedTueEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Wednesday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Wednesday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedWednesdayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Wednesday Time
        try {
            String key = "Wednesday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedWedEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Thursday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Thursday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedThursdayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Thursday Time
        try {
            String key = "Thursday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedThuEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Friday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Friday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedFridayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Friday Time
        try {
            String key = "Friday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedFriEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}
        // Saturday Enabled
        try {
            // if the System Status Toggle button DOES NOT match the fetched data, Prep for PiPush
            String key = "Saturday";
            String dayToggle = ((ToggleButton) schedule.getView().findViewById(R.id.waterSchedSaturdayToggle)).getText().toString();
            String dayToggleFromFetch = (Boolean.parseBoolean(((JSONArray) TimerTriggering.get(key)).getString(0))) ? "Enabled" : "Disabled";
            if (!(dayToggle.equals(dayToggleFromFetch))) {
                ((JSONArray) TimerTriggering.get(key)).put(0, dayToggle.equals("Enabled"));
                timerchanged = true;
            }
        } catch (Exception e) {e.printStackTrace();}
        // Saturday Time
        try {
            String key = "Saturday";
            Log.d(TAG, "UpdatePush: " + TimerTriggering.get(key).toString());
            int currVal = Integer.parseInt(((EditText) schedule.getView().findViewById(R.id.waterSchedSatEdit)).getText().toString());
            if (currVal != ((JSONArray) TimerTriggering.get(key)).getInt(1)) {
                if (currVal <= 2359 && currVal >= 0) {
                    ((JSONArray) TimerTriggering.get(key)).put(1, currVal);
                    timerchanged = true;
                } else ToastMessage("Day Start Time: Range Error", "Short");
            }
        } catch (Exception e) {e.printStackTrace();}


        if (thresholdsChanged) {
            URL webUrl = ServerConnect.buildURL(URL[2] + PATHS[2]); // Thresholds Update
            new PiPush(Thresholds.toString()).execute(webUrl);
        }
        if (timerchanged) {
            URL webUrl = ServerConnect.buildURL(URL[2] + PATHS[1]); // TimerTriggering Update
            new PiPush(TimerTriggering.toString()).execute(webUrl);
        }
    }


    // Function to create a popup message to the user.
    public void ToastMessage(String message, String dur) {
        int duration = 0;
        // Lets make a pop-up message indicating success.
        Context context = getApplicationContext();
        CharSequence text = message;

        if (dur.equals("Long")) duration = Toast.LENGTH_LONG;
        else duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    // Function to get the phones DHCP Gateway address
    public String getGatewayAddress(){
        int ipaddr = -1;
        String ipaddress = null;

        //https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
        // https://developer.android.com/reference/android/net/DhcpInfo.html
        try {
            // WifiManager is a /library/component/API exposed by
            // the Android OS --> We get a handle to it.
            // Then, we query the DHCP information for the Gateway
            WifiManager wifiMngr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            ipaddr = wifiMngr.getDhcpInfo().gateway;

            // The Value returned is a (32bit) number --> we need
            // to convert it into Octets (groups of 8bits)
            ipaddress = String.format("%d.%d.%d.%d",
                    (ipaddr & 0xFF),
                    (ipaddr >> 8 & 0xFF),
                    (ipaddr >> 16 & 0xFF),
                    (ipaddr >> 24 & 0xFF)
            );
            Log.d("S.D.A.getGatewayAddress", "try " + ipaddress);
        }
        catch (Exception e) {
            Log.d("S.D.A.getGatewayAddress", "catch " + ipaddr);
        }

        if(ipaddr == -1) return null;
        return ipaddress;
    }
}
