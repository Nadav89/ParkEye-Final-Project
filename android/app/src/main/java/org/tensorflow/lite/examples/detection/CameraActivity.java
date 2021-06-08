/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tracking.DetectorResult;
import org.tensorflow.lite.examples.detection.tracking.LocationData;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;
  private static final String TAG = "CameraActivity";
  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  protected TextView frameValueTextView, locationValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private SwitchCompat apiSwitchCompat;
  private TextView threadsTextView;

  public DetectorResult detectorResult;
  /////////////////////////FireStore fireBase////////////////////////
  private FirebaseAuth fAuth;
  private String userId;
  private FirebaseFirestore fStore = FirebaseFirestore.getInstance();
  public int frameProcess=0;

  /////////////////////////////////////////GPS///////////////////////////////////////////////////

  public static final int DEFAULT_UPDATE_INTERVAL = 1; // How much seconds
  public static final int FAST_UPDATE_INTERVAL = 1; // How much seconds
  private static final int PERMISSIONS_FINE_LOCATION = 99;
  private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = (float) 0.5;
  //TextView text_locationData;
 // TextView latitude_text, longitude_text,userMessage_text,address_text; //Old Variables

  //current location  //SQLite
//  DataBaseHelper dataBaseHelper;
//  DataBaseHelperDetector dataBaseHelperDetector;
  Location currentLocation;
  Location tempLocation;
  Location trueLocationUpdate;
  double trueLat=0;
  double trueLong=0;
  float accuracy_in_meters=0;
  float bearing_in_degrees=0;
  float checkDistance=0;
  String country=null;
  String city=null;
  String street=null;
  float distanceBetween2Points;
  //Location request is a config file for all setting related to FuseLocationProviderClient.
  LocationRequest locationRequest;
  //event that is triggered whenever the update interval is met.
  LocationCallback locationCallBack = new LocationCallback() {

    @Override
    public void onLocationResult(LocationResult locationResult) {
      super.onLocationResult(locationResult);

      //save the location
      updateUIValues(locationResult.getLastLocation());
    }
  };
  //Google's API for location services, the majority of the app functions using this class.
  FusedLocationProviderClient fusedLocationProviderClient;
/////////////////////////////////////////////End GPS/////////////////////////////////////////////////

////////tilt the phone/////////////////////////////

  public Accelerometer accelerometer;
  public boolean checkTilt=false;
  //////////end tilt the phone///////////////////////
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    detectorResult= new DetectorResult();


    setContentView(R.layout.tfe_od_activity_camera);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    apiSwitchCompat = findViewById(R.id.api_info_switch);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //                int width = bottomSheetLayout.getMeasuredWidth();
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
              case BottomSheetBehavior.STATE_HIDDEN:
                break;
              case BottomSheetBehavior.STATE_EXPANDED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24);
                }
                break;
              case BottomSheetBehavior.STATE_COLLAPSED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24);
                }
                break;
              case BottomSheetBehavior.STATE_DRAGGING:
                break;
              case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetArrowImageView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24);
                break;
            }
          }

          @Override
          public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

    frameValueTextView = findViewById(R.id.frame_info);
    locationValueTextView = findViewById(R.id.location_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);

    apiSwitchCompat.setOnCheckedChangeListener(this);

    plusImageView.setOnClickListener(this);
    minusImageView.setOnClickListener(this);

    /////////////////////////FireStore fireBase////////////////////////
    fAuth = FirebaseAuth.getInstance();
    fStore = FirebaseFirestore.getInstance();

    /////////////////////////////////////GPS////////////////////////////////////
    distanceBetween2Points = 0;
    tempLocation = new Location("");
    trueLocationUpdate = new Location("");
    //SQLite
//    dataBaseHelper = new DataBaseHelper(CameraActivity.this);
//    dataBaseHelperDetector = new DataBaseHelperDetector(CameraActivity.this);

    //dataBaseHelper.clearSQLiteDataBase();
   // dataBaseHelperDetector.clearSQLiteDataBase();


    ///////////////////GPS//////////////////////////////////
    //set all properties of LocationRequest
    locationRequest = new LocationRequest();

    //how often dose the default location check occur?
    locationRequest.setInterval(500 * DEFAULT_UPDATE_INTERVAL); //set the interval in which you want to get locations
    //how often dose the default location check occur when set to the most frequency update?
    //locationRequest.setFastestInterval(500 * FAST_UPDATE_INTERVAL); //if a location is available sooner you can get it (i.e. another app is using the location services).
    //locationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    ///get permission from the user
    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(CameraActivity.this);
    updateGPS();
    startLocationUpdates(); //GPS Related
  }
///////////////End On Create////////////////////

  private void startLocationUpdates() {
    Toast.makeText(this, "Location is being tracked", Toast.LENGTH_SHORT).show();

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //  ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      Toast.makeText(this, ":((((((((((((((", Toast.LENGTH_SHORT).show();
      return;
    }
    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
    updateGPS();
  }

  private void stopLocationUpdates() {
    Toast.makeText(this, "Location is NOT being tracked", Toast.LENGTH_SHORT).show();
    fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
  }

  private void updateGPS(){
    //get permission from the user to track GPS.
    //get the current location from the fused client.
    //update th UI - i.e set all properties in their associated text view items.
    if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
      //user provided the permission

      fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
          //we got permissions. Put the value of location into the UI components.
          updateUIValues(location);
          currentLocation = location;
        }
      });
    }
    else{
      //permission not granted yet

      if(Build.VERSION.SDK_INT  >= Build.VERSION_CODES.M){
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
      }
    }
  }

  private void updateUIValues(Location location) {
    //update all of the text view objects with a new location

    DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SS");
    Date date = new Date();
    String currentDateTimeString = dateFormat.format(date);
   Timestamp currentDateTimeStamp= Timestamp.now();

    //String currentDateTimeString = java.text.DateFormat.getDateTimeInstance().format(new Date());
    // textView is the TextView view that should display it
    // Toast.makeText(this, currentDateTimeString, Toast.LENGTH_SHORT).show();

    //calculate distance between 2 points
    distanceBetween2Points = location.distanceTo(tempLocation);
    tempLocation = location;

    /// Get address from location///




    ///End Get Address From Location///
    ////////Snap to nearest road////////
    String lat=Double.toString(location.getLatitude());
    String lon= Double.toString(location.getLongitude());
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

                  Geocoder geocoder = new Geocoder(CameraActivity.this, Locale.ENGLISH);
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
                 checkDistance=location.distanceTo(trueLocationUpdate);
                 accuracy_in_meters=location.getAccuracy();
                 bearing_in_degrees=location.getBearing();

                 if(checkDistance > 50 || accuracy_in_meters >= 6){ //|| location.getSpeed()<2
                   trueLat=0;
                   trueLong=0;
                 }

                  showFrameInfo("Speed: "+String.valueOf(location.getSpeed()*3.6)+"[km/h], Bearing: "+ String.valueOf(bearing_in_degrees)+" Accuracy: "+String.valueOf(accuracy_in_meters));
                 // mJSONList.set(0,new JSONItem(trueLat,trueLong)); One of the tests, currently not needed

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

    /////////////////////////// Start Data base ///////////////////////////////////////
  ///////SQL Lite///////
   //DataBaseHelperDetector dataBaseHelperDetector = new DataBaseHelperDetector(CameraActivity.this);
  // DetectorResult detectorResult = new DetectorResult(dataBaseHelperDetector.getTheLastUpdate());

   int parking_status = detectorResult.getParking_status();
   String time_detector = detectorResult.getTime_detector();

   final LocationData locationData = new LocationData(-1,city,street,trueLat,trueLong,currentDateTimeString,distanceBetween2Points, parking_status,time_detector,accuracy_in_meters);

//   DataBaseHelper dataBaseHelper = new DataBaseHelper(CameraActivity.this);
//   Boolean success = dataBaseHelper.addOne(locationData);

    //Toast.makeText(CameraActivity.this,"Success add to DataBase SQLite " + success,Toast.LENGTH_SHORT).show();
    ///////End SQL Lite///////

    //////Start Firestore Database///////
    Map<String,Object> fsLocation = new HashMap<>();
    fsLocation.put("City",city);
    fsLocation.put("Street",street);
    fsLocation.put("Latitude",trueLat);
    fsLocation.put("Longitude",trueLong);
    fsLocation.put("Detections",parking_status);
    fsLocation.put("Time",currentDateTimeStamp);
    fsLocation.put("Bearing", bearing_in_degrees);

    userId = fAuth.getCurrentUser().getUid();
    if ((trueLat != 0 || trueLong != 0) && checkTilt && location.getSpeed()>2) {

      fStore.collection("Users").document(userId).collection("DetectionData").document("Data").set(fsLocation)
              .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                  //Toast.makeText(CameraActivity.this, "Location saved", Toast.LENGTH_SHORT).show();
                }
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Toast.makeText(CameraActivity.this, "Error!", Toast.LENGTH_SHORT).show();
                  Log.d(TAG, e.toString());
                }
              });
    }
    //////End Firestore Database///////
    showLocationInfo(locationData.Coordinates());
    //Toast.makeText(MainActivity.this,locationData.toString(),Toast.LENGTH_SHORT).show();
    ///////////////////////////END Data base///////////////////////////////////////



  }



  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };

      processImage();

  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };


        processImage();


    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();
    accelerometer.register(); //related to tilt
    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);
    accelerometer.unregister(); //related to tilt
    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
      LOGGER.d("onStop " + this);
    stopLocationUpdates();
    super.onStop();


  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
          updateGPS();
        }
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }


  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showLocationInfo(String locationInfo) {
    locationValueTextView.setText(locationInfo);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);
}
