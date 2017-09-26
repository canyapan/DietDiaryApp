package com.canyapan.dietdiaryapp.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.canyapan.dietdiaryapp.MainActivity;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.DailyReminderHelper;

public class DailyReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "DailyReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received");

        if (EventHelper.hasEventToday(context) == 0) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationCompat.CATEGORY_REMINDER)
                    .setTicker(context.getString(R.string.daily_notification_title))
                    .setSmallIcon(R.drawable.app_icon_notify)
                    .setContentTitle(context.getString(R.string.daily_notification_title))
                    .setContentText(context.getString(R.string.daily_notification_message))
                    .setAutoCancel(true)
                    //.setLights(Color.GREEN, 1, 1)
                    //.setSound()
                    //.setVibrate()
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentIntent(TaskStackBuilder.create(context)
                            .addParentStack(MainActivity.class)
                            .addNextIntent(new Intent(context, MainActivity.class))
                            .getPendingIntent(DailyReminderHelper.REQUEST_CODE, 0));
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(TAG, R.id.daily_notification, builder.build());
        }

        // Setup next alarm.
        DailyReminderHelper.register(context);
    }
}
