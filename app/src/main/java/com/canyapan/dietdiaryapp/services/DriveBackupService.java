package com.canyapan.dietdiaryapp.services;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.jaredrummler.android.device.DeviceName;

import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
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

public class DriveBackupService extends JobService {
    public static final String TAG = "DriveBackupService";

    public static final String KEY_DATA_CHANGED_BOOLEAN = "DATA CHANGED";

    private static final String KEY_ID = "ID";
    private static final String KEY_DATE = "Date";
    private static final String KEY_TIME = "Time";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_TITLE = "Title";
    private static final String KEY_DESC = "Description";

    public static final CustomPropertyKey DRIVE_KEY_APP_ID = new CustomPropertyKey("AppID", CustomPropertyKey.PRIVATE);
    public static final CustomPropertyKey DRIVE_KEY_DEVICE_NAME = new CustomPropertyKey("DeviceName", CustomPropertyKey.PRIVATE);

    private static final String BACKUP_FILE_NAME = "backup.json";
    private static final String BACKUP_FILE_NAME_COMPRESSED = "backup.zip";

    private JobParameters mJobParameters = null;
    private SharedPreferences mSharedPreferences = null;

    private DriveClient mDriveClient = null;
    private DriveResourceClient mDriveResourceClient = null;

    private File mBackupFile = null;
    private String mAppId = null;
    private String mDriveId = null;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started");
        mJobParameters = jobParameters;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (null == mSharedPreferences) {
            Log.e(TAG, "SharedPreferences is null");
            return false;
        }

        mAppId = mSharedPreferences.getString(KEY_APP_ID, null);
        mDriveId = mSharedPreferences.getString(KEY_BACKUP_FILE_DRIVE_ID_STRING, null);

        if (null == mAppId) {
            Log.e(TAG, "AppId is null");
            return false;
        }

        // Check if user has added any new data since last backup.
        // So won't run a backup if there isn't anything new to backup.
        if (mSharedPreferences.getBoolean(KEY_DATA_CHANGED_BOOLEAN, false)) {
            Log.d(TAG, "Nothing new to backup.");
            return false;
        }

        try {
            // Write file in the app cache dir.
            File f = new File(getCacheDir(), BACKUP_FILE_NAME);
            File cf = new File(getCacheDir(), BACKUP_FILE_NAME_COMPRESSED);
            writeBackupFile(f);
            compressBackupFile(cf, f);

            mBackupFile = cf;

            if (loadDriveApiClients()) {
                // -> Check if driveID exists
                // false - Create new file if not
                // true  - Write over the current file if the drive ID exists
                if (null != mDriveId && !mDriveId.isEmpty()) {
                    modifyExistingDriveBackup();
                } else {
                    createNewDriveBackup();
                }

                return true;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            Log.e(TAG, "EXCEPTION", e);
            deleteBackupFile();
        }

        return false;
    }

    private void createNewDriveBackup() {
        final Task<DriveFolder> appFolderTask = getmDriveResourceClient().getAppFolder();
        final Task<DriveContents> createContentsTask = getmDriveResourceClient().createContents();
        Tasks.whenAll(appFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        Log.d(TAG, "Creating drive file... ");

                        DriveContents driveContents = createContentsTask.getResult();

                        writeBackupToOutputStream(driveContents.getOutputStream());

                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setTitle(BACKUP_FILE_NAME_COMPRESSED)
                                .setMimeType("application/zip")
                                .setCustomProperty(DRIVE_KEY_APP_ID, mAppId)
                                .setCustomProperty(DRIVE_KEY_DEVICE_NAME, DeviceName.getDeviceName())
                                .build();

                        return getmDriveResourceClient().createFile(appFolderTask.getResult(), metadataChangeSet, driveContents);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DriveFile>() {
                    @Override
                    public void onSuccess(DriveFile driveFile) {
                        mDriveId = driveFile.getDriveId().encodeToString();
                        Log.d(TAG, "Drive file created " + mDriveId);

                        finishJob(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error while trying to create file.", e);
                        finishJob();
                    }
                });
    }

    private void modifyExistingDriveBackup() {
        final DriveId driveId = DriveId.decodeFromString(mDriveId);
        getmDriveResourceClient().openFile(driveId.asDriveFile(), DriveFile.MODE_WRITE_ONLY)
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        DriveContents driveContents = task.getResult();

                        writeBackupToOutputStream(driveContents.getOutputStream());

                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setTitle(BACKUP_FILE_NAME_COMPRESSED)
                                .setMimeType("application/zip")
                                .setCustomProperty(DRIVE_KEY_APP_ID, mAppId)
                                .setCustomProperty(DRIVE_KEY_DEVICE_NAME, DeviceName.getDeviceName())
                                .build();

                        return getmDriveResourceClient().commitContents(driveContents, metadataChangeSet);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Drive file modified " + mDriveId);
                        finishJob(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Couldn't open drive file. Maybe deleted by user.");
                        mDriveId = null; // Create a new file

                        createNewDriveBackup();
                    }
                });
    }

    private void writeBackupToOutputStream(final OutputStream outputStream) {
        // Write backup into Drive AppFolder
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mBackupFile);
            IOUtils.copy(inputStream, outputStream);
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

            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Input stream cannot be closed.", e);
                }
            }
        }
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

    private void writeBackupFile(@NonNull File outputFile) throws IOException {
        OutputStreamWriter os = null;
        JsonWriter writer = null;
        try {
            os = new OutputStreamWriter(new FileOutputStream(outputFile, false), "UTF-8");
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
            } else if (null != os) { // Writer will close this normally.
                try {
                    os.flush();
                    os.close();

                } catch (IOException e) {
                    Log.w(TAG, "Output stream cannot be closed.", e);
                }
            }
        }
    }

    private void compressBackupFile(@NonNull final File outputFile, @NonNull final File inputFile) throws IOException, NoSuchAlgorithmException {
        if (!inputFile.exists()) {
            throw new IOException("Backup file is gone."); // :S
        }

        InputStream inputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {
            inputStream = new FileInputStream(inputFile);
            zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile, false));

            zipOutputStream.setLevel(6);

            final ZipEntry zipEntry = new ZipEntry(BACKUP_FILE_NAME);
            zipEntry.setSize(inputFile.length());

            zipOutputStream.putNextEntry(zipEntry);

            CRC32 crc32 = new CRC32();
            crc32.reset();

            final byte[] buffer = new byte[1024];

            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, len);
                crc32.update(buffer, 0, len);
            }

            zipEntry.setCrc(crc32.getValue());

            zipOutputStream.closeEntry();
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
            Resources resources = ResourcesHelper.getResourcesForEnglish(getApplicationContext());
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

    private boolean loadDriveApiClients() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            mDriveClient = Drive.getDriveClient(getApplicationContext(), signInAccount);
            mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), signInAccount);

            return true;
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job stopped.");

        return false;
    }

    private void setLastBackupTime() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putLong(KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG, DateTime.now().getMillis());
        editor.putString(KEY_BACKUP_FILE_DRIVE_ID_STRING, mDriveId);
        editor.apply();
    }

    @SuppressWarnings("unused")
    private DriveClient getmDriveClient() {
        return mDriveClient;
    }

    private DriveResourceClient getmDriveResourceClient() {
        return mDriveResourceClient;
    }
}
