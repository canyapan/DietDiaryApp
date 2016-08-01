package com.canyapan.dietdiaryapp;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;

/**
 * Use Google Cloud Backup. So, user will never loose their data again.
 */
public class BackupAgent extends BackupAgentHelper {
    public static final String KEY_PREFS = "PREFS";
    public static final String KEY_DB = "DB";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferencesBackupHelper prefHelper = new SharedPreferencesBackupHelper(this, getApplicationContext().getPackageName() + "_preferences");
        addHelper(KEY_PREFS, prefHelper);

        FileBackupHelper dbHelper = new FileBackupHelper(this, "../databases/Events.db");
        addHelper(KEY_DB, dbHelper);
    }

}
