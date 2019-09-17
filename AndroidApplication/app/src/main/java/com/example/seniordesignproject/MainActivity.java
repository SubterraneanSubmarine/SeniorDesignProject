package com.example.seniordesignproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "S.D.A.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void ButtonClick(View view){
        // Here is the start of our code: We want to try accessing the RPi webserver
        // We use the URL java utility to build a URL, then we send it over
        // to a thread to run --> AsyncTask (PiQuery)

// #TODO Get rid of use input for URL to server: Instead, scan network for server
        // Get address from user input at textbox
        String inputTxt = ((EditText) findViewById(R.id.urlText)).getText().toString();

        // If we have a URL, lets process it.
        if (Patterns.WEB_URL.matcher(inputTxt).matches()) { // Sanity Check; Do we have a URL?
            Log.d(TAG, "We have a URL!!");
            try {
                URL webUrl = ServerConnect.buildURL(inputTxt.toString());
                new PiQuery().execute(webUrl);
            } catch (Exception e) {
                String msg = e.getMessage().toString();
                Log.d(TAG, "Exception 47: " + msg);
            }
        }
        else {
            // User gave a bad URL: Tell them with a pop-up message
            Context context = getApplicationContext();
            CharSequence text = "Bad URL Input";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }

    }


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
            TextView textResult = (TextView) findViewById(R.id.textResponse);
            textResult.setText(result);

            // Lets make a pop-up message indicating success.
            Context context = getApplicationContext();
            CharSequence text = "Success!!!";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

        }
    }
}
