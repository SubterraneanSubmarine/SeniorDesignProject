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
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
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
    // holdup the main/UI thread with 'blocking' tasks.
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

    // Here is our function that will be used to push data to the RPi
    public static void postJson(URL url, String payload) throws IOException {
        Log.d(TAG, "postJson from URL=" + url + " Payload: " + payload);
        // Using the Android class/library HttpURLConnection, we open a
        // connection to the RPi server. If it doesn't work, an Exception is thrown.

        // https://stackoverflow.com/questions/46328854/post-request-with-java-asynctask
        // https://developer.android.com/reference/java/net/HttpURLConnection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            // Setup the connection for POSTing JSON data.
            connection.setDoOutput(true);  // Defaults connection type to "POST"
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setFixedLengthStreamingMode(payload.length());

            // Prep to write packet that will to transmitted
            OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
            outputStream.write(payload.getBytes(Charset.forName("UTF-8")));
            outputStream.flush();
            outputStream.close();

            // Packet ready for shipping. Make connection to RPi
            connection.connect();

            // Get a response code for logging
            Log.d(TAG, "PostJSON:Response: "+ connection.getResponseCode() + "  payload: " + payload);
        } catch (Exception e){
            e.printStackTrace();
        }

        // Close the connection.
        finally {
            connection.disconnect();
        }
    }
}

