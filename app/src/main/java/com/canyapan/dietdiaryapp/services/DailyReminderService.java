package com.canyapan.dietdiaryapp.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.canyapan.dietdiaryapp.MainActivity;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DailyReminderService extends JobService {
    public static final String TAG = "DailyReminderService";

    private static final int REQUEST_CODE = 1100;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");

        final Context context = getApplicationContext();

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
                            .getPendingIntent(REQUEST_CODE, 0));
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(TAG, R.id.daily_notification, builder.build());
        }

        // Setup next reminder.
        DailyReminderServiceHelper.setup(context);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped");
        return false;
    }
}
