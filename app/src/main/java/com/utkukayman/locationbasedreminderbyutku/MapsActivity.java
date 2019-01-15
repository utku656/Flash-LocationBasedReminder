package com.utkukayman.locationbasedreminderbyutku;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.utkukayman.locationbasedreminderbyutku.models.PlaceInfo;
import com.utkukayman.locationbasedreminderbyutku.models.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener
        , GoogleMap.OnMarkerClickListener
        ,OnCompleteListener<Void>
{

    private static final String TAG = "MapsActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int NEW_REMINDER_REQUEST_CODE = 330;
    //whole world
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40,-168),new LatLng(71,136));

    //--
    private AutoCompleteTextView mSearchText;
    private ImageView mGps;
    private ImageView mGeofence;
    private EditText mNotification;
    private EditText mRadius;

    private boolean mPermissionsGranted = false;
    private GoogleMap gMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutoCompleteAdapter autocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;
    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    private PlaceInfo mPlace;
    private Marker marker;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private final String EXTRA_LAT_LNG = "EXTRA_LAT_LNG";
    private final String EXTRA_ZOOM = "EXTRA_ZOOM";


    private ReminderDetails reminder = new ReminderDetails(null, null,  null);

    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mGeofence = (ImageView) findViewById(R.id.ic_circle);
        mRadius = (EditText)findViewById(R.id.RadiusText);
        mNotification=(EditText)findViewById(R.id.NotText);


        getLocPermission();

        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofenceList = new ArrayList<>();
    }
    /******************************************/


    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        gMap = googleMap;
        gMap.getUiSettings().setMapToolbarEnabled(false);
        //gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(EXTRA_LAT_LNG,EXTRA_ZOOM));
        buttonForGeofence();
        if (mPermissionsGranted) {
            getDeviceLocation();
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
            gMap.setMyLocationEnabled(true); //blue dot on map
            gMap.getUiSettings().setMyLocationButtonEnabled(false); //Google's find my location button
            gMap.getUiSettings().setMapToolbarEnabled(false); //Set default directions button false
            gMap.setOnMarkerClickListener(this);

            init();
        }
    }
    private void init(){
        Log.d(TAG, "init: initiliazing");


        mSearchText.setOnItemClickListener(autoCompleteClickListener);

        mGeoDataClient = Places.getGeoDataClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        autocompleteAdapter = new PlaceAutoCompleteAdapter(this,mGeoDataClient,LAT_LNG_BOUNDS,null);


        mSearchText.setAdapter(autocompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH||actionId == EditorInfo.IME_ACTION_DONE
                        ||keyEvent.getAction() == KeyEvent.ACTION_DOWN||keyEvent.getAction()==KeyEvent.KEYCODE_ENTER){

                    //call method for searching
                    geoLocate();

                }
                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });


        hideSoftKeyboard();
        showReminders();
    }

    private void showReminders(){
        gMap.clear();
        for (ReminderDetails reminder : Utils.getAll(this)) {
            Utils.showReminderInMap(this,gMap,reminder);

        }
    }
    private void buttonForGeofence() {
        Log.d(TAG, "buttonForGeofence: inside");
        mGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reminder.latLng = gMap.getCameraPosition().target;
                hideSoftKeyboard();
                Log.d(TAG, "second mgeofence");
//                String radiusValue=mRadius.getText().toString();
//                double finalRadius=Integer.parseInt(radiusValue);

                reminder.radius=Double.parseDouble(mRadius.getText().toString());

                Log.i("NewReminderr", "onClick: "+mRadius.getText().toString());

                reminder.message=mNotification.getText().toString();
                Log.i("NewReminderr", "onClick: "+mNotification.getText().toString());

                if(reminder.message.isEmpty()||reminder.message==null) {
                    mNotification.setError(getString(R.string.error_required));
                    Log.d(TAG, "error message.null");
                }
                else if(reminder.radius==null  ) {              //  bakılması gerekli ************************************** Double null dönmüyor.
                    Log.d(TAG, "error radius.null");
                    mRadius.setError(getString(R.string.error_required));
                }
                else{

                    Log.d(TAG, "adding reminder");
                    addReminder(reminder);
                }
            }
        });
        Log.d(TAG, "reminderupdate reminder");
        showReminderUpdate();
    }

    private void showReminderUpdate() {
        Log.d(TAG, "showReminderUpdate: inside");
//        gMap.clear();
        Utils.showReminderInMap(this,gMap,reminder);
    }


//    private void setButtonOnClickListener(View view){
//        Intent intent = new Intent(this,MapsActivity.class);
//        intent.putExtra("EXTRA_LAT_LNG",gMap.getCameraPosition().target);
//        intent.putExtra("EXTRA_ZOOM",gMap.getCameraPosition().zoom);
//        startActivityForResult(intent,NEW_REMINDER_REQUEST_CODE);
//
//
//    }

    private void geoLocate(){
        hideSoftKeyboard();
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {

            list = geocoder.getFromLocationName(searchString,1);
        }catch (IOException e){
            Log.d(TAG, "geoLocate: IOException"+e.getMessage());
        }

        if(list.size()>0){
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: address"+address.toString());

            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting device location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.d(TAG, "onComplete: found location!!");
                            Location currentLoc = (Location) task.getResult();

                            moveCamera(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), DEFAULT_ZOOM,"My Location");

                        } else {
                            Log.d(TAG, "onComplete: current location not found");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException" + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom,String title) {
        Log.d(TAG, "MoveCamera: moving camera to" + latLng);
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        //create and drop marker on map
        if(title!="My Location"){
            if(marker != null)
                marker.remove();
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);

            marker = gMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    private void initMap() {
        Log.d(TAG, "initMap: inside");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);
    }

    private void getLocPermission() {
        Log.d(TAG, "getLocPermission: inside");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mPermissionsGranted = true;
                initMap();

            } else {
                ActivityCompat.requestPermissions(this, permissions, 123);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionsGranted = false;
        Log.d(TAG, "onRequestPermissionsResult: inside");
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mPermissionsGranted = false;
                            return;
                        }
                    }
                    mPermissionsGranted = true;
                    //if permissions granted initialize map
                    initMap();

                }
            }
        }
    }



    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick: "+marker.getTitle());
        ReminderDetails reminder = Utils.get(this,marker.getTag().toString());

        if (reminder != null) {
            showReminderRemoveAlert(reminder);
        }
        return false;
    }

    private void showReminderRemoveAlert(final ReminderDetails reminder) {
        Log.d(TAG, "showReminderRemoveAlert: inside");
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Remove reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        //Utils.removeReminder(reminder);
                        removeReminder(reminder);
                        //Snackbar.make(this, R.string.reminder_removed_success, Snackbar.LENGTH_LONG).show()
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();


    }

    void removeReminder(ReminderDetails reminder){
        Log.d(TAG, "removeReminder: inside");
        ArrayList<String> mGeofenceList = new ArrayList<>();
        mGeofenceList.add(reminder.id);
        List<ReminderDetails> reminders = Utils.getAll(this);
        for(int i = 0; i<reminders.size();i++){
            if(reminders.get(i).id.equals(reminder.id)){
                reminders.remove(i);
                //break;
            }
        }
        Utils.saveAll(getBaseContext(),reminders);
        showReminders();
        mGeofencingClient.removeGeofences(mGeofenceList);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void hideSoftKeyboard(){
        //hide keyboard
        Log.d(TAG, "hideSoftKeyboard: hide keyboard");
        InputMethodManager InputMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        InputMM.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: inside");
        if (requestCode == NEW_REMINDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            showReminders();

            ReminderDetails reminder = Utils.getLast(this);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(reminder.latLng,DEFAULT_ZOOM));


            View container = findViewById(android.R.id.content);
            if (container != null) {
                Snackbar.make(container, R.string.reminder_added_success, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    //-----------------------------------------------------

    private AdapterView.OnItemClickListener autoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();
            Log.d(TAG, "AdaterView: inside");
            //Log.d(TAG, "onItemClick: getItem id:"+autocompleteAdapter.getItem(i).toString());
            final AutocompletePrediction item = autocompleteAdapter.getItem(i);

            final String placeID = item.getPlaceId();
            Task<PlaceBufferResponse> placeResult = mGeoDataClient.getPlaceById(placeID);
            placeResult.addOnCompleteListener(mUpdatePlaceDetailsCallback);

        }
    };

    private OnCompleteListener<PlaceBufferResponse> mUpdatePlaceDetailsCallback = new OnCompleteListener<PlaceBufferResponse>() {
        @Override
        public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
            if(task.isSuccessful()){
                Log.d(TAG, "onComplete: Task success");
                PlaceBufferResponse places = task.getResult();
                final Place myPlace = places.get(0);
                try{
                   /* mPlace = new PlaceInfo(myPlace.getName().toString(),myPlace.getAddress().toString(),
                            myPlace.getPhoneNumber().toString(),myPlace.getId(),myPlace.getWebsiteUri(),
                            myPlace.getLatLng(),myPlace.getRating(),myPlace.getAttributions().toString());*/
                    mPlace = new PlaceInfo();
                    mPlace.setName(myPlace.getName().toString());
                    mPlace.setAddress(myPlace.getAddress().toString());
                    //mPlace.setAttributions(myPlace.getAttributions().toString());
                    mPlace.setId(myPlace.getId());
                    mPlace.setLatLng(myPlace.getLatLng());
                    mPlace.setRating(myPlace.getRating());
                    mPlace.setPhoneNumber(myPlace.getPhoneNumber().toString());
                    mPlace.setWebURL(myPlace.getWebsiteUri());
                    Log.d(TAG, "onComplete: place:"+mPlace.toString());
                }catch (NullPointerException e){
                    Log.e(TAG,e.getMessage());
                }
                moveCamera(mPlace.getLatLng(),DEFAULT_ZOOM,mPlace.getName());
//                Log.d(TAG,"place details: "+myPlace.getAttributions());
//                Log.d(TAG,"place details: "+myPlace.getViewport());
//                Log.d(TAG,"place details: "+myPlace.getPhoneNumber());
//                Log.d(TAG,"place details: "+myPlace.getWebsiteUri());
//                Log.d(TAG,"place details: "+myPlace.getId());
//                Log.d(TAG,"place details: "+myPlace.getAddress());
//                Log.d(TAG,"place details: "+myPlace.getLatLng());
                places.release();

            }else{
                Log.e(TAG,"Place not Found");
            }
        }
    };



    private void addReminder(ReminderDetails reminder){
        Geofence geofence=buildGeofence(reminder);
        mGeofenceList.add(geofence);
        Log.d(TAG, "addReminder: inside");
        if(geofence!=null&& ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mGeofencingClient.addGeofences(buildGeofencingReq(geofence),getGeofencePendingIntent()).addOnCompleteListener(this);
        }
    }

    private GeofencingRequest buildGeofencingReq(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        Log.d(TAG, "GeofencingRequest: inside");
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private Geofence buildGeofence(ReminderDetails reminder) {
        double lat =reminder.latLng.latitude;
        double longitude=reminder.latLng.longitude;
        double radius=reminder.radius;
        Log.d(TAG, "buildGeofence: inside");
        return new Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                        lat,
                        longitude,
                        (float)radius
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();


    }

    private PendingIntent getGeofencePendingIntent() {

        Log.d(TAG, "PendingIntent : inside");

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }



    @Override
    public void onComplete(@NonNull Task<Void> task) {
        Log.d(TAG, "onComplete: inside");
        if (task.isSuccessful()) {
            List<ReminderDetails> reminderArrayList = Utils.getAll(this);
            reminderArrayList.add(reminder);
            Utils.saveAll(this, reminderArrayList);
            Log.d(TAG, "onComplete: "+new Gson().toJson(mGeofenceList));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        "ServiceChannel",
                        "Example Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(serviceChannel);
            }


            setResult(Activity.RESULT_OK);
            finish();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceError.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }

    }


}