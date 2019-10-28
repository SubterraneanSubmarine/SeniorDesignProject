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
 */
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

// Here we define a class that will interface with the RPi server
public class ServerConnect {
    private static final String TAG = "S.D.A.ServerConnect";  // Used for Debug output

    // empty constructor
    private ServerConnect(){}

    // This function will try to create a viable URL that we will try to connect to.
    public static URL buildURL(String title) {
        URL url = null;
        try {
            url = new URL(title);
        }
        catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "buildURL");
        }
        return url;
    }

    // This function is called from our AsyncTask -- since we are not allowed to
    // holdup the main/UI thread with 'blocking' tasks.  // TODO Find reference for this?
    public static String getJson(URL url) throws IOException {
        Log.d(TAG, "getJson from URL=" + url);
        // Using the Android class/library HttpURLConnection, we open a
        // connection to the RPi server. If it doesn't work, an Exception is thrown.
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);  // This is a 10sec timeout

        // upon connecting: read the incoming data.
        try {
            InputStream stream = connection.getInputStream();
            // https://developer.android.com/reference/java/util/Scanner?hl=en
            Scanner scanner = new Scanner(stream);
            // We will use this 'end of message' delimiter to know when to stop reading data
            scanner.useDelimiter("~");  // in the JSONSrv.py message, three '~' will be appended

            // If there is something to read/receive, we will return it.
            // Returning our data is made available to the onPostExecute AsyncTask thread function!
            boolean hasData = scanner.hasNext();
            if (hasData) {
                String result = scanner.next();
                // Log.d(TAG, "HasData! " + result);
                return result;
            } else {
                Log.d(TAG, "getJSON In the Else");
                return null;
            }
        }
        catch (Exception e) {
            String msg = e.toString();
            Log.d(TAG, "getJSON Catch: " + msg);
            return null;
        }

        // Close our connection to the RPi server.
        finally {
            connection.disconnect();
        }
    }

    public static String postJson(URL url, JSONObject payload) throws IOException {
        // TODO Push data to the server
        return "TODO";
    }
}

