package com.canyapan.dietdiaryapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.canyapan.dietdiaryapp.adapters.SpinnerArrayAdapter;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.crashlytics.android.Crashlytics;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.text.MessageFormat;

public class CreateEditEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEditEventActivity";

    public static final String KEY_EVENT_PARCELABLE = "EVENT";
    public static final String KEY_POSITION_INT = "POSITION";
    public static final String KEY_ORG_DATE_SERIALIZABLE = "ORG_DATE";
    public static final String KEY_EXCEPTION_SERIALIZABLE = "EXCEPTION";
    public static final int RESULT_ERROR = -2;
    public static final int RESULT_FAILED = -1;
    public static final int RESULT_CANCELLED = 0;
    public static final int RESULT_INSERTED = 1;
    public static final int RESULT_UPDATED = 2;
    public static final int RESULT_DELETED = 3;
    public static final int REQUEST_CREATE_EDIT = 1;

    private int mPosition;
    private Event mEvent;
    private LocalDate mOrgDate;

    private ActionBar mActionBar;
    private TextView tvDatePicker, tvTimePicker;
    private Spinner spTypes;
    private AutoCompleteTextView actvDescription;

    private MenuItem miDelete, miCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_edit_event);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (null != toolbar) {
            setSupportActionBar(toolbar);
        }
        mActionBar = getSupportActionBar();
        if (null != mActionBar) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            mEvent = savedInstanceState.getParcelable(KEY_EVENT_PARCELABLE);
            mPosition = savedInstanceState.getInt(KEY_POSITION_INT, -1);
            mOrgDate = (LocalDate) savedInstanceState.getSerializable(KEY_ORG_DATE_SERIALIZABLE);
        } else {
            mEvent = getIntent().getParcelableExtra(KEY_EVENT_PARCELABLE);
            mPosition = getIntent().getIntExtra(KEY_POSITION_INT, -1);
            mOrgDate = null;
        }

        spTypes = (Spinner) findViewById(R.id.spTypes);
        tvDatePicker = (TextView) findViewById(R.id.tvDatePicker);
        tvTimePicker = (TextView) findViewById(R.id.tvTimePicker);
        actvDescription = (AutoCompleteTextView) findViewById(R.id.etDescription);

        switch (mEvent.getType()) {
            case Event.TYPE_FOOD:
                setSpinnerContents(spTypes, R.array.spinner_event_food_types, mEvent.getSubType());
                break;
            case Event.TYPE_DRINK:
                setSpinnerContents(spTypes, R.array.spinner_event_drink_types, mEvent.getSubType());
                break;
            default:
                setSpinnerContents(spTypes, R.array.spinner_event_types, mEvent.getType() - 2, 2, R.array.spinner_event_types_res);

                ImageView icon = (ImageView) findViewById(R.id.ivTypes);
                if (null != icon) {
                    icon.setVisibility(ImageView.GONE);
                }
                break;
        }

        if (mEvent.getID() == -1) { // CREATE
            if (null == mEvent.getTime()) {
                mEvent.setTime(new LocalTime());
            }
        } else { // EDIT
            if (null != mActionBar) {
                mActionBar.setTitle(getString(R.string.activity_edit_event_title));
            }
            actvDescription.setText(mEvent.getDescription());
            if (null == mOrgDate) {
                mOrgDate = mEvent.getDate();
            }
        }

        tvDatePicker.setText(DateTimeHelper.convertLocalDateToString(mEvent.getDate()));
        tvTimePicker.setText(DateTimeHelper.convertLocalTimeToString(this, mEvent.getTime()));

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_dropdown_item_1line, null,
                new String[]{"Description"},
                new int[]{android.R.id.text1},
                0);

        adapter.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
            @Override
            public CharSequence convertToString(Cursor cursor) {
                final int colIndex = cursor.getColumnIndexOrThrow("Description");
                return cursor.getString(colIndex);
            }
        });
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence description) {
                if (null != description) {
                    return EventHelper.getCursorOfDescriptionsByPartial(CreateEditEventActivity.this, description.toString());
                }

                return null;
            }
        });

        actvDescription.setAdapter(adapter);
    }

    private void setSpinnerContents(Spinner spinner, @ArrayRes int spinnerContents, int selectedIndex) {
        setSpinnerContents(spinner, spinnerContents, selectedIndex, 0, 0);
    }

    private void setSpinnerContents(Spinner spinner, @ArrayRes int spinnerContents, int selectedIndex,
                                    final int offset, @ArrayRes int spinnerIcons) {
        final SpinnerArrayAdapter arrayAdapter = new SpinnerArrayAdapter(CreateEditEventActivity.this,
                spinnerContents, spinnerIcons, false, offset);
        spinner.setAdapter(arrayAdapter);
        spinner.setSelection(selectedIndex);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(KEY_EVENT_PARCELABLE, mEvent);
        intent.putExtra(KEY_POSITION_INT, mPosition);
        intent.putExtra(KEY_ORG_DATE_SERIALIZABLE, mOrgDate);

        setResult(RESULT_CANCELLED, intent);

        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_EVENT_PARCELABLE, mEvent);
        outState.putInt(KEY_POSITION_INT, mPosition);
        outState.putSerializable(KEY_ORG_DATE_SERIALIZABLE, mOrgDate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_edit_event_activity, menu);

        if (mPosition >= 0) { // EDIT
            miDelete = menu.findItem(R.id.action_delete);
            miDelete.setVisible(true);

            miCopy = menu.findItem(R.id.action_copy);
            miCopy.setVisible(true);
        } else {
            miDelete = miCopy = null;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        intent.putExtra(KEY_EVENT_PARCELABLE, mEvent);
        intent.putExtra(KEY_POSITION_INT, mPosition);
        intent.putExtra(KEY_ORG_DATE_SERIALIZABLE, mOrgDate);

        SQLiteDatabase db = null;
        try {
            final DatabaseHelper dbHelper = new DatabaseHelper(CreateEditEventActivity.this);
            db = dbHelper.getWritableDatabase();

            switch (item.getItemId()) {
                case android.R.id.home:
                    setResult(RESULT_CANCELLED, intent);
                    CreateEditEventActivity.this.finish();
                    return true;
                case R.id.action_save:
                    switch (mEvent.getType()) {
                        case Event.TYPE_FOOD:
                            mEvent.setSubType(spTypes.getSelectedItemPosition());
                            break;
                        case Event.TYPE_DRINK:
                            mEvent.setSubType(spTypes.getSelectedItemPosition());
                            break;
                        default:
                            mEvent.setType(spTypes.getSelectedItemPosition() +
                                    ((SpinnerArrayAdapter) spTypes.getAdapter()).getOffset());
                            break;
                    }
                    mEvent.setDescription(actvDescription.getText().toString());

                    BackupManager backupManager = new BackupManager(this);

                    if (mPosition >= 0) { // UPDATE
                        if (EventHelper.update(db, mEvent)) {
                            setResult(RESULT_UPDATED, intent);

                            backupManager.dataChanged();
                        } else {
                            Log.e(TAG, "Update operation failed.");
                            setResult(RESULT_FAILED, intent);
                        }
                    } else { // INSERT
                        if (EventHelper.insert(db, mEvent)) {
                            Log.d(TAG, MessageFormat.format("A new record inserted to the database with id {0,number,integer}.", mEvent.getID()));
                            setResult(RESULT_INSERTED, intent);

                            backupManager.dataChanged();
                        } else {
                            Log.e(TAG, "Insert operation failed.");
                            setResult(RESULT_FAILED, intent);
                        }
                    }

                    CreateEditEventActivity.this.finish();
                    return true;
                case R.id.action_delete: // DELETE
                    if (EventHelper.delete(db, mEvent)) {
                        if (!mEvent.getDate().isEqual(mOrgDate)) {
                            mEvent.setDate(mOrgDate);
                            intent.putExtra(KEY_EVENT_PARCELABLE, mEvent);
                        }
                        setResult(RESULT_DELETED, intent);
                    } else {
                        Log.e(TAG, "Delete operation returned false.");
                        setResult(RESULT_FAILED, intent);
                    }

                    CreateEditEventActivity.this.finish();
                    return true;
                case R.id.action_copy:
                    mOrgDate = null;
                    mPosition = -1;
                    mEvent.setID(-1);
                    mEvent.setDate(new LocalDate());

                    tvDatePicker.setText(DateTimeHelper.convertLocalDateToString(mEvent.getDate()));
                    miCopy.setVisible(false);
                    miDelete.setVisible(false);

                    if (null != mActionBar) {
                        mActionBar.setTitle(getString(R.string.activity_create_event_title));
                    }

                    return true;
            }
        } catch (SQLiteException e) {
            Crashlytics.logException(e);
            Log.e(TAG, "Content cannot be prepared probably a DB issue.", e);

            intent.putExtra(KEY_EXCEPTION_SERIALIZABLE, e);
            setResult(RESULT_ERROR, intent);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(TAG, "Content cannot be prepared.", e);

            intent.putExtra(KEY_EXCEPTION_SERIALIZABLE, e);
            setResult(RESULT_ERROR, intent);
        } finally {
            if (null != db && db.isOpen()) {
                db.close();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void onDatePickerButtonClicked(View view) {
        DatePickerDialog datePicker = new DatePickerDialog(CreateEditEventActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        mEvent.setDate(new LocalDate(year, monthOfYear + 1, dayOfMonth));
                        Log.d(TAG, MessageFormat.format("date selected {0}", mEvent.getDate()));

                        tvDatePicker.setText(DateTimeHelper.convertLocalDateToString(mEvent.getDate()));
                    }
                }, mEvent.getDate().getYear(), mEvent.getDate().getMonthOfYear() - 1, mEvent.getDate().getDayOfMonth()
        );

        datePicker.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(android.R.string.ok), datePicker);
        datePicker.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), datePicker);

        datePicker.show();
    }

    public void onTimePickerButtonClicked(View view) {
        TimePickerDialog timePicker = new TimePickerDialog(CreateEditEventActivity.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mEvent.setTime(new LocalTime(hourOfDay, minute));
                        Log.d(TAG, MessageFormat.format("time selected {0}", mEvent.getTime()));

                        tvTimePicker.setText(DateTimeHelper.convertLocalTimeToString(CreateEditEventActivity.this, mEvent.getTime()));
                    }
                }, mEvent.getTime().getHourOfDay(), mEvent.getTime().getMinuteOfHour(), DateTimeHelper.is24HourMode(this)
        );

        timePicker.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(android.R.string.ok), timePicker);
        timePicker.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), timePicker);

        timePicker.show();
    }
}
