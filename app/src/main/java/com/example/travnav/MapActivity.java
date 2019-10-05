package com.example.travnav;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.travnav.utils.Constant.DESTINATION_COUNT_HEIGHT_BANDWIDTH;
import static com.example.travnav.utils.Constant.DESTINATION_LIST_HEIGHT;

/**
 * Created by User on 10/2/2017.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, DestinationAdapterToMapActivityCallback {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //widgets
    private EditText sourceEditText;
    private ImageView mGps;
    private Button addDestinationBtn;
    private RecyclerView destinationsRcyclrVw;

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLocation;
    private List<String> destinations;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        sourceEditText = (EditText) findViewById(R.id.source);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        addDestinationBtn = (Button) findViewById(R.id.btn_add_destination);
        destinationsRcyclrVw = (RecyclerView) findViewById(R.id.recycler_view_destinations);

        getLocationPermission();
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();
        }
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful() && task.getResult()!=null){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            setCurrentLocation(currentLocation);
                            sourceEditText.setText("Use Current Location");
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                    DEFAULT_ZOOM,
                                    "My Location");

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    public void setCurrentLocation(Location location) {
        currentLocation = new Location(location);
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude );
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title);
        mMap.addMarker(options);
        hideSoftKeyboard();
    }

    private void init(){
        Log.d(TAG, "init: initializing");
        sourceEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                    String source = sourceEditText.getText().toString();
                    if (source.trim().equalsIgnoreCase("")) {
                        sourceEditText.setError("Source can not be blank");
                    }else if (!source.equals("Use Current Location")) {
                        //execute our method for searching
                        geoLocateAndMoveCamera(source);
                    }
                    return true;
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

        destinations = new ArrayList<>();

        // Define a layout for RecyclerView
        mLayoutManager = new LinearLayoutManager(this);
        destinationsRcyclrVw.setLayoutManager(mLayoutManager);

        // Initialize a new instance of RecyclerView Adapter instance
        mAdapter = new DestinationAdapter(this, destinations);

        // Set the adapter for RecyclerView
        destinationsRcyclrVw.setAdapter(mAdapter);

        addDestinationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destinations.add("Add");
                int position = destinations.size() - 1;
                mAdapter.notifyItemInserted(position);
                destinationsRcyclrVw.scrollToPosition(position);

                changeHeightOfRecyclerView();
            }
        });
    }

    public void changeHeightOfRecyclerView() {
        ViewGroup.LayoutParams params=destinationsRcyclrVw.getLayoutParams();
        if (destinations.size() - 1 > DESTINATION_COUNT_HEIGHT_BANDWIDTH) {
            params.height=DESTINATION_LIST_HEIGHT;
        }else {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        destinationsRcyclrVw.setLayoutParams(params);
    }

    private void geoLocateAndMoveCamera(String searchString){
        hideSoftKeyboard();
        Log.d(TAG, "geoLocate: geolocating");

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size() > 0){
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard(){
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public Location convertToLocation(Address address) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(address.getLatitude());
        location.setLongitude(address.getLongitude());
        return location;
    }

    public void OptimisePath(View view) {

//        for (int childCount = destinationsRcyclrVw.getChildCount(), i = 0; i < childCount; ++i) {
//            final RecyclerView.ViewHolder holder = destinationsRcyclrVw.getChildViewHolder(destinationsRcyclrVw.getChildAt(i));
//
//        }


        ArrayList<Location> locations =getLocationsFromEditTexts();

        Intent intent = new Intent(MapActivity.this, OptimisePathActivity.class);
        intent.putParcelableArrayListExtra("LOCATIONS", locations);
        startActivity(intent);
    }

    public ArrayList<Location> getLocationsFromEditTexts() {
        ArrayList<Location> locations = new ArrayList<>();
        String source = sourceEditText.getText().toString();
        if (source.equals("Use Current Location")) {
            locations.add(currentLocation);
        }else {
            locations.add(geoLocate(source));
        }
        for (String destination: destinations) {
            locations.add(geoLocate(destination));
        }
        return locations;
    }

    private Location geoLocate(String searchString){
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size() > 0){
            Address address = list.get(0);
            return convertToLocation(address);
        }
        return null;
    }

    @Override
    public void addDestination(String destination) {
        destinations.remove("Add");
        destinations.add(destination);
        geoLocateAndMoveCamera(destination);
    }

    @Override
    public void deleteDestination(String destination) {
        if (!destination.trim().equalsIgnoreCase(""))
            destinations.remove(destination);
    }
}










