package org.tensorflow.lite.examples.detection;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class myNotification extends Application {
    public static final String NOTIFICATION_ID="Notification";
    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel=new NotificationChannel(
                    NOTIFICATION_ID,
                    "Notification",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationChannel.setDescription("This is a Notification");

            NotificationManager manager=getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }
    }
}

