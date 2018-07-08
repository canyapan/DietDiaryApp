package com.canyapan.dietdiaryapp.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.canyapan.dietdiaryapp.Application;
import com.canyapan.dietdiaryapp.BuildConfig;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.canyapan.dietdiaryapp.helpers.SharedFileHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

import static com.canyapan.dietdiaryapp.Application.FILE_PROVIDER;
import static com.canyapan.dietdiaryapp.helpers.MimeTypes.MIME_TYPE_CSV;

abstract class BackupAsyncTask extends AsyncTask<Void, Integer, Boolean> {
    static final int TO_EXTERNAL = 0;
    static final int TO_SHARE = 1;

    private BackupFragment backupFragment;
    private final File mFile;
    private final int mDestination;
    private final AtomicInteger mProgress = new AtomicInteger(0);
    private String mErrorString = null;

    BackupAsyncTask(BackupFragment backupFragment, int destination) throws BackupException {
        this.backupFragment = backupFragment;
        mDestination = destination;

        String fileName = getFileName(backupFragment.getString(R.string.app_name), backupFragment.mFromDate, backupFragment.mToDate);

        switch (destination) {
            case TO_EXTERNAL:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File dir = new File(
                            Environment.getExternalStorageDirectory(),
                            Application.APP_DIR);

                    if (dir.mkdirs()) {
                        MediaScannerConnection.scanFile(backupFragment.getContext(), new String[]{dir.getPath()}, null, null);
                    }

                    mFile = new File(dir, fileName);
                } else {
                    Log.e(BackupFragment.TAG, "SD Card unavailable.");
                    throw new BackupException(backupFragment.getContext(), R.string.backup_sd_card_unavailable);
                }
                break;
            case TO_SHARE:
                try {
                    mFile = SharedFileHelper.getSharedFile(backupFragment.getContext(), fileName);
                } catch (IOException e) {
                    throw new BackupException(backupFragment.getContext(), R.string.cannot_create_file_path);
                }
                break;
            default:
                throw new BackupException(backupFragment.getContext(), R.string.backup_unimplemented_destination);
        }
    }

    protected void onPreExecute() {
        // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
        backupFragment.mProgressDialog = new ProgressDialog(backupFragment.getContext());
        backupFragment.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        backupFragment.mProgressDialog.setTitle(R.string.backup_progress_title);
        backupFragment.mProgressDialog.setIndeterminate(false);
        backupFragment.mProgressDialog.setCancelable(false);
        backupFragment.mProgressDialog.setMax(100);
        backupFragment.mProgressDialog.show();
        backupFragment.mProgressDialog.setProgress(mProgress.get());
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            Resources engResources = ResourcesHelper.getResourcesForEnglish(backupFragment.getContext());

            final OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(mFile, false), "UTF-8");
            os.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark

            start(os, engResources);
            publishProgress(1);

            db = backupFragment.mDatabaseHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DATE + " <= ?",
                    new String[]{backupFragment.mFromDate.toString(DatabaseHelper.DB_DATE_FORMATTER), backupFragment.mToDate.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            publishProgress(5);

            Event model;
            if (cursor.moveToFirst()) {
                final int count = cursor.getCount();
                Log.d(BackupFragment.TAG, MessageFormat.format("Exporting {0,number,integer} records.", count));

                int current = 0;
                int percent = 0, percent_;
                do {
                    model = EventHelper.parse(cursor);

                    percent_ = (int) Math.floor(++current * 95 / count);
                    if (percent < percent_) {
                        percent = percent_;
                        publishProgress(percent + 5);
                    }

                    write(model);
                } while (cursor.moveToNext());
            }

            publishProgress(100);
            return true;
        } catch (IOException e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(BackupFragment.TAG, "Content cannot be prepared probably a IO issue.", e);
            mErrorString = backupFragment.getString(R.string.backup_io_exception);
        } catch (SQLiteException e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(BackupFragment.TAG, "Content cannot be prepared probably a DB issue.", e);
            mErrorString = backupFragment.getString(R.string.backup_sql_exception);
        } catch (Exception e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(BackupFragment.TAG, "Content cannot be prepared.", e);
            mErrorString = backupFragment.getString(R.string.backup_exception);
        } finally {
            try {
                end();
            } catch (IOException ignore) {
            }

            if (null != cursor) {
                cursor.close();
            }

            if (null != db && db.isOpen()) {
                db.close();
            }
        }

        return false;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values.length > 0 && values[0] != null) {
            mProgress.set(values[0]);
            backupFragment.mProgressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        backupFragment.mAsyncTask = null;

        if (backupFragment.mProgressDialog.isShowing()) {
            backupFragment.mProgressDialog.dismiss();
        }

        if (null == result || result.equals(false)) {
            Snackbar.make(backupFragment.mGridLayout, mErrorString, Snackbar.LENGTH_INDEFINITE).show();
        } else if (result.equals(true)) {
            Snackbar.make(backupFragment.mGridLayout, backupFragment.getString(R.string.backup_external_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

            if (mDestination == TO_SHARE) {
                Uri uri = FileProvider.getUriForFile(backupFragment.getContext(), FILE_PROVIDER, mFile);

                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(backupFragment.getActivity())
                        .setType(MIME_TYPE_CSV)
                        .setStream(uri);

                builder.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                builder.startChooser();

                backupFragment.mListener.onShareComplete(Uri.fromFile(mFile), backupFragment.mFromDate, backupFragment.mToDate);
            } else {
                // initiate media scan and put the new things into the path array to
                // make the scanner aware of the location and the files
                MediaScannerConnection.scanFile(backupFragment.getContext(), new String[]{mFile.getPath()}, null, null);

                backupFragment.mListener.onBackupComplete(Uri.fromFile(mFile), backupFragment.mFromDate, backupFragment.mToDate);
            }
        }
    }

    protected abstract String getFileName(String prepend, LocalDate startDate, LocalDate endDate);

    protected abstract void start(OutputStreamWriter outputStream, Resources resources) throws IOException;

    protected abstract void write(Event event) throws IOException;

    protected abstract void end() throws IOException;
}
