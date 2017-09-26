package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.canyapan.dietdiaryapp.services.DriveBackupService;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

public class DriveBackupServiceHelper {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        //Bundle myExtrasBundle = new Bundle();
        //myExtrasBundle.putString("some_key", "some_value");

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
                .setTrigger(Trigger.executionWindow(0, 60))
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

        dispatcher.mustSchedule(myJob);

        /*ComponentName serviceComponent = new ComponentName(context, DriveBackupService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_DRIVE_BACKUP, serviceComponent)
                .setMinimumLatency(10000)
                .setOverrideDeadline(60000)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setRequiresDeviceIdle(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());*/
    }
}
