package com.example.seniordesignproject;

// ECE 4800 - Senior Project
// David Carlson and Bryce Martin
// Inspiration for this code drawn from:
// https://app.pluralsight.com/library/courses/android-studio-connected-app-building-first/table-of-contents
//              by Simone Alessandria


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

        try {
            InputStream stream = connection.getInputStream();
            Scanner scanner = new Scanner(stream);
            // TODO Consider a differnet Delimiter -- I think it is breaking stuff...
            scanner.useDelimiter("\\A");

            boolean hasData = scanner.hasNext();
            if (hasData) {
                String result = scanner.next();
                Log.d(TAG, "HasData! " + result);
                return scanner.next();
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

