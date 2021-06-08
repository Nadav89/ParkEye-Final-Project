package org.tensorflow.lite.examples.detection;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.YoloV4Classifier;
import org.tensorflow.lite.examples.detection.tracking.DataBaseHelper;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
    private static final String NOTIFICATION_ID ="Notification" ;
    DataBaseHelper dataBaseHelper;

    ////////tilt the phone/////////////////////////////

    private Accelerometer accelerometer;
    public boolean checkTilt=false;
    //////////end tilt the phone///////////////////////


//////////////////////// Menu///////////////////////////////

    StringBuilder stringBuilder = new StringBuilder();
    private TextView helpText;
    private TextView titleHelpText;
    private ScrollView helpViewerText;
    private Button bt_cancel;

    private NotificationManagerCompat   notificationManagerCompat;

    public MainActivity() {
    }

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu1) {
            Toast.makeText(getApplicationContext(), "Help Menu 1", Toast.LENGTH_SHORT).show();
            setHelpViewer();
        }
        return super.onOptionsItemSelected(item);
    }


    public void setHelpViewer(){
        dialogBuilder = new AlertDialog.Builder(this);
        final View helpPopupView = getLayoutInflater().inflate(R.layout.helppopup,null);

        dialogBuilder.setView(helpPopupView);
        dialog = dialogBuilder.create();
        dialog.show();

    }
    //////////////////////End Menu///////////////////////////////



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NavigateButton = findViewById(R.id.NavigateButton);
        //clearSQLite = findViewById(R.id.clearSQLite);
        //bt_showList = findViewById(R.id.bt_showList);
        bt_showCamera = findViewById(R.id.bt_showCamera);
       // detectButton = findViewById(R.id.detectButton);
        imageView = findViewById(R.id.imageView);
        check_tilt_text=findViewById(R.id.check_tilt);
        helpButton=findViewById(R.id.help_button);

//        NavigateButton.setOnClickListener(
//
//                v -> startActivity(new Intent(MainActivity.this, MapsActivity.class))
//        );
        NavigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    Intent i = new Intent(MainActivity.this, MapsActivity.class);
                    startActivity(i);

            }
        });
        initBox();

//        bt_showList.setOnClickListener(
//
//                v -> startActivity(new Intent(MainActivity.this, ShowLocationList.class))
//        );

        bt_showCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkTilt) {
                    Intent i = new Intent(MainActivity.this, DetectorActivity.class);
                    startActivity(i);
                }else{
                    Toast.makeText(MainActivity.this, "Put Your Phone In The Direction Of The Drive!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dataBaseHelper = new DataBaseHelper(MainActivity.this);
//        clearSQLite.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dataBaseHelper.clearSQLiteDataBase();
//                Toast.makeText(MainActivity.this, "Database has cleared", Toast.LENGTH_SHORT).show();
//            }
//        });
        ////////tilt the phone/////////////////////////////

        //text
        accelerometer = new Accelerometer(this);
        accelerometer.setListener(new Accelerometer.Listener() {
            @Override
            public void onTranslation(float tx, float ty, float tz) {
                String tiltPhone2 = "tx = "+tx+" ; ty = "+ty+" ; tz= "+tz;

                if(ty < 8){
                    checkTilt=false;
                    check_tilt_text.setText("Set your phone in the direction of the car");
                }else{
                    checkTilt=true;
                    check_tilt_text.setText("");

                }

            }
        });
        //////////end tilt the phone///////////////////////



        /////////////Notification Icon///////////////

    notificationManagerCompat=NotificationManagerCompat.from(this);
        sendOnChannel();
        /////////End Notification Icon//////////////
    }/////End On Create/////////

    public void helpButtonDisplay(View v){
        dialogBuilder = new AlertDialog.Builder(this);
        final View helpPopupView = getLayoutInflater().inflate(R.layout.helppopup,null);

        dialogBuilder.setView(helpPopupView);
        dialog = dialogBuilder.create();
        dialog.show();
    }

public void sendOnChannel(){
        Intent activityIntent=new Intent(this, MainActivity.class);
    PendingIntent contentIntent=PendingIntent.getActivity(this, 0,activityIntent,0);
    Notification notification= new NotificationCompat.Builder(this,NOTIFICATION_ID)
            .setSmallIcon(R.drawable.parkeye_logo_03)
            .setContentTitle("ParkEye")
            .setContentText("Press To Return To The Application")
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

    notificationManagerCompat.notify(1,notification);
}
///////////////Logout///////////////////////
    public void logout(View view) {
        FirebaseAuth.getInstance().signOut(); //logout
        startActivity(new Intent(getApplicationContext(), Login.class));
        finish();
    }
///////////////Logout///////////////////////

    private static final Logger LOGGER = new Logger();

    public static final int TF_OD_API_INPUT_SIZE = 192;

    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "yolov4-tiny-192.tflite";

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/Vehicles.txt";

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = false;
    private Integer sensorOrientation = 90;

    private Classifier detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private OverlayView trackingOverlay;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;

    private Button clearSQLite, bt_showList,bt_showCamera;  //GPS Related
    private Button NavigateButton;
    private ImageView imageView;
    private TextView check_tilt_text;
    private Button helpButton;
    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        tracker = new MultiBoxTracker(this);
        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));

        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);

        try {
            detector =
                    YoloV4Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private void handleResult(Bitmap bitmap, List<Classifier.Recognition> results) {
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

        for (final Classifier.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                canvas.drawRect(location, paint);
//                cropToFrameTransform.mapRect(location);
//
//                result.setLocation(location);
//                mappedRecognitions.add(result);
            }
        }
//        tracker.trackResults(mappedRecognitions, new Random().nextInt());
//        trackingOverlay.postInvalidate();
        imageView.setImageBitmap(bitmap);
    }
    ////////tilt the phone/////////////////////////////

    @Override
    protected void onResume() {
        super.onResume();
        accelerometer.register();

    }

    @Override
    protected void onPause() {
        super.onPause();
        accelerometer.unregister();

    }
    //////////end tilt the phone///////////////////////


}
