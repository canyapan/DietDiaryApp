package com.canyapan.dietdiaryapp.preference;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.widget.TimePicker;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;

import org.joda.time.LocalTime;

public class TimePreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_HOUR = "TimePreferenceDialogFragment.hour";
    private static final String SAVE_STATE_MIN = "TimePreferenceDialogFragment.min";

    private TimePicker mTimePicker;
    private int mHour, mMinute;

    public static TimePreferenceDialogFragmentCompat newInstance(String key) {
        final TimePreferenceDialogFragmentCompat fragment =
                new TimePreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final TimePreferenceCompat preference = getTimePreference();
            mHour = preference.getHour();
            mMinute = preference.getMinute();
        } else {
            mHour = savedInstanceState.getInt(SAVE_STATE_HOUR);
            mMinute = savedInstanceState.getInt(SAVE_STATE_MIN);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_HOUR, getHour());
        outState.putInt(SAVE_STATE_MIN, getMinute());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {

        if (positiveResult) {
            final TimePreferenceCompat preference = getTimePreference();

            String value = new LocalTime(getHour(), getMinute()).toString(DatabaseHelper.DB_TIME_FORMATTER);
            if (preference.callChangeListener(value)) {
                preference.setTime(getHour(), getMinute());
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mTimePicker = view.findViewById(R.id.aTimePicker);

        // Exception: There is no TimePicker with the id 'edit' in the dialog.
        if (mTimePicker == null) {
            throw new IllegalStateException("Dialog view must contain a TimePicker with id 'aTimePicker'");
        }

        mTimePicker.setIs24HourView(DateTimeHelper.is24HourMode(getContext()));
        setHour(mHour);
        setMinute(mMinute);
    }

    private TimePreferenceCompat getTimePreference() {
        return (TimePreferenceCompat) getPreference();
    }

    private int getHour() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mTimePicker.getHour();
        } else {
            //noinspection deprecation
            return mTimePicker.getCurrentHour();
        }
    }

    private void setHour(int hour) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTimePicker.setHour(hour);
        } else {
            //noinspection deprecation
            mTimePicker.setCurrentHour(hour);
        }
    }

    private int getMinute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mTimePicker.getMinute();
        } else {
            //noinspection deprecation
            return mTimePicker.getCurrentMinute();
        }
    }

    private void setMinute(int minute) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTimePicker.setMinute(minute);
        } else {
            //noinspection deprecation
            mTimePicker.setCurrentMinute(minute);
        }
    }

}
