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

 */

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
//import com.google.gson.stream.JsonWriter;
import org.json.JSONException;
import org.json.JSONObject;
//import java.net.Inet4Address;
//import java.net.InetAddress;
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


    // Here we will define some global variables

    // Our Async Task will save variable into this array
    // [0] <- TimerControl/State
    // [1] <- TimerControl/DaysZonesTimes
    // [2] <- TimerControl/Thresholds
    // [3] <- Xbee3/Dump
    String[] PiResponses = {"", "", "", ""};
    // Then we will parse the responses into dictionaries  //TODO going to think abou this
    JSONObject DaysZonesTimes;
    JSONObject Thresholds;
    JSONObject XbeeSensors;

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
        transaction.add(R.id.frameWindow, sensors, "SensorPage");
        transaction.hide(sensors);
        transaction.add(R.id.frameWindow, schedule, "SchedulePage");
        transaction.hide(schedule);
        transaction.add(R.id.frameWindow, mainPage, "MainPage");
        transaction.show(mainPage);
        transaction.commit();  // Think of this as "Update the GUI'


        // Attempt Pi connection
        UpdateData();
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
        // TODO Replace this with the TabLayout
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }
        SwitchToFragment(mainPage);
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void SensorsButton(View view) {
        // TODO Replace this with the TabLayout
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }
        SwitchToFragment(sensors);
    }

    // This function is linked to a GUI Button: When pressed this runs
    public void ScheduleButton(View view) {
        // TODO Replace this with the TabLayout
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'", "Short");
            return;
        }
        SwitchToFragment(schedule);
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

            // TODO Perhaps we do REGEX validation on data
            //  to check to be sure it is formatted correctly.... (hummm)
            // If a server response is bad/empty, then 'break fast'
            if (result == null | result == "null") {
                HaveData = false;
                connection.setText("Error");
                return;
            }

            // For each AsyncTask that runs, save its return value into our array
            PiResponses[saveIndex] = result;

            // TODO Perhaps: Save the strings into JSON objects as they arrive--checking for errors
            // Then set a flag if there was a data retrieve error.

            // If each AsyncTask has completed, except the last one, then the
            // last one will do data processing and posting to the User
            if (PiResponses[3] != "") {
                HaveData = true;
                connection.setText("Success");

                // TODO Why does PiResponses[0] <string class> not compare to "false|true" ???
                ((TextView) findViewById(R.id.sysEnabled)).setText((PiResponses[0].equals("false")) ? "Disabled" : "Enabled");
                Log.d(TAG, "class " + (PiResponses[0]).getClass().toString());

                // TODO Set data in the rest of the views
                ((TextView) findViewById(R.id.sysEnableLabel)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.sysEnabled)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.moistureLabel)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.moisture)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.tempuratureLabel)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.tempurature)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.windLabel)).setTextColor(Color.rgb(0, 0, 0));
                ((TextView) findViewById(R.id.wind)).setTextColor(Color.rgb(0, 0, 0));
                ((Button) findViewById(R.id.viewTimer)).setTextColor(Color.rgb(0, 0, 0));
                ((Button) findViewById(R.id.viewSensors)).setTextColor(Color.rgb(0, 0, 0));


                // TODO Get the data unpacked and shown to the user! IF There is an error, we need to stop the threads!
                try {
                    DaysZonesTimes = new JSONObject(PiResponses[1]);
//                    Log.d(TAG, DaysZonesTimes.getJSONArray("Monday").getString(1));  // {"Monday": [false, 100, 250]} --> 100
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    Thresholds = new JSONObject(PiResponses[2]);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // TODO Get XbeeSensor data read into a usable object thing!
                try {
                    XbeeSensors = new JSONObject(PiResponses[3]);
//                    Log.d(TAG, XbeeSensors.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ToastMessage("Success!!!", "Short");
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
        String[] URL = {"http://seniorproject.figureix.com:8008",
                "http://192.168.1.104:8008"
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
            // Try to ping the RPi server, and wait for the return value (a zero == success)
            Process pingAddr = runtime.exec("/system/bin/ping -c 1 192.168.1.104");
            int retVal = pingAddr.waitFor();
            Log.d(TAG, "UpdateData: Ping result=" + retVal);
            // If we can ping the RPi server (we are in the same network)
            if (retVal == 0) {
                for (int i = 0; i < PATHS.length; i++) {
                    try {
                        // Request data from the RPi server, using the LocalLAN IP Address
                        URL webUrl = ServerConnect.buildURL(URL[1] + PATHS[i]);
                        new PiQuery(i).execute(webUrl);
                    } catch (Exception e) {
                        Log.d("UpdateData", "We have Try Error");
                    }
                }
            }
            // Else, we are not in the same network, attempt Figureix
            else{
                for (int i = 0; i < PATHS.length; i++) {
                    try {
                        // Request data from the RPi server, using DDNS routing over the internet.
                        URL webUrl = ServerConnect.buildURL(URL[0] + PATHS[i]);
                        new PiQuery(i).execute(webUrl);
                    } catch (Exception e) {
                        Log.d("UpdateData", "We have Try Error");
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to give us quick swaps/transactions of the several fragments we have going.
    // Essentially, when a new/different Fragment is requested, we
    // will hide everything, then show the requested fragment
    public void SwitchToFragment(Fragment switchTo) {

        // TODO Think-about: Do this programmically / iteratively
        transaction = manager.beginTransaction();

        if (manager.findFragmentByTag("SchedulePage").isVisible()) transaction.hide(schedule);
        if (manager.findFragmentByTag("SensorPage").isVisible()) transaction.hide(sensors);
        if (manager.findFragmentByTag("MainPage").isVisible()) transaction.hide(mainPage);

        transaction.show(switchTo);
        transaction.commit();
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
