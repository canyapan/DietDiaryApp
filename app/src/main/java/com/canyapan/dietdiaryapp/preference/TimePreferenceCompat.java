package com.canyapan.dietdiaryapp.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;

import org.joda.time.LocalTime;

public class TimePreferenceCompat extends android.support.v7.preference.DialogPreference {
    private static final String DEFAULT_VALUE = "00:00";
    private int mHour, mMinute;
    private String mSummary;

    public TimePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setPositiveButtonText(context.getString(android.R.string.ok));
        setNegativeButtonText(context.getString(android.R.string.cancel));
        // To hide title bar.
        setDialogTitle("");
    }

    public TimePreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TimePreferenceCompat(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public TimePreferenceCompat(Context context) {
        this(context, null);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.pref_time_picker;
    }

    void setTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
        // this must be standard format. User may change the device language.
        persistString(getLocalTime().toString(DatabaseHelper.DB_TIME_FORMATTER));
        notifyChanged();
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    public LocalTime getLocalTime() {
        return new LocalTime(getHour(), getMinute());
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /**
     * Returns the summary of this ListPreference. If the summary
     * has a {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place.
     *
     * @return the summary with appropriate string substitution
     */
    @Override
    public CharSequence getSummary() {
        final CharSequence time = DateTimeHelper.convertLocalTimeToString(getContext(), getHour(), getMinute());
        if (mSummary == null) {
            return super.getSummary();
        } else {
            return String.format(mSummary, time);
        }
    }

    /**
     * Sets the summary for this Preference with a CharSequence.
     * If the summary has a
     * {@linkplain java.lang.String#format String formatting}
     * marker in it (i.e. "%s" or "%1$s"), then the current entry
     * value will be substituted in its place when it's retrieved.
     *
     * @param summary The summary for the preference.
     */
    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
        if (summary == null && mSummary != null) {
            mSummary = null;
        } else if (summary != null && !summary.equals(mSummary)) {
            mSummary = summary.toString();
        }
    }

    @Override
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

    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        } else {
            TimePreferenceCompat.SavedState myState = new TimePreferenceCompat.SavedState(superState);
            myState.mHour = mHour;
            myState.mMinute = mMinute;
            return myState;
        }
    }

    protected void onRestoreInstanceState(Parcelable state) {
        if (state != null && state.getClass().equals(TimePreferenceCompat.SavedState.class)) {
            TimePreferenceCompat.SavedState myState = (TimePreferenceCompat.SavedState) state;
            super.onRestoreInstanceState(myState.getSuperState());
            setTime(myState.mHour, myState.mMinute);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<TimePreferenceCompat.SavedState> CREATOR = new Creator<TimePreferenceCompat.SavedState>() {
            public TimePreferenceCompat.SavedState createFromParcel(Parcel in) {
                return new TimePreferenceCompat.SavedState(in);
            }

            public TimePreferenceCompat.SavedState[] newArray(int size) {
                return new TimePreferenceCompat.SavedState[size];
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
