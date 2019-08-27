package com.example.firsttest;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //Location
    static MainActivity instance;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    private TextView txt_location;

    //Location Google API Request
    private Button requestButton;
    private Button moveButton;
    public TextView results;
    public String LatLong;
    public double myLat;
    public double myLng;

    //Sceneform AR Fragment
    private ArFragment arFragment;

    //Create Renderable Objects from 2D
    private ViewRenderable planeRenderable_0;
    private ViewRenderable planeRenderable_1;
    private ViewRenderable planeRenderable_2;

    View plane_view;
    TextView textView;

    View plane_view_1;
    TextView textView_1;

    View plane_view_2;
    TextView textView_2;

    CompletableFuture<ViewRenderable> FutureRenderer;
    CompletableFuture<ViewRenderable> FutureRenderer_1;
    CompletableFuture<ViewRenderable> FutureRenderer_2;

    //for debug/error log
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    //how many results do we want to show
    private final int maxResultCount = 3;

    //create Nodes to hold the Planes
    Node base = new Node();
    Node model_1 = new Node();
    Node model_2 = new Node();
    Node model_3 = new Node();

    //compass
    private ImageView image;
    int mAzimuth;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mMagnetometer;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;


    //Start Method ---------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        //set main layout view
        setContentView(R.layout.activity_main);
        instance = this;

        //compass
        image = findViewById(R.id.imageViewCompass);
        // initialize android device sensor
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //find views
        txt_location = findViewById(R.id.location);
        requestButton = findViewById(R.id.sendRequest);
        moveButton = findViewById(R.id.move);
        results = findViewById(R.id.results);

        //create arFragment here
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        //find my planes, inflate them and connect with different renderers, if only one renderer, it overwrites the old Planes :O TODO: find solution
        LinearLayout mainLayout = findViewById(R.id.testview);
        LayoutInflater inflater = getLayoutInflater();
        plane_view = inflater.inflate(R.layout.plane, mainLayout, false);
        textView = plane_view.findViewById(R.id.plane_view);

        LinearLayout mainLayout_1 = findViewById(R.id.testview_1);
        LayoutInflater inflater_1 = getLayoutInflater();
        plane_view_1 = inflater_1.inflate(R.layout.plane_1, mainLayout_1, false);
        textView_1 = plane_view_1.findViewById(R.id.plane_view_1);

        LinearLayout mainLayout_2 = findViewById(R.id.testview_2);
        LayoutInflater inflater_2 = getLayoutInflater();
        plane_view_2 = inflater_2.inflate(R.layout.plane_2, mainLayout_2, false);
        textView_2 = plane_view_2.findViewById(R.id.plane_view_2);

        FutureRenderer = ViewRenderable.builder().setView(instance, plane_view).build();
        FutureRenderer_1 = ViewRenderable.builder().setView(instance, plane_view_1).build();
        FutureRenderer_2 = ViewRenderable.builder().setView(instance, plane_view_2).build();

        //shadows from planes look baaaaad
        arFragment.getArSceneView().getPlaneRenderer().setShadowReceiver(false);
        //create tap listener
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

            // Create the Anchor
            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());


            // Fill the Nodes with Model and Position and add it to the anchor.
            model_1.setParent(base);
            model_1.setRenderable(planeRenderable_0);
            model_1.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));

            model_2.setParent(base);
            model_2.setRenderable(planeRenderable_1);
            model_2.setLocalPosition(new Vector3(-0.5f, 0.0f, 0.0f));

            model_3.setParent(base);
            model_3.setRenderable(planeRenderable_2);
            model_3.setLocalPosition(new Vector3(0.5f, 0.0f, 0.0f));

            anchorNode.addChild(base);

            rotateCorrectly();
        });

        //gps
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "You Must accept this location", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

        //maps api request
        requestButton.setOnClickListener(v -> {
            if (LatLong != null) {
                new GetJSONTask().execute();
                //Log.d("result from http request", result);
            } else {
                Log.d("http request", "location is null");
            }

        });

        //test for live movement
        moveButton.setOnClickListener(v -> {
            model_3.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), 90f));
        });
    }

    public void rotateCorrectly() {
        //TODO: get Kompass info
        //calcualate direction
        //turn planes in correct rotation and position


    }

    //--------------------------------------- GPS --------------------------------------------------
    public static MainActivity getInstance() {
        return instance;
    }

    private void updateLocation() {
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MyLocationService.class);
        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    public void updateTextViewAndMyPos(final String value, double lat, double lng) {
        MainActivity.this.runOnUiThread(() -> txt_location.setText(value));
        LatLong = value;
        myLat = lat;
        myLng = lng;
    }

    //compass logic -------------------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // to stop the listener and save battery
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        image.setRotation(-mAzimuth);
        }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    //own class for async task, to avoid NetworkOnMainThreadException
    //api call, generating strings, gernerating plane models with correct Text on it
    private class GetJSONTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            try {
                //do google api call
                JSONObject jsonObject = HttpRequest.sendHttpRequest();

                String resultString = "";
                // int length = jsonObject.getJSONArray("results").length();

                String ResultString_1 = null;
                String ResultString_2 = null;
                String ResultString_3 = null;

                //build strings
                for (int x = 1; x <= maxResultCount; x++) {

                    String name = jsonObject.getJSONArray("results").getJSONObject(x).getString("name");
                    String lat = jsonObject.getJSONArray("results").getJSONObject(x).getJSONObject("geometry").getJSONObject("location").getString("lat");
                    String lng = jsonObject.getJSONArray("results").getJSONObject(x).getJSONObject("geometry").getJSONObject("location").getString("lng");

                    double placeLat = Double.parseDouble(lat);
                    double placeLng = Double.parseDouble(lng);
                    double dist = distance(MainActivity.instance.myLat, MainActivity.instance.myLng, placeLat, placeLng);
                    int distInMetersRounded = (int) (dist * 1000);

                    String ResultString = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters";

                    if (x == 1) {
                        ResultString_1 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters";
                    }
                    if (x == 2) {
                        ResultString_2 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters";
                    }
                    if (x == 3) {
                        ResultString_3 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters";
                    }

                    resultString = resultString + ResultString;
                }

                // Build a renderable from a 2D View.
                planeRenderable_0 = buildRendererView(ResultString_1, 1);
                planeRenderable_1 = buildRendererView(ResultString_2, 2);
                planeRenderable_2 = buildRendererView(ResultString_3, 3);

                //Update Places List on UI Thread
                final String finalResultString = resultString;
                MainActivity.this.runOnUiThread(() -> results.setText(finalResultString));

            } catch (IOException | JSONException e) {
                Log.d("doInBackgroud", "Unable to retrieve data. URL may be invalid.");
            }
            return null;
        }


        private ViewRenderable buildRendererView(String oneResultString, int x) {
            if (x == 1) {
                textView.setText(oneResultString);
                final ViewRenderable[] temp = new ViewRenderable[1];

                CompletableFuture.allOf(
                        FutureRenderer)
                        .handle(
                                (notUsed, throwable) -> {
                                    if (throwable != null) {
                                        return null;
                                    }
                                    try {
                                        temp[0] = FutureRenderer.get();
                                    } catch (InterruptedException | ExecutionException ex) {
                                    }
                                    return temp[0];
                                });
                return temp[0];
            }
            if (x == 2) {
                textView_1.setText(oneResultString);
                final ViewRenderable[] temp = new ViewRenderable[1];

                CompletableFuture.allOf(
                        FutureRenderer_1)
                        .handle(
                                (notUsed, throwable) -> {
                                    if (throwable != null) {
                                        return null;
                                    }
                                    try {
                                        temp[0] = FutureRenderer_1.get();
                                    } catch (InterruptedException | ExecutionException ex) {
                                    }
                                    return temp[0];
                                });
                return temp[0];
            }

            if (x == 3) {
                textView_2.setText(oneResultString);
                final ViewRenderable[] temp = new ViewRenderable[1];

                CompletableFuture.allOf(
                        FutureRenderer_2)
                        .handle(
                                (notUsed, throwable) -> {
                                    if (throwable != null) {
                                        return null;
                                    }
                                    try {
                                        temp[0] = FutureRenderer_2.get();
                                    } catch (InterruptedException | ExecutionException ex) {
                                    }
                                    return temp[0];
                                });
                return temp[0];
            }

            return null;
        }

        //calculate distance from two lat/lon points
        private double distance(double lat1, double lon1, double lat2, double lon2) {
            double theta = lon1 - lon2;
            double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
            dist = Math.acos(dist);
            dist = rad2deg(dist);
            dist = dist * 60 * 1.1515;
            //to Kilometer
            dist = dist * 1.609344;
            return (dist);
        }

        private double deg2rad(double deg) {
            return (deg * Math.PI / 180.0);
        }

        private double rad2deg(double rad) {
            return (rad * 180.0 / Math.PI);
        }
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
