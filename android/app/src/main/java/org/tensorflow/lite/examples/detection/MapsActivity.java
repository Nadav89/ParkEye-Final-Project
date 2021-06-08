package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonLineStringStyle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.tracking.DataBaseHelper;
import org.tensorflow.lite.examples.detection.tracking.ParkData;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    public static final int timeToUpdate = 60; // How much seconds
    public Location currentlocation;
    public Location trueLocationUpdate;
    float checkDistance=0;
    double trueLat=0;
    double trueLong=0;
    String country=null;
    String city=null;
    String street=null;
    View mapView; //Related to 'My Location' button
    private GoogleMap mMap;
    Button cameraButton;
    Button leaving_parking;
    TextView clock_view;
    //Search Button//
    SupportMapFragment mapFragment;
    SearchView searchView;
    //End Search Button//
    //GeoJson//
    GeoJsonLayer layer = null;
    double capacity = 0;
    //End GeoJson//

    /////////////////////////FireStore fireBase////////////////////////
    private FirebaseFirestore fStore = FirebaseFirestore.getInstance();
    private FirebaseAuth fAuth;
    private String userId;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
    ;
    private CollectionReference ColRefCity;
    private DocumentReference DocRefCity;
    private CollectionReference ColRefStreet;
    private DocumentReference DocRefStreet;
    private CollectionReference ColRefPark;
    private DocumentReference DocRefPark;
    private double latitude;
    private double longitude;
    private int available;
    private Timestamp updateTime;
    /////////////////////////End FireStore fireBase////////////////////////
    FusedLocationProviderClient fusedLocationProviderClient;
    private int ACCESS_LOCATION_REQUEST_CODE = 10001;
    private ArrayList<Marker> mTripMarkers = new ArrayList<>(); //Array for markers from search button


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        trueLocationUpdate=new Location("");
        currentlocation=new Location("");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapView = mapFragment.getView(); //Related to 'My Location' button
        mapFragment.getMapAsync(this);
        DataBaseHelper dataBaseHelper = new DataBaseHelper(MapsActivity.this);
        //Leaving Parking Space Button
        leaving_parking = findViewById(R.id.leaving_parking);
        clock_view = findViewById(R.id.clock_view);
        //Search Button//
        searchView = findViewById(R.id.sv_location);

        showTime(clock_view);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
       /*
        cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(

                v -> startActivity(new Intent(this, DetectorActivity.class))
        );
        */
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                removeTripMarkers();
                String location = searchView.getQuery().toString();
                List<Address> addressList = null;
                if (location != null || !location.equals("")) {
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                        if (addressList.size() != 0) {
                            Address address = addressList.get(0);
                            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            mTripMarkers.add(marker);
                        } else {
                            Toast.makeText(MapsActivity.this, "Address not found", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mapFragment.getMapAsync(this);
        //End Search Button//

        Thread thread = new Thread() {
            @Override
            public void run() {

                try {
                    while (!isInterrupted()) {
                        Thread.sleep(timeToUpdate * 1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.clear();
                                getAllParkingSpaces();

                                capacityUpdate(new Callback() {
                                    @Override
                                    public void myResponseCallback(List<DocumentSnapshot> list) {
                                        int i = 0;
                                        for (DocumentSnapshot d : list) {
                                            Street street = d.toObject(Street.class);
                                            String street_name = list.get(i).getId();
                                            double capacity = street.getCapacity();
                                            for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through feature
                                                if (feature.getProperty("name").equals(street_name)) {
                                                    //capacity= Double.parseDouble(feature.getProperty("capacity"));
                                                    feature.setProperty("capacity", String.valueOf(capacity));
                                                    GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();

                                                    if (capacity < 50) {
                                                        lineStringStyle.setColor(Color.GREEN);
                                                        feature.setLineStringStyle(lineStringStyle);
                                                    } else if (capacity >= 50 && capacity < 80) {
                                                        lineStringStyle.setColor(Color.YELLOW);
                                                        feature.setLineStringStyle(lineStringStyle);
                                                    } else {
                                                        lineStringStyle.setColor(Color.RED);
                                                        feature.setLineStringStyle(lineStringStyle);
                                                    }
                                                    lineStringStyle.setWidth(5);
                                                }
                                                layer.addLayerToMap();
                                            }
                                            i++;

                                        }

                                    }
                                });
                                showTime(clock_view);
                            }
                        });

                    } //End while
                }//End try

                catch (Exception e) {
                    Toast.makeText(MapsActivity.this, "Error in Timer", Toast.LENGTH_SHORT).show();
                }

            }
        };
        thread.start();


        leaving_parking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        getLocation();
                        Toast.makeText(MapsActivity.this, "The Parking Will Be Updated, Thank you! :D ", Toast.LENGTH_SHORT).show();

                    } else {
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
                    }

                }
        });


    } //////////End On Create///////////////


    //Removes the previous marker when another search has begun
    private void removeTripMarkers() {
        for (Marker marker : mTripMarkers) {
            marker.remove();
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
        mMap = googleMap;
        DataBaseHelper dataBaseHelper = new DataBaseHelper(MapsActivity.this);
        //showLocationOnMap(dataBaseHelper);

        /*
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SS");
        Date date = new Date();

        if(date.getTime()>18 && date.getTime()<06) {
            mMap.setMapType(GoogleMap.); //set map type
        }
        else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL); //set map type
        }

         */

        getAllParkingSpaces();
        try {
            layer = new GeoJsonLayer(mMap, R.raw.newbavli, this);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
////////////Read capacity from geojson file and update street colors////////////
        capacityUpdate(new Callback() {
            @Override
            public void myResponseCallback(List<DocumentSnapshot> list) {
                int i = 0;
                for (DocumentSnapshot d : list) {
                    Street street = d.toObject(Street.class);
                    String street_name = list.get(i).getId();
                    double capacity = street.getCapacity();
                    for (GeoJsonFeature feature : layer.getFeatures()) {  //loop through feature
                        if (feature.getProperty("name").equals(street_name)) {
                            //capacity= Double.parseDouble(feature.getProperty("capacity"));
                            feature.setProperty("capacity", String.valueOf(capacity));
                            GeoJsonLineStringStyle lineStringStyle = new GeoJsonLineStringStyle();

                            if (capacity < 50) {
                                lineStringStyle.setColor(Color.GREEN);
                                feature.setLineStringStyle(lineStringStyle);
                            } else if (capacity >= 50 && capacity < 80) {
                                lineStringStyle.setColor(Color.YELLOW);
                                feature.setLineStringStyle(lineStringStyle);
                            } else {
                                lineStringStyle.setColor(Color.RED);
                                feature.setLineStringStyle(lineStringStyle);
                            }
                            lineStringStyle.setWidth(5);
                        }
                        layer.addLayerToMap();
                    }
                    i++;

                }

            }
        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        }

        //Moves the 'My Location' button to the bottom left corner
        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 170);
        }
        //Add Compass to the UI


    }


    private void enableUserLocation() {
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
        mMap.setMyLocationEnabled(true);

    }

    interface Callback {
        void myResponseCallback(List<DocumentSnapshot> list);
    }

    private void capacityUpdate(final Callback callback) {
        fStore.collection("City").document("Tel Aviv-Yafo").collection("Street").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                if (!queryDocumentSnapshots.isEmpty()) {
                    List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                    callback.myResponseCallback(list);
                }
            }
        });

    }


    private void getAllParkingSpaces() {
        /////////////Import from Firestore/////////

        ColRefCity = fStore.collection("City");

        ColRefCity.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

            @Override
            public void onSuccess(QuerySnapshot queryDocumentCity) {
                for (QueryDocumentSnapshot documentCity : queryDocumentCity) { //loop of all document in City collection

                    DocRefCity = fStore.collection("City").document(documentCity.getId());
                    ColRefStreet = DocRefCity.collection("Street");

                    ColRefStreet.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentStreet) {
                            for (QueryDocumentSnapshot documentStreet : queryDocumentStreet) { //loop of all document in Street collection
                                DocRefStreet = ColRefStreet.document(documentStreet.getId());
                                ColRefPark = DocRefStreet.collection("Parking");
                                ColRefPark.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentPark) {
                                        for (QueryDocumentSnapshot documentPark : queryDocumentPark) { //loop of all document in Parking collection

                                            ParkData parkData = documentPark.toObject(ParkData.class);
                                            LatLng latlng = new LatLng(parkData.getLatitude(), parkData.getLongitude());
                                            MarkerOptions markerOptions = new MarkerOptions();
                                            markerOptions.position(latlng);
                                            markerOptions.title(parkData.DataToString() + "; ID: " + documentPark.getId());
                                            if(parkData.getPotentialParkingSpots()!=0) {
                                                if (parkData.getAvailable() == 0) {
                                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                                } else if (parkData.getAvailable() == 1) {
                                                    //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                                    markerOptions.icon((BitmapDescriptorFactory.fromResource(R.drawable.parkeye_yellow_marker)));
                                                    mMap.addMarker(markerOptions);
                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19.0f));
                                                } else {
                                                    // markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                                    markerOptions.icon((BitmapDescriptorFactory.fromResource(R.drawable.parkeye_green_marker)));
                                                    mMap.addMarker(markerOptions);
                                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19.0f));
                                                }
                                            } 

                                        }

                                    }
                                });

                                Log.i(null, "onSuccess: ");

                            }
                        }
                    });

                }
                Log.i(null, "onSuccess: Pause Here to Check");
            }
        });
        /////////////End Import from Firestore/////////


    }

    public Task<HttpsCallableResult> leavingParking() {


        ////////Snap to nearest road////////
        String lat=Double.toString(currentlocation.getLatitude());
        String lon= Double.toString(currentlocation.getLongitude());
        // ArrayList<JSONItem> mJSONList=new ArrayList<>(); One of the tests, currently not needed
        //mJSONList.add(new JSONItem(0,0)); One of the tests, currently not needed
        RequestQueue requestQueue= Volley.newRequestQueue(this);
        JsonObjectRequest objectRequest=new JsonObjectRequest(
                Request.Method.GET,
                "https://roads.googleapis.com/v1/nearestRoads?points=" + lat + "," + lon + "&key=AIzaSyB6wHXrPrI9x2dc1hmooVwJeo2_BgVfGgs",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e("Rest Response", response.toString());

                        try {
                            JSONArray jsonArray = response.getJSONArray("snappedPoints");
                            JSONObject trueLocationArray= jsonArray.getJSONObject(0);
                            JSONObject trueLocation=trueLocationArray.getJSONObject("location");

                            trueLat =trueLocation.getDouble("latitude");
                            trueLong =trueLocation.getDouble("longitude");

                            Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.ENGLISH);
                            try{
                                List<Address> addresses = geocoder.getFromLocation(trueLat,trueLong,1);
                                country= addresses.get(0).getCountryName();
                                street= addresses.get(0).getThoroughfare();
                                city=addresses.get(0).getLocality();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                //address_text.setText("Unable to get street address");
                            }
                            trueLocationUpdate.setLatitude(trueLat);
                            trueLocationUpdate.setLongitude(trueLong);
                            checkDistance=currentlocation.distanceTo(trueLocationUpdate);
                            if(checkDistance>50 ){
                                trueLat=0;
                                trueLong=0;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Rest Response", error.toString());
                    }
                }
        );
        requestQueue.add(objectRequest);

        ////////End Snap to nearest road////////
        Map<String,Object> data= new HashMap<>();
        data.put("Latitude",trueLat);
        data.put("Longitude",trueLong);
        data.put("City",city);
        data.put("Street", street);
        return mFunctions
                .getHttpsCallable("leavingParking")
                .call(data).addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                    @Override
                    public void onSuccess(HttpsCallableResult httpsCallableResult) {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                    }
                });

    }

    private void getLocation() {
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
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                currentlocation=task.getResult();
                if(currentlocation!= null){
                    Geocoder geocoder= new Geocoder(MapsActivity.this, Locale.ENGLISH);
                    leavingParking();
                }
            }
        });
    }

    private void showParkingLocationsOnMap(List<ParkData> ParkingList) {

        for(int i=0; i<ParkingList.size(); i++) {

            LatLng latlng = new LatLng(ParkingList.get(i).getLatitude(),ParkingList.get(i).getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latlng);
            markerOptions.title(ParkingList.get(i).DataToString());
            if(ParkingList.get(i).getAvailable()==0){
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            }
            else if(ParkingList.get(i).getAvailable()==1){
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            }
            else{
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            }

            mMap.addMarker(markerOptions);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19.0f));
        }
    }
    public void showTime(TextView clockView){
        Calendar calendar  = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
        String currentTime = simpleDateFormat.format(calendar.getTime());
        clockView.setText("Last Updated: "+currentTime);
    }




}
//    private void showLocationOnMap(DataBaseHelper dataBaseHelper) {
//
//        List<LocationData> locationDataList = dataBaseHelper.getEveryone();
//
//        for(int i=0; i<locationDataList.size(); i++) {
//            LatLng latlng = new LatLng(locationDataList.get(i).getLatitude(),locationDataList.get(i).getLongitude());
//            MarkerOptions markerOptions = new MarkerOptions();
//            markerOptions.position(latlng);
//            markerOptions.title("(" + latlng.latitude + ", " + latlng.longitude + ")" + locationDataList.get(i).getTime());
//            mMap.addMarker(markerOptions);
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 19.0f));
//        }
//    }
//}

