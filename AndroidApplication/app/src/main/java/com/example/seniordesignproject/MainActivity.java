package com.example.seniordesignproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "S.D.A.MainActivity";

    private static final String[] URL = {"http://vpn.figureix.com:8008", "http://192.168.1.104:8008"};
    private static final String[] PATHS = {"/TimerControl/State/",
                                           "/TimerControl/DaysZonesTimes/",
                                           "/Thresholds/",
                                           "/Xbee3/Dump/"
                                          };

    private static final String payload = "{\"Saturday\": [false, 1700, 1800], \"Monday\": [false, 100, 230], \"Thursday\": [false, 1610, 1645], \"Tuesday\": [false, 245, 1000], \"Wednesday\": [false, 1245, 1300], \"Sunday\": [false, 0, 100], \"Friday\": [false, 2345, 50]}";


    public boolean HaveData = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here A thread is born: to continuously check HaveData, then update colors of text in activity
        UpdateColors updatecolors = new UpdateColors();
        updatecolors.start();
    }

    // TODO Create function for other buttons :: https://www.youtube.com/watch?v=bgIUdb-7Rqo
    // link to other activites?
    // pageviewer ?

    public void ButtonClick(View view){
        // Here is the start of our code: We want to try accessing the RPi webserver
        // We use the URL java utility to build a URL, then we send it over
        // to a thread to run --> AsyncTask (PiQuery)

// #TODO Scan network for server, then use it/that
        int urlpass = 0;

        // TODO Figure out how to catch an ASYNC task -- else, code contines w/o error.
        try {
            URL webUrl = ServerConnect.buildURL(URL[urlpass] + PATHS[0]);
            new PiQuery().execute(webUrl);
        } catch (Exception e) {
            urlpass = 1;
            try {
                URL webUrl = ServerConnect.buildURL(URL[urlpass] + PATHS[0]);
                new PiQuery().execute(webUrl);
            } catch (Exception f) {
                urlpass = -1;
            }
        }
        Log.d(TAG, "urlPass: " + urlpass);

        if (urlpass == -1) {
            TextView textResult = (TextView) findViewById(R.id.connectStatus);
            textResult.setText("Error");
            return;
        }
        HaveData = true;
        TextView textResult = (TextView) findViewById(R.id.connectStatus);
        textResult.setText("Success");



//        // If we have a URL, lets process it.
//        if (Patterns.WEB_URL.matcher(inputTxt).matches()) { // Sanity Check; Do we have a URL?
//            Log.d(TAG, "We have a URL!!");
//            try {
//                URL webUrl = ServerConnect.buildURL(inputTxt.toString());
//                new PiQuery().execute(webUrl);
//            } catch (Exception e) {
//                String msg = e.getMessage().toString();
//                Log.d(TAG, "Exception 47: " + msg);
//            }
//        }
//        else {
//            // User gave a bad URL: Tell them with a pop-up message
//            Context context = getApplicationContext();
//            CharSequence text = "Bad URL Input";
//            int duration = Toast.LENGTH_SHORT;
//            Toast toast = Toast.makeText(context, text, duration);
//            toast.show();
//        }

    }

    // TODO Consider adding a waiting/loading thread-task to let user know stuff is happening
    // PiQuery is our 'custom/tailored' task to fetch a web page
    public class PiQuery extends AsyncTask<URL, Void, String> {

        // Thread process. It takes a URL, then sends it to our ServerConnect function
        //                                                       (which runs on the thread)
        @Override
        protected String doInBackground(URL... urls) { // this is a Library/API specific name/function
            URL searchURL = urls[0];
            String result = null;
            // Try connecting to the PiServer, and return the payload from it.
            try{
                result = ServerConnect.getJson(searchURL);
                Log.d(TAG, result);
            }
            catch (Exception e) {
                String msg = e.getMessage().toString();
                Log.d(TAG, "Exception 78: " + msg);
            }
            return result;
        }

        // On thread/task finish, return body as text -- Print it out on screen
        @Override
        protected void onPostExecute (String result) {
            TextView textResult = (TextView) findViewById(R.id.sysEnabled);
            textResult.setText(result);

            // Lets make a pop-up message indicating success.
            Context context = getApplicationContext();
            CharSequence text = "Success!!!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        }
    }

    // TODO Make this more dynamic, and include greying out the TouchBoxes
    public class UpdateColors extends Thread {
        UpdateColors(){
            // Nothing to construct
        }
        public void run() {
            TextView sysEnabledLabel = (TextView) findViewById(R.id.sysEnableLabel);
            TextView sysEnabled = (TextView) findViewById(R.id.sysEnabled);
            TextView moistureLabel = (TextView) findViewById(R.id.moistureLabel);
            TextView moistured = (TextView) findViewById(R.id.moisture);
            TextView tempLabel = (TextView) findViewById(R.id.tempuratureLabel);
            TextView temperature = (TextView) findViewById(R.id.tempurature);
            TextView windLabel = (TextView) findViewById(R.id.windLabel);
            TextView wind = (TextView) findViewById(R.id.wind);
//            TextView sensorButton = (TextView) findViewById(R.id.sysEnableLabel);
//            TextView scheduleButton = (TextView) findViewById(R.id.sysEnableLabel);
            while(true){
                if (HaveData){
                    sysEnabled.setTextColor(Color.rgb(0,0,0)); // Black
                    sysEnabledLabel.setTextColor(Color.rgb(0,0,0));
                    moistureLabel.setTextColor(Color.rgb(0,0,0));
                    moistured.setTextColor(Color.rgb(0,0,0));
                    temperature.setTextColor(Color.rgb(0,0,0));
                    tempLabel.setTextColor(Color.rgb(0,0,0));
                    wind.setTextColor(Color.rgb(0,0,0));
                    windLabel.setTextColor(Color.rgb(0,0,0));
                }
                else {
                    sysEnabled.setTextColor(Color.rgb(170,170, 170)); // Grey
                    sysEnabledLabel.setTextColor(Color.rgb(170,170, 170));
                    moistured.setTextColor(Color.rgb(170,170, 170));
                    moistureLabel.setTextColor(Color.rgb(170,170, 170));
                    temperature.setTextColor(Color.rgb(170,170, 170));
                    tempLabel.setTextColor(Color.rgb(170,170, 170));
                    wind.setTextColor(Color.rgb(170,170, 170));
                    windLabel.setTextColor(Color.rgb(170,170, 170));
                }
            }
        }
    }
}
