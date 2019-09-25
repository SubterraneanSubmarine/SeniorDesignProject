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


import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ServerConnect {

    private static final String TAG = "S.D.A.ServerConnect";

    // empty constructor
    private ServerConnect(){}

    // Address that will not change
    //public final static String BASE_URL = "http://vpn.figureix.com:8008/";

    public static URL buildURL(String title) {
        //String full_url = BASE_URL + "?q=" + title;
        String BASE_URL = title;
        URL url = null;
        try {
            url = new URL(BASE_URL);
        }
        catch (Exception e){
            e.printStackTrace();
            Log.d(TAG, "Exception 38");
        }
        return url;
    }

    public static String getJson(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);

        try {
            InputStream stream = connection.getInputStream();
            Scanner scanner = new Scanner(stream);
            scanner.useDelimiter("\\A");

            boolean hasData = scanner.hasNext();
            if (hasData) {
                String result = scanner.next();
                // Log.d(TAG, "HasData! " + result);
                return result;
            } else {
                Log.d(TAG, "In the Else: 57");
                return null;
            }
        }
        catch (Exception e) {
            String msg = e.toString();
            Log.d(TAG, "Exception 62: " + msg);
            return null;
        }
        finally {
            connection.disconnect();
        }
    }
}

