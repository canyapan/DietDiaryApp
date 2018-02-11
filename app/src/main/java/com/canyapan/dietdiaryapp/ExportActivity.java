package com.canyapan.dietdiaryapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.helpers.FixedDatePickerDialog;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import static com.canyapan.dietdiaryapp.ExportAsyncTask.TO_EMAIL;

/**
 * Exports application data as email or html file to better visualize by user.
 * Also allows user to share it.
 */
public class ExportActivity extends AppCompatActivity implements View.OnClickListener, ExportAsyncTask.OnExportListener {
    private static final String TAG = "ExportActivity";

    private static final String KEY_FROM_DATE_SERIALIZABLE = "FROM DATE";
    private static final String KEY_TO_DATE_SERIALIZABLE = "TO DATE";
    private static final String KEY_SELECTED_FORMAT_INT = "FORMAT";

    private TextView tvFromDatePicker, tvToDatePicker;
    private Spinner spFormats;
    private ProgressBar pbToolbarProgressBar;

    private LocalDate mFromDate, mToDate;

    private DatabaseHelper mDatabaseHelper;

    private ExportAsyncTask mAsyncTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pbToolbarProgressBar = findViewById(R.id.toolbarProgressBar);
        tvFromDatePicker = findViewById(R.id.tvFromDatePicker);
        tvToDatePicker = findViewById(R.id.tvToDatePicker);
        spFormats = findViewById(R.id.spFormats);

        mDatabaseHelper = new DatabaseHelper(this);

        if (null != savedInstanceState) {
            mFromDate = (LocalDate) savedInstanceState.getSerializable(KEY_FROM_DATE_SERIALIZABLE);
            mToDate = (LocalDate) savedInstanceState.getSerializable(KEY_TO_DATE_SERIALIZABLE);
        } else {
            mFromDate = getFirstDate();
            mToDate = LocalDate.now();
        }

        tvFromDatePicker.setOnClickListener(this);
        tvToDatePicker.setOnClickListener(this);

        tvFromDatePicker.setText(DateTimeHelper.convertLocalDateToString(mFromDate));
        tvToDatePicker.setText(DateTimeHelper.convertLocalDateToString(mToDate));

        if (null != savedInstanceState) {
            spFormats.setSelection(savedInstanceState.getInt(KEY_SELECTED_FORMAT_INT, 0));
        }

        if (null != mAsyncTask) { // There is an ongoing task here.
            // To start progress dialog again.
            onExportStarting();
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(KEY_FROM_DATE_SERIALIZABLE, mFromDate);
        outState.putSerializable(KEY_TO_DATE_SERIALIZABLE, mToDate);
        outState.putInt(KEY_SELECTED_FORMAT_INT, spFormats.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (null != mAsyncTask) {
            Toast.makeText(this, R.string.export_ongoing_progress, Toast.LENGTH_LONG).show();
            return;
        }

        super.onBackPressed();
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

        DatePickerDialog datePicker = new FixedDatePickerDialog(this,
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
    public void onExportStarting() {
        pbToolbarProgressBar.setProgress(0);
        pbToolbarProgressBar.setIndeterminate(true);
        pbToolbarProgressBar.setVisibility(ProgressBar.VISIBLE);
    }

    @Override
    public void onExportProgress(int percentage) {
        if (pbToolbarProgressBar.isIndeterminate()) {
            pbToolbarProgressBar.setIndeterminate(false);
        }

        pbToolbarProgressBar.setProgress(percentage);
    }

    @Override
    public void onExportComplete(final LocalDate startDate, final LocalDate endDate, final int destination, final File file, final long recordsExported) {
        mAsyncTask = null;

        Snackbar.make(findViewById(android.R.id.content), getString(R.string.backup_external_successful, file.getName()), Snackbar.LENGTH_SHORT).show();

        if (destination == TO_EMAIL) {

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setType(SharingSupportProvider.MIME_TYPE_HTML);
            intent.putExtra(Intent.EXTRA_SUBJECT,
                    MessageFormat.format("Diet Diary, {0} – {1}",
                            startDate.toString(DatabaseHelper.DB_DATE_FORMATTER),
                            endDate.toString(DatabaseHelper.DB_DATE_FORMATTER)));

            try {
                if (file.length() < 1024 * 1024) { // Add html data directly into email body
                    intent.putExtra(Intent.EXTRA_TEXT, FileUtils.readFileToString(file, "UTF-8"));
                } else { // Add html data as attachment
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file));
                    intent.putExtra(Intent.EXTRA_TEXT, "todo work on this too"); // TODO empty email needs some HTML too
                }

                //startActivity(Intent.createChooser(intent, context.getText(R.string.sh
            } catch (IOException e) {
                Log.e(TAG, "Cannot start email intent.", e);
                // TODO show snackbar and let user know about it :(
                // TODO send a fabric exception about it
            }
        }
    }

    @Override
    public void onExportFailed(String message) {
        mAsyncTask = null;

        pbToolbarProgressBar.setVisibility(ProgressBar.GONE);

        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE).show();
    }
}
