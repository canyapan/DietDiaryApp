package com.canyapan.dietdiaryapp.fragments;

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
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.canyapan.dietdiaryapp.BuildConfig;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.opencsv.CSVReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//noinspection ResultOfMethodCallIgnored
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
                try {
                    getFileFromDrive();
                } catch (RestoreException e) {
                    FileUtils.deleteQuietly(mFile);
                    Log.e(TAG, "Couldn't file from drive.", e);
                    mErrorString = e.getMessage();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            final DatabaseHelper databaseHelper = new DatabaseHelper(mContextRef.get());
            SQLiteDatabase db = null;
            try {
                Resources engResources = ResourcesHelper.getResourcesForEnglish(mContextRef.get());

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

                Event event;
                while ((event = readNext()) != null) {
                    if (mCancelled.get()) {
                        return -2L;
                    }

                    if (!EventHelper.insert(db, event)) {
                        // Bring event from database.
                        final Event e = EventHelper.getEventByID(db, event.getID());
                        // Check if they are equal
                        if (event.equals(e)) {
                            Log.d(TAG, "Record already exists. " + event.toString());
                            continue; // There is the same record, so pass it.
                        } else {
                            event.setID(-1); // Clear ID, because this is a different event.
                            if (!EventHelper.insert(db, event)) {
                                Log.w(TAG, "Failed to insert a record. " + event.toString());
                                continue; // Failed to insert.
                            }
                        }
                    }

                    inserted++;
                }

                Log.d(TAG, "Records inserted " + inserted);
                db.setTransactionSuccessful();
                return inserted;
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
                    FileUtils.deleteQuietly(mFile);
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

        private void getFileFromDrive() throws RestoreException, ExecutionException, InterruptedException {
            final DriveId driveId = DriveId.decodeFromString(mDriveId);

            Task<DriveContents> driveContentsTask = mDriveResourceClient.openFile(driveId.asDriveFile(), DriveFile.MODE_READ_ONLY);

            Tasks.await(driveContentsTask);

            if (!driveContentsTask.isSuccessful()) {
                throw new RestoreException("Error while trying to fetch file contents.", driveContentsTask.getException());
            }

            final DriveContents driveContents = driveContentsTask.getResult();

            InputStream is = null;
            ZipInputStream zis = null;
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mFile, false);
                is = driveContents.getInputStream();
                zis = new ZipInputStream(is);
                ZipEntry ze = zis.getNextEntry();

                if (null == ze) {
                    throw new RestoreException("Compressed backup doesn't include any file.");
                }

                CRC32 crc32 = new CRC32();
                crc32.reset();

                final byte[] buffer = new byte[1024];

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    crc32.update(buffer, 0, len);
                }

                if (crc32.getValue() != ze.getCrc()) {
                    throw new RestoreException("Checksum is wrong. File corrupted");
                }
            } catch (IOException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }

                throw new RestoreException("Unable to download backup file.", e);
            } finally {
                if (null != zis) {
                    try {
                        zis.close();
                    } catch (IOException ignore) {
                    }
                }

                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException ignore) {
                    }
                }

                if (null != fos) {
                    try {
                        fos.flush();
                        fos.close();
                    } catch (IOException ignore) {
                    }
                }
            }
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

        protected abstract Event readNext() throws IOException;

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
                        reader.skipValue();
                        break;
                    case "Ver":
                        reader.skipValue();
                        break;
                    case "Device":
                        reader.skipValue();
                        break;
                    case "Settings":
                        reader.skipValue();
                        // TODO: Read and apply previous settings.
                        break;
                    case "Events":
                        reader.beginArray();
                        return; // This will be read @readNext()
                }
            }
        }

        @Override
        protected Event readNext() throws IOException {
            if (!reader.hasNext()) {
                return null;
            }

            if (reader.peek() == JsonToken.END_ARRAY) {
                return null;
            }

            try {
                return parseRecord();
            } catch (IOException e) {
                throw new IOException(getExceptionText(R.string.restore_json_corrupted), e);
            }
        }

        private Event parseRecord() throws IOException {
            Long id = null;
            LocalDate date = null;
            LocalTime time = null;
            Integer type = null, subType = null;
            String desc = null;

            reader.beginObject();

            while (reader.hasNext()) {
                if (reader.peek() == JsonToken.END_OBJECT) {
                    break;
                }

                switch (reader.nextName()) {
                    case "ID":
                        id = readID();
                        break;
                    case "Date":
                        date = readDate();
                        updateDateRange(date); // sorry for this, but I didn't want to parse date on super, again. :(
                        break;
                    case "Time":
                        time = readTime();
                        break;
                    case "Type":
                        type = readType();
                        break;
                    case "Title":
                        subType = readSubType(type);
                        break;
                    case "Description":
                        desc = readDescription();
                        break;
                }

            }

            reader.endObject();
            index++;

            if (null == id) {
                throw new IllegalArgumentException("ID cannot be null");
            } else if (null == date) {
                throw new IllegalArgumentException("Date cannot be null");
            } else if (null == time) {
                throw new IllegalArgumentException("Time cannot be null");
            } else if (null == type) {
                throw new IllegalArgumentException("Type cannot be null");
            } else if (null == subType) {
                throw new IllegalArgumentException("SubType cannot be null");
            } else if (null == desc) {
                throw new IllegalArgumentException("Description cannot be null");
            }

            return new Event(id, date, time, type, subType, desc);
        }

        @NonNull
        private Long readID() throws IOException {
            try {
                return reader.nextLong();
            } catch (IOException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON id cannot be parsed. record: {0}", index));
                throw e;
            }
        }

        @NonNull
        private LocalDate readDate() throws IOException {
            String temp = null;
            try {
                temp = reader.nextString();
                return LocalDate.parse(temp, DatabaseHelper.DB_DATE_FORMATTER);
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON date cannot be parsed. record: {0} date: {1}", index, temp));
                throw e;
            }
        }

        @NonNull
        private LocalTime readTime() throws IOException {
            String temp = null;
            try {
                temp = reader.nextString();
                return LocalTime.parse(temp, DatabaseHelper.DB_TIME_FORMATTER);
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON time cannot be parsed. record: {0} time: {1}", index, temp));
                throw e;
            }
        }

        @NonNull
        private Integer readType() throws IOException {
            String temp = null;
            try {
                temp = reader.nextString();
                return typesMap.get(temp);
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON type cannot be identified. record: {0} type: {1}", index, temp));
                throw new IOException("Type cannot be identified. " + temp);
            }
        }

        @NonNull
        private Integer readSubType(Integer type) throws IOException {
            if (null == type) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON type required to identify subType. record: {0}", index));
                throw new IOException("Type required to identify subType.");
            }

            String temp = null;
            try {
                temp = reader.nextString();
                switch (type) {
                    case 0:
                        return foodTypesMap.get(temp);
                    case 1:
                        return drinkTypesMap.get(temp);
                    default:
                        return 0;
                }
            } catch (IllegalArgumentException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON subtype cannot be identified. record: {0} type: {1} subtype: {2}", index, type, temp));
                throw new IOException("SubType cannot be identified. " + temp);
            }
        }

        @NonNull
        private String readDescription() throws IOException {
            try {
                return reader.nextString();
            } catch (IOException e) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON description cannot be parsed. record: {0}", index));
                throw e;
            }
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
        protected Event readNext() throws IOException {
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

        private Event parseRecord(final String[] record, final long index) throws IOException {
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

            return new Event(id, date, time, type, subType, record[5]);
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
