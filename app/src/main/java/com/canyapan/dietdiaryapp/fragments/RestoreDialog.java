package com.canyapan.dietdiaryapp.fragments;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.canyapan.dietdiaryapp.BuildConfig;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.tasks.Task;
import com.opencsv.CSVReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

class RestoreDialog extends AlertDialog {
    public static final String TAG = "RestoreDialog";

    private RestoreAsyncTask mAsyncTask;

    private RestoreDialog(final Context context) {
        super(context);

        final LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressWarnings("RedundantCast") final View view = inflater.inflate(R.layout.dialog_restore, (ViewGroup) null);

        setView(view);
        setCancelable(false);

        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (null != mAsyncTask) {
                    mAsyncTask.mCancelled.lazySet(true);
                }
            }
        });
    }

    RestoreDialog(@NonNull final Context context, @NonNull final File file, @NonNull final OnRestoreListener listener) throws RestoreException {
        this(context);

        if (file.getName().toLowerCase().endsWith(".json")) {
            mAsyncTask = (RestoreAsyncTask) new RestoreFromJsonAsyncTask(context, file, listener).execute();
        } else if (file.getName().toLowerCase().endsWith(".csv")) {
            mAsyncTask = (RestoreAsyncTask) new RestoreFromCsvAsyncTask(context, file, listener).execute();
        } else {
            throw new UnsupportedOperationException(context.getString(R.string.backup_unimplemented_destination));
        }
    }

    RestoreDialog(@NonNull final Context context, @NonNull final DriveClient driveClient, @NonNull final DriveResourceClient driveResourceClient, @NonNull final String driveId, @NonNull final OnRestoreListener listener) throws RestoreException {
        this(context);

        mAsyncTask = (RestoreAsyncTask) new RestoreFromJsonAsyncTask(context, driveClient, driveResourceClient, driveId, listener).execute();
    }

    boolean isEnded() {
        return null == mAsyncTask || mAsyncTask.mEnded.get();
    }

    private static abstract class RestoreAsyncTask extends AsyncTask<Void, Integer, Long> {
        private final File mFile;
        private final OnRestoreListener mListener;
        private final DriveClient mDriveClient;
        private final DriveResourceClient mDriveResourceClient;
        private final String mDriveId;

        private final WeakReference<Context> mContextRef;

        private AtomicBoolean mCancelled, mEnded;
        private LocalDate mStartDate = null, mEndDate = null;
        private String mErrorString = null;

        RestoreAsyncTask(final Context context, final File file, final OnRestoreListener listener) throws RestoreException {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                    || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                if (!file.exists()) {
                    Log.e(TAG, "File not exists.");
                    throw new RestoreException(context, R.string.restore_file_not_exists);
                }

                if (!file.canRead()) {
                    Log.e(TAG, "File not readable.");
                    throw new RestoreException(context, R.string.restore_file_not_readable);
                }

                if (file.length() == 0) {
                    Log.e(TAG, "File empty.");
                    throw new RestoreException(context, R.string.restore_file_empty);
                }

                mFile = file;
                mContextRef = new WeakReference<>(context);
                mListener = listener;
                mCancelled = new AtomicBoolean();
                mEnded = new AtomicBoolean();
                mDriveClient = null;
                mDriveResourceClient = null;
                mDriveId = null;

                try {
                    checkFile(file);
                } catch (IOException e) {
                    throw new RestoreException(e.getMessage(), e);
                }
            } else {
                Log.e(TAG, "SD Card unavailable.");
                throw new RestoreException(context, R.string.backup_sd_card_unavailable);
            }
        }

        RestoreAsyncTask(final Context context, final DriveClient driveClient, final DriveResourceClient driveResourceClient,
                         final String driveId, final OnRestoreListener listener) throws RestoreException {
            // Create a temporary file in app cache dir.
            mFile = new File(context.getCacheDir(), "gdrive_cache.json");
            mContextRef = new WeakReference<>(context);
            mListener = listener;
            mCancelled = new AtomicBoolean();
            mEnded = new AtomicBoolean();
            mDriveClient = driveClient;
            mDriveResourceClient = driveResourceClient;
            mDriveId = driveId;

            /*if (!mFile.canWrite()) {
                Log.e(TAG, "App Cache Dir unavailable.");
                throw new RestoreException(context, R.string.backup_sd_card_unavailable);
            }*/
        }

        @Override
        protected Long doInBackground(Void... params) {
            if (null != mDriveClient && null != mDriveResourceClient) {
                Task<DriveId> driveIdTask = mDriveClient.getDriveId(mDriveId);
                try {
                    driveIdTask.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!driveIdTask.isComplete() || !driveIdTask.isSuccessful()) {
                    Log.e(TAG, "Couldn't get drive id. Maybe deleted by user.", driveIdTask.getException());
                    mErrorString = "Couldn't get drive id. Maybe deleted by user. " + driveIdTask.getException().getMessage();
                    return -1L;
                }

                final DriveId driveId = driveIdTask.getResult();

                Task<DriveContents> driveContentsTask = mDriveResourceClient.openFile(driveId.asDriveFile(), DriveFile.MODE_READ_ONLY);

                try {
                    driveContentsTask.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!driveContentsTask.isComplete() || !driveContentsTask.isSuccessful()) {
                    Log.e(TAG, "Error while trying to fetch file contents.", driveContentsTask.getException());
                    mErrorString = "Error while trying to fetch file contents. " + driveContentsTask.getException().getMessage();
                    return -1L;
                }

                final DriveContents driveContents = driveContentsTask.getResult();

                InputStream is = null;
                try {
                    is = driveContents.getInputStream();
                    FileUtils.copyInputStreamToFile(is, mFile);
                } catch (IOException e) {
                    if (BuildConfig.CRASHLYTICS_ENABLED) {
                        Crashlytics.logException(e);
                    }
                    Log.e(TAG, "Unable to download backup file.", e);
                    mErrorString = mContextRef.get().getString(R.string.restore_io_exception);

                    mFile.delete();

                    return -1L;
                } finally {
                    if (null != is) {
                        try {
                            is.close();
                        } catch (IOException ignore) {
                        }
                    }
                }

                // TODO UNZIP FILE IT
            }

            DatabaseHelper databaseHelper = new DatabaseHelper(mContextRef.get());
            SQLiteDatabase db = null;

            try {
                Resources engResources = ResourcesHelper.getEngResources(mContextRef.get());

                BOMInputStream is = new BOMInputStream(new FileInputStream(mFile));
                String charset;
                if (is.hasBOM()) {
                    Log.d(TAG, "File has Unicode BOM for " + is.getBOMCharsetName());
                    charset = is.getBOMCharsetName();
                } else {
                    Log.d(TAG, "Using UTF-8 charset to read the file.");
                    charset = "UTF-8";
                }

                final InputStreamReader inputStream = new InputStreamReader(is, charset);
                start(inputStream, engResources);

                db = databaseHelper.getWritableDatabase();
                db.beginTransaction();

                long inserted = 0;
                ContentValues values;
                while ((values = readNext()) != null) {
                    if (mCancelled.get()) {
                        return -2L;
                    }

                    Log.d(TAG, values.toString());
                    long rowID = db.insert(DatabaseHelper.DBT_EVENT, DatabaseHelper.DBC_EVENT_DESC, values);
                    if (rowID < 0) {
                        Log.e(TAG, "Record cannot be inserted. index: " + inserted);
                        throw new RestoreException(mContextRef.get(), R.string.restore_already_existing_records);
                    }

                    inserted++;
                }

                Log.d(TAG, "Records inserted " + inserted);
                db.setTransactionSuccessful();
                return inserted;
            } catch (RestoreException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                mErrorString = e.getMessage();
            } catch (IOException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                Log.e(TAG, "Content cannot be imported. Probably a IO issue.", e);
                mErrorString = mContextRef.get().getString(R.string.restore_io_exception);
            } catch (SQLiteException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                Log.e(TAG, "Content cannot be imported. Probably a DB issue.", e);
                mErrorString = mContextRef.get().getString(R.string.restore_sql_exception);
            } catch (Exception e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                Log.e(TAG, "Content cannot be imported.", e);
                mErrorString = mContextRef.get().getString(R.string.restore_exception);
            } finally {
                try {
                    end();
                } catch (IOException ignore) {
                }

                if (null != mDriveClient) {
                    // This is a restore from drive and @mFile is a temp file. So, I should delete it.
                    mFile.delete();
                }

                if (null != db) {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                    if (db.isOpen()) {
                        db.close();
                    }
                }
            }

            return -1L;
        }

        @Override
        protected void onPostExecute(Long result) {
            mEnded.set(true);
            if (result >= 0) {
                if (null == mDriveId) {
                    mListener.onRestoreComplete(mFile.getPath(), mStartDate, mEndDate, result);
                } else {
                    mListener.onRestoreComplete(mDriveId, mStartDate, mEndDate, result);
                }
            } else if (result == -1) { // -1 failed
                if (null == mDriveId) {
                    mListener.onRestoreFailed(mFile.getPath(), mErrorString);
                } else {
                    mListener.onRestoreFailed(mDriveId, mErrorString);
                }
            } // -2 cancelled
        }

        String getExceptionText(@StringRes int resId) {
            return mContextRef.get().getString(resId);
        }

        /**
         * Updated the date range of the imported record set.
         * This range will be used for updating UI afterwards.
         *
         * @param date the date of the record.
         */
        void updateDateRange(LocalDate date) {
            if (null == mStartDate) {
                mStartDate = date;
            } else if (date.isBefore(mStartDate)) {
                mStartDate = date;
            }

            if (null == mEndDate) {
                mEndDate = date;
            } else if (date.isAfter(mEndDate)) {
                mEndDate = date;
            }
        }

        protected abstract void checkFile(File file) throws IOException;

        protected abstract void start(InputStreamReader inputStream, Resources resources) throws IOException;

        protected abstract ContentValues readNext() throws IOException;

        protected abstract void end() throws IOException;
    }

    private static class RestoreFromJsonAsyncTask extends RestoreDialog.RestoreAsyncTask {
        private JsonReader reader = null;
        private HashMap<String, Integer>
                typesMap = null,
                foodTypesMap = null,
                drinkTypesMap = null;
        private long index = 0;

        RestoreFromJsonAsyncTask(@NonNull final Context context, @NonNull final File file, @NonNull final OnRestoreListener listener) throws RestoreException {
            super(context, file, listener);
        }

        RestoreFromJsonAsyncTask(@NonNull final Context context, @NonNull final DriveClient driveClient, @NonNull final DriveResourceClient driveResourceClient,
                                 @NonNull final String driveId, @NonNull final OnRestoreListener listener) throws RestoreException {
            super(context, driveClient, driveResourceClient, driveId, listener);
        }

        @Override
        protected void checkFile(File file) throws IOException {
            if (!file.getName().toLowerCase().endsWith(".json")) {
                Log.e(RestoreFragment.TAG, "File is not a JSON.");
                throw new IOException(getExceptionText(R.string.restore_file_not_json));
            }
        }

        @Override
        protected void start(InputStreamReader inputStream, Resources resources) throws IOException {
            reader = new JsonReader(inputStream);

            String[] types = resources.getStringArray(R.array.spinner_event_types);
            typesMap = new HashMap<>(types.length);
            for (int i = 0; i < types.length; i++) {
                typesMap.put(types[i], i);
            }

            String[] foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
            foodTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < foodTypes.length; i++) {
                foodTypesMap.put(foodTypes[i], i);
            }

            String[] drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
            drinkTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < drinkTypes.length; i++) {
                drinkTypesMap.put(drinkTypes[i], i);
            }

            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "App":
                        break;
                    case "Ver":
                        break;
                    case "Events":
                        reader.beginArray();
                        return;
                    default:
                        reader.skipValue();
                }
            }
        }

        @Override
        protected ContentValues readNext() throws IOException {
            if (!reader.hasNext()) {
                return null;
            }

            reader.beginObject();
            try {
                return parseRecord();
            } catch (IOException e) {
                throw new IOException(getExceptionText(R.string.restore_json_corrupted), e);
            } finally {
                reader.endObject();
                index++;
            }
        }

        private ContentValues parseRecord() throws IOException {
            Long id;
            LocalDate date;
            LocalTime time;
            Integer type, subType;
            String temp = null;

            try {
                id = reader.nextLong();
            } catch (IOException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON id cannot be parsed. record: {0}",
                        index));
                throw e;
            }

            try {
                temp = reader.nextString();
                date = LocalDate.parse(temp, DatabaseHelper.DB_DATE_FORMATTER);

                updateDateRange(date); // sorry for this, but I didn't want to parse date on super, again. :(
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON date cannot be parsed. record: {0} date: {1}",
                        index, temp));
                throw e;
            }

            try {
                temp = reader.nextString();
                time = LocalTime.parse(temp, DatabaseHelper.DB_TIME_FORMATTER);
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON time cannot be parsed. record: {0} time: {1}",
                        index, temp));
                throw e;
            }

            temp = reader.nextString();
            type = typesMap.get(temp);
            if (null == type) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON type cannot be identified. record: {0} type: {1}",
                        index, temp));
                throw new IOException("Type cannot be identified. " + temp);
            } else {
                temp = reader.nextString();
                switch (type) {
                    case 0:
                        subType = foodTypesMap.get(temp);
                        break;
                    case 1:
                        subType = drinkTypesMap.get(temp);
                        break;
                    default:
                        subType = 0;
                }

                if (null == subType) {
                    Log.e(RestoreFragment.TAG, MessageFormat.format("JSON subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                            index, type, temp));
                    throw new IOException("SubType cannot be identified. " + temp);
                }
            }

            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.DBC_EVENT_ROW_ID, id);
            values.put(DatabaseHelper.DBC_EVENT_DATE, date.toString(DatabaseHelper.DB_DATE_FORMATTER));
            values.put(DatabaseHelper.DBC_EVENT_TIME, time.toString(DatabaseHelper.DB_TIME_FORMATTER));
            values.put(DatabaseHelper.DBC_EVENT_TYPE, type);
            values.put(DatabaseHelper.DBC_EVENT_SUBTYPE, subType);
            values.put(DatabaseHelper.DBC_EVENT_DESC, reader.nextString());

            return values;
        }

        @Override
        protected void end() {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static class RestoreFromCsvAsyncTask extends RestoreDialog.RestoreAsyncTask {
        private CSVReader reader = null;
        private HashMap<String, Integer>
                typesMap = null,
                foodTypesMap = null,
                drinkTypesMap = null;
        private String[] csvHeaders;

        RestoreFromCsvAsyncTask(@NonNull final Context context, @NonNull final File file, @NonNull final OnRestoreListener listener) throws RestoreException {
            super(context, file, listener);
        }

        @Override
        protected void checkFile(@NonNull File file) throws IOException {
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                Log.e(RestoreFragment.TAG, "File is not a CSV.");
                throw new IOException(getExceptionText(R.string.restore_file_not_csv));
            }
        }

        @Override
        protected void start(InputStreamReader inputStream, Resources resources) throws IOException {
            reader = new CSVReader(inputStream);

            String[] types = resources.getStringArray(R.array.spinner_event_types);
            typesMap = new HashMap<>(types.length);
            for (int i = 0; i < types.length; i++) {
                typesMap.put(types[i], i);
            }

            String[] foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
            foodTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < foodTypes.length; i++) {
                foodTypesMap.put(foodTypes[i], i);
            }

            String[] drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
            drinkTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < drinkTypes.length; i++) {
                drinkTypesMap.put(drinkTypes[i], i);
            }

            csvHeaders = resources.getStringArray(R.array.csv_headers);

            checkHeaders();
        }

        private void checkHeaders() throws IOException {
            String[] record = reader.readNext();

            if (!Arrays.equals(csvHeaders, record)) {
                Log.e(RestoreFragment.TAG, "CSV headers does not match.");
                throw new IOException(getExceptionText(R.string.restore_csv_corrupted));
            }
        }

        @Override
        protected ContentValues readNext() throws IOException {
            String[] record;
            if ((record = reader.readNext()) == null) {
                return null;
            }

            try {
                return parseRecord(record, reader.getRecordsRead());
            } catch (IOException e) {
                throw new IOException(getExceptionText(R.string.restore_csv_corrupted), e);
            }
        }

        private ContentValues parseRecord(final String[] record, final long index) throws IOException {
            Long id;
            LocalDate date;
            LocalTime time;
            Integer type, subType;

            try {
                id = Long.parseLong(record[0]);
            } catch (NumberFormatException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("CSV id cannot be parsed. record: {0} id: {1}",
                        index, record[0]));
                throw e;
            }

            try {
                date = LocalDate.parse(record[1], DatabaseHelper.DB_DATE_FORMATTER);

                updateDateRange(date); // sorry for this, but I didn't want to parse date on super, again. :(
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("CSV date cannot be parsed. record: {0} date: {1}",
                        index, record[1]));
                throw e;
            }

            try {
                time = LocalTime.parse(record[2], DatabaseHelper.DB_TIME_FORMATTER);
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("CSV time cannot be parsed. record: {0} time: {1}",
                        index, record[2]));
                throw e;
            }

            type = typesMap.get(record[3]);
            if (null == type) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("CSV type cannot be identified. record: {0} type: {1}",
                        index, record[3]));
                throw new IOException("Type cannot be identified. " + record[3]);
            } else {
                switch (type) {
                    case 0:
                        subType = foodTypesMap.get(record[4]);
                        break;
                    case 1:
                        subType = drinkTypesMap.get(record[4]);
                        break;
                    default:
                        subType = 0;
                }

                if (null == subType) {
                    Log.e(RestoreFragment.TAG, MessageFormat.format("CSV subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                            index, record[3], record[4]));
                    throw new IOException("SubType cannot be identified. " + record[4]);
                }
            }

            ContentValues values = new ContentValues();

            values.put(DatabaseHelper.DBC_EVENT_ROW_ID, id);
            values.put(DatabaseHelper.DBC_EVENT_DATE, date.toString(DatabaseHelper.DB_DATE_FORMATTER));
            values.put(DatabaseHelper.DBC_EVENT_TIME, time.toString(DatabaseHelper.DB_TIME_FORMATTER));
            values.put(DatabaseHelper.DBC_EVENT_TYPE, type);
            values.put(DatabaseHelper.DBC_EVENT_SUBTYPE, subType);
            values.put(DatabaseHelper.DBC_EVENT_DESC, record[5]);

            return values;
        }

        @Override
        protected void end() {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    interface OnRestoreListener {
        void onRestoreComplete(String tag, LocalDate startDate, LocalDate endDate, long recordsInserted);

        void onRestoreFailed(String tag, String message);
    }
}
