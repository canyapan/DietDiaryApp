package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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

import com.canyapan.dietdiaryapp.BuildConfig;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.helpers.FixedDatePickerDialog;
import com.canyapan.dietdiaryapp.helpers.ResourcesHelper;
import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalDate;

import java.text.MessageFormat;

public class BackupFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "BackupFragment";
    private static final String KEY_FROM_DATE_SERIALIZABLE = "FROM DATE";
    private static final String KEY_TO_DATE_SERIALIZABLE = "TO DATE";
    private static final String KEY_SELECTED_FORMAT_INT = "FORMAT";
    private static final int REQUEST_EXTERNAL_STORAGE = 30;

    protected OnFragmentInteractionListener mListener;

    protected GridLayout mGridLayout;
    private TextView tvFromDatePicker, tvToDatePicker;
    private Spinner spFormats;

    protected LocalDate mFromDate, mToDate;

    protected DatabaseHelper mDatabaseHelper;
    protected ProgressDialog mProgressDialog;
    protected BackupAsyncTask mAsyncTask = null;

    public static BackupFragment newInstance() {
        return new BackupFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mGridLayout = (GridLayout) inflater.inflate(R.layout.fragment_backup_gridlayout, container, false);

        tvFromDatePicker = mGridLayout.findViewById(R.id.tvFromDatePicker);
        tvToDatePicker = mGridLayout.findViewById(R.id.tvToDatePicker);
        spFormats = mGridLayout.findViewById(R.id.spFormats);

        ResourcesHelper.setLeftCompoundDrawable(mGridLayout, R.id.tvFrom, R.drawable.calendar_today);
        ResourcesHelper.setLeftCompoundDrawable(mGridLayout, R.id.tvTo, R.drawable.calendar);
        ResourcesHelper.setLeftCompoundDrawable(mGridLayout, R.id.tvType, R.drawable.file);

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
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
                    mAsyncTask = (BackupAsyncTask) new BackupToJSON(this, BackupAsyncTask.TO_EXTERNAL).execute();
                } catch (BackupException e) {
                    if (BuildConfig.CRASHLYTICS_ENABLED) {
                        Crashlytics.logException(e);
                    }
                    Log.e(TAG, "Save to external storage unsuccessful.", e);
                }

                return true;
            case R.id.action_share:
                try {
                    mAsyncTask = (BackupAsyncTask) new BackupToJSON(this, BackupAsyncTask.TO_SHARE).execute();
                } catch (BackupException e) {
                    if (BuildConfig.CRASHLYTICS_ENABLED) {
                        Crashlytics.logException(e);
                    }
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

        DatePickerDialog datePicker = new FixedDatePickerDialog(getContext(),
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
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
            Log.e(TAG, "Content cannot be prepared probably a DB issue.", e);
        } catch (Exception e) {
            if (BuildConfig.CRASHLYTICS_ENABLED) {
                Crashlytics.logException(e);
            }
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
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                switch (spFormats.getSelectedItemPosition()) {
                    case 0: // JSON
                        mAsyncTask = (BackupAsyncTask) new BackupToJSON(this, BackupAsyncTask.TO_EXTERNAL).execute();
                        break;
                    default:
                        throw new BackupException(getContext(), R.string.backup_unimplemented_destination);

                }

            } catch (BackupException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                Log.e(TAG, "Save to external storage unsuccessful.", e);
            }
        } else {
            Toast.makeText(getContext(), R.string.backup_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    public boolean isWiFiConnected() {
        WifiManager wifiMgr = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr != null && wifiMgr.isWifiEnabled()) {

            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            return !(wifiInfo != null && wifiInfo.getNetworkId() == -1);

        } else {
            return false;
        }
    }

    public interface OnFragmentInteractionListener {
        void onBackupComplete(Uri uri, LocalDate startDate, LocalDate endDate);

        void onShareComplete(Uri uri, LocalDate startDate, LocalDate endDate);
    }

}
