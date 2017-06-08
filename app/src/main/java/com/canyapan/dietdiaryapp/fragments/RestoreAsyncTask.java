package com.canyapan.dietdiaryapp.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.io.input.BOMInputStream;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

abstract class RestoreAsyncTask extends AsyncTask<Void, Integer, Boolean> {
    private RestoreFragment restoreFragment;
    private LocalDate mStartDate = null, mEndDate = null;
    private final File mFile;
    private String mErrorString = null;

    RestoreAsyncTask(RestoreFragment restoreFragment, File file) throws RestoreException {
        this.restoreFragment = restoreFragment;
        mFile = file;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            if (!mFile.exists()) {
                Log.e(RestoreFragment.TAG, "File not exists.");
                throw new RestoreException(restoreFragment.getContext(), R.string.restore_file_not_exists);
            }

            if (!mFile.canRead()) {
                Log.e(RestoreFragment.TAG, "File not readable.");
                throw new RestoreException(restoreFragment.getContext(), R.string.restore_file_not_readable);
            }

            if (mFile.length() == 0) {
                Log.e(RestoreFragment.TAG, "File empty.");
                throw new RestoreException(restoreFragment.getContext(), R.string.restore_file_empty);
            }

            try {
                checkFile(file);
            } catch (IOException e) {
                throw new RestoreException(e.getMessage(), e);
            }
        } else {
            Log.e(RestoreFragment.TAG, "SD Card unavailable.");
            throw new RestoreException(restoreFragment.getContext(), R.string.backup_sd_card_unavailable);
        }
    }

    protected void onPreExecute() {
        // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
        restoreFragment.mProgressDialog = new ProgressDialog(restoreFragment.getContext());
        restoreFragment.mProgressDialog.setTitle(R.string.restore_progress_title);
        restoreFragment.mProgressDialog.setIndeterminate(true);
        restoreFragment.mProgressDialog.setCancelable(false);
        restoreFragment.mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SQLiteDatabase db = null;

        try {
            Resources engResources = ResourcesHelper.getEngResources(restoreFragment.getContext());

            BOMInputStream is = new BOMInputStream(new FileInputStream(mFile));
            String charset;
            if (is.hasBOM()) {
                Log.d(RestoreFragment.TAG, "File has Unicode BOM for " + is.getBOMCharsetName());
                charset = is.getBOMCharsetName();
            } else {
                Log.d(RestoreFragment.TAG, "Using UTF-8 charset to read the file.");
                charset = "UTF-8";
            }

            final InputStreamReader inputStream = new InputStreamReader(is, charset);
            start(inputStream, engResources);

            db = restoreFragment.mDatabaseHelper.getWritableDatabase();
            db.beginTransaction();

            int inserted = 0;
            ContentValues values;
            while ((values = readNext()) != null) {
                Log.d(RestoreFragment.TAG, values.toString());

                if (restoreFragment.isForceEnabled()) {
                    values.remove(DatabaseHelper.DBC_EVENT_ROW_ID);
                }

                long rowID = db.insert(DatabaseHelper.DBT_EVENT, DatabaseHelper.DBC_EVENT_DESC, values);
                if (rowID < 0) {
                    Log.e(RestoreFragment.TAG, "Record cannot be inserted. index: " + inserted);
                    throw new RestoreException(restoreFragment.getContext(), R.string.restore_already_existing_records);
                }

                inserted++;
            }

            Log.d(RestoreFragment.TAG, "Records inserted " + inserted);
            db.setTransactionSuccessful();
            return true;
        } catch (RestoreException e) {
            Crashlytics.logException(e);
            mErrorString = e.getMessage();
        } catch (IOException e) {
            Crashlytics.logException(e);
            Log.e(RestoreFragment.TAG, "Content cannot be imported. Probably a IO issue.", e);
            mErrorString = restoreFragment.getString(R.string.restore_io_exception);
        } catch (SQLiteException e) {
            Crashlytics.logException(e);
            Log.e(RestoreFragment.TAG, "Content cannot be imported. Probably a DB issue.", e);
            mErrorString = restoreFragment.getString(R.string.restore_sql_exception);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(RestoreFragment.TAG, "Content cannot be imported.", e);
            mErrorString = restoreFragment.getString(R.string.restore_exception);
        } finally {
            try {
                end();
            } catch (IOException ignore) {
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

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        restoreFragment.mAsyncTask = null;

        if (restoreFragment.mProgressDialog.isShowing()) {
            restoreFragment.mProgressDialog.dismiss();
        }

        if (null == result || result.equals(false)) {
            Snackbar.make(restoreFragment.mLinearLayout, mErrorString, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snack_bar_dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Just dismiss
                        }
                    }).show();
        } else if (result.equals(true)) {
            Snackbar.make(restoreFragment.mLinearLayout, restoreFragment.getString(R.string.restore_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

            restoreFragment.mListener.onImportComplete(Uri.fromFile(mFile), mStartDate, mEndDate);
        }
    }

    String getExceptionText(@StringRes int resId) {
        return restoreFragment.getString(resId);
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
