package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.services.DailyReminderService;
import com.canyapan.dietdiaryapp.services.DriveBackupService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_WIFI_ONLY_BOOL;

public class DriveBackupServiceHelper {
    private static final String DEFAULT_TIME = "21:00";

    public static boolean setup(@NonNull final Context context) {
        final LocalTime time = LocalTime.parse(DEFAULT_TIME, DatabaseHelper.DB_TIME_FORMATTER);
        return setup(context, getSecondsUntilTime(time), isWaitForWiFi(context));
    }

    public static boolean setupImmediate(@NonNull final Context context) {
        return setup(context, -1, false);
    }

    private static boolean setup(@NonNull final Context context, final int timeInSeconds, final boolean waitUnmeteredNetwork) {
        if (!isBackupActive(context)) {
            return false;
        }

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job.Builder obBuilder = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(DriveBackupService.class)
                // uniquely identifies the job
                .setTag(DriveBackupService.TAG)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL);

        if (waitUnmeteredNetwork) {
            // only run on an unmetered network
            obBuilder.addConstraint(Constraint.ON_UNMETERED_NETWORK);
        }

        if (timeInSeconds >= 0) {
            // Run after selected time and any time in one hour
            obBuilder.setTrigger(Trigger.executionWindow(timeInSeconds, timeInSeconds + 1800));
        } else {
            obBuilder.setTrigger(Trigger.NOW);
        }

        return dispatcher.schedule(obBuilder.build()) == FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS;
    }

    public static boolean cancel(@NonNull final Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        return dispatcher.cancel(DailyReminderService.TAG) == FirebaseJobDispatcher.CANCEL_RESULT_SUCCESS;
    }

    private static boolean isBackupActive(@NonNull final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(KEY_BACKUP_ACTIVE_BOOL, false);
    }

    private static boolean isWaitForWiFi(@NonNull final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(KEY_BACKUP_WIFI_ONLY_BOOL, true);
    }

    private static int getSecondsUntilTime(@NonNull final LocalTime time) {
        LocalDateTime alarmClock = LocalDateTime.now()
                .withHourOfDay(time.getHourOfDay())
                .withMinuteOfHour(time.getMinuteOfHour())
                .withSecondOfMinute(time.getSecondOfMinute())
                .withMillisOfSecond(time.getMillisOfSecond());

        if (alarmClock.isBefore(LocalDateTime.now())) {
            alarmClock = alarmClock.plusDays(1);
        }

        return Seconds.secondsBetween(LocalDateTime.now(), alarmClock).getSeconds();
    }
}
