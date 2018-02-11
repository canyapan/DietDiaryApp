package com.canyapan.dietdiaryapp.services;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.canyapan.dietdiaryapp.MainActivity;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class DailyReminderService extends JobService {
    public static final String TAG = "DailyReminderService";

    private static final int REQUEST_CODE = 1100;

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");

        //Drawable drawable = ContextCompat.getDrawable(this, R.drawable.app_icon_notify_large);
        //Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        if (EventHelper.hasEventToday(this) == 0) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationCompat.CATEGORY_REMINDER)
                    .setTicker(getString(R.string.daily_notification_title))
                    .setSmallIcon(R.drawable.app_icon_notify)
                    //.setLargeIcon(bitmap)
                    .setContentTitle(getString(R.string.daily_notification_title))
                    .setContentText(getString(R.string.daily_notification_message))
                    .setAutoCancel(true)
                    //.setLights(Color.GREEN, 1, 1)
                    //.setSound()
                    //.setVibrate()
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setContentIntent(TaskStackBuilder.create(this)
                            .addParentStack(MainActivity.class)
                            .addNextIntent(new Intent(this, MainActivity.class))
                            .getPendingIntent(REQUEST_CODE, 0));
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(TAG, R.id.daily_notification, builder.build());
        }

        // Setup next reminder.
        DailyReminderServiceHelper.setup(this);

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped");
        return false;
    }
}
