package com.locationmonitor;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.locationmonitor.entities.LocationModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class LocationTrackerActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap googleMap;
    private LocationManager locationManager;
    private PendingIntent pendingIntent;
    private GoogleApiClient googleApiClient;
    // LatLng currentLocation;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    String deviceId;
    LatLng hospitalLocation = new LatLng(12.9170033, 80.0531244);
    private DatabaseReference databaseReference;
    private List<LocationModel> patientsTrack;
    private CircleOptions circleOptions;
    private Intent intent;
    private final String NOTIFICATION_INTENT_TYPE = "Notification";
    private LatLng patientLocation;
    private String radiusStr;
    private static int radius;
    private Button btnNavigate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("on create of locationtracker activi");
        System.out.println("savedInstanceState " + savedInstanceState);
        System.out.println("getIntent " + getIntent().getType());
        setContentView(R.layout.activity_maps);
        intent = getIntent();
        if (intent.getType() != null && intent.getType().equals("normal")) {
            Bundle bundle = intent.getExtras();
            radiusStr = bundle.getString("radius");
            radius = Integer.valueOf(radiusStr);
            radius = radius * 1000;
            patientsTrack = (List<LocationModel>) bundle.getSerializable("location");
        }
        btnNavigate = (Button) findViewById(R.id.navigate);
        btnNavigate.setOnClickListener(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        FirebaseApp.initializeApp(getApplicationContext());


        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseDatabase.getReference("radius").setValue(radius);
        // DatabaseReference databaseReference = mFirebaseDatabase.getReference("https://patienttracker-def27.firebaseio.com/");
        // get reference to 'users' node
        databaseReference = mFirebaseDatabase.getReference().child("users");


        System.out.println("Db refer " + databaseReference);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println("on start of onDataChange");
                patientsTrack = new ArrayList<LocationModel>();

                for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                    LocationModel location = new LocationModel();
                    location.setDeviceId(childDataSnapshot.getKey());
                    location.setLatitude(childDataSnapshot.child("latitude").getValue().toString());
                    location.setLongitude(childDataSnapshot.child("longitude").getValue().toString());
                    location.setTimeObserved(childDataSnapshot.child("timeObserved").getValue().toString());
                    location.setDeviceName(childDataSnapshot.child("deviceName").getValue().toString());
                    System.out.println("**********************");
                    Log.v("Got value getKey ", "" + childDataSnapshot.getKey()); //displays the key for the node
                    Log.v("Got value latitude ", "" + childDataSnapshot.child("latitude").getValue());
                    Log.v("Got value longitude ", "" + childDataSnapshot.child("longitude").getValue());
                    Log.v("Got value deviceId ", "" + childDataSnapshot.child("deviceId").getValue());
                    Log.v("Got value timeObserved ", "" + childDataSnapshot.child("timeObserved").getValue());  //gives the value for given keyname
                    Log.v("Got value deviceId ", "" + childDataSnapshot.child("deviceName").getValue());
                    System.out.println("**********************");
                    patientsTrack.add(location);
                }
                doProximityCheck();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("on cancelled of event listener " + databaseError);

            }
        });


    }


    private void buildGoogleApiClient() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d("TAG", "--onMapReady" + (googleMap != null));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Enabling MyLocation Layer of Google Map
        this.googleMap.setMyLocationEnabled(true);
        buildGoogleApiClient();
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setIndoorEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.googleMap.setOnMapClickListener(this);
        this.googleMap.setOnMapLongClickListener(this);
        googleApiClient.connect();
        loadCoordinates(hospitalLocation);
        addFense();
        // Add a marker in Sydney and move the camera
        //   LatLng sydney = new LatLng(-34, 151);
        // this.googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void loadCoordinates(LatLng location) {
        // Getting LocationManager object from System Service LOCATION_SERVICE


        //  getLocation();
        if (location != null) {
            LatLng loc = new LatLng(location.latitude, location.longitude);
            drawCircle(loc);
            drawMarker(loc, "Hospital Campus");
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
            this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        }
    }


    /* private void checkLocation(MarkerOptions markerOptions,CircleOptions circleOptions){
         Location.distanceBetween(markerOptions.getPosition().latitude,
                 markerOptions.getPosition().longitude, circleOptions.getCenter().latitude,
                 circleOptions.getCenter().longitude, distance);

         if (distance[0] > mCircle.getRadius()) {
             //Do what you need

         }else if (distance[0] < mCircle.getRadius()) {
             //Do what you need
         }
     }*/
    private void drawMarker(LatLng point, String title) {
        Log.d("TAG", "--drawMarker" + (point != null));
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(title);
        markerOptions.position(point);
        this.googleMap.addMarker(markerOptions);

    }

    private void drawCircle(LatLng point) {
        Log.d("TAG", "--drawCircle" + (point != null));
        circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);
        Log.d("TAG", "--this.googleMap" + (this.googleMap != null));
        this.googleMap.addCircle(circleOptions);
    }

    @Override
    public void onMapClick(LatLng point) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (intent != null && intent.getType() != null && intent.getType().equals(NOTIFICATION_INTENT_TYPE)) {
            //Draw route
            routeMe();
        }
    }

    private void routeMe() {
        // Checks, whether start and end locations are captured
        if (patientLocation != null && hospitalLocation != null) {

            // Getting URL to the Google Directions API
            String url = getUrl(patientLocation, hospitalLocation);
            Log.d("onMapClick", url.toString());
            FetchUrl FetchUrl = new FetchUrl();

            // Start downloading json data from Google Directions API
            FetchUrl.execute(url);
            //move map camera
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(patientLocation));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        }
    }

    private void addFense() {
        // Removes the existing marker from the Google Map
        googleMap.clear();

        // Drawing marker on the map
        drawMarker(hospitalLocation, "Hospital Campus");

        // Drawing circle on the map
        // Drawing circle on the map
        drawCircle(hospitalLocation);

        // This intent will call the activity ProximityActivity
        Intent proximityIntent = new Intent("com.emishealth.patienttracker.proximity.receiver");
        proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Creating a pending intent which will be invoked by LocationManager when the specified region is
        // Creating a pending intent which will be invoked by LocationManager when the specified region is
        // entered or exited
        pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, proximityIntent, 0);

        // Setting proximity alert
        // The pending intent will be invoked when the device enters or exits the region 20 meters
        // away from the marked point
        // The -1 indicates that, the monitor will not be expired
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.addProximityAlert(hospitalLocation.latitude, hospitalLocation.longitude, 20, 1000000, pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    /**
     * Returns the consumer friendly device name
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d("TAG", "--onMapLongClickListener");
       /* Intent proximityIntent = new Intent("com.emishealth.patienttracker.proximity.receiver");
        proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityIntent, 0);
        // Removing the proximity alert
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.removeProximityAlert(pendingIntent);

        // Removing the marker and circle from the Google Map
        googleMap.clear();*/


    }

    public String loadIMEI() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 12);
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) this
                    .getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = telephonyManager.getDeviceId();
        }


        System.out.println("ime " + deviceId);
        return deviceId;
    }


    public String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String millisInString = dateFormat.format(new Date());
        return millisInString;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 12: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted!
                    loadIMEI();
                } else {
                    // permission denied
                }
                return;
            }

        }
    }

    private void addNotification(String s) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Patient Alert")
                        .setSound(soundUri)
                        .setSmallIcon(R.drawable.cast_ic_notification_forward)
                        .setContentText(s + " is moving out from safe zone");

        Intent notificationIntent = new Intent(this, LocationTrackerActivity.class);
        notificationIntent.setType(NOTIFICATION_INTENT_TYPE);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        //Vibration
        builder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        //LED
        builder.setLights(Color.RED, 3000, 3000);
        //Ton
        builder.setSound(Uri.parse("android.resource://com.locationmonitor/" + R.raw.alert));//parse("uri://sadfasdfasdf.mp3"));
        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    //Method to check the patient is in safe zone
    private void doProximityCheck() {
        float[] distance = new float[2];
        for (int i = 0; i < patientsTrack.size(); i++) {
            Location startPoint = new Location("locationA");
            startPoint.setLatitude(Double.valueOf(patientsTrack.get(i).getLatitude()));
            startPoint.setLongitude(Double.valueOf(patientsTrack.get(i).getLongitude()));

            Location endPoint = new Location("locationA");
            endPoint.setLatitude(hospitalLocation.latitude);
            endPoint.setLongitude(hospitalLocation.longitude);

            double distance1 = startPoint.distanceTo(endPoint);
            System.out.println(" circleOptions.getRadius() " + circleOptions.getRadius());
            System.out.println(" distance[0] " + distance1);
            String deviceName = patientsTrack.get(i).getDeviceName();
            String[] device = deviceName.split(" ");
            if (device[0] != null && device[0].equalsIgnoreCase("HTC"))
                deviceName = "John";
            if (device[0] != null && device[0].equalsIgnoreCase("MOTOROLA"))
                deviceName = "John";
            if (device[0] != null && device[0].equalsIgnoreCase("SAMSUNG"))
                deviceName = "Mathew";
            drawMarker(new LatLng(Double.valueOf(patientsTrack.get(i).getLatitude()), Double.valueOf(patientsTrack.get(i).getLongitude())), "Patient " + deviceName);
            if (distance1 > circleOptions.getRadius()) {
                patientLocation = new LatLng(Double.valueOf(patientsTrack.get(i).getLatitude()), Double.valueOf(patientsTrack.get(i).getLongitude()));
                addNotification("Patient " + deviceName);

            } else if (distance1 < circleOptions.getRadius()) {


                Toast.makeText(this, "Patient is in safe zone ", Toast.LENGTH_LONG).show();

            }

//            Location.distanceBetween(Double.valueOf(patientsTrack.get(i).getLatitude()),
//                    Double.valueOf(patientsTrack.get(i).getLongitude()), hospitalLocation.latitude,
//                    hospitalLocation.longitude, distance);
//            if (circleOptions != null) {
//                System.out.println(" circleOptions.getRadius() "+ circleOptions.getRadius());
//                System.out.println(" distance[0] "+ distance[0]);
//
//
//                if (distance[0] > circleOptions.getRadius()) {
//                    addNotification("Patient " + patientsTrack.get(i).getDeviceName());
//
//                } else if (distance[0] < circleOptions.getRadius()) {
//                    drawMarker(new LatLng(Double.valueOf(patientsTrack.get(i).getLatitude()), Double.valueOf(patientsTrack.get(i).getLongitude())), "Patient " + i + 1);
//                    Toast.makeText(this, "Patient is in safe zone ", Toast.LENGTH_LONG).show();
//
//                }
//            }
        }

    }

    //
//    public class TokenAcc extends FirebaseInstanceIdService {
//        @Override
//        public void onTokenRefresh() {
//            String token = FirebaseInstanceId.getInstance().getToken();
//            sendRegToServer(token);
//
//
//        }
//    }
//
//    private void sendRegToServer(String token) {
//    }
    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    public class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            if (result != null && result.size() > 0) {
                ArrayList<LatLng> points;
                PolylineOptions lineOptions = null;

                // Traversing through all the routes
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(Color.RED);

                    Log.d("onPostExecute", "onPostExecute lineoptions decoded");

                }

                // Drawing polyline in the Google Map for the i-th route
                if (lineOptions != null) {
                    googleMap.addPolyline(lineOptions);
                } else {
                    Log.d("onPostExecute", "without Polylines drawn");
                }
            }
        }
    }

    public class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }


    }

    private String downloadUrl(String strUrl) throws IOException {
        System.out.println("on start of download url ");
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    public class DataParser {

        /**
         * Receives a JSONObject and returns a list of lists containing latitude and longitude
         */
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

            List<List<HashMap<String, String>>> routes = new ArrayList<>();
            JSONArray jRoutes;
            JSONArray jLegs;
            JSONArray jSteps;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<>();

                    /** Traversing all legs */
                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude));
                                hm.put("lng", Double.toString((list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }


            return routes;
        }


        /**
         * Method to decode polyline points
         * Courtesy : https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }

    @Override
    public void onClick(View v) {
        triggerNavigation();
    }

    private void triggerNavigation() {
        LatLng destination = new LatLng(Double.parseDouble(patientsTrack.get(0).getLatitude()),Double.parseDouble(patientsTrack.get(0).getLongitude()));
        Log.d("TGA","--destination "+destination.latitude+"---"+destination.longitude);
        Intent navigation = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.google_navigate_uri) + destination.latitude + "," + destination.longitude));// 10.985936 + "," + 76.965408)//TODO need to add dynamic destination coordinates
        navigation.setClassName(getResources().getString(R.string.google_map_package), getResources().getString(R.string.google_map_class));
        // To avoid crash if system cannot find app to handle intent
        if (navigation.resolveActivity(getPackageManager()) != null) {
            startActivity(navigation);
        }
    }


}
