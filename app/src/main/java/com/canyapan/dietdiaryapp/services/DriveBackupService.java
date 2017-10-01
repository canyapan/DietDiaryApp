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
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_NOW;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DriveBackupService extends JobService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<DriveApi.DriveContentsResult> {
    public static final String TAG = "DriveBackupService";

    private static final String KEY_ID = "ID";
    private static final String KEY_DATE = "Date";
    private static final String KEY_TIME = "Time";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_TITLE = "Title";
    private static final String KEY_DESC = "Description";

    private WeakReference<GoogleApiClient> mGoogleApiClientRef = null;
    private WeakReference<JobParameters> mJobParameters = null;
    private WeakReference<File> mBackupFile = null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");
        mJobParameters = new WeakReference<>(jobParameters);

        try {
            mBackupFile = new WeakReference<>(createBackupData());

            mGoogleApiClientRef = new WeakReference<>(getGoogleApiClient());
            connectGoogleApiClient();

            return true;
        } catch (IOException e) {
            Log.e(TAG, "EXCEPTION", e);
        }

        return false;
    }

    private void finishJob() {
        finishJob(false);
    }

    private void finishJob(boolean isSuccessful) {
        if (isSuccessful) {
            setLastBackupTime();
        }

        if (null != mJobParameters && null != mJobParameters.get()) {
            jobFinished(mJobParameters.get(), false);
        }
    }

    private File createBackupData() throws IOException {
        final File f = new File(getCacheDir(), "backup.json");

        OutputStreamWriter os = null;
        JsonWriter writer = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            os = new OutputStreamWriter(new FileOutputStream(f, false), "UTF-8");
            os.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark

            writer = new JsonWriter(os);
            writer.setIndent("  ");

            writer.beginObject();
            writer.name("App").value("DietDiaryApp");
            writer.name("Ver").value(1);
            writer.name("Events").beginArray();

            Resources resources = ResourcesHelper.getEngResources(getApplicationContext());
            final String[] types = resources.getStringArray(R.array.spinner_event_types);
            final String[] foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
            final String[] drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);


            DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
            db = dbHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    null, null, null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            if (cursor.moveToFirst()) {
                final int count = cursor.getCount();
                Log.d(BackupFragment.TAG, MessageFormat.format("Exporting {0,number,integer} records.", count));

                do {
                    write(writer, types, foodTypes, drinkTypes, EventHelper.parse(cursor));
                } while (cursor.moveToNext());
            }

            writer.endArray();
            writer.endObject();

            Log.d(TAG, "Backup file created.");

            return f;
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

            if (null != cursor) {
                cursor.close();
            }

            if (null != db && db.isOpen()) {
                db.close();
            }
        }
    }

    private void write(JsonWriter writer, String[] types, String[] foodTypes, String[] drinkTypes, Event event) throws IOException {
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
        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            if (!mGoogleApiClientRef.get().isConnecting() && !mGoogleApiClientRef.get().isConnected()) {
                mGoogleApiClientRef.get().connect();
            }
        }
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped.");

        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            if (mGoogleApiClientRef.get().isConnecting() || mGoogleApiClientRef.get().isConnected()) {
                mGoogleApiClientRef.get().disconnect();
            }
        }

        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Drive API connected.");

        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            Drive.DriveApi.newDriveContents(mGoogleApiClientRef.get())
                    .setResultCallback(this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Drive API connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Unable to connect drive. Message : " + connectionResult.getErrorMessage());

        if (null != mBackupFile && null != mBackupFile.get()) {
            if (!mBackupFile.get().delete()) {
                Log.e(TAG, "Unable to delete compressed backup data.");
            }
        }

        finishJob();
    }

    @Override
    public void onResult(@NonNull final DriveApi.DriveContentsResult driveContentsResult) {
        if (!driveContentsResult.getStatus().isSuccess()) {
            Log.e(TAG, "Error while trying to create new file contents");

            finishJob();
            return;
        }

        OutputStream os = null;
        try {
            os = driveContentsResult.getDriveContents().getOutputStream();
            compressBackupDataToStream(os);

        } catch (IOException e) {
            Log.e(TAG, "Failed to write backup data to drive", e);

            if (null != mBackupFile && null != mBackupFile.get()) {
                if (!mBackupFile.get().delete()) {
                    Log.e(TAG, "Unable to delete compressed backup data.");
                }
            }

            finishJob();
        } finally {
            if (null != os) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    Log.w(TAG, "Unable to closer drive OutputStream.", e);
                }
            }
        }

        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("backup.zip")
                .setMimeType("application/zip")
                .build();

        DriveApi.MetadataBufferResult result = Drive.DriveApi.getAppFolder(mGoogleApiClientRef.get())
                .listChildren(mGoogleApiClientRef.get()).await();

        for (Metadata m : result.getMetadataBuffer()) {

            m.getDriveId().asDriveFile().delete(mGoogleApiClientRef.get());

        }

        Drive.DriveApi.getAppFolder(mGoogleApiClientRef.get())
                .createFile(mGoogleApiClientRef.get(), changeSet, driveContentsResult.getDriveContents())
                .setResultCallback(
                        new ResultCallback<DriveFolder.DriveFileResult>() {
                            @Override
                            public void onResult(@NonNull DriveFolder.DriveFileResult driveFileResult) {
                                if (driveFileResult.getStatus().isSuccess()) {
                                    Log.d(TAG, "Drive file created " + driveFileResult.getDriveFile().getDriveId().encodeToString());
                                    finishJob(true);
                                } else {
                                    Log.e(TAG, "Error while trying to create new file contents. Message : " + driveFileResult.getStatus().getStatusMessage());
                                    finishJob();
                                }
                            }
                        }
                );
    }

    private void setLastBackupTime() {
        long now = DateTime.now().getMillis();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_BACKUP_NOW, String.valueOf(now));
        editor.apply();
    }

    private void compressBackupDataToStream(OutputStream outputStream) throws IOException {
        if (null == mBackupFile || null == mBackupFile.get() || !mBackupFile.get().exists()) {
            throw new IOException("Backup file is gone.");
        }

        InputStream inputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            inputStream = new FileInputStream(mBackupFile.get());
            zipOutputStream = new ZipOutputStream(outputStream);

            zipOutputStream.setLevel(6);

            final byte[] buffer = new byte[1024];

            final ZipEntry zipEntry = new ZipEntry(mBackupFile.get().getName());
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
