package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.canyapan.dietdiaryapp.Application;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.adapters.SpinnerArrayAdapter;
import com.canyapan.dietdiaryapp.adapters.SpinnerItem;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.opencsv.CSVReader;

import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ImportFragment extends Fragment {
    public static final String TAG = "ImportFragment";
    private static final String KEY_SELECTED_FILE_INDEX_INT = "SELECTED FILE";
    private static final String KEY_FILES_PARCELABLE = "FILES";
    private static final int REQUEST_EXTERNAL_STORAGE = 20;

    private OnFragmentInteractionListener mListener;

    private LinearLayout mLinearLayout;
    private Spinner mSpinner;

    private ArrayList<SpinnerItem> mSpinnerItems = null;

    private DatabaseHelper mDatabaseHelper;
    private ProgressDialog mProgressDialog;
    private ImportAsyncTask mAsyncTask = null;

    public ImportFragment() {
    }

    public static ImportFragment newInstance() {
        return new ImportFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mDatabaseHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_import_linearlayout, container, false);

        mSpinner = (Spinner) mLinearLayout.findViewById(R.id.spFiles);

        if (savedInstanceState != null) {
            mSpinnerItems = savedInstanceState.getParcelableArrayList(KEY_FILES_PARCELABLE);
            int selectedIndex = savedInstanceState.getInt(KEY_SELECTED_FILE_INDEX_INT);

            mSpinner.setAdapter(new SpinnerArrayAdapter(getContext(), mSpinnerItems));
            mSpinner.setSelection(selectedIndex);
        }

        if (null == mSpinnerItems) {
            loadSpinnerItems();
        }

        if (null != mAsyncTask) {
            // To start progress dialog again.
            mAsyncTask.onPreExecute();
        }

        return mLinearLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(KEY_FILES_PARCELABLE, mSpinnerItems);
        outState.putInt(KEY_SELECTED_FILE_INDEX_INT, mSpinner.getSelectedItemPosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_import_fragment, menu);

        Log.d(TAG, "External storage state: " + Environment.getExternalStorageState());
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            menu.findItem(R.id.action_save).setEnabled(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (null == mSpinnerItems || mSpinnerItems.size() == 1) {
                    // Fail safety.
                    return true;
                }

                if (mSpinner.getSelectedItemPosition() == 0) {
                    // Skip the select helper.
                    return true;
                }

                try {
                    mAsyncTask = (ImportAsyncTask) new ImportAsyncTask((File) mSpinnerItems.get(mSpinner.getSelectedItemPosition()).getTag()).execute();
                } catch (ImportException e) {
                    Log.e(TAG, "Import from external storage unsuccessful.", e);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSpinnerItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                getActivity().requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, REQUEST_EXTERNAL_STORAGE);

                return;
            }
        }

        Log.d(TAG, "Loading items...");
        File[] files = getSupportedFiles();

        mSpinnerItems = new ArrayList<>(files.length);
        mSpinnerItems.add(new SpinnerItem(getString(R.string.import_spinner_hint), R.drawable.tab_import, true));
        for (File f : files) {
            mSpinnerItems.add(new SpinnerItem(f.getName(), f.getName().toLowerCase().endsWith(".csv") ?
                    R.drawable.file_delimited : R.drawable.file, false, f));
        }

        mSpinner.setAdapter(new SpinnerArrayAdapter(getContext(), mSpinnerItems));
    }

    private File[] getSupportedFiles() {
        final File dirApp = new File(
                Environment.getExternalStorageDirectory(),
                Application.APP_DIR);

        return ArrayUtils.addAll(getSupportedFiles(dirApp), getSupportedFiles(Environment.getExternalStorageDirectory()));
    }

    private File[] getSupportedFiles(File dir) {
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.toLowerCase().endsWith(".csv");
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSpinnerItems();
        } else {
            Toast.makeText(getContext(), R.string.import_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    public interface OnFragmentInteractionListener {
        void onImportComplete(Uri uri, LocalDate startDate, LocalDate endDate);
    }

    private class ImportAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private final File mFile;
        LocalDate mStartDate = null, mEndDate = null;
        private String mErrorString = null;

        ImportAsyncTask(File file) throws ImportException {
            mFile = file;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                    || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                if (!mFile.exists()) {
                    Log.e(TAG, "File not exists.");
                    throw new ImportException(R.string.import_file_not_exists);
                }

                if (!mFile.canRead()) {
                    Log.e(TAG, "File not readable.");
                    throw new ImportException(R.string.import_file_not_readable);
                }

                if (mFile.length() == 0) {
                    Log.e(TAG, "File empty.");
                    throw new ImportException(R.string.import_file_empty);
                }

                if (!mFile.getName().toLowerCase().endsWith(".csv")) {
                    Log.e(TAG, "File is not a CSV.");
                    throw new ImportException(R.string.import_file_not_csv);
                }
            } else {
                Log.e(TAG, "SD Card unavailable.");
                throw new ImportException(R.string.export_sd_card_unavailable);
            }
        }

        protected void onPreExecute() {
            // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setTitle(R.string.import_progress_title);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            SQLiteDatabase db = null;
            CSVReader csvReader = null;
            try {
                Resources engResources = ResourcesHelper.getEngResources(getContext());

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
                    Log.d(TAG, "File has Unicode BOM for " + is.getBOMCharsetName());
                    charset = is.getBOMCharsetName();
                } else {
                    Log.d(TAG, "Using UTF-8 charset to read the file.");
                    charset = "UTF-8";
                }

                final InputStreamReader reader = new InputStreamReader(is, charset);
                csvReader = new CSVReader(reader);

                db = mDatabaseHelper.getWritableDatabase();
                db.beginTransaction();

                int inserted = 0;
                String[] record;
                while ((record = csvReader.readNext()) != null) {
                    if (csvReader.getRecordsRead() == 1) {
                        if (!Arrays.equals(engResources.getStringArray(R.array.csv_headers), record)) {
                            Log.e(TAG, "CSV headers does not match.");
                            throw new ImportException(R.string.import_csv_corrupted);
                        }

                        continue;
                    }

                    ContentValues values = parseRecord(typesMap, foodTypesMap, drinkTypesMap,
                            record, csvReader.getRecordsRead());

                    Log.d(TAG, values.toString());

                    long rowID = db.insert(DatabaseHelper.DBT_EVENT, DatabaseHelper.DBC_EVENT_DESC, values);
                    if (rowID < 0) {
                        Log.e(TAG, "Record cannot be inserted. record: " + csvReader.getRecordsRead());
                        throw new ImportException(R.string.import_csv_already_existing_records);
                    }

                    inserted++;
                }

                Log.d(TAG, "Records inserted " + inserted);
                db.setTransactionSuccessful();
                return true;
            } catch (ImportException e) {
                mErrorString = e.getMessage();
            } catch (IOException e) {
                Log.e(TAG, "Content cannot be imported. Probably a IO issue.", e);
                mErrorString = getString(R.string.import_csv_io_exception);
            } catch (SQLiteException e) {
                Log.e(TAG, "Content cannot be imported. Probably a DB issue.", e);
                mErrorString = getString(R.string.import_csv_sql_exception);
            } catch (Exception e) {
                Log.e(TAG, "Content cannot be imported.", e);
                mErrorString = getString(R.string.import_csv_exception);
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
                                          final String[] record, final long index) throws ImportException {
            Long id;
            LocalDate date;
            LocalTime time;
            Integer type, subType;

            try {
                id = Long.parseLong(record[0]);
            } catch (NumberFormatException e) {
                Log.e(TAG, MessageFormat.format("CSV id cannot be parsed. record: {0} id: {1}",
                        index, record[0]));
                throw new ImportException(R.string.import_csv_corrupted, e);
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
                Log.e(TAG, MessageFormat.format("CSV date cannot be parsed. record: {0} date: {1}",
                        index, record[1]));
                throw new ImportException(R.string.import_csv_corrupted, e);
            }

            try {
                time = LocalTime.parse(record[2], DatabaseHelper.DB_TIME_FORMATTER);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, MessageFormat.format("CSV time cannot be parsed. record: {0} time: {1}",
                        index, record[2]));
                throw new ImportException(R.string.import_csv_corrupted);
            }

            type = typesMap.get(record[3]);
            if (null == type) {
                Log.e(TAG, MessageFormat.format("CSV type cannot be identified. record: {0} type: {1}",
                        index, record[3]));
                throw new ImportException(R.string.import_csv_corrupted);
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
                    Log.d(TAG, MessageFormat.format("CSV subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                            index, record[3], record[4]));
                    throw new ImportException(R.string.import_csv_corrupted);
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
            mAsyncTask = null;

            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (null == result || result.equals(false)) {
                Snackbar.make(mLinearLayout, mErrorString, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.snack_bar_dismiss, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Just dismiss
                            }
                        }).show();
            } else if (result.equals(true)) {
                Snackbar.make(mLinearLayout, getString(R.string.import_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

                mListener.onImportComplete(Uri.fromFile(mFile), mStartDate, mEndDate);
            }
        }
    }

    class ImportException extends Exception {
        ImportException(String message) {
            super(message);
        }

        ImportException(String message, Throwable cause) {
            super(message, cause);
        }

        ImportException(@StringRes int message) {
            this(getString(message));
        }

        ImportException(@StringRes int message, Throwable cause) {
            this(getString(message), cause);
        }
    }

}
