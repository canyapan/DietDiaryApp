package com.canyapan.dietdiaryapp;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

/**
 * Use Google Cloud Backup. So, user will never loose their data again.
 */
public class BackupAgent extends BackupAgentHelper {
    public static final String KEY_PREFS = "PREFS";
    public static final String KEY_DB = "DB";

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferencesBackupHelper prefHelper = new SharedPreferencesBackupHelper(this, "com.canyapan.dietdiaryapp_preferences");
        addHelper(KEY_PREFS, prefHelper);

        FileBackupHelper dbHelper = new FileBackupHelper(this, "../databases/Events.db");
        addHelper(KEY_DB, dbHelper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
                         ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode,
                          ParcelFileDescriptor newState) throws IOException {
        super.onRestore(data, appVersionCode, newState);
    }
}
