package com.canyapan.dietdiaryapp.helpers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.fragments.SettingsSupportFragment;
import com.canyapan.dietdiaryapp.receivers.DailyReminderReceiver;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

public class DailyReminderHelper {
    public static final int REQUEST_CODE = 1100;

    private static final String DEFAULT_TIME = "19:00";
    private static final String KEY_CREATED_ON_SERIALIZABLE = "DAILY_ALARM_CREATE_TIME";

    public static void register(@NonNull final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(SettingsSupportFragment.KEY_NOTIFICATIONS_ACTIVE, false)
                && preferences.getBoolean(SettingsSupportFragment.KEY_NOTIFICATIONS_DAILY_REMAINDER, false)) {
            final LocalTime time = LocalTime.parse(
                    preferences.getString(SettingsSupportFragment.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME, DEFAULT_TIME),
                    DatabaseHelper.DB_TIME_FORMATTER);

            register(context, getNextAlarmTime(time));
        }
    }

    public static void register(@NonNull final Context context, final int hour, final int minute) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(SettingsSupportFragment.KEY_NOTIFICATIONS_ACTIVE, false)) {
            final LocalTime time = new LocalTime(hour, minute);

            register(context, getNextAlarmTime(time));
        }
    }

    private static void register(@NonNull final Context context, final long timeInMilliseconds) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, timeInMilliseconds, getPendingIntent(context));
    }

    private static PendingIntent getPendingIntent(@NonNull final Context context) {
        final Intent intent = new Intent(context, DailyReminderReceiver.class);
        intent.putExtra(KEY_CREATED_ON_SERIALIZABLE, LocalDateTime.now());

        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void cancel(@NonNull final Context context) {
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getPendingIntent(context));
    }

    private static long getNextAlarmTime(@NonNull final LocalTime time) {
        LocalDateTime alarmClock = LocalDateTime.now()
                .withHourOfDay(time.getHourOfDay())
                .withMinuteOfHour(time.getMinuteOfHour())
                .withSecondOfMinute(time.getSecondOfMinute())
                .withMillisOfSecond(time.getMillisOfSecond());

        if (alarmClock.isBefore(LocalDateTime.now())) {
            alarmClock = alarmClock.plusDays(1);
        }

        return alarmClock.toDateTime().getMillis();
    }
}
