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
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE;

public class DriveBackupServiceHelper {
    private static final String DEFAULT_TIME = "21:00";

    public static void setup(@NonNull final Context context) {
        if (isBackupActive(context)) {
            final LocalTime time = LocalTime.parse(DEFAULT_TIME, DatabaseHelper.DB_TIME_FORMATTER);

            setup(context, getSecondsUntilTime(time));
        }
    }

    public static void setupImmediate(@NonNull final Context context) {
        if (isBackupActive(context)) {
            setup(context, -1);
        }
    }

    private static void setup(@NonNull final Context context, final int timeInSeconds) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        //Bundle myExtrasBundle = new Bundle();
        //myExtrasBundle.putString("some_key", "some_value");
        JobTrigger trigger;
        if (timeInSeconds >= 0) {
            trigger = Trigger.executionWindow(timeInSeconds, timeInSeconds + 1800);
        } else {
            trigger = Trigger.NOW;
        }

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(DriveBackupService.class)
                // uniquely identifies the job
                .setTag(DriveBackupService.TAG)
                // one-off job
                .setRecurring(false)
                // don't persist past a device reboot
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                // start between 0 and 60 seconds from now
                .setTrigger(trigger)
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                // constraints that need to be satisfied for the job to run
                .setConstraints(
                        // only run on an unmetered network
                        Constraint.ON_UNMETERED_NETWORK
                )
                //.setExtras(myExtrasBundle)
                .build();

        dispatcher.schedule(myJob);
    }

    public static void cancel(@NonNull final Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(DailyReminderService.TAG);
    }

    private static boolean isBackupActive(@NonNull final Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(KEY_BACKUP_ACTIVE, false);
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
