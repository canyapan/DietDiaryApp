package com.canyapan.dietdiaryapp.fragments;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.crashlytics.android.Crashlytics;
import com.opencsv.CSVReader;

import org.apache.commons.io.input.BOMInputStream;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;

class RestoreAsyncTask extends AsyncTask<Void, Integer, Boolean> {
    private RestoreFragment restoreFragment;
    private final File mFile;
    private LocalDate mStartDate = null, mEndDate = null;
    private String mErrorString = null;

    RestoreAsyncTask(RestoreFragment restoreFragment, File file) throws RestoreException {
        this.restoreFragment = restoreFragment;
        mFile = file;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            if (!mFile.exists()) {
                Log.e(RestoreFragment.TAG, "File not exists.");
                throw new RestoreException(restoreFragment.getContext(), R.string.import_file_not_exists);
            }

            if (!mFile.canRead()) {
                Log.e(RestoreFragment.TAG, "File not readable.");
                throw new RestoreException(restoreFragment.getContext(), R.string.import_file_not_readable);
            }

            if (mFile.length() == 0) {
                Log.e(RestoreFragment.TAG, "File empty.");
                throw new RestoreException(restoreFragment.getContext(), R.string.import_file_empty);
            }

            if (!mFile.getName().toLowerCase().endsWith(".csv")) {
                Log.e(RestoreFragment.TAG, "File is not a CSV.");
                throw new RestoreException(restoreFragment.getContext(), R.string.import_file_not_csv);
            }
        } else {
            Log.e(RestoreFragment.TAG, "SD Card unavailable.");
            throw new RestoreException(restoreFragment.getContext(), R.string.backup_sd_card_unavailable);
        }
    }

    protected void onPreExecute() {
        // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
        restoreFragment.mProgressDialog = new ProgressDialog(restoreFragment.getContext());
        restoreFragment.mProgressDialog.setTitle(R.string.import_progress_title);
        restoreFragment.mProgressDialog.setIndeterminate(true);
        restoreFragment.mProgressDialog.setCancelable(false);
        restoreFragment.mProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        SQLiteDatabase db = null;
        CSVReader csvReader = null;
        try {
            Resources engResources = ResourcesHelper.getEngResources(restoreFragment.getContext());

            String[] types = engResources.getStringArray(R.array.spinner_event_types);
            HashMap<String, Integer> typesMap = new HashMap<>(types.length);
            for (int i = 0; i < types.length; i++) {
                typesMap.put(types[i], i);
            }

            String[] foodTypes = engResources.getStringArray(R.array.spinner_event_food_types);
            HashMap<String, Integer> foodTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < foodTypes.length; i++) {
                foodTypesMap.put(foodTypes[i], i);
            }

            String[] drinkTypes = engResources.getStringArray(R.array.spinner_event_drink_types);
            HashMap<String, Integer> drinkTypesMap = new HashMap<>(types.length);
            for (int i = 0; i < drinkTypes.length; i++) {
                drinkTypesMap.put(drinkTypes[i], i);
            }

            BOMInputStream is = new BOMInputStream(new FileInputStream(mFile));
            String charset;
            if (is.hasBOM()) {
                Log.d(RestoreFragment.TAG, "File has Unicode BOM for " + is.getBOMCharsetName());
                charset = is.getBOMCharsetName();
            } else {
                Log.d(RestoreFragment.TAG, "Using UTF-8 charset to read the file.");
                charset = "UTF-8";
            }

            final InputStreamReader reader = new InputStreamReader(is, charset);
            csvReader = new CSVReader(reader);

            db = restoreFragment.mDatabaseHelper.getWritableDatabase();
            db.beginTransaction();

            int inserted = 0;
            String[] record;
            while ((record = csvReader.readNext()) != null) {
                if (csvReader.getRecordsRead() == 1) {
                    if (!Arrays.equals(engResources.getStringArray(R.array.csv_headers), record)) {
                        Log.e(RestoreFragment.TAG, "CSV headers does not match.");
                        throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted);
                    }

                    continue;
                }

                ContentValues values = parseRecord(typesMap, foodTypesMap, drinkTypesMap,
                        record, csvReader.getRecordsRead());

                Log.d(RestoreFragment.TAG, values.toString());

                long rowID = db.insert(DatabaseHelper.DBT_EVENT, DatabaseHelper.DBC_EVENT_DESC, values);
                if (rowID < 0) {
                    Log.e(RestoreFragment.TAG, "Record cannot be inserted. record: " + csvReader.getRecordsRead());
                    throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_already_existing_records);
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
            mErrorString = restoreFragment.getString(R.string.import_csv_io_exception);
        } catch (SQLiteException e) {
            Crashlytics.logException(e);
            Log.e(RestoreFragment.TAG, "Content cannot be imported. Probably a DB issue.", e);
            mErrorString = restoreFragment.getString(R.string.import_csv_sql_exception);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(RestoreFragment.TAG, "Content cannot be imported.", e);
            mErrorString = restoreFragment.getString(R.string.import_csv_exception);
        } finally {
            if (null != csvReader) {
                try {
                    csvReader.close();
                } catch (IOException ignore) {
                }
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

    private ContentValues parseRecord(final HashMap<String, Integer> typesMap,
                                      final HashMap<String, Integer> foodTypesMap,
                                      final HashMap<String, Integer> drinkTypesMap,
                                      final String[] record, final long index) throws RestoreException {
        Long id;
        LocalDate date;
        LocalTime time;
        Integer type, subType;

        try {
            id = Long.parseLong(record[0]);
        } catch (NumberFormatException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV id cannot be parsed. record: {0} id: {1}",
                    index, record[0]));
            throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted, e);
        }

        try {
            date = LocalDate.parse(record[1], DatabaseHelper.DB_DATE_FORMATTER);

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
        } catch (IllegalArgumentException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV date cannot be parsed. record: {0} date: {1}",
                    index, record[1]));
            throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted, e);
        }

        try {
            time = LocalTime.parse(record[2], DatabaseHelper.DB_TIME_FORMATTER);
        } catch (IllegalArgumentException e) {
            Log.d(RestoreFragment.TAG, MessageFormat.format("CSV time cannot be parsed. record: {0} time: {1}",
                    index, record[2]));
            throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted);
        }

        type = typesMap.get(record[3]);
        if (null == type) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV type cannot be identified. record: {0} type: {1}",
                    index, record[3]));
            throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted);
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
                Log.d(RestoreFragment.TAG, MessageFormat.format("CSV subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                        index, record[3], record[4]));
                throw new RestoreException(restoreFragment.getContext(), R.string.import_csv_corrupted);
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
            Snackbar.make(restoreFragment.mLinearLayout, restoreFragment.getString(R.string.import_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

            restoreFragment.mListener.onImportComplete(Uri.fromFile(mFile), mStartDate, mEndDate);
        }
    }
}
