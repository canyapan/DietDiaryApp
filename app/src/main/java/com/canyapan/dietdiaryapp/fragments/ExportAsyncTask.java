package com.canyapan.dietdiaryapp.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.util.Log;

import com.canyapan.dietdiaryapp.Application;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.SharingSupportProvider;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.canyapan.dietdiaryapp.models.Event;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

abstract class ExportAsyncTask extends AsyncTask<Void, Integer, Boolean> {
    public static final int TO_EXTERNAL = 0;
    public static final int TO_SHARE = 1;

    protected ExportFragment exportFragment;
    private final File mFile;
    private final int mDestination;
    private final AtomicInteger mProgress = new AtomicInteger(0);
    private String mErrorString = null;

    ExportAsyncTask(ExportFragment exportFragment, int destination) throws ExportException {
        this.exportFragment = exportFragment;
        mDestination = destination;

        String fileName = getFileName(exportFragment.getString(R.string.app_name), exportFragment.mFromDate, exportFragment.mToDate);

        switch (destination) {
            case TO_EXTERNAL:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    File dir = new File(
                            Environment.getExternalStorageDirectory(),
                            Application.APP_DIR);
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();

                    mFile = new File(dir, fileName);
                } else {
                    Log.e(ExportFragment.TAG, "SD Card unavailable.");
                    throw new ExportException(exportFragment.getContext(), R.string.export_sd_card_unavailable);
                }
                break;
            case TO_SHARE:
                mFile = new File(exportFragment.getContext().getCacheDir(), fileName);
                break;
            default:
                throw new ExportException(exportFragment.getContext(), R.string.export_unimplemented_destination);
        }
    }

    protected abstract String getFileName(String prepend, LocalDate startDate, LocalDate endDate);

    protected void onPreExecute() {
        // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
        exportFragment.mProgressDialog = new ProgressDialog(exportFragment.getContext());
        exportFragment.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        exportFragment.mProgressDialog.setTitle(R.string.export_progress_title);
        exportFragment.mProgressDialog.setIndeterminate(false);
        exportFragment.mProgressDialog.setCancelable(false);
        exportFragment.mProgressDialog.setMax(100);
        exportFragment.mProgressDialog.show();
        exportFragment.mProgressDialog.setProgress(mProgress.get());
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            Resources engResources = ResourcesHelper.getEngResources(exportFragment.getContext());

            final OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(mFile, false), "UTF-8");
            os.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark
            start(os, engResources);
            publishProgress(1);

            db = exportFragment.mDatabaseHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DATE + " <= ?",
                    new String[]{exportFragment.mFromDate.toString(DatabaseHelper.DB_DATE_FORMATTER), exportFragment.mToDate.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            publishProgress(5);

            Event model;
            if (cursor.moveToFirst()) {
                final int count = cursor.getCount();
                Log.d(ExportFragment.TAG, MessageFormat.format("Exporting {0,number,integer} records.", count));

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
            Log.e(ExportFragment.TAG, "Content cannot be prepared probably a IO issue.", e);
            mErrorString = exportFragment.getString(R.string.export_csv_io_exception);
        } catch (SQLiteException e) {
            Log.e(ExportFragment.TAG, "Content cannot be prepared probably a DB issue.", e);
            mErrorString = exportFragment.getString(R.string.export_csv_sql_exception);
        } catch (Exception e) {
            Log.e(ExportFragment.TAG, "Content cannot be prepared.", e);
            mErrorString = exportFragment.getString(R.string.export_csv_exception);
        } finally {
            end();

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
            exportFragment.mProgressDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        exportFragment.mAsyncTask = null;

        if (exportFragment.mProgressDialog.isShowing()) {
            exportFragment.mProgressDialog.dismiss();
        }

        if (null == result || result.equals(false)) {
            Snackbar.make(exportFragment.mGridLayout, mErrorString, Snackbar.LENGTH_INDEFINITE).show();
        } else if (result.equals(true)) {
            Snackbar.make(exportFragment.mGridLayout, exportFragment.getString(R.string.export_external_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

            if (mDestination == TO_SHARE) {
                ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(exportFragment.getActivity())
                        .setType(SharingSupportProvider.MIME_TYPE_CSV)
                        .setStream(Uri.parse(SharingSupportProvider.CONTENT_URI_PREFIX + mFile.getName()));

                builder.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                builder.startChooser();

                exportFragment.mListener.onShared(Uri.fromFile(mFile), exportFragment.mFromDate, exportFragment.mToDate);
            } else {
                exportFragment.mListener.onExported(Uri.fromFile(mFile), exportFragment.mFromDate, exportFragment.mToDate);
            }
        }
    }

    protected abstract void start(OutputStreamWriter outputStream, Resources resources);

    protected abstract void write(Event event);

    protected abstract void end();
}
