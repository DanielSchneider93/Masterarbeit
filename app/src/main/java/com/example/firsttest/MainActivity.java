package com.example.firsttest;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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

    //Location Google API Request
    public TextView results;
    public String LatLong;
    public double myLat;
    public double myLng;

    //Sceneform AR Fragment
    private ArFragment arFragment;

    //Create Renderable Objects from 2D
    private ViewRenderable planeRenderable_0 = null;
    private ViewRenderable planeRenderable_1 = null;
    private ViewRenderable planeRenderable_2 = null;

    View plane_view;
    TextView textView;

    View plane_view_1;
    TextView textView_1;

    View plane_view_2;
    TextView textView_2;

    //for rating
    ImageView imageView_1;
    ImageView imageView_2;
    ImageView imageView_3;

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

    //search bar
    public EditText simpleEditText;
    public String searchQuery;

    //Start Method ---------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        // dexter for location permissions
        Dexter.withActivity(MainActivity.this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Log.d("onpermissiongranted", "permission granted to location");
                        updateLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "You Must accept this location", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                        Log.d("onPermissionRationaleShouldBeShown", "Request Location Permission");

                    }
                }).check();

        //dexter get camera permissions
        Dexter.withActivity(MainActivity.this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        // check for permanent denial of permission
                        if (response.isPermanentlyDenied()) {
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

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
        simpleEditText = findViewById(R.id.editText);

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

        imageView_1 = plane_view.findViewById(R.id.imView);
        imageView_2 = plane_view_1.findViewById(R.id.imView_1);
        imageView_3 = plane_view_2.findViewById(R.id.imView_2);

        FutureRenderer = ViewRenderable.builder().setView(instance, plane_view).build();
        FutureRenderer_1 = ViewRenderable.builder().setView(instance, plane_view_1).build();
        FutureRenderer_2 = ViewRenderable.builder().setView(instance, plane_view_2).build();

        //shadows from planes look soooo baaaaad
        arFragment.getArSceneView().getPlaneRenderer().setShadowReceiver(false);

        //create tap listener
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

            if (planeRenderable_0 != null || planeRenderable_1 != null || planeRenderable_2 != null) {
                // Create the Anchor
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());


                // Fill the Nodes with Model and Position and add it to the anchor.
                model_1.setParent(base);
                model_1.setRenderable(planeRenderable_0);
                model_1.setLocalPosition(new Vector3(0.0f, 0.75f, 0.0f));
                model_1.setLocalRotation(Quaternion.axisAngle(new Vector3(1f, 0, 0), 0f));

                model_2.setParent(base);
                model_2.setRenderable(planeRenderable_1);
                model_2.setLocalPosition(new Vector3(-0.9f, 0.75f, 0.2f));
                model_2.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 25f));

                model_3.setParent(base);
                model_3.setRenderable(planeRenderable_2);
                model_3.setLocalPosition(new Vector3(0.9f, 0.75f, 0.2f));
                model_3.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0f), -25f));

                anchorNode.addChild(base);
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Info");
                alertDialog.setMessage("Search for something first!");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        });

        //get searchquery from textedit and give it to google search hhtp request
        simpleEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchQuery = v.getText().toString();
                //simpleEditText.setVisibility(View.GONE);

                if (LatLong != null) {
                    new GetJSONTask().execute();
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Info");
                    alertDialog.setMessage("Please activate GPS.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                    //Toast.makeText(MainActivity.this, "Please activate Location.", Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });
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

    public void updateMyPos(final String value, double lat, double lng) {
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
                JSONObject jsonObject = HttpRequest.sendHttpRequest(searchQuery);

               int count = jsonObject.getJSONArray("results").length();

                String resultString = "";

                String ResultString_1 = null;
                String ResultString_2 = null;
                String ResultString_3 = null;

                Double rating_1 = null;
                Double rating_2 = null;
                Double rating_3 = null;

                //build strings
                for (int x = 1; x <= maxResultCount; x++) {

                    String name = jsonObject.getJSONArray("results").getJSONObject(x).getString("name");
                    String lat = jsonObject.getJSONArray("results").getJSONObject(x).getJSONObject("geometry").getJSONObject("location").getString("lat");
                    String lng = jsonObject.getJSONArray("results").getJSONObject(x).getJSONObject("geometry").getJSONObject("location").getString("lng");
                    String ratingString = jsonObject.getJSONArray("results").getJSONObject(x).getString("rating");
                    Double rating = Double.parseDouble(ratingString);

                    double placeLat = Double.parseDouble(lat);
                    double placeLng = Double.parseDouble(lng);
                    double dist = distance(MainActivity.instance.myLat, MainActivity.instance.myLng, placeLat, placeLng);
                    double dir = getDirection(MainActivity.instance.myLat, MainActivity.instance.myLng, placeLat, placeLng);

                    String where = null;

                    if (dir >= 350 || dir <= 10)
                        where = "N";
                    if (dir < 350 && dir > 280)
                        where = "NW";
                    if (dir <= 280 && dir > 260)
                        where = "W";
                    if (dir <= 260 && dir > 190)
                        where = "SW";
                    if (dir <= 190 && dir > 170)
                        where = "S";
                    if (dir <= 170 && dir > 100)
                        where = "SE";
                    if (dir <= 100 && dir > 80)
                        where = "E";
                    if (dir <= 80 && dir > 10)
                        where = "NE";

                    int distInMetersRounded = (int) (dist * 1000);

                    if (x == 1) {
                        ResultString_1 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters " + "- Direction " + where;
                        rating_1 = rating;
                    }
                    if (x == 2) {
                        ResultString_2 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters " + "- Direction " + where;
                        rating_2 = rating;
                    }
                    if (x == 3) {
                        ResultString_3 = "\n" + name + "\n" + " Distance: " + distInMetersRounded + " Meters " + "- Direction " + where;
                        rating_3 = rating;
                    }
                }

                // Build a renderable from a 2D View.
                planeRenderable_0 = buildRendererView(ResultString_1, 1, rating_1);
                planeRenderable_1 = buildRendererView(ResultString_2, 2, rating_2);
                planeRenderable_2 = buildRendererView(ResultString_3, 3, rating_3);

                MainActivity.getInstance().runOnUiThread(() -> Toast.makeText(MainActivity.this, "Search Successful, found " + count + " Results!", Toast.LENGTH_SHORT).show());
            } catch (IOException | JSONException e) {
                MainActivity.getInstance().runOnUiThread(() -> Toast.makeText(MainActivity.this, "Nothing found, please search for something different!", Toast.LENGTH_SHORT).show());
            }
            return null;
        }


        private ViewRenderable buildRendererView(String oneResultString, int x, Double rating) {
            if (x == 1) {
                runOnUiThread(() -> textView.setText(oneResultString));

                if (rating >= 4.5)
                    runOnUiThread(() -> imageView_1.setImageResource(R.drawable.s5));
                else if (rating >= 3.5)
                    runOnUiThread(() -> imageView_1.setImageResource(R.drawable.s4));
                else if (rating >= 2.5)
                    runOnUiThread(() -> imageView_1.setImageResource(R.drawable.s3));
                else if (rating >= 1.5)
                    runOnUiThread(() -> imageView_1.setImageResource(R.drawable.s2));
                else if (rating >= 0.5)
                    runOnUiThread(() -> imageView_1.setImageResource(R.drawable.s1));

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
                runOnUiThread(() -> textView_1.setText(oneResultString));

                if (rating >= 4.5)
                    runOnUiThread(() -> imageView_2.setImageResource(R.drawable.s5));
                else if (rating >= 3.5)
                    runOnUiThread(() -> imageView_2.setImageResource(R.drawable.s4));
                else if (rating >= 2.5)
                    runOnUiThread(() -> imageView_2.setImageResource(R.drawable.s3));
                else if (rating >= 1.5)
                    runOnUiThread(() -> imageView_2.setImageResource(R.drawable.s2));
                else if (rating >= 0.5)
                    runOnUiThread(() -> imageView_2.setImageResource(R.drawable.s1));

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
                runOnUiThread(() -> textView_2.setText(oneResultString));

                if (rating >= 4.5)
                    runOnUiThread(() -> imageView_3.setImageResource(R.drawable.s5));
                else if (rating >= 3.5)
                    runOnUiThread(() -> imageView_3.setImageResource(R.drawable.s4));
                else if (rating >= 2.5)
                    runOnUiThread(() -> imageView_3.setImageResource(R.drawable.s3));
                else if (rating >= 1.5)
                    runOnUiThread(() -> imageView_3.setImageResource(R.drawable.s2));
                else if (rating >= 0.5)
                    runOnUiThread(() -> imageView_3.setImageResource(R.drawable.s1));

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

        private double getDirection(double lat1, double lng1, double lat2, double lng2) {

            double PI = Math.PI;
            double dTeta = Math.log(Math.tan((lat2 / 2) + (PI / 4)) / Math.tan((lat1 / 2) + (PI / 4)));
            double dLon = Math.abs(lng1 - lng2);
            double teta = Math.atan2(dLon, dTeta);
            double direction = Math.round(Math.toDegrees(teta));
            return direction; //direction in degree
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

    //If permanently denied -> open app settings
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("Go to SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}
