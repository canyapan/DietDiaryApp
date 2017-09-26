package com.canyapan.dietdiaryapp.services;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DriveBackupService extends JobService {
    public static final String TAG = "DriveBackupService";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");


        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped");
        return true;
    }
}
