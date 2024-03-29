package com.tuts.prakash.simpleocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.CountDownTimer;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    int PERMISSION_ID = 44;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    FusedLocationProviderClient mFusedLocationClient;
    SurfaceView mCameraView;
    TextView mTextView, latTextView, lonTextView, infoText;
    CameraSource mCameraSource;
    MyCountDownTimer myCountDownTimer;
    int progress;
    Map <String, Object> sightingEntries = new HashMap<>();
    boolean match = false;
    Toast toast;
    Map <String, String> amberEntries = new HashMap<String, String>();
    HashSet <String> foundEntries = new HashSet<>();

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    private static final String TAG = "MainActivity";
    private static final int requestPermissionID = 101;
    Map <String, String> locationEntry = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);
        latTextView = findViewById(R.id.latTextView);
        lonTextView = findViewById(R.id.lonTextView);
        infoText = findViewById(R.id.info);
        infoText.setSelected(true);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //myCountDownTimer = new MyCountDownTimer(10000, 1000);
        //myCountDownTimer.start();

        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                sightingEntries = (Map<String, Object>) ((Map<String, Object>) dataSnapshot.getValue()).get("AmberMatch");
                if (sightingEntries == null) {
                    sightingEntries = new HashMap<>();
                }
                Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) dataSnapshot.getValue()).get("MockAmberAlert");
                if (map != null) {
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        Map<String, Object> vehicle_info = (Map<String, Object>) ((Map<String, Object>) entry.getValue()).get("vehicle_information");
                        amberEntries.put((String) vehicle_info.get("license_plate"), entry.getKey());
                    }
                }
                String scrollingInfo = "";
                for ( String key : amberEntries.keySet() ) {
                    scrollingInfo += key + "                    ";
                }
                infoText.setText(scrollingInfo);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
        getLastLocation();

        startCameraSource();

        Toast.makeText(getApplicationContext(), "Scanning...", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    latTextView.setText(location.getLatitude() + "");
                                    lonTextView.setText(location.getLongitude() + "");
                                    locationEntry.put("Latitude", String.valueOf(location.getLatitude()));
                                    locationEntry.put("Longitude", String.valueOf(location.getLongitude()));
                                    Log.d(TAG, locationEntry.toString());
                                }
                            }
                        }
                );
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }


    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        // mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latTextView.setText(mLastLocation.getLatitude() + "");
            lonTextView.setText(mLastLocation.getLongitude() + "");
            Log.d(TAG, locationEntry.toString());
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                }
            }
        }
    }

    private void startCameraSource() {

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                    match = false;

                                }
                                // Search through entries for match
                                String detectedText = stringBuilder.toString();
                                for (Map.Entry<String, String> entry : amberEntries.entrySet()) {
                                    String key = entry.getKey();
                                    // If match found
                                    if (detectedText.contains(key)) {
                                        Log.d(TAG, "ALERT_FOUND: " + key);
                                        foundEntries.add(key);

                                        // Show found toast
                                        Context context = getApplicationContext();
                                        CharSequence text = "Match Found!";
                                        int duration = Toast.LENGTH_LONG;
                                        Toast toast = Toast.makeText(context, text, duration);
                                        toast.show();

                                        // Send sighting to Firebase
                                        // Get sighting time
                                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss MM-dd-yyyy", Locale.getDefault());
                                        SimpleDateFormat sdfNoSec = new SimpleDateFormat("HH:mm:ss MM-dd-yyyy", Locale.getDefault());
                                        String timeKey = sdf.format(new Date());
                                        String currentDateandTime = sdfNoSec.format(new Date());


                                        // Building db entry
                                        Map<String, Object> sightingEntry = new HashMap<>();
                                        sightingEntry.put("AmberAlert", amberEntries.get(key));
                                        sightingEntry.put("Location", locationEntry);
                                        sightingEntry.put("Time", currentDateandTime);
                                        sightingEntries.put(key, sightingEntry);

                                        myRef.child("AmberMatch").updateChildren(sightingEntries);
                                        Log.d(TAG, locationEntry.toString());

                                        // Send sighting via Notivize
                                        if (detectedText.contains(key)) {
                                            Log.d(TAG, "FOUND: " + key);
                                            match = true;
                                        }

                                        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                                        // Request a string response from the provided URL.
                                        try {
                                            RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                                            String URL = "https://events-api.notivize.com/applications/afb03cdb-854d-4056-834b-b1974b7bdbf7/event_flows/365dd972-7eb4-4a7b-b8a0-a02113f5f98a/events";
                                            JSONObject jsonBody = new JSONObject();
                                            jsonBody.put( "police_email", "austinyuan2@gmail.com");
                                            jsonBody.put("latitude", locationEntry.get("Latitude"));
                                            jsonBody.put("longitude", locationEntry.get("Longitude"));
                                            jsonBody.put("time", currentDateandTime);
                                            jsonBody.put("license_plate", key);
                                            final String requestBody = jsonBody.toString();

                                            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    Log.i("VOLLEY", response);
                                                }
                                            }, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    Log.e("VOLLEY", error.toString());
                                                }
                                            }) {
                                                @Override
                                                public String getBodyContentType() {
                                                    return "application/json; charset=utf-8";
                                                }

                                                @Override
                                                public byte[] getBody() throws AuthFailureError {
                                                    try {
                                                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                                                    } catch (UnsupportedEncodingException uee) {
                                                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                                                        return null;
                                                    }
                                                }

                                                @Override
                                                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                                                    String responseString = "";
                                                    if (response != null) {
                                                        responseString = String.valueOf(response.statusCode);
                                                        Log.d(TAG, "Sending msg...\n" + responseString + "\n...Sent!");
                                                    }
                                                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                                                }
                                            };

                                            requestQueue.add(stringRequest);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    mTextView.setText(detectedText);
                                }
                                if (match) {
                                    Toast.makeText(getApplicationContext(), "Match Found!", Toast.LENGTH_LONG).show();
                                }
                                match = false;
                            }
                        });
                    }
                }
            });
        }
    }

    class MyCountDownTimer extends CountDownTimer {

        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            progress = (int) (millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
