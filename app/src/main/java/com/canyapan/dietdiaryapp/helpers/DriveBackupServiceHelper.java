package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.preference.PreferenceKeys;
import com.canyapan.dietdiaryapp.services.DailyReminderService;
import com.canyapan.dietdiaryapp.services.DriveBackupService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Seconds;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_WIFI_ONLY_BOOL;

public class DriveBackupServiceHelper {
    private static final String DEFAULT_TIME = "21:00";

    public static boolean setup(@NonNull final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        long lastBackupTimestamp = sharedPreferences.getLong(KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG, -1);
        LocalDate lastBackup = lastBackupTimestamp >= 0 ? new LocalDate(lastBackupTimestamp) : null;

        Frequency frequency = Frequency.identify(sharedPreferences.getString(PreferenceKeys.KEY_BACKUP_FREQUENCY_STRING, "0"));

        return frequency != Frequency.NEVER && setup(context, getSecondsUntilTime(frequency, lastBackup), isWaitForWiFi(context));
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
                // persist past a device reboot (requires boot receiver permission)
                .setLifetime(Lifetime.FOREVER)
                // overwrite an existing job with the same tag
                .setReplaceCurrent(true)
                // retry with exponential backoff
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR);

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

    private static int getSecondsUntilTime(@NonNull final Frequency frequency, final LocalDate lastUpdate) {
        final LocalTime time = LocalTime.parse(DEFAULT_TIME, DatabaseHelper.DB_TIME_FORMATTER);

        LocalDateTime alarmClock;
        if (null == lastUpdate) { // Never run before
            alarmClock = LocalDate.now().toLocalDateTime(time);
        } else { // Run before
            alarmClock = lastUpdate.toLocalDateTime(time);
            switch (frequency) {
                case DAILY:
                    alarmClock = alarmClock.plusDays(1);
                    break;
                case WEEKLY:
                    alarmClock = alarmClock.plusWeeks(1);
                    break;
                case MONTHLY:
                    alarmClock = alarmClock.plusMonths(1);
                    break;
                default:
                    throw new IllegalStateException("Unknown value " + frequency);
            }
        }

        // May be pointing some time before now.
        while (alarmClock.isBefore(LocalDateTime.now())) {
            alarmClock = alarmClock.plusDays(1);
        }

        return Seconds.secondsBetween(LocalDateTime.now(), alarmClock).getSeconds();
    }

    private enum Frequency {
        NEVER, DAILY, WEEKLY, MONTHLY;

        public static Frequency identify(@NonNull String frequency) {
            switch (frequency) {
                case "-1":
                    return NEVER;
                case "0":
                    return DAILY;
                case "1":
                    return WEEKLY;
                case "2":
                    return MONTHLY;
                default:
                    throw new IllegalStateException("Unknown value " + frequency);
            }
        }
    }
}
