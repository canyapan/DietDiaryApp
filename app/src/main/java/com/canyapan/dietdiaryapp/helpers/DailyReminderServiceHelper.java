package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.services.DailyReminderService;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING;

public class DailyReminderServiceHelper {
    private static final String DEFAULT_TIME = "19:00";

    public static void setup(@NonNull final Context context) {
        setup(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static void setup(@NonNull final Context context, final SharedPreferences sharedPreferences) {
        if (sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ACTIVE_BOOL, false)
                && sharedPreferences.getBoolean(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL, false)) {
            final LocalTime time = LocalTime.parse(
                    sharedPreferences.getString(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING, DEFAULT_TIME),
                    DatabaseHelper.DB_TIME_FORMATTER);

            setup(context, getSecondsUntilTime(time));
        }
    }

    private static void setup(@NonNull final Context context, final int timeInSeconds) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        Job myJob = dispatcher.newJobBuilder()
                // the JobService that will be called
                .setService(DailyReminderService.class)
                // uniquely identifies the job
                .setTag(DailyReminderService.TAG)
                // one-off job
                .setRecurring(false)
                // persist past a device reboot (requires boot receiver permission)
                .setLifetime(Lifetime.FOREVER)
                // start between 0 and 60 seconds from now
                .setTrigger(Trigger.executionWindow(timeInSeconds, timeInSeconds + 30))
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                //.setExtras(myExtrasBundle)
                .build();

        dispatcher.mustSchedule(myJob);
    }

    public static void cancel(@NonNull final Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(DailyReminderService.TAG);
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
