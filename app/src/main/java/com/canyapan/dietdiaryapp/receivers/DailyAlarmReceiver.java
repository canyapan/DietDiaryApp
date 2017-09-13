package com.canyapan.dietdiaryapp.receivers;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.canyapan.dietdiaryapp.MainActivity;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.SettingsActivity;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class DailyAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 1100;
    public static final String DEFAULT_TIME = "19:00";
    public static final String KEY_CREATED_ON_SERIALIZABLE = "CREATED";
    private static final String TAG = "DailyAlarmReceiver";

    public static void register(@NonNull final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(SettingsActivity.KEY_NOTIFICATIONS_ACTIVE, true)) {
            register(context, preferences);
        }
    }

    public static void register(@NonNull final Context context, final int hour, final int minute) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(SettingsActivity.KEY_NOTIFICATIONS_ACTIVE, true)) {
            final LocalTime time = new LocalTime(hour, minute);

            register(context, getNextAlarmTime(time));
        }
    }

    public static void register(@NonNull final Context context, @NonNull final SharedPreferences preferences) {
        if (preferences.getBoolean(SettingsActivity.KEY_NOTIFICATIONS_DAILY_REMAINDER, true)) {
            final LocalTime time = LocalTime.parse(
                    preferences.getString(SettingsActivity.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME, DEFAULT_TIME),
                    DatabaseHelper.DB_TIME_FORMATTER);

            register(context, getNextAlarmTime(time));
        }
    }

    private static void register(@NonNull final Context context, final long timeInMilliseconds) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, timeInMilliseconds, getPendingIntent(context));
    }

    private static PendingIntent getPendingIntent(@NonNull final Context context) {
        final Intent intent = new Intent(context, DailyAlarmReceiver.class);
        intent.putExtra(KEY_CREATED_ON_SERIALIZABLE, LocalDateTime.now());

        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void cancel(@NonNull final Context context) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    private static long getNextAlarmTime(@NonNull final LocalTime time) {
        LocalDateTime alarmClock = LocalDateTime.now().withTime(
                time.getHourOfDay(), time.getMinuteOfHour(), time.getSecondOfMinute(), time.getMillisOfSecond());

        if (alarmClock.isBefore(LocalDateTime.now())) {
            alarmClock = alarmClock.plusDays(1);
        }

        return alarmClock.toDateTime().getMillis();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!preferences.getBoolean(SettingsActivity.KEY_NOTIFICATIONS_ACTIVE, true)) {
            return;
        }

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

        // Setup next alarm.
        DailyAlarmReceiver.register(context, preferences);
    }
}
