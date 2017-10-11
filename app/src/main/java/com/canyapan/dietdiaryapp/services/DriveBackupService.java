package com.canyapan.dietdiaryapp.services;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.preference.PreferenceManager;
import android.util.JsonWriter;
import android.util.Log;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.fragments.BackupFragment;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.jaredrummler.android.device.DeviceName;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_APP_ID;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_FILE_DRIVE_ID_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_GENERAL_CLOCK_MODE_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DriveBackupService extends JobService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DriveApi.DriveContentsResult> {
    public static final String TAG = "DriveBackupService";

    private static final String KEY_ID = "ID";
    private static final String KEY_DATE = "Date";
    private static final String KEY_TIME = "Time";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_TITLE = "Title";
    private static final String KEY_DESC = "Description";

    public static final CustomPropertyKey DRIVE_KEY_APP_ID = new CustomPropertyKey("AppID", CustomPropertyKey.PRIVATE);
    public static final CustomPropertyKey DRIVE_KEY_DEVICE_NAME = new CustomPropertyKey("DeviceName", CustomPropertyKey.PRIVATE);

    private JobParameters mJobParameters = null;
    private SharedPreferences mSharedPreferences = null;
    private GoogleApiClient mGoogleApiClient = null;
    private File mBackupFile = null;
    private String mAppId = null;
    private String mDriveId = null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");
        mJobParameters = jobParameters;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (null != mSharedPreferences) {
            mAppId = mSharedPreferences.getString(KEY_APP_ID, null);
            mDriveId = mSharedPreferences.getString(KEY_BACKUP_FILE_DRIVE_ID_STRING, null);
        }

        if (null == mAppId) {
            Log.e(TAG, "AppId is null");
            return false;
        }

        try {
            // Write file in the app cache dir.
            mBackupFile = new File(getCacheDir(), "backup.json");
            writeBackupData();

            mGoogleApiClient = getGoogleApiClient();
            connectGoogleApiClient();

            return true;
        } catch (IOException e) {
            Log.e(TAG, "EXCEPTION", e);
            deleteBackupFile();
        }

        return false;
    }

    private void deleteBackupFile() {
        if (null != mBackupFile) {
            if (!mBackupFile.delete()) {
                Log.e(TAG, "Unable to delete backup data.");
            }
        }
    }

    private void finishJob() {
        finishJob(false);
    }

    private void finishJob(boolean isSuccessful) {
        deleteBackupFile();

        if (isSuccessful) {
            setLastBackupTime();
        }

        jobFinished(mJobParameters, false);
    }

    private void writeBackupData() throws IOException {
        OutputStreamWriter os = null;
        JsonWriter writer = null;
        try {
            os = new OutputStreamWriter(new FileOutputStream(mBackupFile, false), "UTF-8");
            os.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark

            writer = new JsonWriter(os);
            writer.setIndent("  ");

            writer.beginObject();

            writer.name("App").value("DietDiaryApp");
            writer.name("Ver").value(1);

            writeDeviceProperties(writer);
            writeSettings(writer);
            writeEvents(writer);

            writer.endObject();

            Log.d(TAG, "Backup file created.");
        } catch (IOException e) {
            Log.e(TAG, "Backup file creation failed.");
            throw e;
        } finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();

                } catch (IOException e) {
                    Log.w(TAG, "JSON stream cannot be closed.", e);
                }
            }

            if (null != os) {
                try {
                    os.flush();
                    os.close();

                } catch (IOException e) {
                    Log.w(TAG, "Output stream cannot be closed.", e);
                }
            }
        }
    }

    private void writeDeviceProperties(@NonNull final JsonWriter writer) throws IOException {
        // Write device properties
        writer.name("Device")
                .beginObject()
                .name("Name").value(DeviceName.getDeviceName())
                .name("Manufacturer").value(Build.MANUFACTURER)
                .name("Brand").value(Build.BRAND)
                .name("Product").value(Build.PRODUCT)
                .name("Model").value(Build.MODEL)
                .endObject();
    }

    private void writeSettings(@NonNull final JsonWriter writer) throws IOException {
        // Write settings
        writer.name("Settings").beginObject();

        if (mSharedPreferences.contains(KEY_GENERAL_CLOCK_MODE_STRING)) {
            writer.name(KEY_GENERAL_CLOCK_MODE_STRING)
                    .value(mSharedPreferences.getString(KEY_GENERAL_CLOCK_MODE_STRING, "-1"));
        }

        if (mSharedPreferences.contains(KEY_NOTIFICATIONS_ACTIVE_BOOL)) {
            writer.name(KEY_NOTIFICATIONS_ACTIVE_BOOL)
                    .value(mSharedPreferences.getBoolean(KEY_NOTIFICATIONS_ACTIVE_BOOL, true));
        }

        if (mSharedPreferences.contains(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL)) {
            writer.name(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL)
                    .value(mSharedPreferences.getBoolean(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL, true));
        }

        if (mSharedPreferences.contains(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING)) {
            writer.name(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING)
                    .value(mSharedPreferences.getString(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING, "19:00"));
        }

        writer.endObject();
    }

    private void writeEvents(@NonNull final JsonWriter writer) throws IOException {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            Resources resources = ResourcesHelper.getEngResources(getApplicationContext());
            final String[] types = resources.getStringArray(R.array.spinner_event_types);
            final String[] foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
            final String[] drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);

            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            db = dbHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    null, null, null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            writer.name("Events").beginArray();

            if (cursor.moveToFirst()) {
                final int count = cursor.getCount();
                Log.d(BackupFragment.TAG, MessageFormat.format("Exporting {0,number,integer} records.", count));

                do {
                    writeEvent(writer, types, foodTypes, drinkTypes, EventHelper.parse(cursor));
                } while (cursor.moveToNext());
            }

            writer.endArray();
        } finally {
            if (null != cursor) {
                cursor.close();
            }

            if (null != db && db.isOpen()) {
                db.close();
            }
        }
    }

    private void writeEvent(@NonNull final JsonWriter writer, @NonNull final String[] types,
                            @NonNull final String[] foodTypes, @NonNull final String[] drinkTypes,
                            @NonNull Event event) throws IOException {
        String subType;
        switch (event.getType()) {
            case Event.TYPE_FOOD:
                subType = foodTypes[event.getSubType()];
                break;
            case Event.TYPE_DRINK:
                subType = drinkTypes[event.getSubType()];
                break;
            default:
                subType = "";
        }

        writer.beginObject();
        writer.name(KEY_ID).value(event.getID());
        writer.name(KEY_DATE).value(event.getDate().toString(DatabaseHelper.DB_DATE_FORMATTER));
        writer.name(KEY_TIME).value(event.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER));
        writer.name(KEY_TYPE).value(types[event.getType()]);
        writer.name(KEY_TITLE).value(subType);
        writer.name(KEY_DESC).value(event.getDescription());
        writer.endObject();
    }

    private GoogleApiClient getGoogleApiClient() {
        return new GoogleApiClient.Builder(getApplicationContext())
                .useDefaultAccount()
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void connectGoogleApiClient() {
        if (null != mGoogleApiClient) {
            if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped.");

        if (null != mGoogleApiClient) {
            if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }

        return false;
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "Drive API connected.");

        // -> Check if driveID exists
        // false - Create new file if not
        // true  - Write over the current file if the drive ID exists
        if (null != mGoogleApiClient) {
            if (null != mDriveId && !mDriveId.isEmpty()) {
                Drive.DriveApi.fetchDriveId(mGoogleApiClient, mDriveId)
                        .setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
                            @Override
                            public void onResult(@NonNull DriveApi.DriveIdResult driveIdResult) {
                                if (!driveIdResult.getStatus().isSuccess()) {
                                    Log.w(TAG, "Couldn't get drive id. Maybe deleted by user. ");

                                    mDriveId = null; // Create a new file
                                    onConnected(bundle); // Let's try this again

                                    return;
                                }

                                driveIdResult.getDriveId().asDriveFile()
                                        .open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                                        .setResultCallback(DriveBackupService.this);

                            }
                        });
            } else {
                Drive.DriveApi.newDriveContents(mGoogleApiClient) // Create a new file
                        .setResultCallback(this);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Drive API connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Unable to connect drive. Message : " + connectionResult.getErrorMessage());

        finishJob();
    }

    public void onResult(@NonNull final DriveApi.DriveContentsResult driveContentsResult) {
        if (!driveContentsResult.getStatus().isSuccess()) {
            Log.e(TAG, "Error while trying to create new file contents. " + driveContentsResult.getStatus().getStatusMessage());

            finishJob();
            return;
        }

        final DriveContents driveContents = driveContentsResult.getDriveContents();

        // Compress and write backup into Drive AppFolder
        OutputStream outputStream = driveContents.getOutputStream();
        try {
            compressBackupDataIntoStream(outputStream);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write backup data to drive", e);

            finishJob();
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Unable to closer drive OutputStream.", e);
                }
            }
        }

        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                .setTitle(MessageFormat.format("backup.zip", mAppId))
                .setMimeType("application/zip")
                .setCustomProperty(DRIVE_KEY_APP_ID, mAppId)
                .setCustomProperty(DRIVE_KEY_DEVICE_NAME, DeviceName.getDeviceName())
                .build();

        if (null != driveContents.getDriveId()) { // Modifying a file
            driveContents.commit(mGoogleApiClient, metadataChangeSet)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.getStatus().isSuccess()) {
                                Log.d(TAG, "Drive file modified " + mDriveId);
                            } else {
                                Log.e(TAG, "Error while trying to modify file. " + status.getStatus().getStatusMessage());
                            }

                            mDriveId = driveContents.getDriveId().encodeToString();

                            finishJob(status.getStatus().isSuccess());
                        }
                    });
        } else { // Creating a new file
            Drive.DriveApi.getAppFolder(mGoogleApiClient)
                    .createFile(mGoogleApiClient, metadataChangeSet, driveContents)
                    .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                        @Override
                        public void onResult(@NonNull DriveFolder.DriveFileResult driveFileResult) {
                            if (driveFileResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Drive file created " + driveFileResult.getDriveFile().getDriveId());
                            } else {
                                Log.e(TAG, "Error while trying to create file. " + driveFileResult.getStatus().getStatusMessage());
                            }

                            mDriveId = driveFileResult.getDriveFile().getDriveId().toString();

                            finishJob(driveFileResult.getStatus().isSuccess());
                        }
                    });
        }
    }

    private void setLastBackupTime() {
        long now = DateTime.now().getMillis();
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG, now);
        editor.putString(KEY_BACKUP_FILE_DRIVE_ID_STRING, mDriveId);
        editor.apply();
    }

    private void compressBackupDataIntoStream(final OutputStream outputStream) throws IOException {
        if (null == mBackupFile || !mBackupFile.exists()) {
            throw new IOException("Backup file is gone."); // :S
        }

        InputStream inputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            inputStream = new FileInputStream(mBackupFile);
            zipOutputStream = new ZipOutputStream(outputStream);

            zipOutputStream.setLevel(6);

            final byte[] buffer = new byte[1024];

            final ZipEntry zipEntry = new ZipEntry(mBackupFile.getName());
            zipEntry.setSize((long) buffer.length);

            zipOutputStream.putNextEntry(zipEntry);

            CRC32 crc32 = new CRC32();
            crc32.reset();

            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
                crc32.update(buffer, 0, len);
            }

            zipEntry.setCrc(crc32.getValue());

            zipOutputStream.finish();

            Log.d(TAG, "Compressed.");
        } catch (Exception e) {
            Log.e(TAG, "Compression failed.");
            throw e;
        } finally {
            if (null != zipOutputStream) {
                try {
                    zipOutputStream.flush();
                    zipOutputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Compressed file stream cannot be closed.", e);
                }
            }

            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Input stream cannot be closed.", e);
                }
            }
        }
    }
}
