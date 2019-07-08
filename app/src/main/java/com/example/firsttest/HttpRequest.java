package com.example.firsttest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class HttpRequest {

    protected static JSONObject sendHttpRequest() throws IOException, JSONException {

        String latLong = MainActivity.instance.LatLong;
        String API_KEY = "AIzaSyDr1centttEHIsLx9rIsdbj4R_UckJwPcI";
        String radius = "2000"; //in Meter
        String type = "gym";

        String urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + latLong + "&radius=" +  radius + "&type=" + type + "&key=" + API_KEY;
        System.out.println(urlString);
        String jsonString;
        JSONObject json;

        URL url = new URL(urlString);

        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);

        json = new JSONObject(jsonString);
        return json;
    }
}
