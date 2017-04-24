package com.pcontroller.controllers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pcontroller.R;
import com.pcontroller.entities.LocationModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationReceiver extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener {

    private GoogleMap googleMap;
    private LocationManager locationManager;
    private PendingIntent pendingIntent;
    private GoogleApiClient googleApiClient;
    private double longitude;
    private double latitude;
    private DatabaseReference databaseReference;
    private List<LocationModel> patientsTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        System.out.println(" on start of saveToDB");
        FirebaseApp.initializeApp(getApplicationContext());
        //   mFirebaseInstance = FirebaseDatabase.getInstance();
//        mFirebaseDatabase = mFirebaseInstance.getReference("users");
//
//
//
//        // store app title to 'app_title' node
//        mFirebaseInstance.getReference("Track").setValue("TrackerDB");
//
//        mFirebaseDatabase.child(loadIMEI()).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                LocationModel user = dataSnapshot.getValue(LocationModel.class);
//                addNotification();
//
//                Log.d("Mapsact Location value ", "lat : " + user.getLatitude() + ", lon " + user.getLongitude());
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                // Failed to read value
//                Log.w("MapsAct", "Failed to read value.", error.toException());
//            }
//        });


        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        // DatabaseReference databaseReference = mFirebaseDatabase.getReference("https://patienttracker-def27.firebaseio.com/");
        // get reference to 'users' node
        databaseReference = mFirebaseDatabase.getReference("users");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                patientsTrack = new ArrayList<LocationModel>();

                for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                    LocationModel location = new LocationModel();
                    location.setDeviceId(childDataSnapshot.getKey());
                    location.setLatitude(childDataSnapshot.child("latitude").getValue().toString());
                    location.setLongitude(childDataSnapshot.child("longitude").getValue().toString());
                    location.setTimeObserved(childDataSnapshot.child("timeObserved").getValue().toString());
                    System.out.println("**********************");
                    Log.v("Got value getKey ", "" + childDataSnapshot.getKey()); //displays the key for the node
                    Log.v("Got value latitude ", "" + childDataSnapshot.child("latitude").getValue());
                    Log.v("Got value longitude ", "" + childDataSnapshot.child("longitude").getValue());
                    Log.v("Got value deviceId ", "" + childDataSnapshot.child("deviceId").getValue());
                    Log.v("Got value timeObserved ", "" + childDataSnapshot.child("timeObserved").getValue());  //gives the value for given keyname
                    System.out.println("**********************");
                }
                doProximityCheck();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        // Add a marker in Sydney and move the camera
        //   LatLng sydney = new LatLng(-34, 151);
        // this.googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void loadCoordinates() {
        // Getting LocationManager object from System Service LOCATION_SERVICE
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //  getLocation();
        if (latitude != 0F) {
            Log.d("TAG", "---latitude not null " + (new LatLng(latitude, longitude) != null));
            LatLng loc = new LatLng(latitude, longitude);
            drawCircle(loc);
            drawMarker(loc);
            this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
            this.googleMap.animateCamera(CameraUpdateFactory.zoomTo(20));
        }
    }

    private void drawMarker(LatLng point) {
        Log.d("TAG", "--drawMarker" + (point != null));
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        this.googleMap.addMarker(markerOptions);
    }

    private void drawCircle(LatLng point) {
        Log.d("TAG", "--drawCircle" + (point != null));
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(5);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);
        Log.d("TAG", "--this.googleMap" + (this.googleMap != null));
        this.googleMap.addCircle(circleOptions);
    }

    @Override
    public void onMapClick(LatLng point) {
        // Removes the existing marker from the Google Map
        googleMap.clear();

        // Drawing marker on the map
        drawMarker(point);

        // Drawing circle on the map
        drawCircle(point);

        // This intent will call the activity ProximityActivity
        Intent proximityIntent = new Intent("com.emishealth.patienttracker.proximity.receiver");
        proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Creating a pending intent which will be invoked by LocationManager when the specified region is
        // entered or exited
        pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityIntent, 0);

        // Setting proximity alert
        // The pending intent will be invoked when the device enters or exits the region 20 meters
        // away from the marked point
        // The -1 indicates that, the monitor will not be expired
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.addProximityAlert(point.latitude, point.longitude, 5, -1, pendingIntent);
        Toast.makeText(getBaseContext(), "Proximity Alert is added", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
            //   LatLng loc = new LatLng(latitude, longitude);
            loadCoordinates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.d("TAG", "--onMapLongClickListener");
        Intent proximityIntent = new Intent("com.emishealth.patienttracker.proximity.receiver");
        proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityIntent, 0);
        // Removing the proximity alert
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        googleMap.clear();


    }

    public String loadIMEI() {
        TelephonyManager telephonyManager = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }


    public String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String millisInString = dateFormat.format(new Date());
        return millisInString;
    }

    private void addNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Alert")
                        .setSmallIcon(R.drawable.cast_ic_notification_forward)
                        .setContentText("Patient is moving out from safe zone");

        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }


    //Method to check the patient is in safe zone
    private void doProximityCheck() {
        float[] distance = new float[2];
        for (int i = 0; i < patientsTrack.size(); i++) {

            Location.distanceBetween(Double.valueOf(patientsTrack.get(i).getLatitude()),
                    Double.valueOf(patientsTrack.get(i).getLongitude()), 13.0067074,
                    80.2022314, distance);


//            if (distance[0] > mCircle.getRadius()) {
//                //Do what you need
//
//            } else if (distance[0] < mCircle.getRadius()) {
////Do what you need
//            }
        }

        addNotification();
    }


}
