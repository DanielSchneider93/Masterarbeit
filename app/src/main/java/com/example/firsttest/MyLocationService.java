package com.example.firsttest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.LocationResult;

public class MyLocationService extends BroadcastReceiver {

    public static final String ACTION_PROCESS_UPDATE= "com.example.firsttest.UPDATE_Location";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null)
        {
            final String action = intent.getAction();
            if(ACTION_PROCESS_UPDATE.equals(action)){
                LocationResult result = LocationResult.extractResult(intent);
                if(result != null){
                    Location location = result.getLastLocation();

                    String location_string = new StringBuilder(""+location.getLatitude()).append(",").append(location.getLongitude()).toString();
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    try{
                        MainActivity.getInstance().updateTextViewAndMyPos(location_string, lat, lng);
                    }catch(Exception e){
                       // Toast.makeText(context, location_string, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
