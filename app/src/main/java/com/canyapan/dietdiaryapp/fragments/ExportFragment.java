package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
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
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.canyapan.dietdiaryapp.Application;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.SharingSupportProvider;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.opencsv.CSVWriter;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class ExportFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "ExportFragment";
    private static final String KEY_FROM_DATE_SERIALIZABLE = "FROM DATE";
    private static final String KEY_TO_DATE_SERIALIZABLE = "TO DATE";
    private static final String KEY_SELECTED_FORMAT_INT = "FORMAT";
    private static final int REQUEST_EXTERNAL_STORAGE = 30;

    private OnFragmentInteractionListener mListener;

    private GridLayout mGridLayout;
    private TextView tvFromDatePicker, tvToDatePicker;
    private Spinner spFormats;

    private LocalDate mFromDate, mToDate;

    private DatabaseHelper mDatabaseHelper;
    private ProgressDialog mProgressDialog;
    private ExportAsyncTask mAsyncTask = null;

    public static ExportFragment newInstance() {
        return new ExportFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mDatabaseHelper = new DatabaseHelper(getContext());

        if (null != savedInstanceState) {
            mFromDate = (LocalDate) savedInstanceState.getSerializable(KEY_FROM_DATE_SERIALIZABLE);
            mToDate = (LocalDate) savedInstanceState.getSerializable(KEY_TO_DATE_SERIALIZABLE);
        } else {
            mFromDate = getFirstDate();
            mToDate = LocalDate.now();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mGridLayout = (GridLayout) inflater.inflate(R.layout.fragment_export_gridlayout, container, false);

        tvFromDatePicker = (TextView) mGridLayout.findViewById(R.id.tvFromDatePicker);
        tvToDatePicker = (TextView) mGridLayout.findViewById(R.id.tvToDatePicker);
        spFormats = (Spinner) mGridLayout.findViewById(R.id.spFormats);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ((TextView) mGridLayout.findViewById(R.id.tvFrom))
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.calendar_today, 0, 0, 0);
            ((TextView) mGridLayout.findViewById(R.id.tvTo))
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.calendar, 0, 0, 0);
            ((TextView) mGridLayout.findViewById(R.id.tvType))
                    .setCompoundDrawablesWithIntrinsicBounds(R.drawable.file, 0, 0, 0);
        }

        tvFromDatePicker.setOnClickListener(this);
        tvToDatePicker.setOnClickListener(this);

        tvFromDatePicker.setText(DateTimeHelper.convertLocalDateToString(mFromDate));
        tvToDatePicker.setText(DateTimeHelper.convertLocalDateToString(mToDate));

        if (null != savedInstanceState) {
            spFormats.setSelection(savedInstanceState.getInt(KEY_SELECTED_FORMAT_INT, 0));
        }

        if (null != mAsyncTask) {
            // To start progress dialog again.
            mAsyncTask.onPreExecute();
        }

        return mGridLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(KEY_FROM_DATE_SERIALIZABLE, mFromDate);
        outState.putSerializable(KEY_TO_DATE_SERIALIZABLE, mToDate);
        outState.putInt(KEY_SELECTED_FORMAT_INT, spFormats.getSelectedItemPosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_export_fragment, menu);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            menu.findItem(R.id.action_save).setEnabled(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        getActivity().requestPermissions(
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                }, REQUEST_EXTERNAL_STORAGE);

                        return true;
                    }
                }

                try {
                    mAsyncTask = (ExportAsyncTask) new ExportAsyncTask(ExportAsyncTask.TO_EXTERNAL).execute();
                } catch (ExportException e) {
                    Log.e(TAG, "Save to external storage unsuccessful.", e);
                }

                return true;
            case R.id.action_share:
                try {
                    mAsyncTask = (ExportAsyncTask) new ExportAsyncTask(ExportAsyncTask.TO_SHARE).execute();
                } catch (ExportException e) {
                    Log.e(TAG, "Share unsuccessful.", e);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
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
    public void onClick(final View v) {
        LocalDate date;
        if (v.getId() == R.id.tvFromDatePicker) {
            date = mFromDate;
        } else if (v.getId() == R.id.tvToDatePicker) {
            date = mToDate;
        } else {
            return;
        }

        DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        LocalDate newDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                        Log.d(TAG, MessageFormat.format("date selected {0}", newDate));

                        if (v.getId() == R.id.tvFromDatePicker) {
                            mFromDate = newDate;
                            tvFromDatePicker.setText(DateTimeHelper.convertLocalDateToString(newDate));

                            if (mFromDate.isAfter(mToDate)) {
                                mToDate = mFromDate;
                                tvToDatePicker.setText(DateTimeHelper.convertLocalDateToString(newDate));
                            }
                        } else {
                            mToDate = newDate;
                            tvToDatePicker.setText(DateTimeHelper.convertLocalDateToString(newDate));

                            if (mToDate.isBefore(mFromDate)) {
                                mFromDate = mToDate;
                                tvFromDatePicker.setText(DateTimeHelper.convertLocalDateToString(newDate));
                            }
                        }
                    }
                }, date.getYear(), date.getMonthOfYear() - 1, date.getDayOfMonth()
        );

        datePicker.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(android.R.string.ok), datePicker);
        datePicker.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), datePicker);

        datePicker.show();
    }

    @NonNull
    private LocalDate getFirstDate() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = mDatabaseHelper.getReadableDatabase();
            cursor = db.query(DatabaseHelper.DBT_EVENT,
                    new String[]{DatabaseHelper.DBC_EVENT_DATE,},
                    null, null, null, null, DatabaseHelper.DBC_EVENT_DATE, "1");

            if (cursor.moveToFirst()) {
                return LocalDate.parse(cursor.getString(0), DatabaseHelper.DB_DATE_FORMATTER);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Content cannot be prepared probably a DB issue.", e);
        } catch (Exception e) {
            Log.e(TAG, "Content cannot be prepared.", e);
        } finally {
            if (null != cursor) {
                cursor.close();
            }

            if (null != db && db.isOpen()) {
                db.close();
            }
        }

        return LocalDate.now();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                mAsyncTask = (ExportAsyncTask) new ExportAsyncTask(ExportAsyncTask.TO_EXTERNAL).execute();
            } catch (ExportException e) {
                Log.e(TAG, "Save to external storage unsuccessful.", e);
            }
        } else {
            Toast.makeText(getContext(), R.string.export_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    public interface OnFragmentInteractionListener {
        void onExported(Uri uri, LocalDate startDate, LocalDate endDate);

        void onShared(Uri uri, LocalDate startDate, LocalDate endDate);
    }

    private class ExportAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private static final int TO_EXTERNAL = 0;
        private static final int TO_SHARE = 1;

        private final File mFile;
        private final int mDestination;
        private final AtomicInteger mProgress = new AtomicInteger(0);
        private String mErrorString = null;

        ExportAsyncTask(int destination) throws ExportException {
            mDestination = destination;

            String fileName = MessageFormat.format("{0} {1} {2}.csv",
                    getString(R.string.app_name),
                    mFromDate.toString("yyyy-MM-dd"),
                    mToDate.toString("yyyy-MM-dd"));

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
                        Log.e(TAG, "SD Card unavailable.");
                        throw new ExportException(R.string.export_sd_card_unavailable);
                    }
                    break;
                case TO_SHARE:
                    /*for (File f :
                            getContext().getCacheDir().listFiles(new FilenameFilter() {
                                @Override
                                public boolean accept(File dir, String filename) {
                                    return filename.startsWith("temp") && filename.endsWith(".csv");
                                }
                            })) {
                        Log.d(TAG, "Deleting temp file " + f.getName());
                        f.delete();
                    }*/

                    mFile = new File(getContext().getCacheDir(), fileName);
                    break;
                default:
                    throw new ExportException(R.string.export_unimplemented_destination);
            }
        }

        protected void onPreExecute() {
            // Heads up: This method is also used onCreateView to recreate progress dialog on configuration change.
            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setTitle(R.string.export_progress_title);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMax(100);
            mProgressDialog.show();
            mProgressDialog.setProgress(mProgress.get());
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            CSVWriter csvWriter = null;
            try {
                Resources engResources = ResourcesHelper.getEngResources(getContext());

                final OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(mFile, false), "UTF-8");
                os.write('\uFEFF'); // Unicode character, U+FEFF BYTE ORDER MARK (BOM) | https://en.wikipedia.org/wiki/Byte_order_mark
                publishProgress(1);

                csvWriter = new CSVWriter(os);

                csvWriter.writeNext(engResources.getStringArray(R.array.csv_headers));

                db = mDatabaseHelper.getReadableDatabase();
                cursor = db.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                        DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DATE + " <= ?",
                        new String[]{mFromDate.toString(DatabaseHelper.DB_DATE_FORMATTER), mToDate.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                        null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

                publishProgress(5);

                Event model;
                if (cursor.moveToFirst()) {
                    int count = cursor.getCount();
                    Log.d(TAG, MessageFormat.format("There are {0,number,integer} records exporting.", count));

                    String[] types = engResources.getStringArray(R.array.spinner_event_types);
                    String[] foodTypes = engResources.getStringArray(R.array.spinner_event_food_types);
                    String[] drinkTypes = engResources.getStringArray(R.array.spinner_event_drink_types);

                    String subType;
                    int current = 0;
                    int percent = 0, percent_;
                    do {
                        model = EventHelper.parse(cursor);

                        percent_ = (int) Math.floor(++current * 95 / count);
                        if (percent < percent_) {
                            percent = percent_;
                            publishProgress(percent + 5);
                        }

                        switch (model.getType()) {
                            case Event.TYPE_FOOD:
                                subType = foodTypes[model.getSubType()];
                                break;
                            case Event.TYPE_DRINK:
                                subType = drinkTypes[model.getSubType()];
                                break;
                            default:
                                subType = "";
                        }

                        csvWriter.writeNext(new String[]{
                                Long.toString(model.getID()),
                                model.getDate().toString(DatabaseHelper.DB_DATE_FORMATTER),
                                model.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER),
                                types[model.getType()],
                                subType,
                                model.getDescription()
                        });

                    } while (cursor.moveToNext());
                }

                publishProgress(100);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Content cannot be prepared probably a IO issue.", e);
                mErrorString = getString(R.string.export_csv_io_exception);
            } catch (SQLiteException e) {
                Log.e(TAG, "Content cannot be prepared probably a DB issue.", e);
                mErrorString = getString(R.string.export_csv_sql_exception);
            } catch (Exception e) {
                Log.e(TAG, "Content cannot be prepared.", e);
                mErrorString = getString(R.string.export_csv_exception);
            } finally {
                if (null != csvWriter) {
                    try {
                        csvWriter.close();
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

            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (values.length > 0 && values[0] != null) {
                mProgress.set(values[0]);
                mProgressDialog.setProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mAsyncTask = null;

            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (null == result || result.equals(false)) {
                Snackbar.make(mGridLayout, mErrorString, Snackbar.LENGTH_INDEFINITE).show();
            } else if (result.equals(true)) {
                Snackbar.make(mGridLayout, getString(R.string.export_external_successful, mFile.getName()), Snackbar.LENGTH_SHORT).show();

                if (mDestination == TO_SHARE) {
                    ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(getActivity())
                            .setType(SharingSupportProvider.MIME_TYPE_CSV)
                            .setStream(Uri.parse(SharingSupportProvider.CONTENT_URI_PREFIX + mFile.getName()));

                    builder.getIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    builder.startChooser();

                    mListener.onShared(Uri.fromFile(mFile), mFromDate, mToDate);
                } else {
                    mListener.onExported(Uri.fromFile(mFile), mFromDate, mToDate);
                }
            }
        }
    }

    class ExportException extends Exception {
        public ExportException(String message) {
            super(message);
        }

        public ExportException(@StringRes int message) {
            this(getString(message));
        }
    }
}
