package com.arturagapov.speechmaster;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Artur Agapov on 09.11.2016.
 */
public class NotificationScheduledService extends IntentService {
    // An ID used to post the notification.
    private static final int NOTIFICATION_ID = 200003;

    public NotificationScheduledService() {
        super("NotificationScheduledService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(getClass().getSimpleName(), "I ran!");

        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.DAY_OF_WEEK) == 1 || calendar.get(Calendar.DAY_OF_WEEK) == 3 || calendar.get(Calendar.DAY_OF_WEEK) == 5) {
            if (calendar.get(Calendar.HOUR_OF_DAY) == 9 && calendar.get(Calendar.MINUTE) >= 10 && calendar.get(Calendar.MINUTE) <= 11) {
                String message;
                message = (getResources().getString(R.string.notify_message_long) + getResources().getString(R.string.app_name));

                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Intent activityIntent = new Intent(this, SpeechMasterActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                Notification.Builder builder = new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.logo))
                        .setContentIntent(pendingIntent)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setTicker(getResources().getString(R.string.notify_message))
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
                //.build();

                Notification notification;
                notification = builder.build();

                nm.notify(NOTIFICATION_ID, notification);
            }
        }
    }
}
