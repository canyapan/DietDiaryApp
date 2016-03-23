package com.canyapan.dietdiaryapp.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;

import org.joda.time.LocalTime;

public class TimePreference extends android.preference.DialogPreference {
    private static final String DEFAULT_VALUE = "00:00";
    private int mHour, mMinute;
    private TimePicker picker = null;

    public TimePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setPositiveButtonText(context.getString(android.R.string.ok));
        setNegativeButtonText(context.getString(android.R.string.cancel));
        // To hide title bar.
        setDialogTitle("");
    }

    public TimePreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public TimePreference(Context context) {
        this(context, null);
    }

    public void setTime(int hour, int minute) {
        boolean wasBlocking = shouldDisableDependents();
        mHour = hour;
        mMinute = minute;
        // this must be standard format. User may change the device language.
        persistString(getLocalTime().toString(DatabaseHelper.DB_TIME_FORMATTER));
        boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    public LocalTime getLocalTime() {
        return new LocalTime(mHour, mMinute);
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String value;
        if (restoreValue) {
            if (defaultValue == null) {
                value = getPersistedString(DEFAULT_VALUE);
            } else {
                value = getPersistedString((String) defaultValue);
            }
        } else {
            value = (String) defaultValue;
        }

        LocalTime time = LocalTime.parse(value, DatabaseHelper.DB_TIME_FORMATTER);
        setTime(time.getHourOfDay(), time.getMinuteOfHour());
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return picker;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        picker.setIs24HourView(DateTimeHelper.is24HourMode(getContext()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            picker.setHour(mHour);
            picker.setMinute(mMinute);
        } else {
            //noinspection deprecation
            picker.setCurrentHour(mHour);
            //noinspection deprecation
            picker.setCurrentMinute(mMinute);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // TimePicker methods renamed at API 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mHour = picker.getHour();
                mMinute = picker.getMinute();
            } else {
                //noinspection deprecation
                mHour = picker.getCurrentHour();
                //noinspection deprecation
                mMinute = picker.getCurrentMinute();
            }

            String value = getLocalTime().toString(DatabaseHelper.DB_TIME_FORMATTER);
            if (callChangeListener(value)) {
                persistString(value);
            }
        }
    }

    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        } else {
            TimePreference.SavedState myState = new TimePreference.SavedState(superState);
            myState.mHour = mHour;
            myState.mMinute = mMinute;
            return myState;
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null && state.getClass().equals(TimePreference.SavedState.class)) {
            TimePreference.SavedState myState = (TimePreference.SavedState) state;
            super.onRestoreInstanceState(myState.getSuperState());
            setTime(myState.mHour, myState.mMinute);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<TimePreference.SavedState> CREATOR = new Creator<TimePreference.SavedState>() {
            public TimePreference.SavedState createFromParcel(Parcel in) {
                return new TimePreference.SavedState(in);
            }

            public TimePreference.SavedState[] newArray(int size) {
                return new TimePreference.SavedState[size];
            }
        };

        int mHour, mMinute;

        public SavedState(Parcel source) {
            super(source);
            mHour = source.readInt();
            mMinute = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
        }
    }
}
