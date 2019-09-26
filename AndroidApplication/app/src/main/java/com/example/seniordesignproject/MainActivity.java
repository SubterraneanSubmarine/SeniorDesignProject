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

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.stream.JsonWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "S.D.A.MainActivity";  // Used for debug output


    // #TODO Scan network for server, then use it/that
    private static final String[] URL = {"http://vpn.figureix.com:8008",
            "http://192.168.1.104:8008"
    };
    private static final String[] PATHS = {"/TimerControl/State/",
            "/TimerControl/DaysZonesTimes/",
            "/TimerControl/Thresholds/",
            "/Xbee3/Dump/"
    };

    private static final String payload = "{\"Saturday\": [false, 1700, 1800], \"Monday\": [false, 100, 230], \"Thursday\": [false, 1610, 1645], \"Tuesday\": [false, 245, 1000], \"Wednesday\": [false, 1245, 1300], \"Sunday\": [false, 0, 100], \"Friday\": [false, 2345, 50]}";


    // Our Async Task will save variable into this array
    // [0] <- TimerControl/State
    // [1] <- TimerControl/DaysZonesTimes
    // [2] <- TimerControl/Thresholds
    // [3] <- Xbee3/Dump
    String[] PiResponses = {"", "", "", ""};
    public boolean HaveData = false;


    // Then we will parse the responses into dictionaries
    boolean State;
    JSONObject DaysZonesTimes;
    JSONObject Thresholds;
    JSONObject XbeeSensors;
//    Gson XbeeSens2;


    // Alloc. pages that will show/receive information
    SensorsPage sensors;
    SchedulePage schedule;
    MainPage mainPage;
    FragmentManager manager;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the fragments and transaction manager
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
        transaction.commit();

        // Attempt Pi connection
        UpdateData();

    }


    public void UpdateButtonFetch(View view) {
        UpdateData();
    }

    public void UpdateButtonPush(View view) {
        // TODO Post data to the Pi
    }

    public void MainButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'");
            return;
        }
        SwitchToFragment(mainPage);
    }

    public void SensorsButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'");
            return;
        }
        SwitchToFragment(sensors);
    }

    public void ScheduleButton(View view) {
        if (!HaveData) {
            ToastMessage("No Data. Press \'Update\'");
            return;
        }
        SwitchToFragment(schedule);
    }

    // PiQuery is our 'custom/tailored' task to fetch a web page
    public class PiQuery extends AsyncTask<URL, Void, String> {
        int saveIndex;
        // Thread process. It takes a URL, then sends it to our ServerConnect function
        //                                                       (which runs on the thread)

        // https://stackoverflow.com/questions/12069669/how-can-you-pass-multiple-primitive-parameters-to-asynctask
        PiQuery(int index) {
            this.saveIndex = index;
        }

        // Provide visual feedback that the app is running/working via loading circle
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected String doInBackground(URL... urls) { // this is a Library/API specific name/function
            URL searchURL = urls[0];
            String result = null;
            // Try connecting to the PiServer, and return the payload from it.
            try {
                result = ServerConnect.getJson(searchURL);
            } catch (Exception e) {
                String msg = e.getMessage().toString();
                Log.d(TAG, "Exception 78: " + msg);
            }
            Log.d(TAG, "169 URLResult: " + result);
            return result;
        }

        // On thread/task finish, return body as text -- Print it out on screen
        @Override
        protected void onPostExecute(String result) {
            progressBar.setVisibility(ProgressBar.INVISIBLE);
            if (result == null | result == "null") {
                HaveData = false;
                TextView textResult = (TextView) findViewById(R.id.connectStatus);
                textResult.setText("Error");
                return;
            }

            PiResponses[saveIndex] = result;

            // TODO Move data manipulation work to its own function, rather than this AsyncTask?
            if (PiResponses[3] != "") {
                HaveData = true;
                TextView connection = (TextView) findViewById(R.id.connectStatus);
                connection.setText("Success");

                ((TextView) findViewById(R.id.sysEnabled)).setText((PiResponses[0] == "false") ? "Disabled" : "Enabled");


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
            }

            ToastMessage("Success!!!");

        }
    }


    public void UpdateData(){
        // Here is the start of our code: We want to try accessing the RPi webserver
        // We use the URL java utility to build a URL, then we send it over
        // to a thread to run --> AsyncTask (PiQuery)

        int urlpass = 1;

        // TODO This for loop finishes before any of the threads throw an error.....
        //  Thus Never attempting the other URL/Link for the server.
        //  I think this goes to show the need for a dynamic query for the RPi
        for (int i = 0; i < PATHS.length; i++) {
            if (urlpass == -1) break;
            if (urlpass == 0) {
                try {
                    URL webUrl = ServerConnect.buildURL(URL[urlpass] + PATHS[i]);
                    new PiQuery(i).execute(webUrl);
                } catch (Exception e) {
                    urlpass = 1;
                }
            }
            if (urlpass == 1) {
                try {
                    URL webUrl = ServerConnect.buildURL(URL[urlpass] + PATHS[i]);
                    new PiQuery(i).execute(webUrl);
                } catch (Exception f) {
                    urlpass = -1;
                }
            }
            Log.d(TAG, "urlPass: " + urlpass);
        }
        urlpass = 0;
    }

    public void SwitchToFragment(Fragment switchTo) {

        // TODO Think-about: Do this programmically / iteratively
        transaction = manager.beginTransaction();

        if (manager.findFragmentByTag("SchedulePage").isVisible()) transaction.hide(schedule);
        if (manager.findFragmentByTag("SensorPage").isVisible()) transaction.hide(sensors);
        if (manager.findFragmentByTag("MainPage").isVisible()) transaction.hide(mainPage);

        transaction.show(switchTo);
        transaction.commit();
    }

    public void ToastMessage(String message) {
        // Lets make a pop-up message indicating success.
        Context context = getApplicationContext();
        CharSequence text = message;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}
