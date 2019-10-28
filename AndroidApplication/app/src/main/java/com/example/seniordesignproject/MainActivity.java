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
//import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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



    // Here we will define some global variables

    // Our Async Task will save variable into this array
    // [0] <- TimerControl/State
    // [1] <- TimerControl/DaysZonesTimes
    // [2] <- TimerControl/Thresholds
    // [3] <- Xbee3/Dump
    public String[] PiResponses = {"", "", "", ""};
    public JSONObject DaysZonesTimes;
    public JSONObject Thresholds;
    public JSONObject XbeeSensors;

    public boolean HaveData = false;


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
        sensors = new SensorsPage();
        schedule = new SchedulePage();
        mainPage = new MainPage();
        manager = getSupportFragmentManager();
        transaction = manager.beginTransaction();
        transaction.add(R.id.frameWindow, mainPage, "MainPage");
        transaction.show(mainPage);
        transaction.commit();  // Think of this as "Update the GUI'



        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.getTabAt(1).select();


        // https://stackoverflow.com/questions/33646586/tablayout-without-using-viewpager
        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        if(!HaveData) tabLayout.getTabAt(1).select();
                        if(tabLayout.getSelectedTabPosition() == 0) SensorsButton(tabLayout.getRootView());
                        if(tabLayout.getSelectedTabPosition() == 1) MainButton(tabLayout.getRootView());
                        if(tabLayout.getSelectedTabPosition() == 2) ScheduleButton(tabLayout.getRootView());
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {

                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        ToastMessage("Hello again", "Short");
                    }
                }
        );



        //  Pre-made temporary data.
        try{
            XbeeSensors = new JSONObject("{'PlaceHolder': {'Moisture': 5, 'Sunlight': 8, 'Battery': 99, 'Sector': 9, 'Iteration': 33333}, 'PlaceHolder2': {'Moisture': 5, 'Sunlight': 8, 'Battery': 99, 'Sector': 9, 'Iteration': 33333}, 'PlaceHolder3': {'Moisture': 5, 'Sunlight': 8, 'Battery': 99, 'Sector': 9, 'Iteration': 33333}}");
            Thresholds = new JSONObject("{'Rain': [10, 3, 15], 'Temperature': [23, 26, 24], 'Wind': [3, 1, 5], 'Moisture': [5, 7, 4]}");
            DaysZonesTimes = new JSONObject("{'Monday': [false, 1245, 1300], 'Tuesday': [false, 245, 1000], 'Wednesday': [false, 0, 100], 'Thursday': [false, 1610, 1645], 'Friday': [false, 100, 230], 'Saturday': [false, 1700, 1800], 'Sunday': [false, 2345, 50]}");
        }
        catch (Exception e){
            e.printStackTrace();
        }


        // Attempt Pi connection
        // UpdateData();
    }


    // This is a simple wrapper function for the GUI Update button.
    public void UpdateButtonFetch(View view) {
        UpdateData();
    }

    // We have a button to fetch information from the RPi, now we need to post data/changes to it.
    public void UpdateButtonPush(View view) {
        // TODO Post data to the Pi and create a button to match!
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void MainButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        transaction = manager.beginTransaction();

        if (manager.findFragmentByTag("MainPage") == null) {
            transaction.add(R.id.frameWindow, mainPage, "MainPage");
        }
        if (manager.findFragmentByTag("SchedulePage") != null && manager.findFragmentByTag("SchedulePage").isVisible()){
            transaction.hide(schedule);
        }
        if (manager.findFragmentByTag("SensorPage") != null && manager.findFragmentByTag("SensorPage").isVisible()) {
            transaction.hide(sensors);
        }

        transaction.replace(R.id.frameWindow, mainPage, "MainPage");

        //transaction.add(R.id.frameWindow, sensors, "SensorPage");
        transaction.show(mainPage);
        transaction.commit();
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void SensorsButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        transaction = manager.beginTransaction();

        if (manager.findFragmentByTag("SensorPage") == null) {
            transaction.add(R.id.frameWindow, sensors, "SensorPage");
        }


        if (manager.findFragmentByTag("SchedulePage") != null && manager.findFragmentByTag("SchedulePage").isVisible()){
            transaction.hide(schedule);
        }
        if (manager.findFragmentByTag("MainPage") != null && manager.findFragmentByTag("MainPage").isVisible()) {
            transaction.hide(mainPage);
        }

        transaction.replace(R.id.frameWindow, sensors, "SensorPage");

        transaction.show(sensors);
        transaction.commit();

//        SwitchToFragment(sensors);
        // TODO Refresh data in recycle view
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void ScheduleButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }

        transaction = manager.beginTransaction();

        if (manager.findFragmentByTag("SchedulePage") == null) {
            transaction.add(R.id.frameWindow, schedule, "SchedulePage");

        }


        if (manager.findFragmentByTag("SensorPage") != null && manager.findFragmentByTag("SensorPage").isVisible()){
            transaction.hide(sensors);
        }
        if (manager.findFragmentByTag("MainPage") != null && manager.findFragmentByTag("MainPage").isVisible()) {
            transaction.hide(mainPage);
        }
        transaction.replace(R.id.frameWindow, schedule, "SchedulePage");
        transaction.show(schedule);
        transaction.commit();
    }



    // PiQuery is our 'custom/tailored' task to fetch a web page
    public class PiQuery extends AsyncTask<URL, Void, String> {
        int saveIndex;
        // This class defines a Thread process.
        // It takes a URL, then sends it to our ServerConnect function (which runs on the thread)

        // Stackoverflow passing parameters -- via constructor (?)
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
                HaveData = false;
                connection.setText("Error");
                return;
            }

            // There is a risk that one of the 1st three URL requests fail, but this still works...
            // For each AsyncTask that runs, save its return value into our array
            PiResponses[saveIndex] = result;

            // TODO Perhaps: Save the strings into JSON objects as they arrive

            // If each AsyncTask has completed, except the last one, then the
            // last one will do data processing and posting to the User
            if (PiResponses[3] != "") {
                HaveData = true;
                connection.setText("Success");

                Log.d(TAG, "class " + (PiResponses[0]).getClass().toString());

//                ((Button) findViewById(R.id.viewTimer)).setTextColor(Color.rgb(0, 0, 0));
//                ((Button) findViewById(R.id.viewSensors)).setTextColor(Color.rgb(0, 0, 0));


                // TODO Get the data unpacked and shown to the user! IF There is an error, we need to stop the threads!
                try {
                    DaysZonesTimes = new JSONObject(PiResponses[1]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    Thresholds = new JSONObject(PiResponses[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    XbeeSensors = new JSONObject(PiResponses[3]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Update Values in Fragments
                if (manager.findFragmentByTag("MainPage") != null) mainPage.updateValues();
                if (manager.findFragmentByTag("SensorPage") != null) sensors.updateValues();
                if (manager.findFragmentByTag("SchedulePage") != null) schedule.updateValues();
            }
        }
    }


    // This Function Attempts to initiate the ServerConnect function, to get data off the RPi
    public void UpdateData(){
        // Here is the start of our code: We want to try accessing the RPi webserver
        // We use the URL java utility to build a URL, then we send it over
        // to a thread to run --> AsyncTask (PiQuery)

        // #TODO Scan network for server, then use it/that
        // https://developer.android.com/reference/java/net/DatagramSocket
        // https://developer.android.com/reference/java/net/DatagramPacket
        // https://developer.android.com/reference/java/net/DatagramSocketImpl

        // Here are two static addresses to the RPi
        String[] URL = {"http://192.168.1.104:8008",
                "http://seniorproject.figureix.com:8008",
                "http://10.0.2.2:8008"
        };
        // And the available file paths on the Pi
        String[] PATHS = {"/TimerControl/State/",
                "/TimerControl/DaysZonesTimes/",
                "/TimerControl/Thresholds/",
                "/Xbee3/Dump/"
        };

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

            if (CurrentGatewayAddress.equals("192.168.1.1")) retVal = 0;
            if (CurrentGatewayAddress.equals("192.168.232.1")) {
                Process pingAddr = runtime.exec("/system/bin/ping -c 1 192.168.1.104");
                retVal = pingAddr.waitFor();  //retVal = 1 on fail, 0 on success
                Log.d(TAG, "UpdateData: Ping result=" + retVal);
                if (retVal == 1) retVal = 2;
            }

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
