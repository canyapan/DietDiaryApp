package com.canyapan.dietdiaryapp;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.Log;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.SharedFileHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This async task class exports records from database into selected destination.
 * Returns count of exported record.
 */
abstract class ExportAsyncTask extends AsyncTask<Void, Integer, Long> {
    static final int TO_EMAIL = 0;
    static final int TO_EXTERNAL = 1;
    static final int TO_SHARE = 2;
    private static final String TAG = "ExportAsyncTask";
    protected final File mFile;
    private final WeakReference<ExportActivity> mExportActivityRef;
    private final OnExportListener mListener;
    private final LocalDate mFromDate, mToDate;
    private final int mDestination;
    private final AtomicInteger mProgress = new AtomicInteger(0);
    private DatabaseHelper mDatabaseHelper;
    private String mErrorString = null;

    ExportAsyncTask(final ExportActivity activity, final int destination,
                    final LocalDate fromDate, final LocalDate toDate,
                    final OnExportListener listener) throws ExportException {
        mExportActivityRef = new WeakReference<>(activity);
        mDestination = destination;
        mListener = listener;
        mFromDate = fromDate;
        mToDate = toDate;

        mDatabaseHelper = new DatabaseHelper(activity);

        final String fileName = getFileName(activity.getString(R.string.app_name), fromDate, toDate);

        switch (destination) {
            case TO_EXTERNAL:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File dir = new File(
                            Environment.getExternalStorageDirectory(),
                            Application.APP_DIR);

                    if (dir.mkdirs()) {
                        MediaScannerConnection.scanFile(activity, new String[]{dir.getPath()}, null, null);
                    }

                    mFile = new File(dir, fileName);
                } else {
                    Log.e(TAG, "SD Card unavailable.");
                    throw new ExportException(activity, R.string.backup_sd_card_unavailable);
                }
                break;
            case TO_EMAIL:
            case TO_SHARE:
                try {
                    mFile = SharedFileHelper.getSharedFile(activity, fileName);
                } catch (IOException e) {
                    throw new ExportException(activity, R.string.cannot_create_file_path, e);
                }
                break;
            default:
                throw new ExportException(activity, R.string.backup_unimplemented_destination);
        }
    }

    protected Resources getResources() {
        return mExportActivityRef.get().getResources();
    }

    protected void onPreExecute() {
        mListener.onExportStarting();
    }

    @Override
    protected Long doInBackground(Void... params) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        OutputStream outputStream = null;

        try {
            if (null != mExportActivityRef.get()) {
                outputStream = new FileOutputStream(mFile, false);

                //writer = new OutputStreamWriter(new FileOutputStream(mFile, false), "UTF-8"); // Would use StandardCharsets.UTF_8 if API level was 19
                //writer.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark

                start(outputStream, mFromDate, mToDate);
                publishProgress(1);
            }

            db = mDatabaseHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DATE + " <= ?",
                    new String[]{mFromDate.toString(DatabaseHelper.DB_DATE_FORMATTER), mToDate.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            publishProgress(5);

            if (cursor.moveToFirst()) {
                final long count = cursor.getCount();
                Log.d(TAG, MessageFormat.format("Exporting {0,number,integer} records.", count));

                LocalDate prev = null;
                Event event;
                long current = 0;
                int percent = 0, percent_;
                do {
                    event = EventHelper.parse(cursor);
                    write(event, !event.getDate().equals(prev), current, count);
                    prev = event.getDate();

                    percent_ = (int) Math.floor(++current * 85 / count);
                    if (percent < percent_) {
                        percent = percent_;
                        publishProgress(percent + 5);
                    }
                } while (cursor.moveToNext());
            }

            end();

            publishProgress(100);
            return 0L;
        } catch (IOException e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(TAG, "Content cannot be prepared probably a IO issue.", e);
            mErrorString = mExportActivityRef.get().getString(R.string.backup_io_exception);
        } catch (SQLiteException e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(TAG, "Content cannot be prepared probably a DB issue.", e);
            mErrorString = mExportActivityRef.get().getString(R.string.backup_sql_exception);
        } catch (Exception e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(TAG, "Content cannot be prepared.", e);
            mErrorString = mExportActivityRef.get().getString(R.string.backup_exception);
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException ignore) {
                }
            }

            if (null != cursor) {
                cursor.close();
            }

            if (null != db && db.isOpen()) {
                db.close();
            }
        }

        return -1L;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 0 && values[0] != null) {
            mProgress.set(values[0]);
            mListener.onExportProgress(mProgress.get());
        }
    }

    @Override
    protected void onPostExecute(Long count) {
        if (null == count || count.compareTo(0L) < 0) {
            mListener.onExportFailed(mErrorString);
        } else {
            mListener.onExportComplete(mFromDate, mToDate, mDestination, mFile, count);
        }
    }

    protected abstract String getFileName(String prepend, LocalDate startDate, LocalDate endDate);

    protected abstract void start(OutputStream outputStream, LocalDate fromDate, LocalDate toDate) throws IOException, ExportException;

    protected abstract void write(Event event, boolean newDay, long index, long count) throws IOException, ExportException;

    protected abstract void end() throws IOException, ExportException;

    interface OnExportListener {
        void onExportStarting();

        void onExportProgress(int percentage);

        void onExportComplete(LocalDate startDate, LocalDate endDate, int destination, File file, long recordsExported);

        void onExportFailed(String message);
    }

    class ExportException extends Exception {

        ExportException(String message) {
            super(message);
        }

        ExportException(String message, Throwable cause) {
            super(message, cause);
        }

        ExportException(@NonNull Context context, @StringRes int message) {
            this(context.getString(message));
        }

        ExportException(@NonNull Context context, @StringRes int message, Throwable cause) {
            this(context.getString(message), cause);
        }
    }

}
